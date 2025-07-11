import java.time.Instant
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException
import org.jenkinsci.plugins.workflow.steps.TimeoutStepExecution
pipeline {

    agent { label 'built-in' }


parameters {
    choice(name: 'TARGET_FUNCTION', choices: 
        [
            'your-service-name',
        ], description: 'Select the target Service'
        )
    string(
        defaultValue: 'master', 
        description: 'Enter your Branch Name', 
        name: 'branch')
    }

    environment{

        ECR_REPO="123456789123.dkr.ecr.us-west-2.amazonaws.com"
        REPO_NAME = "${params.TARGET_FUNCTION}"
        TAG="${params.TARGET_FUNCTION}-${BUILD_NUMBER}"

        SOURCE_PATH="${WORKSPACE}/sourceCode/" 
        SCRIPT_PATH="${WORKSPACE}/deploy/jenkins/scripts/common/"
        AWS_REGION="us-west-2"

        CLUSTER="your-cluster-name"
        SERVICE_NAME="${params.TARGET_FUNCTION}"
        
        ENV_FILE_NAME="arn:aws:s3:::you-tf-state-bucket-name/ecs/${SERVICE_NAME}.env"
        
        SERVICE_SQS_NAME="your-sqs-name"
        DEPLOY_ENV="Environment"

        TASK_ROLE_ARN="arn:aws:iam::123456789123:role/your-role-name"

        TF_PATH="${WORKSPACE}/terraform/mb-tf/${SERVICE_NAME}/"
        EPHEMERAL_DISK_SIZE=21
        ImageURI= ""

        TASK_CPU_SIZE="1024"
        TASK_MEMORY_SIZE="2048"

        MIN_NUMBER_OF_CONTAINER=1
        MAX_NUMBER_OF_CONTAINER=2
        THRESHOLD_NUMBER_MESSAGE=5

        SECURITY_GROUP_NAME= "you-security-group-name"
        SUBNET_NAME = "your-subnet-name"

        TAG_COMPANY = "your-company-name"
        TAG_OWNER = "your-owner-name"
        TAG_SYSTEM = "your-system"
        TAG_ENV = "${DEPLOY_ENV}" 
        TAG_COSTAPP = "YOUR-COS-APP"
        
   }

  stages {

        stage('Selected Parameters') {
            steps {
                script {
                    // Get the values of the parameters
                    def choiceValue = params.TARGET_FUNCTION
                    def stringValue = params.branch
                    echo "TARGET_FUNCTION: ${choiceValue}"
                    echo "branch: ${stringValue}"
                }
            }
        }

    stage('Source Initialize') {
    steps {
        dir('sourceCode') {
            script {
                def selectedOption = params.TARGET_FUNCTION
                echo selectedOption
                if (selectedOption == 'your-service-name') {
                    git(url: 'git@github.com:your-git/your-git-name.git', branch: "${params.branch}", credentialsId: 'jenkins-credentials-id-to-access-git')
                    echo "Git Pulled from ${params.branch}, TF Path: ${TF_PATH}, Service Name: ${SERVICE_NAME}, Image: ${ECR_REPO}/${REPO_NAME}:${TAG}"
                
                    } else { echo "Service Docker File $selectedOption Missing "}
                }
            }
        }
    }

    stage('Container Build') {
            steps { 
                echo "${params.TARGET_FUNCTION}"
                sh '''
                    cd $SOURCE_PATH/your-docker-file-location
                    echo "Current Directory: $(pwd)"
                    echo "Target Function in Shell: \$TARGET_FUNCTION"
                    docker build -f Dockerfile -t \$TARGET_FUNCTION .
                    docker tag \$TARGET_FUNCTION:latest $ECR_REPO/$REPO_NAME:$TAG                    
                '''
            }
            post {
                success {
                  sh '''
                    echo "Build Done"
                  '''
                }
            }
        }    

    stage('ECR Login') {
            steps {
                script{
                    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'jenkins-credentials-id-upload-to-ECR',usernameVariable: 'ACCESSKEY', passwordVariable: 'SECRETKEY']]){
                    sh '''
                        export AWS_ACCESS_KEY_ID=$ACCESSKEY
                        export AWS_SECRET_ACCESS_KEY=$SECRETKEY
                        export AWS_DEFAULT_REGION=$AWS_REGION
                        /usr/local/bin/aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_REPO
                    
                    '''
                    }
                }
            }     
        }        


    stage('ECR Push') {
        steps {
            script {
                try {
                    sh '''
                        docker push $ECR_REPO/$REPO_NAME:$TAG
                    '''
                    ImageURI = "${ECR_REPO}/${REPO_NAME}:${TAG}"
                    echo "Docker Image Address: ${ImageURI}"
                    def imageID = sh(script: "docker images -q $ImageURI", returnStdout: true).trim()
                    if (imageID) {
                        echo "Docker Iamge ID: $imageID"
                        def removeImageCommand = "docker rmi -f $imageID || echo 'Image not found or already removed'"
                        def result = sh(script: removeImageCommand, returnStatus: true)
                        if (result == 0) { echo "Image removed successfully" } 
                        else {echo "Failed to remove image"}
                    } else { echo "Image not found"}
                } catch (Exception e) {
                    echo "Error occurred during ECR Push: ${e.getMessage()}"
                    error("ECR Push failed.")
                }
            }
        }
    }

    stage('Create tfvars File') {
        steps {
            script {
                dir("terraform"){

                    def tfvarsContent = """
                    build = "${BUILD_NUMBER}"
                    image_uri = "${ImageURI}"
                    ecs_service_name = "${SERVICE_NAME}"
                    region = "${AWS_REGION}"
                    env = "${DEPLOY_ENV}"
                    env_file = "${ENV_FILE_NAME}"
                    cpuSize = ${TASK_CPU_SIZE}
                    memorySize = ${TASK_MEMORY_SIZE}
                    iam_task_role_arn = "${TASK_ROLE_ARN}"
                    ecs_cluster_name = "${CLUSTER}"
                    ephemeral_size = ${EPHEMERAL_DISK_SIZE}
                    ecs_cluster_id = "arn:aws:ecs:us-west-2:123456789123:cluster/${CLUSTER}"
                    min_number_Instances = ${MIN_NUMBER_OF_CONTAINER}
                    max_number_Instances = ${MAX_NUMBER_OF_CONTAINER}
                    threshold_num_messages = ${THRESHOLD_NUMBER_MESSAGE}
                    sqs_name = "${SERVICE_SQS_NAME}"
                    tag_company = "${TAG_COMPANY}"
                    tag_owner = "${TAG_OWNER}"
                    tag_system = "${TAG_SYSTEM}"
                    tag_environment = "${TAG_ENV}"
                    cost_app = "${TAG_COSTAPP}"
                    sg_name = "${SECURITY_GROUP_NAME}"
                    subnet_names = "${SUBNET_NAME}"
                    """
                    
                    writeFile(file: "${WORKSPACE}/jenkinsvariables.tfvars", text: tfvarsContent)
                    sh "pwd && ls -l ${WORKSPACE}/jenkinsvariables.tfvars && cat ${WORKSPACE}/jenkinsvariables.tfvars"

                }
            }
        }
    }    

        stage('ECS Deploy'){
            steps{
                script{
                    dir("terraform"){
                        git (url: 'git@github.com:your-git/your-git-name.git',branch: 'main',credentialsId: 'jenkins-credentials-id-to-access-git')
                        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'terraform-iam-user',usernameVariable: 'ACCESSKEY', passwordVariable: 'SECRETKEY']])
                        {
                            sh """
                            export AWS_ACCESS_KEY_ID=$ACCESSKEY
                            export AWS_SECRET_ACCESS_KEY=$SECRETKEY
                            export AWS_DEFAULT_REGION=$AWS_REGION
                            echo "Path is: ${TF_PATH}"
                            echo "Environment is: ${DEPLOY_ENV}"
                            echo "Image is ${ImageURI}"
                            pwd
                            cd ${TF_PATH}
                            pwd
                            terraform init
                            terraform plan  -var-file="${WORKSPACE}/jenkinsvariables.tfvars"
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
                                    echo "⏳ Timeout detected! Elapsed time: ${Instant.now().minusSeconds(startTime.getEpochSecond()).getEpochSecond()} seconds. Setting userInput to 'yes'."
                                    userInput = 'yes'
                                } else 
                                {
                                    echo "🚨 Error: Pipeline was interrupted but NOT due to a timeout. Elapsed time: ${Instant.now().minusSeconds(startTime.getEpochSecond()).getEpochSecond()} seconds."
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
                            echo "Image is ${ImageURI}"
                            cd ${TF_PATH}
                            terraform init
                            terraform plan  -var-file="${WORKSPACE}/jenkinsvariables.tfvars"
                            terraform apply --auto-approve -var-file="${WORKSPACE}/jenkinsvariables.tfvars"
                            ls -lrth ${WORKSPACE}/jenkinsvariables.tfvars
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
                    if (fileExists(env.WORKSPACE)) {
                        echo "Cleaning up workspace: ${env.WORKSPACE}"
                        deleteDir()
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
                    echo "Error during workspace cleanup: ${e.message}"
                }
            }
        }
    }


}
