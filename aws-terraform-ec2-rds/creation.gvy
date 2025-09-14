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

}