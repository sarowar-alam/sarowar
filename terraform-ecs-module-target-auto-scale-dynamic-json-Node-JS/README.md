# 🚀 CPU Load Test Application (ECS Auto-Scaling Project)

## 📘 Overview

This project demonstrates a **complete CI/CD deployment pipeline** using **Jenkins, Docker, and Terraform** to deploy a **Node.js CPU Load Testing Application** on **AWS ECS Fargate** with **auto-scaling** based on CPU utilization.

The application allows you to **simulate real CPU stress** on ECS containers to **trigger scaling events** and test CloudWatch monitoring configurations.

---

## 🌟 Features

### 🧠 Application Features
- **Real CPU Load Generation** using `stress-ng` (100% utilization across all CPU cores).
- **Interactive Web Interface** with:
  - Start/Stop CPU Load controls.
  - Real-time system metrics (CPU & Memory usage).
  - ECS auto-scaling trigger information.
- **REST API Endpoints** for health, metrics, and system info.
- **Auto-refresh metrics** using JavaScript for live performance data.
- **Responsive UI** built with pure HTML, CSS, and JavaScript.

### ☁️ Infrastructure Features
- **ECS Fargate-based deployment** (no EC2 management needed).
- **Auto Scaling (1–3 tasks)** triggered by CPU utilization > 50%.
- **Application Load Balancer (ALB)** with health checks & target groups.
- **VPC with public subnets** and proper route tables.
- **CloudWatch Monitoring & Alarms** for CPU and Memory thresholds.
- **IAM Roles** for ECS tasks and execution.
- **Terraform-managed Infrastructure as Code (IaC)** stored in an S3 remote backend.

### ⚙️ CI/CD Pipeline (Jenkins)
- Automated pipeline to:
  1. Build Docker image and push to AWS ECR.
  2. Dynamically generate Terraform variable inputs.
  3. Apply or destroy AWS infrastructure.
  4. Retrieve ALB URL after deployment.
- Clean workspace and provide build summary with ECS auto-scaling testing steps.
- Supports **parameterized builds** (`apply` or `destroy`).

---

## 🧩 Implementation Process

### 1️⃣ Prerequisites
Ensure the following tools are installed and configured:
- AWS Account with ECR, ECS, and IAM permissions.
- Jenkins server (can be set up using the included `install_jenkins_ubuntu.sh`).
- Terraform v1.0+ and AWS CLI v2.
- Docker and Docker Compose.

### 2️⃣ Infrastructure Setup (Terraform)
Terraform creates all AWS resources required for ECS deployment:
- **Network Layer:** VPC, Subnets, Route Tables, Internet Gateway.
- **Security Layer:** ALB and ECS Security Groups.
- **ECS Layer:** Cluster, Task Definition, Service, and Auto Scaling Policy.
- **Monitoring Layer:** CloudWatch Log Group and Alarms.

Example command (manual run):
```bash
cd terraform
terraform init
terraform apply -var="aws_region=ap-south-1" -var="project_name=cpu-load-test" -var="ecr_image_url=<ECR_IMAGE_URL>" -auto-approve
```

### 3️⃣ Application Layer (Docker + Node.js)
- **Dockerfile** builds a lightweight image based on `node:alpine`.
- Includes `stress-ng` for generating CPU load and `curl` for health checks.
- Exposes port `3000` with a built-in health check for ECS.

Run locally for testing:
```bash
docker build -t cpu-load-test .
docker run -p 3000:3000 cpu-load-test
```

### 4️⃣ CI/CD Pipeline (Jenkinsfile)
The Jenkins pipeline automates deployment with the following stages:
1. **Checkout:** Pulls the source code from GitHub.
2. **Configure AWS Credentials:** Injects AWS keys from Jenkins credentials.
3. **Build Docker Image:** Builds image and tags with build ID.
4. **Push to ECR:** Pushes both latest and versioned tags.
5. **Terraform Plan/Apply/Destroy:** Deploys or destroys the ECS environment.
6. **Post-Deployment Validation:** Tests ALB health and endpoint availability.
7. **Cleanup:** Deletes workspace after completion.

### 5️⃣ Testing Auto-Scaling
1. Access the web app from the ALB DNS (output after apply stage).
2. Click **“🚀 Start REAL CPU Load”**.
3. Monitor **CloudWatch metrics** — ECS service scales up to 3 tasks once CPU > 50%.
4. When stopped, ECS scales back to 1 task.

---

## 🧠 Architecture Diagram (Conceptual)

```
Jenkins → ECR → ECS (Fargate Tasks)
   │         │         │
   │         ├──→ ALB ←┘
   │         │
   └──→ Terraform → AWS Infrastructure (VPC, SG, CloudWatch)
```

---

## 🧰 File Structure
```
├── Dockerfile
├── Jenkinsfile.groovy
├── package.json
├── server.js
├── public/index.html
├── terraform/
│   ├── backend.tf
│   ├── main.tf
│   ├── providers.tf
│   └── variables.tf
└── install_jenkins_ubuntu.sh
```

---

## 🧑‍💻 Author
**Md Sarowar Alam**  
Cloud & DevOps Engineer  
📧 [sarowar@hotmail.com]

---

## 🪄 License
OWN (Private use only)
