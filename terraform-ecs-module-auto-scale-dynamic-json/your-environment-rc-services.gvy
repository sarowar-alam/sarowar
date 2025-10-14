import java.time.Instant
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException
import org.jenkinsci.plugins.workflow.steps.TimeoutStepExecution

def ECS_CHOICES = [
    'your-ecs-service-name-01',
    'your-ecs-service-name-02',
    'your-ecs-service-name-03',
    'your-ecs-service-name-N'  // Add more ECS service names as needed

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
        defaultValue: 'RC', 
        description: 'Enter your Branch Name', 
        name: 'branch')      
    }

    environment {
        JSON_FILE_PATH = "terraform-ecs-module-auto-scale-dynamic-json/ecs-services.json"

        AWS_REGION="us-west-2"
        ECR_REPO="YOUR_ACCOUNT_ID.dkr.ecr.us-west-2.amazonaws.com"
        REPO_NAME = "${params.ECS_GLOBAL}"
        TAG="${params.ECS_GLOBAL}-${BUILD_NUMBER}"
        SOURCE_PATH="${WORKSPACE}/sourceCode" 

        TASK_ROLE_ARN="arn:aws:iam::YOUR_ACCOUNT_ID:role/Name_of_Your_Task_Role"
        TF_PATH="${WORKSPACE}/TerraFormCode/terraform-ecs-module-auto-scale-dynamic-json"

        SECURITY_GROUP_NAME= "Your_Security_Group_Name"
        SUBNET_NAME = "Your_Subnet_Name"

        TAG_COMPANY = "Your_Company_Name"
        TAG_OWNER = "Your_Owner_Name"
        TAG_SYSTEM = "YOur_System_Name"
        TAG_COSTAPP = "Your_Cost_Application"        
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
                            echo "Threshold: ${selectedService.threshold}"
                            echo "SQS: ${selectedService.sqs}"
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
                            env.THRESHOLD_NUMBER_MESSAGE = selectedService.threshold.toString()
                            env.SERVICE_SQS_NAME = selectedService.sqs.toString()
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
                    echo "Instance Range: ${env.SELECTED_MIN_INSTANCES} - ${env.SELECTED_MAX_INSTANCES}"
                    echo "Ready for deployment"
                }
            }
        }

        stage('ContainerBuild') {
            steps {
                dir('sourceCode') {
                    script{

                        def selectedOption = params.ECS_GLOBAL
                        echo selectedOption
                        try {
                            git(url: 'git@github.com:your_company_git/your_git_repo_name.git', branch: "${params.branch}", credentialsId: 'jenkins-git-access-key-id')
                            echo "Git Pulled from ${params.branch}, Service Name: ${params.ECS_GLOBAL}, Docker file path: ${env.SELECTED_DOCKER_FILE_PATH} Image: ${ECR_REPO}/${REPO_NAME}:${TAG}"
                            sh """
                            pwd
                            ls -lrth
                            """
                        } catch (Exception e) {
                            echo "Error occurred: ${e.getMessage()}"
                            currentBuild.result = 'FAILURE'
                            error("TerraformInit stage failed: ${e.getMessage()}")
                        }
                        echo "ECS Service Name: ${params.ECS_GLOBAL}, Building the Image as: ${ECR_REPO}/${REPO_NAME}:${TAG}"
                        sh """
                            cd ${SOURCE_PATH} || { echo "Failed to change directory to ${SOURCE_PATH}"; exit 1; }
                            echo "Building Docker image from: ${env.SELECTED_DOCKER_FILE_PATH}"
                            # Build the image
                            
                            if [[ "${env.SELECTED_DOCKER_FILE_PATH}" == *"Dockerfile"* ]]; then

                                echo "Building Image with ${env.SELECTED_DOCKER_FILE_PATH}"
                                if ! docker build -f ${env.SELECTED_DOCKER_FILE_PATH} -t ${env.ECS_GLOBAL}:latest -t ${ECR_REPO}/${REPO_NAME}:${TAG} .; then
                                echo "Docker build failed"
                                exit 1
                                fi
                            else
                                cd "${env.SELECTED_DOCKER_FILE_PATH}" || { echo "Failed to change directory to ${env.SELECTED_DOCKER_FILE_PATH}"; exit 1; }
                                echo "Building Image with Dockerfile"
                                if ! docker build -f Dockerfile -t ${env.ECS_GLOBAL}:latest -t ${ECR_REPO}/${REPO_NAME}:${TAG} .; then
                                echo "Docker build failed"
                                exit 1
                                fi                                
                            fi

                            echo "Docker images created:"
                            docker images | grep -E "(${env.ECS_GLOBAL}|${REPO_NAME})"
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
                            // Run Trivy scan and capture output
                            def trivyOutput = sh(
                                script: """
                                    set +e  # Continue on error to capture output even if vulnerabilities found
                                    trivy image --format table --exit-code 0 --severity CRITICAL,HIGH,MEDIUM ${ECR_REPO}/${REPO_NAME}:${TAG}
                                """,
                                returnStdout: true
                            ).trim()
                            
                            echo "=== TRIVY SCAN RESULTS ==="
                            echo trivyOutput
                            echo "=== END TRIVY SCAN RESULTS ==="
                            
                            // Count vulnerabilities by severity
                            def criticalCount = trivyOutput.count('CRITICAL')
                            def highCount = trivyOutput.count('HIGH')
                            def mediumCount = trivyOutput.count('MEDIUM')
                            
                            // Print summary
                            echo """
                            ===== TRIVY VULNERABILITY SUMMARY =====
                            CRITICAL Vulnerabilities: ${criticalCount}
                            HIGH Vulnerabilities: ${highCount}
                            MEDIUM Vulnerabilities: ${mediumCount}
                            =======================================
                            """
                            
                            // Quality gates - handle vulnerabilities based on severity
                            if (criticalCount > 0) {
                                error("Critical vulnerabilities detected in the image. Build failed.")
                            } else if (highCount > 0) {
                                error("High vulnerabilities detected in the image. Build failed.")
                            } else if (mediumCount > 0) {
                                error("Medium vulnerabilities detected in the image. Build failed.")
                            } else {
                                echo "No CRITICAL vulnerabilities detected."
                            }
                        } catch (Exception e) {
                            echo "Trivy scan failed: ${e.getMessage()}"
                            currentBuild.result = 'UNSTABLE'
                            // Don't fail the entire build, just mark as unstable
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
                                if ! /usr/local/bin/aws sts get-caller-identity; then
                                    echo "AWS credentials are invalid"
                                    exit 1
                                fi
                                
                                # Check if ECR repository exists, create if it doesn't
                                if ! /usr/local/bin/aws ecr describe-repositories --repository-names ${REPO_NAME} >/dev/null 2>&1; then
                                    echo "Creating ECR repository: ${REPO_NAME}"
                                    if ! /usr/local/bin/aws ecr create-repository \\
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
                                    
                                    # Update repository settings with error handling
                                    if ! /usr/local/bin/aws ecr put-image-tag-mutability \\
                                        --repository-name ${REPO_NAME} \\
                                        --image-tag-mutability MUTABLE; then
                                        echo "Warning: Failed to update image tag mutability"
                                    fi
                                    
                                    if ! /usr/local/bin/aws ecr put-image-scanning-configuration \\
                                        --repository-name ${REPO_NAME} \\
                                        --image-scanning-configuration scanOnPush=true; then
                                        echo "Warning: Failed to update image scanning configuration"
                                    fi
                                    
                                    echo "Repository settings updated"
                                fi
                                
                                # Login to ECR
                                if ! /usr/local/bin/aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_REPO; then
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
                        """
                        echo "Docker Image Address: ${ImageURI}"
                        
                        // SET THE ENVIRONMENT VARIABLE HERE - after successful push
                        env.SuccessImageURI = ImageURI
                        echo "Stored image URI in environment variable: ${env.SuccessImageURI}"
                        
                        // Cleanup local image
                        def imageID = sh(script: "docker images -q ${ImageURI}", returnStdout: true).trim()
                        if (imageID) {
                            echo "Docker Image ID: ${imageID}"
                            def removeImageCommand = "docker rmi -f ${imageID} || echo 'Image not found or already removed'"
                            def result = sh(script: removeImageCommand, returnStatus: true)
                            if (result == 0) { 
                                echo "Image removed successfully" 
                            } else {
                                echo "Failed to remove image (exit code: ${result})"
                            }
                        } else { 
                            echo "Image not found locally" 
                        }
                        
                    } catch (Exception e) {
                        echo "Error occurred during ECR Push: ${e.getMessage()}"
                        
                        // Attempt cleanup even on failure
                        try {
                            def ImageURI = "${ECR_REPO}/${REPO_NAME}:${TAG}"
                            def imageID = sh(script: "docker images -q ${ImageURI}", returnStdout: true).trim()
                            if (imageID) {
                                sh "docker rmi -f ${imageID} || true"
                                echo "Cleaned up image after failed push"
                            }
                        } catch (Exception cleanupError) {
                            echo "Warning: Failed to cleanup image after push failure: ${cleanupError.getMessage()}"
                        }
                        
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
                            git (url: 'git@github.com:your_company_git/your_terraform_repo_name.git', branch: "main", credentialsId: 'your-jenkins-git-access-key-id')
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
                        build = "${BUILD_NUMBER}"
                        image_uri = "${env.SuccessImageURI}"

                        ecs_service_name = "${env.SERVICE_NAME}"
                        region = "${AWS_REGION}"

                        env_file = "${env.ENV_FILE_NAME}"
                        cpu_size = ${env.SELECTED_CPU}
                        memory_size = ${env.SELECTED_MEMORY}
                        iam_task_role_arn = "${TASK_ROLE_ARN}"
                        ecs_cluster_name = "${env.CLUSTER}"
                        ephemeral_size = ${env.SELECTED_DISK_SIZE}
                        ecs_cluster_id = "arn:aws:ecs:us-west-2:YOUR_AWS_ACCOUNT_ID:cluster/${env.CLUSTER}"
                        min_number_Instances = ${env.SELECTED_MIN_INSTANCES}
                        max_number_Instances = ${env.SELECTED_MAX_INSTANCES}
                        threshold_num_messages = ${env.THRESHOLD_NUMBER_MESSAGE}
                        sqs_name = "${env.SERVICE_SQS_NAME}"
                        tag_company = "${TAG_COMPANY}"
                        tag_owner = "${TAG_OWNER}"
                        tag_system = "${TAG_SYSTEM}"
                        tag_environment = "${env.DEPLOY_ENV}"
                        cost_app = "${TAG_COSTAPP}"
                        sg_name = "${SECURITY_GROUP_NAME}"
                        subnet_names = "${SUBNET_NAME}"
                        """.stripIndent()
                        
                        writeFile(file: "${TF_PATH}/jenkinsvariables.tfvars", text: tfvarsContent)
                        sh "pwd && ls -l ${TF_PATH}/jenkinsvariables.tfvars && cat ${TF_PATH}/jenkinsvariables.tfvars"
                    }
                }
            }
        }


        stage('ECS_Deploy'){
            steps{
                script{
                    dir("${TF_PATH}"){
                        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'your-jenkins-credentials-id',usernameVariable: 'ACCESSKEY', passwordVariable: 'SECRETKEY']])
                        {
                            sh """
                            export AWS_ACCESS_KEY_ID=$ACCESSKEY
                            export AWS_SECRET_ACCESS_KEY=$SECRETKEY
                            export AWS_DEFAULT_REGION=$AWS_REGION
                            echo "Path is: ${TF_PATH}"
                            echo "Environment is: ${env.DEPLOY_ENV}"
                            echo "Image is ${env.SuccessImageURI}"
                            pwd
                            cd ${TF_PATH}
                            pwd
                            terraform init -backend-config="key=ecs/your-environment-name/rc/${env.SERVICE_NAME}.tfstate" -var-file="${TF_PATH}/jenkinsvariables.tfvars"
                            terraform plan  -var-file="${TF_PATH}/jenkinsvariables.tfvars"
                            """ 
                            def userInput = null
                            def startTime = Instant.now()
                            try {
                                timeout(time: 30, unit: 'SECONDS') 
                                {
                                    userInput = input(id: 'userInput', message: 'Click "Proceed" to continue or "Abort" to stop:',parameters: [choice(name: 'CHOICE', choices: ['yes', 'no'], description: 'Click "Proceed" to continue or "Abort" to stop:')])
                                }
                            } catch (FlowInterruptedException e) {
                                if ((Instant.now().minusSeconds(startTime.getEpochSecond()).getEpochSecond()) > 29) 
                                {
                                    echo "Timeout detected! Elapsed time: ${Instant.now().minusSeconds(startTime.getEpochSecond()).getEpochSecond()} seconds. Setting userInput to 'yes'."
                                    userInput = 'yes'
                                } else 
                                {
                                    echo "Error: Pipeline was interrupted but NOT due to a timeout. Elapsed time: ${Instant.now().minusSeconds(startTime.getEpochSecond()).getEpochSecond()} seconds."
                                    currentBuild.result = 'FAILURE'  // Mark the pipeline as failed
                                    error("Pipeline interrupted unexpectedly before timeout.")
                                }
                            }
                            echo "Final User Input: ${userInput}"
                        if (userInput=='yes') {
                            echo 'User clicked "Proceed", continuing...'
                            sh """
                            export AWS_ACCESS_KEY_ID=$ACCESSKEY
                            export AWS_SECRET_ACCESS_KEY=$SECRETKEY
                            export AWS_DEFAULT_REGION=$AWS_REGION
                            echo "Path is: ${TF_PATH}"
                            echo "Image is ${env.SuccessImageURI}"
                            cd ${TF_PATH}
                            terraform init -backend-config="key=ecs/your-environment-name/rc/${env.SERVICE_NAME}.tfstate" -var-file="${TF_PATH}/jenkinsvariables.tfvars"
                            terraform plan  -var-file="${TF_PATH}/jenkinsvariables.tfvars"
                            terraform apply -auto-approve -var-file="${TF_PATH}/jenkinsvariables.tfvars"
                            ls -lrth ${TF_PATH}/jenkinsvariables.tfvars
                            """                         }  
                        } // End of With Credentials                  
                    }
                }
                
            }
        }



    }
    
    post {
        always {
            script {
                try {
                    // Check if the workspace directory exists
                    if (fileExists(env.WORKSPACE)) {
                        echo "Cleaning up workspace: ${env.WORKSPACE}"

                        // First approach: Delete all files and directories in the workspace
                        deleteDir()

                        // Second approach: Use cleanWs for more advanced cleanup with patterns
                        cleanWs(cleanWhenNotBuilt: false,
                                deleteDirs: true,
                                disableDeferredWipeout: true,
                                notFailBuild: true,
                                patterns: [[pattern: '.gitignore', type: 'INCLUDE'],
                                        [pattern: '.propsfile', type: 'EXCLUDE']])
                    } else {
                        echo "Workspace directory does not exist or already cleaned."
                    }
                } catch (Exception e) {
                    // Log the error but do not fail the build
                    echo "Error during workspace cleanup: ${e.message}"
                }
            }
        }
    }
}