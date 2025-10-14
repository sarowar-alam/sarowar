# 🚀 ECS Fargate Deployment Automation with Jenkins & Terraform

### 🗓️ Last Updated: 2025-10-14

---

## 🌟 1. What’s Special About This Code Module

This project represents a **fully automated CI/CD pipeline** that deploys and scales **ECS Fargate services** using **Terraform**, with dynamic configuration and tight integration with **Jenkins** and **AWS services**.  
It’s designed for **zero manual intervention**, **dynamic scaling**, and **repeatable infrastructure deployment** — enabling rapid, secure, and consistent ECS deployments.

### 💡 Highlights
- 100% automated end-to-end deployment
- Modular, reusable Terraform structure
- Dynamic `.tfvars` generation directly from Jenkins parameters
- Secure, vulnerability-scanned Docker image pipeline (Trivy integrated)
- Intelligent autoscaling triggered by SQS queue depth and CloudWatch alarms
- Standardized tagging and environment management

---

## ⚙️ 2. Functional Overview

This system automates the deployment of ECS services through an integrated Jenkins + Terraform + AWS workflow.  
The pipeline covers every aspect of the lifecycle — from build to deploy to scale.

### 🧩 Workflow Summary

1. **Jenkins Pipeline**
   - Reads ECS service configuration (`ecs-services.json`)
   - Builds and scans Docker images
   - Pushes image to Amazon ECR
   - Dynamically generates Terraform `.tfvars` file
   - Executes Terraform plan and apply

2. **Terraform Infrastructure**
   - Provisions ECS Service, Task Definition, and CloudWatch Log Group
   - Creates Autoscaling Targets and Policies for ECS Service
   - Configures CloudWatch Alarms based on SQS queue metrics
   - Enables automatic scale in/out via Application Auto Scaling

---

## 🔑 3. Key Features

### 🧱 Infrastructure as Code (IaC)
All resources (ECS, IAM, CloudWatch, Autoscaling, ECR) are provisioned via Terraform — ensuring repeatability, version control, and easy rollback.

### 🔄 Dynamic Scaling with CloudWatch & SQS
ECS automatically scales in/out based on queue depth:
- Scale-Out → When pending SQS messages increase
- Scale-In → When the queue empties

### 🧪 Built-in Security
- **Trivy** scans Docker images for vulnerabilities (CRITICAL, HIGH, MEDIUM) before pushing to ECR.
- **Read-only task execution roles** ensure secure ECS operations.

### ⚙️ Dynamic tfvars Generation
No manual Terraform variable files needed — Jenkins dynamically creates `.tfvars` with service-specific settings from JSON input.

### 🧰 Modular Design
The code is broken down into logical Terraform modules for reusability across environments (e.g., dev, stage, prod).

### 🔔 Monitoring & Alerts
CloudWatch alarms trigger scaling and alert when thresholds are exceeded, ensuring service stability.

---

## 🛠️ 4. Implementation Steps (Detailed)

### Step 1 — Jenkins Setup
1. Install **Jenkins** with required plugins: Git, Pipeline, AWS Credentials, Terraform.
2. Configure Jenkins global credentials for AWS CLI access.
3. Add Docker and Trivy to the Jenkins build agent.
4. Place your pipeline Groovy script in Jenkins (`automate-ecs-deploy.gvy`).

### Step 2 — Prepare Repositories
- **App Repo** (contains application Dockerfile)
- **Terraform IaC Repo** (contains modules and main.tf)
- **Config Repo** (contains `ecs-services.json` for service-specific parameters)

### Step 3 — Jenkins Parameters
Define Jenkins job parameters:
- `appName`, `environment`, `cpu`, `memory`, `minInstances`, `maxInstances`, `repoUrl`, etc.

### Step 4 — Pipeline Stages
| Stage | Description |
|--------|-------------|
| **ECSConfiguration** | Loads service configuration from JSON |
| **ContainerBuild** | Builds Docker image dynamically |
| **TrivySecurityScan** | Scans image for vulnerabilities |
| **ECRPush** | Pushes image to ECR |
| **TerraformInit** | Clones Terraform IaC repository |
| **Create_tfvars_File** | Creates dynamic `.tfvars` for Terraform |
| **TerraformApply** | Deploys ECS service and scaling infra |

### Step 5 — Terraform Apply
Terraform provisions ECS Service, Task Definition, CloudWatch Logs, and Autoscaling Policies.

### Step 6 — Verification
- Validate ECS service in AWS Console (Tasks, Logs, Scaling).
- Monitor CloudWatch alarms and scaling behavior.
- Confirm successful SQS-driven autoscaling.

---

## 📈 Example Architecture

**Flow:** Jenkins → Docker Build → Trivy Scan → ECR → Terraform → ECS → CloudWatch → SQS (autoscaling)

```text
         ┌────────────┐
         │  Jenkins   │
         └─────┬──────┘
               │
       Build & Scan (Trivy)
               │
          Push to ECR
               │
          Terraform Apply
               │
          ┌─────────────┐
          │ ECS Service │
          └────┬────────┘
               │
         AutoScaling Target
               │
          CloudWatch Alarm
               │
           SQS Queue Depth
```

---

## 🧾 Example Output

After successful run, you’ll have:
- ECS Service running latest image from ECR
- Task auto-scaled based on SQS load
- Terraform-managed infrastructure
- Security-verified image build history

---

## 🏁 Conclusion

This automation framework eliminates manual ECS deployments and brings consistency, scalability, and security to modern infrastructure delivery.  
It’s designed for **DevOps teams building cloud-native microservices** with AWS Fargate, ECS, and Terraform.

---
gitGraph
   commit id: "init" tag: "v0.0"
   branch develop
   commit id: "feat-start"
   commit id: "feat-progress"
   checkout main
   commit id: "hotfix-1"
   checkout develop
   merge main
   commit id: "feat-finish"
   checkout main
   merge develop tag: "v1.0.0"
   commit id: "release-1.0"
   branch hotfix
   commit id: "hotfix-issue"
   checkout main
   merge hotfix tag: "v1.0.1"
   checkout develop
   merge main

---

## 🧑‍💻 Author
**Md. Sarowar Alam**  
Lead DevOps Engineer, Hogarth Worldwide  
📧 Email: sarowar@hotmail.com  
🔗 LinkedIn: [linkedin.com/in/sarowar](https://www.linkedin.com/in/sarowar/)

---
**Tech Stack:** Jenkins • Terraform • AWS ECS • ECR • SQS • CloudWatch • Trivy  
