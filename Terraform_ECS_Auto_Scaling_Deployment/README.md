# Terraform ECS Auto Scaling Deployment with Jenkins

This repository provides a complete solution for deploying an AWS ECS Fargate service using Jenkins and Terraform. The setup supports auto-scaling based on SQS queue depth, CloudWatch alarms, dynamic tfvars file generation, and Docker image automation with ECR.

---

## 🧱 Components

### 1. **Terraform Code Module**
- Manages ECS service, task definition, autoscaling, CloudWatch alarms, and log groups.
- Configured to use an S3 backend for remote state storage.
- All resources are dynamically tagged and parameterized.

### 2. **Jenkins Groovy Pipeline**
- Pulls source and Terraform modules from GitHub.
- Dynamically builds Docker image from `Dockerfile`.
- Pushes image to Amazon ECR.
- Dynamically creates `.tfvars` file from Jenkins parameters/environment.
- Initializes Terraform, plans, and applies the changes.
- Cleans up workspace and temporary files post-deployment.

---

## ⚙️ How It Works

1. **Initialize Jenkins Job** with parameters:
   - `TARGET_FUNCTION`, `branch`, and environment values.

2. **Pull Docker Source Code** from GitHub repository based on `TARGET_FUNCTION`.

3. **Build Docker Image** using Dockerfile located in source repo.

4. **Push to ECR** with `jenkins-credentials-id-upload-to-ECR`.

5. **Generate tfvars File** dynamically using Jenkins env vars:
   - Includes CPU, memory, task role ARN, subnets, SQS name, etc.

6. **Pull Terraform Module** from GitHub.

7. **Terraform Init + Plan + Apply**
   - Uses the generated `.tfvars` file.
   - Deploys ECS service, registers it, sets up alarms & autoscaling.

8. **Cleanup**
   - Removes `.tfvars` and workspace to ensure clean state for next run.

---

## 📂 Directory Structure

```
terraform/
  └── ecs-autoscaling/
      ├── backend.tf
      ├── main.tf
      ├── variables.tf
      ├── ecs-service.tf
      ├── ecs-task-definition.tf
      ├── cloud-watch-alarm-high.tf
      ├── cloud-watch-alarm-low.tf
      └── ecs-autoscaling-target-group.tf

deploy/
  └── jenkins/
      └── scripts/
          └── your-service-name.gvy
```

---

## ✅ Prerequisites

- Jenkins with Docker and AWS CLI installed.
- IAM users with ECR push/pull, ECS, CloudWatch, and S3 access.
- Proper secrets set via Jenkins credentials.
- Source GitHub repo with Dockerfile.
- S3 bucket configured for remote Terraform state.

---

## 🔐 Jenkins Credentials Required

| ID                          | Purpose                    |
|----------------------------|----------------------------|
| `jenkins-credentials-id-to-access-git` | GitHub access           |
| `jenkins-credentials-id-upload-to-ECR` | AWS ECR authentication  |
| `terraform-iam-user`       | Terraform deployment       |

---

## 🚀 Example Jenkins Run

```bash
TARGET_FUNCTION = your-service-name
branch = master
```

The pipeline will:
- Build `your-service-name` Docker image
- Push to `ECR_REPO`
- Deploy ECS service via Terraform
- Scale in/out based on SQS metrics

---

## 🧹 Cleanup

- Jenkins workspace is cleaned after each job.
- `.tfvars` file is temporary and removed post-deploy.

---

## 🤝 Contribution

Feel free to fork and extend the setup for your own services. Contributions and suggestions are welcome.

---

## 🧑‍💻 Author
**Md. Sarowar Alam**  
Lead DevOps Engineer, Hogarth Worldwide  
📧 Email: sarowar@hotmail.com  
🔗 LinkedIn: [linkedin.com/in/sarowar](https://www.linkedin.com/in/sarowar/)

---
