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






    }






}