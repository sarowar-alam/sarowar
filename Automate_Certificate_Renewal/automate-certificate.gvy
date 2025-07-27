pipeline {
    agent { label 'built-in' }

    parameters {
        choice(
            name: 'DOMAIN',
            choices: [
                '*.Your_Domain_Name_01.com',
                '*.Your_Domain_Name_02.com',
                '*.Your_Domain_Name_03.com',
                '*.Your_Domain_Name_04.com',
                '*.Your_Domain_Name_06.net', 
                '*.Your_Domain_Name_05.com'
            ],
            description: 'Select Certificate Name Renew and Update in place ... '
        )
    }

    environment {
        AWS_REGION = "us-west-2"
        PFX_PASS = credentials('PFX_PASS_CREDENTIALS_ID')
        JENKINS_PFX_PASS = credentials('JENKINS_PFX_PASS_CREDENTIALS_ID')
        
        // Server IP mappings
        SERVER_IPS = [
            'Your_Domain_Name_03': [
                'RC': "192.168.1.1",
                'MAINLINE': "192.168.1.2",
                'PROD': ["192.168.1.3", "192.168.1.4"]
            ],
            'Your_Domain_Name_06': [
                'PROD': ["192.168.1.5", "192.168.1.6"]
            ],
            'ZABBIX': [
                'PROD': "192.168.1.7"
            ],
            'JENKINS': [
                'LINUX': "192.168.1.8"
            ]
        ]
        
        // Email recipients mapping
        EMAIL_RECIPIENTS = [
            'Your_Domain_Name_01': [
                'TO': "your_team@wundermanthompson.com; your_team@your_domain__name.com",
                'CC': "your_team@your_domain__name.com; your_team@your_domain__name.com"
            ],
            'Your_Domain_Name_02': [
                'TO': "your_team@wundermanthompson.com; your_team@your_domain__name.com",
                'CC': "your_team@your_domain__name.com; your_team@your_domain__name.com"
            ],
            'Your_Domain_Name_03': [
                'TO': "your_name@your_domain__name.com",
                'CC': "your_name@your_domain__name.com"
            ],
            'Your_Domain_Name_06': [
                'TO': "your_name@your_domain__name.com",
                'CC': "your_name@your_domain__name.com"
            ]
        ]
        
        // AWS resources
        AWS_BUCKET = "Your_Domain_Name_03-logs"
        AWS_BUCKET_PREFIX = "certificates/"
        
        // Script paths
        SCRIPTS = [
            'start_ec2': 'path/to_your_git/start-ec2-servers.py',
            'check_expiry': 'path/to_your_git/ceheck-certificate-expiry.ps1',
            'create_cert': 'path/to_your_git/backup-create-cert.ps1',
            'update_acm': 'path/to_your_git/update-aws-certificate.py',
            'update_iis': 'path/to_your_git/update-iis.ps1',
            'update_iis_robust': 'path/to_your_git/update-iis-robust.ps1',
            'update_jenkins_windows': 'path/to_your_git/update-jenkins-windows.ps1',
            'update_jenkins_linux': 'path/to_your_git/update-jenkins-linux.py',
            'update_zabbix': 'path/to_your_git/update-zabbix-certificate.py',
            'send_email': 'path/to_your_git/send_certificate_email.py'
        ]
        
        CERTIFICATE_UPDATE_STATUS = false   
    }

    stages {
        stage('Initialize') {
            steps {
                script {
                    // Validate parameters
                    if (!params.DOMAIN) {
                        error("Domain parameter is required")
                    }
                    
                    // Extract base domain without wildcard
                    BASE_DOMAIN = params.DOMAIN.replaceFirst(/\*\./, '')
                    
                    // Log initialization
                    echo "Initializing pipeline for domain: ${params.DOMAIN}"
                    echo "Base domain: ${BASE_DOMAIN}"
                    
                    // Set timeout for the entire pipeline
                    timeout(time: 2, unit: 'HOURS') {
                        echo "Pipeline will timeout after 2 hours"
                    }
                }
            }
        }
        
        stage('Start EC2 Instances') {
            steps {
                script {
                    // Map domain to instance IDs
                    def instanceMap = [
                        '*.Your_Domain_Name_01.com': ['i-234jsdhfjk328432a', 'i-234jsdhfjk328432a', 'i-234jsdhfjk328432a', 'i-234jsdhfjk328432a'],
                        '*.Your_Domain_Name_02.com': ['i-234jsdhfjk328432a', 'i-234jsdhfjk328432a'],
                        '*.Your_Domain_Name_03.com': ['i-234jsdhfjk328432a', 'i-234jsdhfjk328432a'],
                        '*.Your_Domain_Name_06.net': ['i-234jsdhfjk328432a', 'i-234jsdhfjk328432a']
                    ]
                    
                    def instanceIds = instanceMap.get(params.DOMAIN, [])
                    
                    if (instanceIds) {
                        echo "Starting EC2 instances: ${instanceIds}"
                        
                        withAWS(credentials: 'your_jenkins_credentials_id', region: env.AWS_REGION) {
                            def exitCode = bat(
                                script: """
                                    python.exe "${SCRIPTS['start_ec2']}" "${instanceIds.join(',')}"
                                """,
                                returnStatus: true
                            )
                            
                            if (exitCode != 0) {
                                error("Failed to start EC2 instances (exit code: ${exitCode})")
                            }
                            echo "EC2 instances started successfully"
                        }
                    } else {
                        echo "No EC2 instances to start for domain: ${params.DOMAIN}"
                    }
                }
            }
        }
        
        stage('Check Certificate Validity') {
            steps {
                script {
                    // Map domains to subdomains
                    def subdomainMap = [
                        'Your_Domain_Name_01.com': 'your_sub_domain',
                        'Your_Domain_Name_02.com': 'your_sub_domain',
                        'Your_Domain_Name_03.com': 'your_sub_domain',
                        'Your_Domain_Name_04.com': 'your_sub_domain',
                        'Your_Domain_Name_06.net': 'your_sub_domain',
                        'Your_Domain_Name_05.com': 'your_sub_domain'
                    ]
                    
                    def subdomain = subdomainMap.get(BASE_DOMAIN, '')
                    if (!subdomain) {
                        error("No subdomain mapping found for base domain: ${BASE_DOMAIN}")
                    }
                    
                    def finalDomain = "${subdomain}.${BASE_DOMAIN}"
                    echo "Checking certificate validity for domain: ${finalDomain}"
                    
                    // Execute PowerShell script to check certificate expiry
                    def output = powershell(
                        returnStdout: true,
                        script: """
                            & "${SCRIPTS['check_expiry']}" -Domain "${finalDomain}" -ErrorAction Stop
                        """
                    ).trim()
                    
                    echo "Certificate check output:\n${output}"
                    
                    // Parse days left from output
                    def daysLeft = (output =~ /(\d+)/) ? (output =~ /(\d+)/)[0][1].toInteger() : 0
                    
                    if (daysLeft == 0) {
                        echo "SSL certificate is invalid or not accessible"
                        CERTIFICATE_UPDATE_STATUS = true
                    } else if (daysLeft < 15) {
                        echo "SSL certificate expires in ${daysLeft} days - renewal required"
                        CERTIFICATE_UPDATE_STATUS = true
                    } else {
                        echo "SSL certificate is valid for ${daysLeft} days - no renewal needed"
                        CERTIFICATE_UPDATE_STATUS = false
                    }
                }
            }
        }
        
        stage('Create Certificate') {
            when {
                expression { return env.CERTIFICATE_UPDATE_STATUS == 'true' }
            }
            steps {
                script {
                    echo "Creating new certificate for domain: ${params.DOMAIN}"
                    
                    withAWS(credentials: 'your_jenkins_credentials_id', region: env.AWS_REGION) {
                        def result = powershell(
                            returnStdout: true,
                            script: """
                                & "${SCRIPTS['create_cert']}" `
                                    -Domain '${params.DOMAIN}' `
                                    -PfxPass '${PFX_PASS}' `
                                    -ErrorAction Stop
                            """
                        ).trim()
                        
                        echo "Certificate creation output:\n${result}"
                        
                        if (result.contains('[RESULT] NEW_CERT')) {
                            echo "✅ New certificate created successfully"
                        } else if (result.contains('[RESULT] REUSED_CERT')) {
                            echo "✅ Existing certificate reused"
                        } else {
                            error("❌ Certificate creation failed")
                        }
                    }
                }
            }
        }
        
        stage('Update AWS Certificate Manager') {
            when {
                expression { return env.CERTIFICATE_UPDATE_STATUS == 'true' }
            }
            steps {
                script {
                    echo "Updating AWS Certificate Manager for domain: ${params.DOMAIN}"
                    
                    withAWS(credentials: 'your_jenkins_credentials_id', region: env.AWS_REGION) {
                        def exitCode = bat(
                            script: """
                                python.exe "${SCRIPTS['update_acm']}" "${params.DOMAIN}"
                            """,
                            returnStatus: true
                        )
                        
                        if (exitCode != 0) {
                            error("Failed to update ACM (exit code: ${exitCode})")
                        }
                        echo "AWS Certificate Manager updated successfully"
                    }
                }
            }
        }
        
        stage('Deploy Certificate to Servers') {
            when {
                expression { return env.CERTIFICATE_UPDATE_STATUS == 'true' }
            }
            steps {
                script {
                    echo "Deploying certificate to servers for domain: ${params.DOMAIN}"
                    
                    // Determine which servers to update based on domain
                    switch(BASE_DOMAIN) {
                        case 'Your_Domain_Name_01.com':
                            updateIisServer(env.SERVER_IPS.Your_Domain_Name_03.RC)
                            break
                            
                        case 'Your_Domain_Name_02.com':
                            updateIisServer(env.SERVER_IPS.Your_Domain_Name_03.MAINLINE)
                            break
                            
                        case 'Your_Domain_Name_03.com':
                            env.SERVER_IPS.Your_Domain_Name_03.PROD.each { ip ->
                                updateIisServerRobust(ip)
                            }
                            break
                            
                        case 'Your_Domain_Name_06.net':
                            env.SERVER_IPS.Your_Domain_Name_06.PROD.each { ip ->
                                updateIisServerRobust(ip)
                            }
                            
                            // Update Jenkins servers
                            updateJenkinsWindows()
                            updateJenkinsLinux(env.SERVER_IPS.JENKINS.LINUX)
                            updateZabbixServer(env.SERVER_IPS.ZABBIX.PROD)
                            break
                            
                        default:
                            echo "No server deployments needed for domain: ${params.DOMAIN}"
                    }
                }
            }
        }
        
        stage('Send Notification') {
            when {
                expression { return env.CERTIFICATE_UPDATE_STATUS == 'true' }
            }
            steps {
                script {
                    def recipients = env.EMAIL_RECIPIENTS[BASE_DOMAIN.replaceAll(/[^a-zA-Z0-9_]/, '_')]
                    
                    if (recipients) {
                        echo "Sending notification email for domain: ${params.DOMAIN}"
                        
                        withAWS(credentials: 'your_jenkins_credentials_id', region: 'us-east-1') {
                            def exitCode = bat(
                                script: """
                                    python.exe "${SCRIPTS['send_email']}" ^
                                        "${params.DOMAIN}" ^
                                        "${recipients.TO}" ^
                                        "${recipients.CC}" ^
                                        "${env.AWS_BUCKET}" ^
                                        "${env.AWS_BUCKET_PREFIX}"
                                """,
                                returnStatus: true
                            )
                            
                            if (exitCode != 0) {
                                echo "WARNING: Email sending failed (exit code: ${exitCode})"
                            } else {
                                echo "Notification email sent successfully"
                            }
                        }
                    } else {
                        echo "No email recipients configured for domain: ${params.DOMAIN}"
                    }
                }
            }
        }
    }
    
    post {
        always {
            script {
                // Clean up workspace
                cleanWs(
                    cleanWhenNotBuilt: false,
                    deleteDirs: true,
                    patterns: [
                        [pattern: '**/*', type: 'INCLUDE'],
                        [pattern: '!.gitignore', type: 'EXCLUDE']
                    ]
                )
                
                // Final status message
                echo "Pipeline execution completed with status: ${currentBuild.currentResult}"
            }
        }
        
        failure {
            script {
                // Send failure notification
                def errorMsg = currentBuild.rawBuild.getLog(100).findAll { 
                    it.contains('ERROR') || it.contains('FAIL') || it.contains('Exception') 
                }.join('\n') ?: "No specific error message captured"
                
                sendFailureEmail(
                    jobName: env.JOB_NAME,
                    buildNumber: env.BUILD_NUMBER,
                    buildUrl: env.BUILD_URL,
                    errorMessage: errorMsg
                )
            }
        }
    }
}

