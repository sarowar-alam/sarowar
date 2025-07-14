pipeline {
    agent any
   parameters 
   {
      string(defaultValue: 'your-fargate-cluster-name', description: 'ECS Cluster Name', name: 'ecs_cluster_name')
      string(defaultValue: '', description: 'ECS Name for Which ZERO TASK will be Implemented', name: 'ecs_service_name')
      string(defaultValue: '', description: 'ECS => SQS URL', name: 'sqs_url')
      string(defaultValue: 'arn:aws:iam::123456789123:role/your-role-name', description: 'Existing IAM Role arn', name: 'iam_role_arn')
      string(defaultValue: 'your-role-policy-name', description: 'Existing IAM Role Inline Policy Name', name: 'iam_role_policyname')
      
      string(defaultValue: 'Production', description: 'Tag Environment', name: 'my_environment')
      string(defaultValue: 'client-name', description: 'Tag System', name: 'my_system')
      string(defaultValue: 'Owner-Name', description: 'Tag System Owner', name: 'my_owner')
      string(defaultValue: 'YOUR_COST_APP_NAME', description: 'Tag Cost App', name: 'my_cost_app')            
      string(defaultValue: 'your-company-name', description: 'Tag Company', name: 'my_company')            
    }
    
    environment
    {
        SOURCE_PATH="${WORKSPACE}/sourceCode" 
        AWS_REGION="us-west-2"
        TF_PATH="${WORKSPACE}/terraform/ops-tf/ecs-zero-task-prod"

        CLOUD_WATCH_ALARM_ARN = ''
        LAMBDA_FUNCTION_ARN = ''
        LAMBDA_FUNCTION_STOP_ARN = ''
        STATE_MACHINE_ARN = ''
        SQS_ARN=''
        ECS_SERVICE_ARN=''
    }

  stages {

        stage('Terraform in Action'){
            steps{
                script{

                    dir("terraform"){
                        git (url: 'git@github.com:your-git/your-git-name.git',branch: 'main',credentialsId: 'your-credentials-to-access-got')
                        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'terraform-iam-user',usernameVariable: 'ACCESSKEY', passwordVariable: 'SECRETKEY']]){
                            echo "Starting sh ... "
                            sh """
                            echo "Path is: ${TF_PATH}"
                            export AWS_ACCESS_KEY_ID=$ACCESSKEY
                            export AWS_SECRET_ACCESS_KEY=$SECRETKEY
                            export AWS_DEFAULT_REGION=$AWS_REGION
                            cd ${TF_PATH}
                            terraform init
                            terraform validate
                            terraform workspace select -or-create  "${ecs_service_name}"
                            terraform plan  -var cluster_name="${ecs_cluster_name}" \
                                            -var ecs_Name="${ecs_service_name}" \
                                            -var sqs_url="${sqs_url}" \
                                            -var iam_role_arn="${iam_role_arn}" \
                                            -var tag_environment="${my_environment}" \
                                            -var tag_system="${my_system}" \
                                            -var tag_owner="${my_owner}" \
                                            -var cost_app="${my_cost_app}" \
                                            -var tag_company="${my_company}"
                            """ 
                            def userInput = input(
                                id: 'userInput',
                                message: 'Click "Proceed" to continue or "Abort" to stop:',
                                parameters: [choice(name: 'CHOICE', choices: 'yes', description: 'Click "Proceed" to continue or "Abort" to stop:')],
                                )
                           if (userInput=='yes') 
                           
                           {
                            
                            echo 'User clicked "Proceed", continuing...'
                            def terraformOutput = sh(script: """
                                export AWS_ACCESS_KEY_ID=$ACCESSKEY
                                export AWS_SECRET_ACCESS_KEY=$SECRETKEY
                                export AWS_DEFAULT_REGION=$AWS_REGION
                                cd ${TF_PATH}
                                echo "Path is: ${TF_PATH}"
                                terraform init -upgrade
                                terraform workspace select -or-create  "${ecs_service_name}"
                                terraform plan  -var cluster_name="${ecs_cluster_name}" \
                                            -var ecs_Name="${ecs_service_name}" \
                                            -var sqs_url="${sqs_url}" \
                                            -var iam_role_arn="${iam_role_arn}" \
                                            -var tag_environment="${my_environment}" \
                                            -var tag_system="${my_system}" \
                                            -var tag_owner="${my_owner}" \
                                            -var cost_app="${my_cost_app}" \
                                            -var tag_company="${my_company}"
                                terraform apply -auto-approve -var cluster_name="${ecs_cluster_name}" \
                                            -var ecs_Name="${ecs_service_name}" \
                                            -var sqs_url="${sqs_url}" \
                                            -var iam_role_arn="${iam_role_arn}" \
                                            -var tag_environment="${my_environment}" \
                                            -var tag_system="${my_system}" \
                                            -var tag_owner="${my_owner}" \
                                            -var cost_app="${my_cost_app}" \
                                            -var tag_company="${my_company}"
                            """, returnStdout: true).trim()                   

                            // Regular expression to match each line and extract the value after the '=' sign
                            def pattern = /(\w+)\s*=\s*"([^"]+)"/
                            terraformOutput.split('\n').each { line ->
                                def cleanedLine = line.replaceAll('\u001B\\[0m', '').trim()
                                if (cleanedLine && cleanedLine != "Outputs:") {
                                    def matcher = (cleanedLine =~ pattern)
                                    if (matcher.matches()) {
                                        def key = matcher[0][1]
                                        def value = matcher[0][2]

                                        switch (key) {
                                            case 'cloud_watch_alarm_arn':
                                                CLOUD_WATCH_ALARM_ARN = value
                                                break
                                            case 'lambda_function_arn':
                                                LAMBDA_FUNCTION_ARN = value
                                                break
                                            case 'lambda_function_stop_arn':
                                                LAMBDA_FUNCTION_STOP_ARN = value
                                                break
                                            case 'sqs_queue_arn':
                                                SQS_ARN = value
                                                break                                                
                                            case 'ecs_service_arn':
                                                ECS_SERVICE_ARN = value
                                                break                                                

                                        }
                                    }
                                }
                            }
                            // Print or use the environment variables as needed
                            println "CLOUD_WATCH_ALARM_ARN_STOP: $CLOUD_WATCH_ALARM_ARN"
                            println "LAMBDA_FUNCTION_ARN: $LAMBDA_FUNCTION_ARN"
                            println "LAMBDA_FUNCTION_STOP_ARN: $LAMBDA_FUNCTION_STOP_ARN"
                            println "ECS Service ARN: $ECS_SERVICE_ARN"
                            println "SQS ARN: $SQS_ARN"                            
                        }                   
                    }
                }
                
            }
        }
    }
    stage('Use Terraform Outputs') {
    steps {
        script {
            dir("terraform"){
            sh """
            cd ${TF_PATH}
            /bin/python update_iam_role.py "${iam_role_arn}" "${iam_role_policyname}" "$ECS_SERVICE_ARN" "$SQS_ARN" "$LAMBDA_FUNCTION_ARN"
            """
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
