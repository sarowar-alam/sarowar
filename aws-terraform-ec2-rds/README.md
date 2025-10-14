# AWS Infrastructure with Terraform

This repository contains Terraform code for provisioning a complete AWS infrastructure, including VPC, EC2 instances, RDS databases, and security groups.  
It is modularized for reusability and supports both Linux and Windows servers.

---

## 📂 Project Structure

```
.
├── modules/
│   ├── ec2/                 # EC2 instance module
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   └── outputs.tf
│   ├── rds/                 # RDS database module
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   └── outputs.tf
│   ├── security-groups/     # Security groups module
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   └── outputs.tf
│   └── vpc/                 # VPC module
│       ├── main.tf
│       ├── variables.tf
│       └── outputs.tf
├── scripts/                 # User data scripts
│   ├── bastion-userdata.ps1
│   ├── ubuntu-userdata.sh
│   └── windows-userdata.ps1
├── backend.tf              # Terraform backend configuration
├── main.tf                 # Main infrastructure configuration
├── outputs.tf             # Output values
├── providers.tf           # Provider configuration
├── variables.tf           # Input variables
└── combine.ps1            # Script to combine all files
```

---

## 🚀 Features

- **VPC Module**
  - Public and private subnets across availability zones
  - NAT gateways for private subnets
  - Internet gateway for public subnets

- **Security Groups**
  - Bastion SG with restricted RDP access
  - Private SG for internal EC2 communication
  - RDS SG allowing access only from private instances

- **EC2 Module**
  - Bastion host (Windows Server 2019)
  - Ubuntu server (22.04 LTS)
  - Windows server (2019)

- **RDS Module**
  - MariaDB instance
  - SQL Server Express instance
  - Encrypted storage, subnet groups, backups

- **User Data Scripts**
  - Linux setup (AWS CLI, MariaDB client, Git, etc.)
  - Windows setup (WinRM, AWS CLI, Git Bash, SSMS)

---

## ⚙️ Prerequisites

- [Terraform >= 1.0](https://developer.hashicorp.com/terraform/downloads)
- AWS account with appropriate IAM permissions
- Configured AWS CLI with named profile (default: `ostad`)

---

## 📌 Configuration

Edit `variables.tf` or provide values via a `.tfvars` file. Example:

```hcl
aws_profile         = "ostad"
region              = "ap-south-1"
environment         = "dev"

vpc_cidr            = "10.0.0.0/16"
public_subnet_cidrs = ["10.0.1.0/24", "10.0.2.0/24"]
private_subnet_cidrs = ["10.0.10.0/24", "10.0.11.0/24"]

allowed_ips         = ["YOUR_IP/32"]

bastion_instance_type = "t3.micro"
ubuntu_instance_type  = "t3.micro"
windows_instance_type = "t3.small"

key_name = "your-keypair-name"

db_username             = "admin"
db_password             = "YourSecurePassword123!"
mariadb_database_name   = "mydb"
sqlserver_database_name = "sqldb"
```

---

## ▶️ Usage

Initialize, plan, and apply:

```bash
terraform init
terraform plan -var-file="dev.tfvars"
terraform apply -var-file="dev.tfvars"
```

To destroy the infrastructure:

```bash
terraform destroy -var-file="dev.tfvars"
```

---

## 📤 Outputs

After applying, Terraform will export:

- **VPC ID**
- **Bastion Public IP**
- **Ubuntu Private IP**
- **Windows Private IP**
- **RDS Endpoints (MariaDB & SQL Server)**

---

## 🛠️ Utility Script

You can use `combine.ps1` to merge all Terraform and script files into a single `combined_files.txt` for review/sharing:

```powershell
.\combine.ps1
```

---

## 🔒 Security Notes

- Restrict `allowed_ips` to your own IP for bastion host access.
- Store `db_password` securely (e.g., use SSM Parameter Store or Secrets Manager).
- Ensure proper IAM role separation for state bucket access.

---

## 🧑‍💻 Author
**Md. Sarowar Alam**  
Lead DevOps Engineer, Hogarth Worldwide  
📧 Email: sarowar@hotmail.com  
🔗 LinkedIn: [linkedin.com/in/sarowar](https://www.linkedin.com/in/sarowar/)

---
