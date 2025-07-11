# Terraform ECS Auto Scaling Deployment

This repository contains Terraform configurations to deploy and manage an **ECS Fargate Service** with **CloudWatch alarms** and **Auto Scaling policies** based on **SQS queue depth**. The stack is modular, scalable, and production-ready, intended to automate ECS task scaling and monitoring.

---

## 📁 File Structure

### `backend.tf`
Configures the remote backend in S3 to store Terraform state files securely.

```hcl
terraform {
  backend "s3" {
    bucket = "your-bucket-name"
    key    = "ecs/client/env/service_name.tfstate"
    region = "us-west-2"
  }
}
```

---

### `main.tf`
Initializes Terraform with the AWS provider and required version.

---

### `variables.tf`
Defines all input variables used across the modules including:
- ECS configuration (`cpuSize`, `memorySize`, `ecs_service_name`, etc.)
- Networking (`sg_name`, `subnet_names`)
- SQS (`sqs_name`, `threshold_num_messages`)
- Tagging (`tag_owner`, `tag_company`, etc.)

> ❗ Note: No `.tfvars` file is included in this module. The required variables are expected to be provided at runtime.

---

## 🚀 ECS Configuration Modules

### `ecs-task-definition.tf`
Defines the ECS Task with:
- Container definition (injected from S3 `env_file`)
- AWS Fargate compatibility
- Logging to CloudWatch
- Ephemeral storage configuration

### `ecs-service.tf`
Deploys the ECS service:
- Attached to a specific cluster
- Uses a FARGATE capacity provider
- Enables execution commands
- Tags applied for cost allocation and traceability

### `ecs-log.tf`
Creates a CloudWatch log group for ECS container logs with a 30-day retention policy.

---

## 📈 Auto Scaling & Alarms

### `ecs-autoscaling-target-group.tf`
Registers the ECS service as a scalable target with min and max capacity defined by user.

---

### `cloud-watch-alarm-high.tf`
Creates a CloudWatch alarm for **high SQS queue depth**:
- Triggers a **scale-out** policy
- Evaluates `ApproximateNumberOfMessagesVisible`

### `cloud-watch-alarm-low.tf`
Creates a CloudWatch alarm for **low SQS queue depth**:
- Triggers a **scale-in** policy

Both alarms:
- Use step scaling policies
- Trigger based on thresholds defined via `threshold_num_messages`
- Depend on ECS service registration

---

## ⚙️ Usage

### Jenkins Pipeline Execution

This Terraform module is designed to work with **Jenkins Pipelines**, where a `.tfvars` file is dynamically generated using parameters and environment variables at runtime.

Typical Jenkins workflow:
1. Jenkins job retrieves parameter/environment values.
2. Dynamically generates a `terraform.tfvars` file in the workspace.
3. Executes Terraform commands:
   ```bash
   terraform init
   terraform plan -var-file="terraform.tfvars"
   terraform apply -var-file="terraform.tfvars"
   ```
4. Deletes the generated `terraform.tfvars` file after execution for security.

---

## 🏷 Tagging Strategy

Each resource is tagged with:

- `Environment` - Deployment environment (e.g., dev, prod)
- `System` - Application or subsystem identifier
- `Owner` - Team or individual responsible
- `CostApp` / `CostUnit` / `Client` - For chargeback and cost allocation

---

## 🔐 Backend State

Your state is stored securely in S3. Make sure:
- Versioning is enabled on the S3 bucket
- Locking is configured (optionally via DynamoDB)

---

## 👥 Contributing

1. Fork the repo
2. Create your feature branch (`git checkout -b feature/xyz`)
3. Commit changes (`git commit -m 'Add feature xyz'`)
4. Push to the branch (`git push origin feature/xyz`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
