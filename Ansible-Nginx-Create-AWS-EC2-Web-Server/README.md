# 🚀 Ansible AWS Infrastructure Project

This project provisions and configures AWS infrastructure using **Ansible** with a static configuration approach.  
It automates the creation of VPC, Subnets, Security Groups, and EC2 instances, and configures web servers with **Nginx**.

---

## 📂 Project Structure

```
ansible-project/
├── activate-venv.sh              # Activate Python virtual environment
├── ansible.cfg                   # Ansible configuration file
├── deploy.sh                     # Main deployment script (infra + server config)
├── run-ansible.sh                # Wrapper script for ansible commands
├── utils.sh                      # Utility helper script for AWS & inventory
├── group_vars/
│   ├── all.yml                   # Global variables for all hosts
│   └── webservers.yml            # Webserver-specific variables
├── inventory/
│   ├── aws_ec2.yml               # Dynamic AWS EC2 inventory plugin config
│   └── key_vars.yml              # Key file path configuration
├── playbooks/
│   ├── 01-setup-aws-infrastructure.yml   # Provision AWS VPC, subnet, SG, EC2
│   ├── 02-configure-servers.yml          # Configure web servers (Nginx, firewall, packages)
│   ├── aws-setup.yml                     # Minimal AWS infra setup example
│   ├── configure-server.yml              # Minimal server setup example
│   └── site.yml                          # Master playbook (infra + config)
└── create_master_node.sh         # Bootstrap script for setting up Ansible master node
```

---

## ⚡ Prerequisites

- Ubuntu-based control machine (Ansible master)
- AWS account with IAM credentials
- Python 3.8+
- Ansible (installed via `create_master_node.sh`)
- AWS CLI installed

---

## 🔧 Setup Instructions

### 1. Clone the repository
```bash
git clone https://github.com/your-repo/ansible-aws-infra.git
cd ansible-aws-infra
```

### 2. Prepare Ansible Master Node
```bash
chmod +x create_master_node.sh
./create_master_node.sh
```

Update your AWS credentials:
```bash
nano ~/.aws/credentials
```

### 3. Activate Virtual Environment
```bash
source activate-venv.sh
```

### 4. Test AWS Connectivity
```bash
ansible localhost -m amazon.aws.aws_caller_info
```

---

## 🚀 Deployment

Run the deployment script (provisions infrastructure + configures servers):

```bash
chmod +x deploy.sh
./deploy.sh
```

Or run playbooks separately:

```bash
ansible-playbook playbooks/01-setup-aws-infrastructure.yml
ansible-playbook playbooks/02-configure-servers.yml
```

---

## 🔍 Useful Commands

```bash
./utils.sh test-aws          # Test AWS connectivity
./utils.sh list-instances    # List AWS EC2 instances
./utils.sh list-groups       # Show Ansible inventory groups
./utils.sh ping-all          # Ping all servers
./utils.sh web-status        # Check Nginx status on web servers
./utils.sh update-inventory  # Refresh AWS inventory cache
```

---

## 📜 Outputs

After deployment, details will be saved in:

```
~/ansible-project/infrastructure-details.txt
```

Example:
```
Project: ansible-company_name
Region: ap-south-1
Web Server Public IP: 3.109.xxx.xxx
SSH: ssh -i /root/your_key_pair_name.pem ubuntu@3.109.xxx.xxx
```

---

## 🛠️ Notes

- Uses **AWS EC2 dynamic inventory plugin**
- Default region: `ap-south-1`
- Default instance type: `t3.micro`
- Default AMI: `Ubuntu 20.04/22.04`
- Firewall (UFW) enabled with SSH, HTTP, HTTPS open

---

## 📧 Contact

Maintainer: **user_name**  
Email: user_name@company_name.com  
Project: `ansible-company_name`
