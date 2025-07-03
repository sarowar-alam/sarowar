# Jenkins ECS Service Discovery Script

This repository contains a Jenkins Groovy script that dynamically retrieves **non-production ECS service ARNs** from a specified **AWS ECS cluster** using the AWS CLI and credentials stored in Jenkins.

## 🔧 Features

- Automatically fetches ECS service ARNs from a given cluster
- Filters out services containing the word `"prod"` (case-insensitive)
- Sorts the service list alphabetically
- Securely retrieves AWS credentials from Jenkins credential store
- Designed to work as a **dynamic dropdown parameter** or script in Jenkins pipelines

## 📁 File

- `dynamic-drop-down-jenkins/dynamic-drop-down.gvy`: Main script used in Jenkins scripted pipeline or Active Choices parameters

## ✅ Prerequisites

- Jenkins installed and running
- AWS CLI installed on the Jenkins host:

- AWS credentials configured in Jenkins (`StandardUsernamePasswordCredentials`)
- A valid ECS cluster with services deployed

C:\Program Files\Amazon\AWSCLIV2\aws.exe

## 🔐 Jenkins Credentials

The script looks for an AWS credential entry in Jenkins with a specific ID:
```groovy
def awsCred = creds.find { it.id == '123456-98765-1243-fgrtr-123456789' }

🌐 AWS Configuration
Region: us-west-2 (default)
ECS Cluster Name: fargate-container-cluster

def cluster = "your-cluster-name"
...
"AWS_DEFAULT_REGION=your-region"
🧪 Sample Output
[
  "arn:aws:ecs:us-west-2:123456789:service/test-api" : "test-api",
  "arn:aws:ecs:us-west-2:123456789:service/dev-worker" : "dev-worker"
]
📝 Notes
Services containing the word prod in their ARN are excluded.

The script can be extended to support multiple clusters or regions.

Ensure the Jenkins server has permission to run the AWS CLI and access the ECS cluster.

📜 License
MIT License — feel free to adapt and use.

🔗 [View the Jenkins Dynamic Dropdown Script](https://github.com/sarowar-alam/sarowar/tree/main/dynamic-drop-down-jenkins)
