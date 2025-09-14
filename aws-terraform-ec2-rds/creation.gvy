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


        stage('CheckoutGitRepository') {
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
                terraform validate
                terraform plan
                '''
            }
        }
        
        stage('TerraformApply') {
            when {
                    expression { IS_CREATE } // Proceed only if validity is less 
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

        stage('TerraformDestroy') {
            when {
                    expression { IS_DELETE } // Proceed only if validity is less 
                }   
            steps {
                timeout(time: 30, unit: 'MINUTES') {
                    bat '''
                    echo Destroying Terraform configuration...
                    cd aws-terraform-ec2-rds
                    terraform init -reconfigure
                    terraform destroy -auto-approve tfplan
                    '''
                }
            }
        }

    }
    
    post{
        always{
            script{
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