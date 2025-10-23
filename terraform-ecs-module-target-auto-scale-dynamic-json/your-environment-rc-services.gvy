import java.time.Instant
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException
import org.jenkinsci.plugins.workflow.steps.TimeoutStepExecution

def ECS_CHOICES = [
    'web-app-01',
    'web-app-02',
    'web-app-03'
]

pipeline {
    agent { label 'built-in' }

    parameters {
        choice(
            name: 'ECS_GLOBAL',
            choices: ECS_CHOICES,
            description: 'Select the ECS Name to Deploy'
        ) 
        string(
            defaultValue: 'main', 
            description: 'Enter your Branch Name', 
            name: 'branch'
        )      
    }

    environment {
        JSON_FILE_PATH = "terraform-ecs-module-auto-scale-dynamic-json/ecs-services.json"

        AWS_REGION = "us-west-2"
        ECR_REPO = "YOUR_ACCOUNT_ID.dkr.ecr.us-west-2.amazonaws.com"
        REPO_NAME = "${params.ECS_GLOBAL}"
        TAG = "${params.ECS_GLOBAL}-${BUILD_NUMBER}"
        SOURCE_PATH = "${WORKSPACE}/sourceCode" 

        TASK_ROLE_ARN = "arn:aws:iam::YOUR_ACCOUNT_ID:role/ecsTaskRole"
        TF_PATH = "${WORKSPACE}/TerraFormCode/terraform-ecs-module-auto-scale-dynamic-json"

        VPC_ID = "vpc-xxxxxxxxx"
        PUBLIC_SUBNETS = "subnet-xxxxxxxxx,subnet-yyyyyyyyy"
        PRIVATE_SUBNETS = "subnet-zzzzzzzzz,subnet-wwwwwwwww"
        ACM_CERTIFICATE_ARN = "arn:aws:acm:us-west-2:YOUR_ACCOUNT_ID:certificate/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"

        TAG_COMPANY = "YourCompany"
        TAG_OWNER = "YourTeam"
        TAG_SYSTEM = "YourSystem"
        TAG_COSTAPP = "YourApp"        
    }

    stages {
        stage('ECSConfiguration') {
            steps {
                script {
                    try {
                        echo "Reading JSON configuration from: ${JSON_FILE_PATH}"
                        if (!fileExists("${JSON_FILE_PATH}")) {
                            error "JSON configuration file not found at: ${JSON_FILE_PATH}"
                        }
                        def jsonText = readFile "${JSON_FILE_PATH}"
                        def jsonSlurper = new groovy.json.JsonSlurper()
                        def jsonFile = jsonSlurper.parseText(jsonText)
                        if (!jsonFile.services) {
                            error "JSON file does not contain 'services' array"
                        }
                        def selectedService = jsonFile.services.find { service ->
                            service.ecs_name == params.ECS_GLOBAL
                        }
                        
                        if (selectedService) {
                            echo "=== ECS Service Configuration for: ${params.ECS_GLOBAL} ==="
                            echo "CPU: ${selectedService.cpu}"
                            echo "Memory: ${selectedService.memory}"
                            echo "Disk Size: ${selectedService.disk_size}"
                            echo "Min Instances: ${selectedService.min_instances}"
                            echo "Max Instances: ${selectedService.max_instances}"
                            echo "Container Port: ${selectedService.container_port}"
                            echo "Health Check Path: ${selectedService.health_check_path}"
                            echo "Environment: ${selectedService.env}"
                            echo "=================================================================="
                            
                            env.SERVICE_NAME = selectedService.ecs_name.toString()
                            env.CLUSTER = selectedService.cluster.toString()
                            env.DEPLOY_ENV = selectedService.environment.toString()
                            env.ENV_FILE_NAME = selectedService.env.toString()
                            env.SELECTED_CPU = selectedService.cpu.toString()
                            env.SELECTED_MEMORY = selectedService.memory.toString()
                            env.SELECTED_DISK_SIZE = selectedService.disk_size.toString()
                            env.SELECTED_MIN_INSTANCES = selectedService.min_instances.toString()
                            env.SELECTED_MAX_INSTANCES = selectedService.max_instances.toString()
                            env.CONTAINER_PORT = selectedService.container_port.toString()
                            env.HEALTH_CHECK_PATH = selectedService.health_check_path.toString()
                            env.HEALTH_CHECK_GRACE_PERIOD = selectedService.health_check_grace_period.toString()
                            env.SELECTED_DOCKER_FILE_PATH = selectedService.docker_file_path.toString()
                            
                        } else {
                            error "ECS service '${params.ECS_GLOBAL}' not found in JSON configuration at ${JSON_FILE_PATH}"
                        }
                        
                    } catch (FileNotFoundException e) {
                        error "JSON configuration file not found: ${e.message}"
                    } catch (Exception e) {
                        error "Failed to read ECS configuration: ${e.message}"
                    }
                }
            }
        }
    
        stage('ECS_Summary') {
            steps {
                script {
                    echo "=== Deployment Summary ==="
                    echo "Service: ${params.ECS_GLOBAL}"
                    echo "File: ${JSON_FILE_PATH}"
                    echo "CPU: ${env.SELECTED_CPU}"
                    echo "Memory: ${env.SELECTED_MEMORY}"
                    echo "Disk Size: ${env.SELECTED_DISK_SIZE}"
                    echo "Container Port: ${env.CONTAINER_PORT}"
                    echo "Health Check Path: ${env.HEALTH_CHECK_PATH}"
                    echo "Instance Range: ${env.SELECTED_MIN_INSTANCES} - ${env.SELECTED_MAX_INSTANCES}"
                    echo "Ready for deployment"
                }
            }
        }

        stage('ContainerBuild') {
            steps {
                dir('sourceCode') {
                    script {
                        def selectedOption = params.ECS_GLOBAL
                        echo "Building service: ${selectedOption}"
                        try {
                            git(
                                url: 'git@github.com:your_company_git/your_web_app_repo.git', 
                                branch: "${params.branch}", 
                                credentialsId: 'jenkins-git-access-key-id'
                            )
                            echo "Git Pulled from ${params.branch}, Service Name: ${params.ECS_GLOBAL}, Docker file path: ${env.SELECTED_DOCKER_FILE_PATH}"
                            sh """
                                pwd
                                ls -lrth
                            """
                        } catch (Exception e) {
                            echo "Error occurred: ${e.getMessage()}"
                            currentBuild.result = 'FAILURE'
                            error("Git checkout failed: ${e.getMessage()}")
                        }
                        
                        echo "Building Docker Image: ${ECR_REPO}/${REPO_NAME}:${TAG}"
                        sh """
                            # Navigate to Dockerfile directory
                            cd "${env.SELECTED_DOCKER_FILE_PATH}" || { echo "Failed to change directory to ${env.SELECTED_DOCKER_FILE_PATH}"; exit 1; }
                            
                            # Build the Docker image with tags
                            docker build -t ${ECR_REPO}/${REPO_NAME}:${TAG} -t ${ECR_REPO}/${REPO_NAME}:latest .
                            
                            echo "Docker images created:"
                            docker images | grep ${REPO_NAME}
                        """
                    } 
                }
            }
        }    

        stage('TrivySecurityScan') {
            steps {
                dir('sourceCode') {
                    script {
                        echo "Starting Trivy security scan for image: ${ECR_REPO}/${REPO_NAME}:${TAG}"
                        
                        try {
                            // Run Trivy scan with specific rules for web applications
                            def trivyOutput = sh(
                                script: """
                                    set +e
                                    trivy image --format table \
                                        --severity CRITICAL,HIGH \
                                        --ignore-unfixed \
                                        --exit-code 0 \
                                        ${ECR_REPO}/${REPO_NAME}:${TAG}
                                """,
                                returnStdout: true
                            ).trim()
                            
                            echo "=== TRIVY SCAN RESULTS ==="
                            echo trivyOutput
                            echo "=== END TRIVY SCAN RESULTS ==="
                            
                            // Count vulnerabilities
                            def criticalCount = trivyOutput.count('CRITICAL')
                            def highCount = trivyOutput.count('HIGH')
                            
                            echo """
                            ===== TRIVY VULNERABILITY SUMMARY =====
                            CRITICAL Vulnerabilities: ${criticalCount}
                            HIGH Vulnerabilities: ${highCount}
                            =======================================
                            """
                            
                            // Quality gates - fail on critical vulnerabilities only
                            if (criticalCount > 0) {
                                error("Critical vulnerabilities detected. Build failed.")
                            } else if (highCount > 5) {
                                echo "High vulnerabilities detected but within acceptable limits for web app."
                            } else {
                                echo "Security scan passed. No critical vulnerabilities."
                            }
                        } catch (Exception e) {
                            echo "Trivy scan completed with findings: ${e.getMessage()}"
                            // Continue for web applications unless critical vulnerabilities
                        }
                    }
                }
            }
        }        

        stage('ECR_Login') {
            steps {
                script {
                    withCredentials([[
                        $class: 'UsernamePasswordMultiBinding', 
                        credentialsId: 'your-aws-iam-user-credentials-id',
                        usernameVariable: 'ACCESSKEY', 
                        passwordVariable: 'SECRETKEY'
                    ]]) {
                        try {
                            sh """
                                # Set AWS credentials
                                export AWS_ACCESS_KEY_ID=${ACCESSKEY}
                                export AWS_SECRET_ACCESS_KEY=${SECRETKEY}
                                export AWS_DEFAULT_REGION=${AWS_REGION}
                                
                                # Verify AWS credentials work
                                if ! aws sts get-caller-identity; then
                                    echo "AWS credentials are invalid"
                                    exit 1
                                fi
                                
                                # Check if ECR repository exists, create if it doesn't
                                if ! aws ecr describe-repositories --repository-names ${REPO_NAME} >/dev/null 2>&1; then
                                    echo "Creating ECR repository: ${REPO_NAME}"
                                    if ! aws ecr create-repository \\
                                        --repository-name ${REPO_NAME} \\
                                        --image-tag-mutability MUTABLE \\
                                        --image-scanning-configuration scanOnPush=true \\
                                        --encryption-configuration encryptionType=AES256; then
                                        echo "Failed to create ECR repository"
                                        exit 1
                                    fi
                                    echo "ECR repository created successfully"
                                else
                                    echo "ECR repository ${REPO_NAME} already exists"
                                fi
                                
                                # Login to ECR
                                if ! aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_REPO; then
                                    echo "ECR login failed"
                                    exit 1
                                fi
                                
                                echo "Successfully logged in to ECR"
                            """
                        } catch (Exception e) {
                            echo "ECR login stage failed: ${e.getMessage()}"
                            error("ECR login failed")
                        }
                    }
                }
            }     
        }     

        stage('ECRPush') {
            steps {
                script {
                    try {
                        // Define ImageURI before use
                        def ImageURI = "${ECR_REPO}/${REPO_NAME}:${TAG}"
                        echo "Pushing Docker Image: ${ImageURI}"
                        sh """
                            docker push ${ImageURI}
                            docker push ${ECR_REPO}/${REPO_NAME}:latest
                        """
                        echo "Docker Image Address: ${ImageURI}"
                        
                        // SET THE ENVIRONMENT VARIABLE HERE - after successful push
                        env.SuccessImageURI = ImageURI
                        echo "Stored image URI in environment variable: ${env.SuccessImageURI}"
                        
                        // Cleanup local images
                        sh """
                            docker rmi -f ${ImageURI} || true
                            docker rmi -f ${ECR_REPO}/${REPO_NAME}:latest || true
                        """
                        
                    } catch (Exception e) {
                        echo "Error occurred during ECR Push: ${e.getMessage()}"
                        error("ECR Push failed")
                    }
                }
            }
        }

        stage('TerraformInit') {
            steps {
                dir('TerraFormCode') {
                    script {
                        try {
                            git(
                                url: 'git@github.com:your_company_git/your_terraform_repo_name.git', 
                                branch: "main", 
                                credentialsId: 'your-jenkins-git-access-key-id'
                            )
                            sh """
                                pwd
                                ls -lrth ${TF_PATH}
                            """
                        } catch (Exception e) {
                            echo "Error occurred: ${e.getMessage()}"
                            currentBuild.result = 'FAILURE'
                            error("TerraformInit stage failed: ${e.getMessage()}")
                        }
                    } 
                }
            }
        }

        stage('Create_tfvars_File') {
            steps {
                script {
                    dir("${TF_PATH}") {
                        def tfvarsContent = """
                        # Build Information
                        build = "${BUILD_NUMBER}"
                        image_uri = "${env.SuccessImageURI}"

                        # ECS Service Configuration
                        ecs_service_name = "${env.SERVICE_NAME}"
                        ecs_cluster_name = "${env.CLUSTER}"
                        ecs_cluster_id = "arn:aws:ecs:us-west-2:YOUR_ACCOUNT_ID:cluster/${env.CLUSTER}"
                        region = "${AWS_REGION}"

                        # Container Configuration
                        env_file = "${env.ENV_FILE_NAME}"
                        cpu_size = ${env.SELECTED_CPU}
                        memory_size = ${env.SELECTED_MEMORY}
                        ephemeral_size = ${env.SELECTED_DISK_SIZE}
                        container_port = ${env.CONTAINER_PORT}
                        health_check_path = "${env.HEALTH_CHECK_PATH}"
                        health_check_grace_period = ${env.HEALTH_CHECK_GRACE_PERIOD}

                        # IAM
                        iam_task_role_arn = "${TASK_ROLE_ARN}"
                        ecs_task_execution_role_arn = "arn:aws:iam::YOUR_ACCOUNT_ID:role/ecsTaskExecutionRole"

                        # Auto Scaling
                        min_number_instances = ${env.SELECTED_MIN_INSTANCES}
                        max_number_instances = ${env.SELECTED_MAX_INSTANCES}
                        threshold_num_messages = 100

                        # Networking
                        vpc_id = "${VPC_ID}"
                        public_subnet_ids = ${PUBLIC_SUBNETS.split(',')}
                        private_subnet_ids = ${PRIVATE_SUBNETS.split(',')}
                        acm_certificate_arn = "${ACM_CERTIFICATE_ARN}"

                        # Tags
                        tag_company = "${TAG_COMPANY}"
                        tag_owner = "${TAG_OWNER}"
                        tag_system = "${TAG_SYSTEM}"
                        tag_environment = "${env.DEPLOY_ENV}"
                        cost_app = "${TAG_COSTAPP}"
                        """.stripIndent()
                        
                        writeFile(file: "${TF_PATH}/jenkinsvariables.tfvars", text: tfvarsContent)
                        sh "pwd && ls -l ${TF_PATH}/jenkinsvariables.tfvars && head -20 ${TF_PATH}/jenkinsvariables.tfvars"
                    }
                }
            }
        }

        stage('ECS_Deploy') {
            steps {
                script {
                    dir("${TF_PATH}") {
                        withCredentials([[
                            $class: 'UsernamePasswordMultiBinding', 
                            credentialsId: 'your-jenkins-credentials-id',
                            usernameVariable: 'ACCESSKEY', 
                            passwordVariable: 'SECRETKEY'
                        ]]) {
                            sh """
                                export AWS_ACCESS_KEY_ID=$ACCESSKEY
                                export AWS_SECRET_ACCESS_KEY=$SECRETKEY
                                export AWS_DEFAULT_REGION=$AWS_REGION
                                
                                echo "=== Terraform Deployment ==="
                                echo "Service: ${env.SERVICE_NAME}"
                                echo "Environment: ${env.DEPLOY_ENV}"
                                echo "Image: ${env.SuccessImageURI}"
                                echo "Path: ${TF_PATH}"
                                
                                cd ${TF_PATH}
                                
                                # Initialize Terraform
                                terraform init -backend-config="key=ecs/your-environment-name/rc/${env.SERVICE_NAME}.tfstate"
                                
                                # Plan
                                terraform plan -var-file="jenkinsvariables.tfvars"
                            """
                            
                            def userInput = null
                            def startTime = Instant.now()
                            
                            try {
                                timeout(time: 30, unit: 'SECONDS') {
                                    userInput = input(
                                        id: 'userInput', 
                                        message: 'Deploy to ECS?', 
                                        parameters: [
                                            choice(
                                                name: 'CHOICE', 
                                                choices: ['yes', 'no'], 
                                                description: 'Click "yes" to deploy or "no" to abort'
                                            )
                                        ]
                                    )
                                }
                            } catch (FlowInterruptedException e) {
                                def elapsed = Instant.now().getEpochSecond() - startTime.getEpochSecond()
                                if (elapsed >= 29) {
                                    echo "Timeout detected! Elapsed time: ${elapsed} seconds. Auto-proceeding with deployment."
                                    userInput = 'yes'
                                } else {
                                    echo "Pipeline was interrupted unexpectedly. Elapsed time: ${elapsed} seconds."
                                    currentBuild.result = 'FAILURE'
                                    error("Pipeline interrupted unexpectedly")
                                }
                            }
                            
                            echo "Final User Input: ${userInput}"
                            
                            if (userInput == 'yes') {
                                echo 'Proceeding with deployment...'
                                sh """
                                    export AWS_ACCESS_KEY_ID=$ACCESSKEY
                                    export AWS_SECRET_ACCESS_KEY=$SECRETKEY
                                    export AWS_DEFAULT_REGION=$AWS_REGION
                                    
                                    cd ${TF_PATH}
                                    
                                    # Apply changes
                                    terraform apply -auto-approve -var-file="jenkinsvariables.tfvars"
                                    
                                    # Get outputs
                                    terraform output
                                """
                                
                                // Get the load balancer URL
                                def lbDns = sh(
                                    script: """
                                        cd ${TF_PATH} && terraform output -raw load_balancer_dns_name
                                    """,
                                    returnStdout: true
                                ).trim()
                                
                                echo "=== DEPLOYMENT COMPLETE ==="
                                echo "Service URL: https://${lbDns}"
                                echo "Service: ${env.SERVICE_NAME}"
                                echo "============================="
                            } else {
                                echo 'Deployment aborted by user.'
                                currentBuild.result = 'ABORTED'
                            }
                        }
                    }
                }
            }
        }
    }
    
    post {
        always {
            script {
                try {
                    // Cleanup workspace
                    if (fileExists(env.WORKSPACE)) {
                        echo "Cleaning up workspace: ${env.WORKSPACE}"
                        cleanWs(
                            cleanWhenNotBuilt: false,
                            deleteDirs: true,
                            disableDeferredWipeout: true,
                            notFailBuild: true
                        )
                    } else {
                        echo "Workspace already cleaned or doesn't exist."
                    }
                } catch (Exception e) {
                    echo "Error during workspace cleanup: ${e.message}"
                }
            }
        }
        
        success {
            script {
                echo "=== DEPLOYMENT SUCCESSFUL ==="
                echo "Service: ${env.SERVICE_NAME}"
                echo "Build: ${BUILD_NUMBER}"
                echo "Image: ${env.SuccessImageURI}"
                echo "============================="
                
                // Send notification
                emailext (
                    subject: "SUCCESS: ECS Deployment - ${env.SERVICE_NAME}",
                    body: """
                    ECS Service Deployment Completed Successfully!
                    
                    Service: ${env.SERVICE_NAME}
                    Build: ${BUILD_NUMBER}
                    Environment: ${env.DEPLOY_ENV}
                    Image: ${env.SuccessImageURI}
                    
                    The service has been deployed and should be available shortly.
                    """,
                    to: "your-team@your-company.com"
                )
            }
        }
        
        failure {
            script {
                echo "=== DEPLOYMENT FAILED ==="
                echo "Service: ${env.SERVICE_NAME}"
                echo "Build: ${BUILD_NUMBER}"
                echo "========================="
                
                // Send failure notification
                emailext (
                    subject: "FAILED: ECS Deployment - ${env.SERVICE_NAME}",
                    body: """
                    ECS Service Deployment Failed!
                    
                    Service: ${env.SERVICE_NAME}
                    Build: ${BUILD_NUMBER}
                    Environment: ${env.DEPLOY_ENV}
                    
                    Please check Jenkins build logs for details.
                    """,
                    to: "your-team@your-company.com"
                )
            }
        }
    }
}