// Helper functions
def updateIisServer(String serverIp) {
    echo "Updating IIS server: ${serverIp}"
    
    withCredentials([usernamePassword(
        credentialsId: 'your_jenkins_credentials_id',
        usernameVariable: 'USERNAME',
        passwordVariable: 'PASSWORD'
    )]) {
        def exitCode = powershell(
            returnStatus: true,
            script: """
                & "${env.SCRIPTS['update_iis']}" `
                    -RemoteIP "${serverIp}" `
                    -Username "${USERNAME}" `
                    -Password "${PASSWORD}" `
                    -CertCN "${params.DOMAIN}" `
                    -PfxPassword "${env.PFX_PASS}" `
                    -ErrorAction Stop
            """
        )
        
        if (exitCode != 0) {
            error("Failed to update IIS server ${serverIp} (exit code: ${exitCode})")
        }
        echo "Successfully updated IIS server: ${serverIp}"
    }
}

def updateIisServerRobust(String serverIp) {
    echo "Updating IIS server (robust): ${serverIp}"
    
    withCredentials([usernamePassword(
        credentialsId: 'your_jenkins_credentials_id',
        usernameVariable: 'USERNAME',
        passwordVariable: 'PASSWORD'
    )]) {
        def exitCode = powershell(
            returnStatus: true,
            script: """
                & "${env.SCRIPTS['update_iis_robust']}" `
                    -RemoteIP "${serverIp}" `
                    -Username "${USERNAME}" `
                    -Password "${PASSWORD}" `
                    -CertCN "${params.DOMAIN}" `
                    -PfxPassword "${env.PFX_PASS}" `
                    -ConfirmDeletion `
                    -ErrorAction Stop
            """
        )
        
        if (exitCode != 0) {
            error("Failed to update IIS server ${serverIp} (exit code: ${exitCode})")
        }
        echo "Successfully updated IIS server (robust): ${serverIp}"
    }
}

