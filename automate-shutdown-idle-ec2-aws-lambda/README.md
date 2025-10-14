# EC2 Auto-Shutdown — Implementation Guide

This repository contains a Lambda function (`lambda_function.py`) that detects idle EC2 instances (by CPU, network and EBS activity), sends a warning email, and automatically stops instances that remain idle after a 15-minute grace period. This README first describes the functionality, then provides a full step-by-step implementation procedure you can run from PowerShell with the AWS CLI.

---

## Functionality (what this Lambda does)

- Scans running EC2 instances matching configured tags (by default: `Environment=Mainline` and `System=XBOX`).
- For each instance, the function evaluates four signals using CloudWatch metrics:
  - CPUUtilization (average, threshold 3%)
  - NetworkIn (average, threshold 10,000 bytes)
  - NetworkOut (average, threshold 10,000 bytes)
  - EBS disk activity (per-volume VolumeReadBytes + VolumeWriteBytes; a volume is considered active if its averaged read+write > ~100 KB)
- Detection rule: if 3 of the 4 signals are "low" the instance is considered unused.
- First detection: sends a warning email using SES and writes state to DynamoDB (`ec2-auto-shutdown-state`) including the warning timestamp.
- If the instance is still unused 15 minutes later (next Lambda run), Lambda stops the instance and sends a shutdown notification, then removes the DynamoDB record.
- Uses IAM AssumeRole to access EC2/CloudWatch and to send email via SES optionally across accounts.

Security & safety notes
- This script will stop EC2 instances. Run first in a non-production environment and use conservative tag filters.
- Ensure SES sender/recipient verification and correct permissions before enabling automatic emails.

---

## Prerequisites

- AWS CLI v2 configured with credentials that can create IAM roles and deploy Lambda (or use the Console).
- PowerShell (Windows PowerShell v5.1 or PowerShell Core) as your shell for commands below.
- Python 3.12+ (for local tests or packaging). Lambda uses Python 3.12 runtime in these examples.
- Target EC2 instances must be tagged: `Environment=Mainline` and `System=XBOX` (or change the tag JSON).

---

## Implementation procedure (from scratch)

Follow these steps in order. Replace placeholders (ACCOUNT IDs, ARNs, emails) with your values.

### Step 1 — Create IAM Roles

1.1 Create Lambda execution role (trust policy for Lambda service)

```powershell
aws iam create-role `
  --role-name EC2AutoShutdownLambdaRole `
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [{
        "Effect": "Allow",
        "Principal": {"Service": "lambda.amazonaws.com"},
        "Action": "sts:AssumeRole"
    }]
  }'
```

1.2 Attach an inline policy to the Lambda execution role (this policy includes DynamoDB and CloudWatch Logs, and allows STS AssumeRole so the Lambda can call STS to assume other roles if needed)

> Note: For production narrow the Resource ARNs instead of `*`.

```powershell
$policy = @'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ec2:DescribeInstances",
        "ec2:StopInstances",
        "ec2:CreateTags",
        "cloudwatch:GetMetricStatistics",
        "sts:AssumeRole",
        "dynamodb:GetItem",
        "dynamodb:PutItem",
        "dynamodb:DeleteItem",
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "*"
    }
  ]
}
'@

aws iam put-role-policy --role-name EC2AutoShutdownLambdaRole --policy-name EC2AutoShutdownPolicy --policy-document $policy
```

1.3 (Optional) If you will assume a monitoring role in the same or other account, create a Monitoring role the Lambda can assume

```powershell
aws iam create-role `
  --role-name EC2MonitoringRole `
  --assume-role-policy-document '{
    "Version":"2012-10-17",
    "Statement":[{
      "Effect":"Allow",
      "Principal":{"AWS":"ARN_OF_LAMBDA_ROLE_OR_ACCOUNT"},
      "Action":"sts:AssumeRole"
    }]
  }'
```

Attach the following minimum policy (example) to `EC2MonitoringRole`:

```powershell
# Example policy JSON - allow EC2 Describe/Stop and CloudWatch GetMetricStatistics
$monitorPolicy = @'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ec2:DescribeInstances",
        "ec2:StopInstances",
        "ec2:CreateTags",
        "cloudwatch:GetMetricStatistics"
      ],
      "Resource": "*"
    }
  ]
}
'@

aws iam put-role-policy --role-name EC2MonitoringRole --policy-name EC2MonitoringPolicy --policy-document $monitorPolicy
```

1.4 (Optional) If you will assume a role to send SES emails, create `SESEmailRole` and attach SES permissions

```powershell
aws iam create-role `
  --role-name SESEmailRole `
  --assume-role-policy-document '{
    "Version":"2012-10-17",
    "Statement":[{
      "Effect":"Allow",
      "Principal":{"AWS":"ARN_OF_LAMBDA_ROLE_OR_ACCOUNT"},
      "Action":"sts:AssumeRole"
    }]
  }'

$sesPolicy = @'
{
  "Version":"2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ses:SendEmail",
        "ses:SendRawEmail"
      ],
      "Resource": "*"
    }
  ]
}
'@

aws iam put-role-policy --role-name SESEmailRole --policy-name SESEmailPolicy --policy-document $sesPolicy
```

> Remember: the monitoring and SES role trust policies must explicitly allow the Lambda (or its role ARN) to assume them.

---

### Step 2 — Create DynamoDB Table

Create the state table used to store warnings and timestamps.

```powershell
aws dynamodb create-table `
  --table-name ec2-auto-shutdown-state `
  --attribute-definitions AttributeName=InstanceId,AttributeType=S `
  --key-schema AttributeName=InstanceId,KeyType=HASH `
  --billing-mode PAY_PER_REQUEST

# Enable TTL for expiry_time (this is done with a separate call)
aws dynamodb update-time-to-live --table-name ec2-auto-shutdown-state --time-to-live-specification "Enabled=true,AttributeName=expiry_time"
```

