pipeline {
    agent { label 'built-in' }


    parameters {
        choice(
            name: 'TERRAFORM_ACTION',
            choices: [
                'Create',
                'Delete' 
            ],
            description: 'Whether you want to create / delete the environment'
        )
    }

    
    environment {

        IS_CREATE = false
        IS_DELETE = false             
    }
    
    stages {


        stage('InitializeVariables') {
            steps {
                script {

                    IS_CREATE = params.TERRAFORM_ACTION == 'Create'
                    IS_DELETE = params.TERRAFORM_ACTION == 'Delete'
                    echo "Creation is: ${IS_CREATE} | Deletion is: ${IS_DELETE}"
                }
            }
        }


        stage('Checkout Git Repository') {
            steps {
                bat 'echo Checking out Terraform code from Git...'
                git branch: 'main', 
                    url: 'https://github.com/sarowar-alam/sarowar.git'
                
                bat 'echo Current directory: %CD%'
                bat 'dir'
            }
        }
        
        
        stage('Terraform Init') {
            when {
                    expression { IS_CREATE } // Proceed only if validity is less 
                }             
            steps {
                bat '''
                cd aws-terraform-ec2-rds
                echo Initializing Terraform...
                terraform init -reconfigure
                '''
            }
        }
        
        stage('Terraform Validate') {
            when {
                    expression { IS_CREATE } // Proceed only if validity is less 
                }               
            steps {
                bat '''
                echo Validating Terraform configuration...
                cd aws-terraform-ec2-rds
                terraform validate
                '''
            }
        }
        
        stage('Terraform Plan') {
            when {
                    expression { IS_CREATE } // Proceed only if validity is less 
                }               
            steps {
                bat '''
                echo Creating execution plan...
                cd aws-terraform-ec2-rds
                terraform plan
                '''
            }
        }
        
        stage('Terraform Apply') {
            when {
                expression { env.IS_CREATE == true }
            }
            
            steps {
                timeout(time: 30, unit: 'MINUTES') {
                    script {
                        def userInput = input(
                            id: 'userInput',
                            message: 'Apply Terraform changes?',
                            parameters: [
                                choice(
                                    name: 'ACTION', 
                                    choices: 'Proceed\nAbort', 
                                    description: 'Choose to proceed with deployment or abort'
                                )
                            ]
                        )
                        
                        if (userInput == 'Proceed') {
                            echo 'User clicked "Proceed", applying Terraform changes...'
                            bat '''
                            echo Applying Terraform configuration...
                            cd aws-terraform-ec2-rds
                            terraform apply -auto-approve
                            '''
                        } else {
                            echo 'User chose to abort deployment'
                            currentBuild.result = 'ABORTED'
                            error('Deployment aborted by user')
                        }
                    }
                }
            }
        }

        stage('Terraform Destroy') {
            when {
                    expression { IS_DELETE } // Proceed only if validity is less 
                }   

            steps {
                timeout(time: 30, unit: 'MINUTES') {
                    input message: 'Apply Terraform changes?', ok: 'Apply'
                    bat '''
                    echo Destroying Terraform configuration...
                    cd aws-terraform-ec2-rds
                    terraform init -reconfigure
                    terraform destroy -auto-approve tfplan
                    '''
                }
            }
        }
        
        stage('Output Results') {
            when {
                    expression { IS_CREATE } // Proceed only if validity is less 
                }   

            steps {
                bat '''
                echo Generating outputs...
                cd aws-terraform-ec2-rds
                terraform output -json > terraform_output.json
                type terraform_output.json
                '''
                archiveArtifacts artifacts: 'terraform_output.json', fingerprint: true
            }
        }
    }
    
    post {
        always {
            bat 'echo Cleaning up workspace...'
            cleanWs()
        }
        success {
            bat 'echo Terraform deployment completed successfully!'
        }
        failure {
            bat 'echo Terraform deployment failed! Check the logs for details.'
        }
    }
}