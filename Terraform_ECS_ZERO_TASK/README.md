
# ECS Zero Task Auto-Scaler with Terraform, Lambda, and SQS

## 🔍 Overview

This Terraform module provisions an intelligent, **cost-optimized ECS Fargate service** where the number of running tasks is **automatically scaled to zero** when not needed — and resumed **only when there's work to process** (based on the number of messages in an Amazon SQS queue). This helps you **drastically reduce ECS Fargate compute cost**.

It utilizes:
- 🛠 **Terraform** to provision AWS resources
- 🖥️ **Lambda functions** to monitor & scale ECS tasks
- 📨 **Amazon SQS** for task-based message triggering
- 🔁 **EventBridge Scheduler** for periodic Lambda invocation
- 🎛️ **Jenkins pipeline** to automate deployment and IAM updates

---

## 📁 Folder Structure

```bash
terraform/
├── backend.tf                    # S3 backend config
├── cloud_watch_to_stop.tf       # (Optional) future CloudWatch integration
├── ecs-zero.auto.tfvars         # Auto-loaded tfvars from Jenkins
├── lambda_to_start.tf           # Lambda to START ECS task if SQS > 0
├── lambda_to_stop.tf            # Lambda to STOP ECS task if idle
├── main.tf                      # Data sources and outputs
├── schedule_to_start_lambda.tf  # EventBridge to trigger START lambda
├── variable.tf                  # Input variables
├── start_ecs.py (zipped)        # Lambda code to start ECS
├── stop_ecs.py (zipped)         # Lambda code to stop ECS
├── update_iam_role.py           # Script to update IAM inline policy
└── zero-task-prod.gvy           # Jenkins Groovy pipeline
```

---

## 🚀 Use Case

**Ideal for ECS workloads with sporadic or queue-based demand.**

This module ensures:
- No idle ECS tasks consuming compute cost
- Tasks start **only when there are pending jobs/messages in SQS**
- Tasks shut down automatically if there’s nothing to process

---

## ⚙️ How It Works

### 1. **Provisioning**
- Jenkins pipeline dynamically injects parameters into Terraform
- Lambda functions are created to:
  - 🔼 Start ECS service (desiredCount=1) if SQS > 0
  - 🔽 Stop ECS service (desiredCount=0) if SQS is empty
- Lambda roles are configured with appropriate permissions via Python script

### 2. **Scheduled Checks**
- **EventBridge Scheduler** triggers `start_ecs.py` every minute
- `start_ecs.py`:
  - Reads SQS queue depth
  - If messages > 0 → sets ECS desired count to 1
- `stop_ecs.py`:
  - Checks ECS metrics (CPU, memory, network, SQS)
  - If idle → stops ECS (desiredCount = 0)

---

## 🧩 Jenkins Pipeline Breakdown (`zero-task-prod.gvy`)

### 🔹 Parameters
- ECS Cluster Name, ECS Service Name
- SQS URL, IAM Role ARN + Policy
- Environment, Owner, Cost App, System Tags

### 🔹 Actions
- Pulls code from Git
- Generates `auto.tfvars` dynamically
- Initializes and applies Terraform
- Extracts outputs (Lambda ARNs, ECS ARN, etc.)
- Calls `update_iam_role.py` to inject ARNs into IAM policy

---

## 🧪 Lambda Logic Highlights

### ✅ `start_ecs.py`
- Runs every minute
- If SQS has > 0 messages and ECS is stopped → starts ECS (desiredCount = 1)

### ⛔ `stop_ecs.py`
- Evaluates:
  - SQS Message Count
  - ECS CPU, Memory, Network usage (via CloudWatch)
- If idle → stops ECS (desiredCount = 0)

---

## 🔐 IAM Considerations

- Use `update_iam_role.py` to programmatically append required Lambda/SQS/ECS ARNs to an existing IAM inline policy.
- Ensure the IAM Role used by Lambda has access to:
  - `ecs:DescribeServices`
  - `ecs:UpdateService`
  - `sqs:GetQueueAttributes`
  - `cloudwatch:GetMetricStatistics`

---

## 📥 Required Inputs

Provided via Jenkins or `.auto.tfvars`:

| Variable           | Description                          |
|--------------------|--------------------------------------|
| `ecs_Name`         | ECS service name                     |
| `cluster_name`     | ECS cluster name                     |
| `sqs_url`          | SQS queue URL                        |
| `iam_role_arn`     | IAM role used by Lambda              |
| `tag_environment`  | Environment tag                      |
| `tag_system`       | System tag                           |
| `tag_owner`        | Owner tag                            |
| `cost_app`         | Cost allocation tag                  |
| `tag_company`      | Company tag                          |

---

## 🧰 Setup Instructions

1. **Configure S3 Backend**
   - Update `backend.tf` with your S3 bucket & prefix

2. **Jenkins Setup**
   - Update Git URL and credentials in `zero-task-prod.gvy`
   - Install required Jenkins plugins (e.g., Git, Pipeline, Credentials Binding)

3. **Lambda Packaging**
   - Zip both `start_ecs.py` → `start_ecs.zip`
   - Zip `stop_ecs.py` → `stop_ecs.zip`

4. **IAM Policy Update**
   - Script `update_iam_role.py` updates inline policy with needed ARNs

---

## ✅ Example Output (from Jenkins)

```
lambda_function_arn = arn:aws:lambda:us-west-2:xxx:function/my-ecs-START
lambda_function_stop_arn = arn:aws:lambda:us-west-2:xxx:function/my-ecs-STOP
ecs_service_arn = arn:aws:ecs:us-west-2:xxx:service/my-ecs
sqs_queue_arn = arn:aws:sqs:us-west-2:xxx:queue/my-queue
```

---

## 💡 Tips & Notes

- `start_ecs.py` sleeps 15s between checks (for stability)
- `stop_ecs.py` monitors CloudWatch metrics to avoid premature shutdown
- Terraform state is isolated per ECS service (via workspace)

---

## 🔗 Contribution & License

Feel free to fork and enhance this module to fit your needs!

Licensed under MIT License.

---

## 🙋‍♂️ Author

**Md. Sarowar Alam**  
🚀 GitHub: [@sarowar-alam](https://github.com/sarowar-alam)  
📫 Connect on [LinkedIn](https://www.linkedin.com/in/sarowar/)

---
