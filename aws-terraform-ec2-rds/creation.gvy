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

    }



}