def updateJenkinsWindows() {
    echo "Updating Jenkins Windows certificate"
    
    try {
        def exitCode = powershell(
            returnStatus: true,
            script: """
                & "${env.SCRIPTS['update_jenkins_windows']}" `
                    -CertCN '${params.DOMAIN}' `
                    -JksPassword (ConvertTo-SecureString -AsPlainText -Force -String '${env.JENKINS_PFX_PASS}') `
                    -PfxPassword (ConvertTo-SecureString -AsPlainText -Force -String '${env.PFX_PASS}') `
                    -ErrorAction Stop
            """
        )
        
        if (exitCode != 0) {
            error("Failed to update Jenkins Windows certificate (exit code: ${exitCode})")
        }
        echo "Successfully updated Jenkins Windows certificate"
    } catch (Exception e) {
        error("Exception while updating Jenkins Windows certificate: ${e.getMessage()}")
    }
}

def updateJenkinsLinux(String serverIp) {
    echo "Updating Jenkins Linux server: ${serverIp}"
    
    def exitCode = bat(
        script: """
            python.exe "${env.SCRIPTS['update_jenkins_linux']}" "${params.DOMAIN}" "${serverIp}"
        """,
        returnStatus: true
    )
    
    if (exitCode != 0) {
        error("Failed to update Jenkins Linux server (exit code: ${exitCode})")
    }
    echo "Successfully updated Jenkins Linux server: ${serverIp}"
}

