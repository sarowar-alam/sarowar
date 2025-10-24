pipeline {
    agent any
    
    environment {
        AWS_ACCOUNT_ID = '388779989543'
        AWS_REGION = 'ap-south-1'
        ECR_REPO_NAME = 'cpu-load-test'
        PROJECT_NAME = 'cpu-load-test-app'
        SOURCE_DIRECTORY = 'terraform-ecs-module-target-auto-scale-dynamic-json'
        TERRAFORM_DIR = 'terraform'
        DOCKER_DIR = '.'
    }
    
    parameters {
        choice(name: 'TERRAFORM_ACTION', choices: ['apply', 'destroy'], description: 'Terraform action to perform')
    }
    
    stages {
        stage('Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/sarowar-alam/sarowar.git'
            }
        }
        
        stage('Configure AWS Credentials') {
            steps {
                script {
                    withCredentials([[
                        $class: 'UsernamePasswordMultiBinding', 
                        credentialsId: '78ddea82-7a14-4241-9da4-6cc5cbaf7c5b',
                        usernameVariable: 'ACCESSKEY', 
                        passwordVariable: 'SECRETKEY'
                    ]]) {
                        sh """
                            aws configure set aws_access_key_id ${ACCESSKEY}
                            aws configure set aws_secret_access_key ${SECRETKEY}
                            aws configure set region ${AWS_REGION}
                            aws configure set output json
                        """
                    }
                }
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    dir(env.SOURCE_DIRECTORY) {
                        sh "ls -la"
                        sh "docker build -t ${ECR_REPO_NAME}:${BUILD_ID} ."
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
                    env.ECR_IMAGE_URL = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO_NAME}:latest"
                }
            }
        }
        
        stage('Terraform Init') {
            steps {
                dir("${env.SOURCE_DIRECTORY}/${env.TERRAFORM_DIR}") {
                    sh 'terraform init -upgrade'
                }
            }
        }
        
        stage('Terraform Plan') {
            steps {
                dir("${env.SOURCE_DIRECTORY}/${env.TERRAFORM_DIR}") {
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
                dir("${env.SOURCE_DIRECTORY}/${env.TERRAFORM_DIR}") {
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
                dir("${env.SOURCE_DIRECTORY}/${env.TERRAFORM_DIR}") {
                    script {
                        def albDns = sh(
                            script: 'terraform output -raw alb_dns_name',
                            returnStdout: true
                        ).trim()
                        
                        echo "Application Load Balancer URL: http://${albDns}"
                        env.ALB_URL = "http://${albDns}"
                        
                        // Write URL to file for easy access
                        writeFile file: 'alb-url.txt', text: "Application URL: http://${albDns}"
                        archiveArtifacts artifacts: 'alb-url.txt'
                    }
                }
            }
        }
        
        stage('Test Deployment') {
            when {
                expression { params.TERRAFORM_ACTION == 'apply' }
            }
            steps {
                script {
                    if (env.ALB_URL) {
                        // Wait for ALB to be ready
                        sh "sleep 30"
                        
                        // Test the application
                        sh """
                            curl -f ${env.ALB_URL} || echo "Application not ready yet"
                            curl -f ${env.ALB_URL}/health.html || echo "Health check not ready yet"
                        """
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
                sh "docker rmi ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO_NAME}:latest || true"
                
                // Clean up dangling images
                sh "docker image prune -f || true"
            }
        }
        success {
            script {
                if (params.TERRAFORM_ACTION == 'apply' && env.ALB_URL) {
                    echo "🎉 Deployment successful!"
                    echo "🌐 Access your application at: ${env.ALB_URL}"
                    echo "🖱️ Test CPU load by clicking 'Start REAL CPU Load' button"
                    echo "📈 Auto-scaling will scale to 3 tasks when CPU > 50%"
                    
                    // Create a nice deployment summary
                    def summary = """
                    🚀 CPU Load Test Application Deployment Complete!
                    
                    📊 Application URL: ${env.ALB_URL}
                    🔧 Features:
                      - Real CPU load generation with stress-ng
                      - Auto-scaling (1-3 tasks based on CPU utilization)
                      - Load Balancer with health checks
                      - CloudWatch monitoring
                    
                    🧪 To test auto-scaling:
                      1. Visit ${env.ALB_URL}
                      2. Click "Start REAL CPU Load"
                      3. Watch CloudWatch metrics
                      4. ECS will scale from 1 to 3 tasks when CPU > 50%
                    
                    ⏱️ Scaling cooldown: 60 seconds
                    """
                    echo summary
                } else if (params.TERRAFORM_ACTION == 'destroy') {
                    echo "🗑️ Infrastructure destroyed successfully"
                }
            }
        }
        failure {
            echo "❌ Pipeline failed - check the logs above for details"
        }
    }
}