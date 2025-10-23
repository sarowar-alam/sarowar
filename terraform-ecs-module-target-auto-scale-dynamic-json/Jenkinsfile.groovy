pipeline {
    agent any
    
    environment {
        AWS_ACCOUNT_ID = 'your-aws-account-id'
        AWS_REGION = 'us-east-1'
        ECR_REPO_NAME = 'cpu-load-test'
        PROJECT_NAME = 'cpu-load-test-app'
        TERRAFORM_DIR = 'terraform'
        DOCKER_DIR = '.'
    }
    
    parameters {
        string(name: 'AWS_ACCESS_KEY_ID', defaultValue: '', description: 'AWS Access Key ID')
        string(name: 'AWS_SECRET_ACCESS_KEY', defaultValue: '', description: 'AWS Secret Access Key')
        choice(name: 'TERRAFORM_ACTION', choices: ['apply', 'destroy'], description: 'Terraform action to perform')
    }
    
    stages {
        stage('Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/your-username/terraform-ecs-module-target-auto-scale-dynamic-json.git'
            }
        }
        
        stage('Configure AWS Credentials') {
            steps {
                script {
                    withCredentials([
                        string(credentialsId: 'aws-access-key-id', variable: 'AWS_ACCESS_KEY_ID'),
                        string(credentialsId: 'aws-secret-access-key', variable: 'AWS_SECRET_ACCESS_KEY')
                    ]) {
                        sh """
                            aws configure set aws_access_key_id ${AWS_ACCESS_KEY_ID}
                            aws configure set aws_secret_access_key ${AWS_SECRET_ACCESS_KEY}
                            aws configure set region ${AWS_REGION}
                            aws configure set output json
                        """
                    }
                }
            }
        }
        
        stage('Build Docker Image') {
            steps {
                dir(env.DOCKER_DIR) {
                    script {
                        docker.build("${ECR_REPO_NAME}:${BUILD_ID}")
                    }
                }
            }
        }
        
        stage('Push to ECR') {
            steps {
                script {
                    // Login to ECR
                    sh """
                        aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com
                    """
                    
                    // Create ECR repository if it doesn't exist
                    sh """
                        aws ecr describe-repositories --repository-names ${ECR_REPO_NAME} || aws ecr create-repository --repository-name ${ECR_REPO_NAME}
                    """
                    
                    // Tag and push image
                    sh """
                        docker tag ${ECR_REPO_NAME}:${BUILD_ID} ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO_NAME}:${BUILD_ID}
                        docker tag ${ECR_REPO_NAME}:${BUILD_ID} ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO_NAME}:latest
                        docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO_NAME}:${BUILD_ID}
                        docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO_NAME}:latest
                    """
                    
                    // Store ECR image URL
                    env.ECR_IMAGE_URL = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO_NAME}:${BUILD_ID}"
                }
            }
        }
        
        stage('Terraform Init') {
            steps {
                dir(env.TERRAFORM_DIR) {
                    sh 'terraform init'
                }
            }
        }
        
        stage('Terraform Plan') {
            steps {
                dir(env.TERRAFORM_DIR) {
                    sh """
                        terraform plan \
                          -var="aws_region=${AWS_REGION}" \
                          -var="project_name=${PROJECT_NAME}" \
                          -var="ecr_image_url=${env.ECR_IMAGE_URL}" \
                          -var="container_name=web-app" \
                          -var="task_cpu=256" \
                          -var="task_memory=512" \
                          -var="desired_count=1" \
                          -out=tfplan
                    """
                }
            }
        }
        
        stage('Terraform Apply/Destroy') {
            steps {
                dir(env.TERRAFORM_DIR) {
                    script {
                        if (params.TERRAFORM_ACTION == 'apply') {
                            sh 'terraform apply -auto-approve tfplan'
                        } else if (params.TERRAFORM_ACTION == 'destroy') {
                            sh """
                                terraform destroy \
                                  -var="aws_region=${AWS_REGION}" \
                                  -var="project_name=${PROJECT_NAME}" \
                                  -var="ecr_image_url=${env.ECR_IMAGE_URL}" \
                                  -var="container_name=web-app" \
                                  -var="task_cpu=256" \
                                  -var="task_memory=512" \
                                  -var="desired_count=1" \
                                  -auto-approve
                            """
                        }
                    }
                }
            }
        }
        
        stage('Get Outputs') {
            when {
                expression { params.TERRAFORM_ACTION == 'apply' }
            }
            steps {
                dir(env.TERRAFORM_DIR) {
                    script {
                        def albDns = sh(
                            script: 'terraform output -raw alb_dns_name',
                            returnStdout: true
                        ).trim()
                        
                        echo "Application Load Balancer URL: http://${albDns}"
                        env.ALB_URL = "http://${albDns}"
                    }
                }
            }
        }
    }
    
    post {
        always {
            script {
                // Clean up Docker images
                sh "docker rmi ${ECR_REPO_NAME}:${BUILD_ID} || true"
                sh "docker rmi ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO_NAME}:${BUILD_ID} || true"
            }
        }
        success {
            script {
                if (params.TERRAFORM_ACTION == 'apply' && env.ALB_URL) {
                    echo "Deployment successful! Access your application at: ${env.ALB_URL}"
                }
            }
        }
    }
}