def updateZabbixServer(String serverIp) {
    echo "Updating Zabbix server: ${serverIp}"
    
    def exitCode = bat(
        script: """
            python.exe "${env.SCRIPTS['update_zabbix']}" "${params.DOMAIN}" "${serverIp}"
        """,
        returnStatus: true
    )
    
    if (exitCode != 0) {
        error("Failed to update Zabbix server (exit code: ${exitCode})")
    }
    echo "Successfully updated Zabbix server: ${serverIp}"
}

def sendFailureEmail(Map args) {
    echo "Sending failure notification email"
    
    withAWS(credentials: 'your_jenkins_credentials_id', region: 'us-east-1') {
        def pythonScript = """
import boto3
import os

def send_email():
    ses = boto3.client('ses',
        aws_access_key_id=os.environ['AWS_ACCESS_KEY_ID'],
        aws_secret_access_key=os.environ['AWS_SECRET_ACCESS_KEY'],
        region_name='us-east-1'
    )
    
    response = ses.send_email(
        Source='DevOps_Jenkins_Automation <noreply@Your_Domain_Name_06.net>',
        Destination={
            'ToAddresses': ['your_team@xbox.com', 'your_team@xbox.com'],
            'CcAddresses': ['your_name@xbox.com', 'your_name@xbox.com']
        },
        Message={
            'Subject': {'Data': f"FAILED: {args.jobName} #{args.buildNumber}"},
            'Body': {
                'Html': {
                    'Data': f\"\"\"<html>
                        <body>
                            <h2>Build Failed</h2>
                            <p><strong>Job:</strong> {args.jobName}</p>
                            <p><strong>Build:</strong> #{args.buildNumber}</p>
                            <p><strong>Console:</strong> <a href="{args.buildUrl}">{args.buildUrl}</a></p>
                            <hr>
                            <h3>Error Details:</h3>
                            <pre style="background:#f5f5f5;padding:10px;border-radius:5px;">{args.errorMessage}</pre>
                        </body>
                    </html>\"\"\"
                }
            }
        }
    )
    print(f"Email sent! Message ID: {response['MessageId']}")

send_email()
"""
        
        writeFile file: 'send_failure_email.py', text: pythonScript
        def output = bat(script: "python send_failure_email.py", returnStdout: true).trim()
        echo "Email sending output: ${output}"
        bat "del send_failure_email.py"
    }
}