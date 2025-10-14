# Ansible Multi-OS Automation Project

This project provides a set of **Ansible playbooks** to manage **Ubuntu** and **Windows servers**, including connection verification, software installation, and system updates. It supports both **SSH** (for Ubuntu) and **WinRM** (for Windows).  

---

## Table of Contents

- [Project Structure](#project-structure)  
- [Prerequisites](#prerequisites)  
- [Inventory Configuration](#inventory-configuration)  
- [Playbooks](#playbooks)  
- [Usage](#usage)  
- [Logs](#logs)  
- [Notes](#notes)  

---

## Project Structure

```
ansible-project/
├── inventory/
│   └── hosts.yml          # Inventory for Ubuntu and Windows servers
├── playbooks/
│   ├── check-connection-hosts.yml   # Test SSH/WinRM connections and hostnames
│   ├── software-verify.yml          # Install required software
│   └── update-systems.yml           # Update Ubuntu & Windows systems
└── create-master-node.txt           # Master node setup instructions
```

---

## Prerequisites

### On Ansible Master (Ubuntu)
- Ubuntu server (Ansible control node)
- Python 3.6+ installed
- SSH access to Ubuntu servers
- WinRM access to Windows servers

### Required Packages
```bash
sudo apt update && sudo apt install -y ansible python3-pip python3-venv sshpass
```

### Optional Virtual Environment
```bash
python3 -m venv ~/ansible-venv
source ~/ansible-venv/bin/activate
pip install --upgrade pip
pip install pywinrm boto3 botocore
```

### Install Required Ansible Collections
```bash
ansible-galaxy collection install ansible.windows
ansible-galaxy collection install community.windows
```

---

## Inventory Configuration

Edit `inventory/hosts.yml` to define your servers:

**Ubuntu Servers**
```yaml
ubuntu_servers:
  hosts:
    ubuntu1:
      ansible_host: 10.0.134.174
      ansible_user: ubuntu
      ansible_ssh_private_key_file: ~/.ssh/your_key_pair_name.pem
```

**Windows Servers**
```yaml
windows_servers:
  hosts:
    win1:
      ansible_host: 10.0.9.136
      ansible_user: Administrator
      ansible_password: "<YOUR_PASSWORD>"
      ansible_connection: winrm
      ansible_winrm_transport: basic
      ansible_port: 5985
```

---

## Playbooks

### 1. `check-connection-hosts.yml`
- Tests connectivity to all servers.
- Retrieves hostnames.
- Supports SSH (Ubuntu) and WinRM (Windows).

### 2. `software-verify.yml`
- Ensures required software is installed:
  - **Ubuntu**: Git, Docker, net-tools, AWS CLI
  - **Windows**: AWS CLI, Git, Python via Chocolatey
- Logs installed software versions.

### 3. `update-systems.yml`
- Updates OS packages on both Ubuntu and Windows servers.
- Saves pre- and post-update package lists and logs.
- Reboots Windows servers if required.

---

## Usage

### Run Connection Check
```bash
ansible-playbook -i inventory/hosts.yml playbooks/check-connection-hosts.yml
```

### Install Required Software
```bash
ansible-playbook -i inventory/hosts.yml playbooks/software-verify.yml
```

### Update Systems
```bash
ansible-playbook -i inventory/hosts.yml playbooks/update-systems.yml
```

---

## Logs

- Ubuntu logs: `~/ubuntu_software_versions.log`, `~/<hostname>-updates.json`  
- Windows logs: `C:\ansible_logs\windows_software_versions.log`, `C:\ansible_logs\<hostname>-updates.json`  

---

## Notes

- Make sure your **PEM keys** have correct permissions: `chmod 600 ~/.ssh/your_key_pair_name.pem`  
- Ensure **WinRM is configured** on Windows servers:  
```powershell
winrm quickconfig
winrm set winrm/config/service/auth '@{Basic="true"}'
winrm set winrm/config/service '@{AllowUnencrypted="true"}'
```  
- For Ubuntu servers, avoid interactive prompts by using:  
```yaml
environment:
  DEBIAN_FRONTEND: noninteractive
```

This setup allows **centralized management** of mixed OS environments with **automation**, **logging**, and **software compliance checks**.
---

## 🧑‍💻 Author
**Md. Sarowar Alam**  
Lead Engineer, Hogarth Worldwide  
📧 Email: sarowar@hotmail.com  
🔗 LinkedIn: [linkedin.com/in/sarowar](https://www.linkedin.com/in/sarowar/)

---
