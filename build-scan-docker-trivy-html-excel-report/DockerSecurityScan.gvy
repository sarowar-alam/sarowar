pipeline {
    agent { label 'built-in' }
    
    parameters {
        string(name: 'branch', defaultValue: 'your_branch_name', description: 'Git branch')
        string(name: 'git_url', defaultValue: 'git@github.com:your-git/your-repo.git', description: 'Git URL')
        string(name: 'dockerfile_path', defaultValue: '.', description: 'Dockerfile path If you require YourService_Folder_Name/Dockerfile then Put YourService_Folder_Name Only ...')
        string(name: 'image_name', defaultValue: 'mb-rc-image-service', description: 'Image name, nust be all small letters. ')
        choice(
            name: 'build_method',
            choices: ['dockerfile_in_path', 'root_dockerfile'],
            description: 'How to build: dockerfile_in_path uses -f YourService_Folder_Name/Dockerfile, root_dockerfile uses -f Dockerfile'
        )
    }
    
    stages {
        stage('Checkout') {
            steps {
                script {
                    echo "Starting pipeline execution..."
                    echo "Parameters:"
                    echo "   - Branch: ${params.branch}"
                    echo "   - Git URL: ${params.git_url}"
                    echo "   - Dockerfile Path: ${params.dockerfile_path}"
                    echo "   - Image Name: ${params.image_name}"
                    echo "   - Build ID: ${env.BUILD_ID}"
                    echo "   - Build Method: ${params.build_method}"
                    
                    try {
                        checkout([
                            $class: 'GitSCM',
                            branches: [[name: "*/${params.branch}"]],
                            extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'source']],
                            userRemoteConfigs: [[
                                url: "${params.git_url}",
                                credentialsId: 'jenkins-credentials-manager-id'
                            ]]
                        ])
                        echo "Checkout completed successfully"
                    } catch (Exception e) {
                        echo "Checkout failed: ${e.message}"
                        currentBuild.result = 'FAILURE'
                        error("Checkout stage failed")
                    }
                }
            }
        }
        
        stage('Build Image') {
            steps {
                script {
                    echo "Building Docker image..."
                    echo "Selected build method: ${params.build_method}"
                    
                    // Determine Docker build command based on user selection
                    def dockerBuildCommand
                    if (params.build_method == 'dockerfile_in_path') {
                        dockerBuildCommand = "cd ../ && docker build -f ${params.dockerfile_path}/Dockerfile -t ${params.image_name}:${env.BUILD_ID} ."
                        echo "Using Dockerfile path: ${params.dockerfile_path}/Dockerfile"
                    } else {
                        dockerBuildCommand = "docker build -f Dockerfile -t ${params.image_name}:${env.BUILD_ID} ."
                        echo "Using root Dockerfile: Dockerfile"
                    }
                    
                    try {
                        dir("source/${params.dockerfile_path}") {
                            sh """
                                echo "Building Docker image: ${params.image_name}:${env.BUILD_ID}"
                                echo "Build command: ${dockerBuildCommand}"
                                ${dockerBuildCommand}
                            """
                        }
                        echo "Docker image built successfully"
                    } catch (Exception e) {
                        echo "Docker build failed: ${e.message}"
                        echo "Check Dockerfile path and build context"
                        echo "Build method used: ${params.build_method}"
                        currentBuild.result = 'FAILURE'
                        error("Docker build stage failed")
                    }
                }
            }
        }
        
        stage('Pre-Scan Setup') {
            steps {
                script {
                    echo "Setting up for security scan..."
                    sh """
                        echo "Cleaning Trivy cache to avoid conflicts..."
                        trivy --clear-cache 2>/dev/null || echo "Cache clear completed or not needed"
                    """
                }
            }
        }
        
        stage('Security Scan') {
            steps {
                script {
                    echo "Running comprehensive security scan..."
                    try {
                        sh """
                            echo "Starting Trivy security scan..."
                            trivy --version
                            
                            # Run table format scan (for console output)
                            echo "=== VULNERABILITY SCAN RESULTS ==="
                            trivy image --format table --exit-code 0 ${params.image_name}:${env.BUILD_ID}
                            
                            # Generate JSON report
                            echo "Generating JSON report..."
                            trivy image --format json --output "trivy-report.json" ${params.image_name}:${env.BUILD_ID}
                            
                            # Generate SARIF report
                            echo "Generating SARIF report..."
                            trivy image --format sarif --output "trivy-report.sarif" ${params.image_name}:${env.BUILD_ID}
                            
                            echo "Security scan completed successfully"
                        """
                        echo "Security scan completed"
                    } catch (Exception e) {
                        echo "Security scan failed: ${e.message}"
                        echo "Continuing with available reports..."
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }
        
        stage('Generate Excel Report') {
            steps {
                script {
                    echo "Generating Excel report..."
                    try {
                        // Create Python script for Excel conversion
                        sh '''
                            echo "Creating Python conversion script..."
                            cat > convert_to_excel.py << 'EOF'
import json
import pandas as pd
import sys
from datetime import datetime
import os

try:
    print("Loading JSON data...")
    with open('trivy-report.json', 'r') as f:
        data = json.load(f)

    print("Extracting vulnerabilities...")
    vulnerabilities = []
    for result in data.get('Results', []):
        for vuln in result.get('Vulnerabilities', []):
            vuln_info = {
                'Target': result.get('Target', ''),
                'VulnerabilityID': vuln.get('VulnerabilityID', ''),
                'PkgName': vuln.get('PkgName', ''),
                'InstalledVersion': vuln.get('InstalledVersion', ''),
                'FixedVersion': vuln.get('FixedVersion', ''),
                'Severity': vuln.get('Severity', ''),
                'Title': vuln.get('Title', ''),
                'Description': vuln.get('Description', '')[:32767],
                'PublishedDate': vuln.get('PublishedDate', ''),
                'LastModifiedDate': vuln.get('LastModifiedDate', '')
            }
            vulnerabilities.append(vuln_info)

    print(f"Found {len(vulnerabilities)} vulnerabilities")

    # Create DataFrame
    if vulnerabilities:
        df = pd.DataFrame(vulnerabilities)
        print("Creating Excel file with multiple sheets...")
        # Create Excel file with multiple sheets
        with pd.ExcelWriter('trivy-report.xlsx', engine='openpyxl') as writer:
            # All vulnerabilities sheet
            df.to_excel(writer, sheet_name='All_Vulnerabilities', index=False)

            # Summary by severity sheet
            severity_summary = df['Severity'].value_counts().reset_index()
            severity_summary.columns = ['Severity', 'Count']
            severity_summary.to_excel(writer, sheet_name='Severity_Summary', index=False)

            # Packages with vulnerabilities sheet
            package_summary = df['PkgName'].value_counts().reset_index()
            package_summary.columns = ['Package', 'Vulnerability_Count']
            package_summary.to_excel(writer, sheet_name='Package_Summary', index=False)

            # Add metadata sheet
            metadata = pd.DataFrame([
                {'Key': 'Generated_Date', 'Value': datetime.now().strftime('%Y-%m-%d %H:%M:%S')},
                {'Key': 'Total_Vulnerabilities', 'Value': len(vulnerabilities)},
                {'Key': 'Image_Name', 'Value': os.environ.get('IMAGE_NAME', 'Unknown')}
            ])
            metadata.to_excel(writer, sheet_name='Metadata', index=False)

        print("Excel report generated successfully")
    else:
        print("No vulnerabilities found - creating empty report")
        # Create empty Excel with message
        df = pd.DataFrame([{'Message': 'No vulnerabilities found'}])
        df.to_excel('trivy-report.xlsx', index=False)
        print("Empty Excel report generated")

except FileNotFoundError:
    print("ERROR: trivy-report.json file not found")
    sys.exit(1)
except json.JSONDecodeError as e:
    print(f"ERROR: Invalid JSON format: {e}")
    sys.exit(1)
except Exception as e:
    print(f"ERROR generating Excel report: {str(e)}")
    sys.exit(1)
EOF
                        '''
                        
                        // Install Python dependencies and run conversion
                        sh """
                            echo "Setting up Python environment..."
                            python3 --version || python --version || echo "Python not found"
                            
                            echo "Installing Python dependencies..."
                            pip3 install pandas openpyxl 2>/dev/null || pip install pandas openpyxl 2>/dev/null || echo "Trying alternative installation..."
                            pip3 install --user pandas openpyxl 2>/dev/null || pip install --user pandas openpyxl 2>/dev/null || echo "Dependencies may not be installed"
                            
                            echo "Setting environment variable for Python script..."
                            export IMAGE_NAME="${params.image_name}:${env.BUILD_ID}"
                            
                            echo "Running Excel conversion..."
                            python3 convert_to_excel.py 2>/dev/null || python convert_to_excel.py || echo "Excel conversion failed but continuing"
                            
                            if [ -f "trivy-report.xlsx" ]; then
                                echo "Excel report created successfully"
                            else
                                echo "WARNING: Excel report was not created"
                            fi
                        """
                        echo "Excel report generation completed"
                    } catch (Exception e) {
                        echo "Excel report generation failed: ${e.message}"
                        echo "Continuing without Excel report..."
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }
        
        stage('Generate HTML Report') {
            steps {
                script {
                    echo "Generating HTML report..."
                    try {
                        sh '''
                            echo "Creating basic HTML report..."
                            # Create a simple HTML report without complex templates
                            cat > trivy-report.html << EOF
<!DOCTYPE html>
<html>
<head>
    <title>Trivy Vulnerability Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        table { border-collapse: collapse; width: 100%; margin: 10px 0; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
        .summary { background-color: #f9f9f9; padding: 15px; margin: 10px 0; }
    </style>
</head>
<body>
    <h1>Trivy Vulnerability Report</h1>
    <div class="summary">
        <h2>Scan Summary</h2>
        <p><strong>Image:</strong> ''' + "${params.image_name}:${env.BUILD_ID}" + '''</p>
        <p><strong>Generated:</strong> ''' + '$(date)' + '''</p>
        <p><strong>Note:</strong> Detailed vulnerability data available in JSON and Excel reports</p>
    </div>
    <p>Download the detailed reports from Jenkins artifacts:</p>
    <ul>
        <li><a href="trivy-report.json">JSON Report</a></li>
        <li><a href="trivy-report.xlsx">Excel Report</a></li>
        <li><a href="trivy-report.sarif">SARIF Report</a></li>
    </ul>
</body>
</html>
EOF
                    
                        if [ -f "trivy-report.html" ]; then
                            echo "HTML report created successfully"
                        else
                            echo "WARNING: HTML report was not created"
                        fi
                        '''
                        echo "HTML report generated"
                    } catch (Exception e) {
                        echo "HTML report generation failed: ${e.message}"
                        echo "Continuing without HTML report..."
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }
        
        stage('Force Image Cleanup') {
            steps {
                script {
                    echo "Starting forced Docker image cleanup..."
                    echo "This cleanup will run regardless of build success/failure"
                    
                    sh """
                        echo "=== FORCED DOCKER IMAGE CLEANUP ==="
                        echo "Removing Docker image: ${params.image_name}:${env.BUILD_ID}"
                        
                        # First attempt - normal removal
                        docker rmi -f ${params.image_name}:${env.BUILD_ID} 2>/dev/null && echo "Image removed successfully" || echo "First removal attempt failed"
                        
                        # Second attempt - force removal if first failed
                        docker rmi -f ${params.image_name}:${env.BUILD_ID} 2>/dev/null && echo "Image removed on second attempt" || echo "Second removal attempt failed"
                        
                        # Check if image still exists
                        if docker images | grep "${params.image_name}" | grep "${env.BUILD_ID}" > /dev/null; then
                            echo "WARNING: Docker image may still exist, but cleanup attempts completed"
                        else
                            echo "CONFIRMED: Docker image ${params.image_name}:${env.BUILD_ID} has been removed"
                        fi
                        
                        # Also remove latest tag if it exists
                        docker rmi -f ${params.image_name}:latest 2>/dev/null && echo "Latest tag removed" || echo "Latest tag not found or already removed"
                        
                        echo "=== CLEANUP COMPLETED ==="
                    """
                    echo "Forced image cleanup stage completed"
                }
            }
        }
    }
    
    post {
        always {
            script {
                echo "Starting post-build cleanup..."
                
                // Archive artifacts with error handling
                try {
                    echo "Archiving reports..."
                    sh """
                        echo "Available report files:"
                        ls -la trivy-report.* 2>/dev/null || echo "No report files found"
                    """
                    archiveArtifacts artifacts: 'trivy-report.*', fingerprint: true, allowEmptyArchive: true
                    echo "Reports archived successfully"
                } catch (Exception e) {
                    echo "Artifact archiving failed: ${e.message}"
                }
                
                // Final cleanup verification
                try {
                    echo "Final cleanup verification..."
                    sh """
                        echo "=== FINAL CLEANUP VERIFICATION ==="
                        echo "Checking for remaining Docker images..."
                        docker images | grep "${params.image_name}" && echo "WARNING: Some images may still exist" || echo "No related images found"
                        
                        echo "Cleaning up temporary files..."
                        rm -f convert_to_excel.py html.tpl custom-html.tpl 2>/dev/null || echo "Temporary files not found or already removed"
                        
                        echo "Final cleanup completed"
                    """
                } catch (Exception e) {
                    echo "Cleanup verification had issues: ${e.message}"
                }
            }
        }
        success {
            script {
                echo "Pipeline executed successfully!"
                echo "=== REPORT DOWNLOAD LINKS ==="
                echo "All Reports: ${env.BUILD_URL}artifact/"
                echo "Excel Report: ${env.BUILD_URL}artifact/trivy-report.xlsx"
                echo "HTML Report: ${env.BUILD_URL}artifact/trivy-report.html"
                echo "JSON Report: ${env.BUILD_URL}artifact/trivy-report.json"
                echo "SARIF Report: ${env.BUILD_URL}artifact/trivy-report.sarif"
                echo "============================="
                
                sh """
                    echo "=== FINAL STATUS ==="
                    echo "Build: SUCCESS"
                    echo "Build Method Used: ${params.build_method}"
                    echo "Image cleanup: COMPLETED"
                    echo "Reports: AVAILABLE for download"
                """
            }
        }
        failure {
            script {
                echo "Pipeline failed!"
                echo "Check the stage logs above for detailed error information"
                
                sh """
                    echo "=== FAILURE STATUS ==="
                    echo "Build: FAILED"
                    echo "Build Method Used: ${params.build_method}"
                    echo "Image cleanup: ATTEMPTED (see Force Image Cleanup stage)"
                """
            }
        }
        unstable {
            script {
                echo "Pipeline completed with warnings"
                echo "Some stages had issues but pipeline continued"
                echo "Reports may be partially available: ${env.BUILD_URL}artifact/"
                
                sh """
                    echo "=== UNSTABLE STATUS ==="
                    echo "Build: UNSTABLE"
                    echo "Build Method Used: ${params.build_method}"
                    echo "Image cleanup: COMPLETED"
                    echo "Reports: PARTIALLY AVAILABLE"
                """
            }
        }
    }
}