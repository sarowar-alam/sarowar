pipeline {
    agent {
        label 'windows'  // Use Windows agent
    }
    
    environment {
        TF_VAR_environment = 'prod'
        TF_VAR_region = 'ap-south-1'
        TF_VAR_vpc_cidr = '10.0.0.0/16'
        TF_VAR_public_subnet_cidrs = '["10.0.1.0/24", "10.0.2.0/24"]'
        TF_VAR_private_subnet_cidrs = '["10.0.3.0/24", "10.0.4.0/24"]'
        TF_VAR_allowed_ips = '["YOUR_PUBLIC_IP/32"]'  // Replace with your actual IP
        TF_VAR_key_name = 'ostad-keypair'
        TF_VAR_bastion_instance_type = 't3.medium'
        TF_VAR_windows_instance_type = 't3.medium'
        TF_VAR_ubuntu_instance_type = 't3.micro'
        TF_VAR_mariadb_database_name = 'mariadbdemo'
        TF_VAR_sqlserver_database_name = 'sqlserverdemo'
    }
    
    stages {
        stage('Checkout Git Repository') {
            steps {
                bat 'echo Checking out Terraform code from Git...'
                git branch: 'main', 
                    url: 'https://github.com/your-username/your-terraform-repo.git'
                
                bat 'echo Current directory: %CD%'
                bat 'dir'
            }
        }
        
        
        stage('Terraform Init') {
            steps {
                bat '''
                echo Initializing Terraform...
                terraform init -reconfigure
                '''
            }
        }
        
        stage('Terraform Validate') {
            steps {
                bat '''
                echo Validating Terraform configuration...
                terraform validate
                '''
            }
        }
        
        stage('Terraform Plan') {
            steps {
                bat '''
                echo Creating execution plan...
                terraform plan -out=tfplan
                '''
                archiveArtifacts artifacts: 'tfplan', fingerprint: true
            }
        }
        
        stage('Terraform Apply') {
            steps {
                timeout(time: 30, unit: 'MINUTES') {
                    input message: 'Apply Terraform changes?', ok: 'Apply'
                    bat '''
                    echo Applying Terraform configuration...
                    terraform apply -auto-approve tfplan
                    '''
                }
            }
        }
        
        stage('Output Results') {
            steps {
                bat '''
                echo Generating outputs...
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