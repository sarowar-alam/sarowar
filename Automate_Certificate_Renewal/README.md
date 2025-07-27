# SSL Certificate Automation Pipeline

This repository contains a Jenkins pipeline and supporting scripts to automate the lifecycle of SSL certificates — from creation, renewal, AWS ACM updates, server deployments (IIS, Jenkins, Zabbix), to email notifications.

---

## Table of Contents

- [Overview](#overview)  
- [Prerequisites](#prerequisites)  
- [Pipeline Workflow](#pipeline-workflow)  
- [Repository Structure](#repository-structure)  
- [Setup & Configuration](#setup--configuration)  
- [Usage](#usage)  
- [Troubleshooting](#troubleshooting)  
- [Contributing](#contributing)  
- [License](#license)

---

## Overview

This pipeline automates:

- Checking SSL certificate expiry for selected domains  
- Creating or renewing certificates via PowerShell scripts  
- Updating certificates in AWS Certificate Manager (ACM)  
- Deploying certificates to IIS web servers (robust and basic methods)  
- Updating Jenkins server certificates (Windows and Linux)  
- Updating Zabbix server certificates  
- Uploading zipped certificates to S3 and sending notification emails  
- Failure notifications with detailed logs  

---

## Prerequisites

- Jenkins with pipeline support  
- AWS credentials with permissions for ACM, S3, and SES  
- Windows servers with PowerShell Remoting enabled (for IIS updates)  
- Python installed on Jenkins agent machines  
- 7-Zip installed on certificate management machine (`C:\Program Files\7-Zip\7z.exe`)  
- PosH-ACME certificate store at `%LOCALAPPDATA%\Posh-ACME\LE_PROD\*`  
- Required Jenkins credentials created (AWS, Windows server credentials, certificate passwords)  

---

## Pipeline Workflow

1. **Initialize**: Validate parameters, set environment variables  
2. **Start EC2 Instances**: Optionally start EC2 servers related to the domain  
3. **Check Certificate Validity**: Run PowerShell script to verify expiry days  
4. **Create Certificate**: Run PowerShell script to generate or renew the cert  
5. **Update AWS ACM**: Reimport updated cert into AWS ACM  
6. **Deploy to Servers**: Update IIS, Jenkins, and Zabbix servers with new cert  
7. **Send Notification**: Upload zipped cert to S3 and send email with download link  
8. **Post Actions**: Cleanup workspace, send failure email on errors  

---

## Repository Structure

| File                          | Description                                |
|-------------------------------|--------------------------------------------|
| `automate-certificate.gvy`    | Jenkins pipeline Groovy script              |
| `update-iis-robust.ps1`       | Robust PowerShell script to update IIS cert |
| `update-jenkins-linux.py`     | Python script to update Jenkins on Linux    |
| `update-jenkins-windows.ps1`  | PowerShell script for Jenkins Windows cert  |
| `backup-create-certificate.ps1` | PowerShell script to create/backup cert    |
| `ceheck-certificate-expiry.ps1` | PowerShell script to check cert expiry     |
| `update-zabbix-certificate.py`| Python script to update Zabbix server cert  |
| `start-ec2-servers.py`        | Python script to start EC2 instances        |
| `upload-certificate-s3-send-mail.py` | Upload zipped cert to S3, send email    |
| `update-aws-certificate.py`   | Script to reimport cert to AWS ACM           |
| `README.md`                   | Project documentation                        |

---

## Setup & Configuration

1. **Configure Jenkins Credentials**  
   - AWS Access keys for ACM, S3, SES  
   - Windows server admin credentials  
   - Certificate password secrets (PFX_PASS, JENKINS_PFX_PASS)  

2. **Edit `automate-certificate.gvy`**  
   - Set domain names in parameters  
   - Update IP addresses for your servers in `SERVER_IPS`  
   - Set email recipients for notifications in `EMAIL_RECIPIENTS`  
   - Update paths in `SCRIPTS` section for your script locations  
   - Set AWS bucket and prefix  

3. **Ensure Prerequisites on Jenkins Agents**  
   - Python, PowerShell, 7-Zip installed and accessible  
   - Required Python modules (`boto3`, etc.)  

4. **Prepare PosH-ACME environment** for certificate storage and renewal on certificate management machine  

---

## Usage

Trigger the Jenkins job with:

- Select the domain to renew from the dropdown  
- Pipeline automatically runs through all steps  
- Monitor console logs for progress and errors  
- Email notifications will be sent on success or failure  

---

## Troubleshooting

- **Certificate files missing**: Verify PosH-ACME cert paths and naming  
- **AWS permissions denied**: Check IAM roles and policies for Jenkins credentials  
- **PowerShell remoting errors**: Ensure WinRM is enabled and accessible on IIS servers  
- **Email not sending**: Verify SES configuration and verified sender/recipient emails  
- **7-Zip failures**: Confirm 7-Zip installation path and access permissions  

---

## Contributing

Feel free to submit issues or pull requests for improvements. Please test your changes before submitting.

---