---

### Step 3 — Package and Deploy Lambda

3.1 Create a zip package containing `lambda_function.py` (no external deps in the provided script):

```powershell
Compress-Archive -Path lambda_function.py -DestinationPath lambda-function.zip
```

3.2 Create the Lambda function and set environment variables (the example sets monitoring and SES role ARNs, region, and tag filters). Replace `ACCOUNT_ID` and ARNs appropriately.

```powershell
$ACCOUNT_ID = (aws sts get-caller-identity --query Account --output text)

aws lambda create-function `
  --function-name ec2-auto-shutdown `
  --runtime python3.12 `
  --role arn:aws:iam::${ACCOUNT_ID}:role/EC2AutoShutdownLambdaRole `
  --handler lambda_function.lambda_handler `
  --zip-file fileb://lambda-function.zip `
  --timeout 300 `
  --environment "Variables={MONITORING_ROLE_ARN=arn:aws:iam::${ACCOUNT_ID}:role/EC2MonitoringRole,SES_ROLE_ARN=arn:aws:iam::${ACCOUNT_ID}:role/SESEmailRole,TARGET_REGION=us-west-2,TAGS_FOR_MATCHING={\"Environment\": \"Mainline\", \"System\": \"XBOX\"}}"
```

Notes:
- The function currently uses SES in `us-east-1` when sending emails; the SES role and sender identity should be configured accordingly.
- If your SES role or monitoring role is in another account, use the appropriate ARNs.

---

### Step 4 — Create EventBridge (CloudWatch Events) Rule (every 15 minutes)

4.1 Create the rule

```powershell
aws events put-rule --name "ec2-auto-shutdown-15min" --schedule-expression "rate(15 minutes)" --state ENABLED --description "Trigger EC2 auto shutdown every 15 minutes"
```

4.2 Add the Lambda as a target

```powershell
$ACCOUNT_ID = (aws sts get-caller-identity --query Account --output text)
$REGION = "us-west-2"

aws events put-targets --rule "ec2-auto-shutdown-15min" --targets "Id"="1","Arn"="arn:aws:lambda:${REGION}:${ACCOUNT_ID}:function:ec2-auto-shutdown"
```

4.3 Give EventBridge permission to invoke the Lambda

```powershell
aws lambda add-permission `
  --function-name ec2-auto-shutdown `
  --statement-id "EventBridgeInvoke" `
  --action "lambda:InvokeFunction" `
  --principal "events.amazonaws.com" `
  --source-arn "arn:aws:events:${REGION}:${ACCOUNT_ID}:rule/ec2-auto-shutdown-15min"
```

---

### Step 5 — Verify SES Configuration

5.1 Verify the sender email or domain (example uses `noreply@xbox.net`)

```powershell
aws ses verify-email-identity --email-address noreply@xbox.net
# or for domain verification:
aws ses verify-domain-identity --domain xbox.net
```

5.2 (Optional) Test sending an email via SES

```powershell
aws ses send-email --from "DevOps_Automation <noreply@xbox.net>" --destination "ToAddresses=karim@xbox.com" --message "Subject={Data=Test},Body={Text={Data=Test email}}"
```

Note: If SES is in sandbox mode, recipients must be verified or you must request production access.

---

### Step 6 — Tag Your EC2 Instances

Ensure your target instances have the tag combination the Lambda filters for (example uses `Environment=Mainline` and `System=XBOX`).

```powershell
aws ec2 create-tags --resources i-1234567890abcdef0 --tags Key=Environment,Value=Mainline Key=System,Value=XBOX
```

---

### Step 7 — Testing

7.1 Invoke the Lambda manually (test run)

```powershell
aws lambda invoke --function-name ec2-auto-shutdown --payload '{}' response.json
Get-Content response.json -Raw
```

7.2 Verify deployed resources

```powershell
aws lambda get-function --function-name ec2-auto-shutdown
aws events describe-rule --name ec2-auto-shutdown-15min
aws dynamodb describe-table --table-name ec2-auto-shutdown-state
```

7.3 Check logs

```powershell
aws logs describe-log-groups --log-group-name-prefix /aws/lambda/ec2-auto-shutdown
aws logs filter-log-events --log-group-name /aws/lambda/ec2-auto-shutdown --limit 50
```

---

## Architecture Summary

EventBridge (15min) → Lambda → Assume Roles → Check EC2 Metrics (EC2 + CloudWatch + EBS) → DynamoDB (state) → Send Emails (SES) → Stop Instances

## Cost considerations
- Primary cost drivers: CloudWatch API calls and Lambda invocations.
- DynamoDB table with on-demand billing and small item size will be low-cost.

## Safety checklist before production
- Verify tags are correct and target only non-critical instances.
- Verify SES sender and recipients and get out of sandbox if needed.
- Test in staging account first.
- Consider adding a `DRY_RUN` environment variable to log actions instead of stopping instances until you're confident.

---

## Success indicators
- Lambda runs every 15 minutes.
- Instances matching the tag filter are detected and assessed.
- Warning emails are sent when instances are first detected idle.
- Instances are stopped 15 minutes after warning if they remain idle.
- DynamoDB tracks warnings and entries expire (TTL).

---

## 🧑‍💻 Author
**Md. Sarowar Alam**  
Lead DevOps Engineer, Hogarth Worldwide  
📧 Email: sarowar@hotmail.com  
🔗 LinkedIn: [linkedin.com/in/sarowar](https://www.linkedin.com/in/sarowar/)

---
