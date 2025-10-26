#!/bin/bash
# =========================================================
# Jenkins + Docker + Terraform + AWS CLI Setup Script
# For Ubuntu 24.04 LTS
# =========================================================

set -e
exec > >(tee /var/log/jenkins-setup.log) 2>&1

echo "==========================================="
echo "🚀 Starting Jenkins + Docker + Terraform + AWS CLI setup"
echo "==========================================="

# ---------------------------------------------------------
# Update System
# ---------------------------------------------------------
echo "Updating system packages..."
sudo apt update -y
sudo apt upgrade -y

# ---------------------------------------------------------
# Install Dependencies
# ---------------------------------------------------------
echo "Installing dependencies..."
sudo apt install -y wget curl zip unzip gnupg2 lsb-release apt-transport-https \
    software-properties-common fontconfig openjdk-17-jre

# ---------------------------------------------------------
# Verify Java
# ---------------------------------------------------------
echo "Verifying Java installation..."
java -version

# ---------------------------------------------------------
# Install Jenkins
# ---------------------------------------------------------
echo "Installing Jenkins..."
wget -O /tmp/jenkins-keyring.asc https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key
sudo mv /tmp/jenkins-keyring.asc /usr/share/keyrings/jenkins-keyring.asc
echo "deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian-stable binary/" \
  | sudo tee /etc/apt/sources.list.d/jenkins.list > /dev/null

sudo apt update -y
sudo apt install -y jenkins
sudo systemctl enable jenkins
sudo systemctl start jenkins

# ---------------------------------------------------------
# Configure Jenkins User Sudo Access
# ---------------------------------------------------------
echo "Configuring Jenkins user sudo access..."
# Add Jenkins user to sudoers with passwordless access for specific commands
sudo bash -c 'cat > /etc/sudoers.d/jenkins' << EOF
# Allow Jenkins user to run specific commands without password
jenkins ALL=(ALL) NOPASSWD: /usr/bin/docker, /usr/bin/docker-compose, /usr/bin/terraform, /usr/bin/aws, /usr/bin/apt, /usr/bin/apt-get, /usr/bin/systemctl
EOF

# Set proper permissions for the sudoers file
sudo chmod 440 /etc/sudoers.d/jenkins

# ---------------------------------------------------------
# Install Docker
# ---------------------------------------------------------
echo "Installing Docker..."
sudo apt-get remove -y docker docker-engine docker.io containerd runc || true
sudo apt-get install -y ca-certificates curl gnupg

sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | \
  sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update -y
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

sudo systemctl enable docker
sudo systemctl start docker

# Add current user and Jenkins to docker group
sudo usermod -aG docker jenkins
sudo usermod -aG docker $USER

# ---------------------------------------------------------
# Install Terraform
# ---------------------------------------------------------
echo "Installing Terraform..."
curl -fsSL https://apt.releases.hashicorp.com/gpg | \
  sudo gpg --dearmor -o /usr/share/keyrings/hashicorp-archive-keyring.gpg

echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] \
https://apt.releases.hashicorp.com $(lsb_release -cs) main" | \
sudo tee /etc/apt/sources.list.d/hashicorp.list

sudo apt-get update -y
sudo apt-get install -y terraform

# ---------------------------------------------------------
# Install AWS CLI v2
# ---------------------------------------------------------
echo "Installing AWS CLI v2..."
cd /tmp
curl -s "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip -q awscliv2.zip
sudo ./aws/install
rm -rf aws awscliv2.zip

# Verify AWS CLI
aws --version

# ---------------------------------------------------------
# Configure Firewall
# ---------------------------------------------------------
echo "Configuring firewall..."
sudo ufw allow 8080
sudo ufw allow ssh
sudo ufw --force enable

# ---------------------------------------------------------
# Create Jenkins working directory and set permissions
# ---------------------------------------------------------
echo "Setting up Jenkins workspace..."
sudo mkdir -p /var/lib/jenkins/workspace
sudo chown -R jenkins:jenkins /var/lib/jenkins/workspace
sudo chmod 755 /var/lib/jenkins/workspace

# ---------------------------------------------------------
# Wait for Jenkins to initialize
# ---------------------------------------------------------
echo "Waiting for Jenkins to initialize..."
sleep 60

# ---------------------------------------------------------
# Gather System Info
# ---------------------------------------------------------
# Try to get public IP (works on cloud instances)
PUBLIC_IP=$(curl -s --connect-timeout 2 http://169.254.169.254/latest/meta-data/public-ipv4 || hostname -I | awk '{print $1}')
PRIVATE_IP=$(hostname -I | awk '{print $1}')
JENKINS_PASS=$(sudo cat /var/lib/jenkins/secrets/initialAdminPassword 2>/dev/null || echo "Run 'sudo cat /var/lib/jenkins/secrets/initialAdminPassword' to get the password")

# ---------------------------------------------------------
# Test Jenkins user sudo access
# ---------------------------------------------------------
echo "Testing Jenkins user sudo access..."
sudo -u jenkins sudo -n docker --version > /dev/null 2>&1 && echo "✅ Jenkins user can run docker commands without password" || echo "⚠️  Jenkins user docker access needs verification"
sudo -u jenkins sudo -n terraform -version > /dev/null 2>&1 && echo "✅ Jenkins user can run terraform commands without password" || echo "⚠️  Jenkins user terraform access needs verification"

# ---------------------------------------------------------
# Output Installation Summary
# ---------------------------------------------------------
echo "==========================================="
echo "✅ Installation Complete!"
echo "==========================================="
echo "Jenkins URL:  http://$PUBLIC_IP:8080"
echo "Private URL:  http://$PRIVATE_IP:8080"
echo "Admin Password: $JENKINS_PASS"
echo "-------------------------------------------"
echo "Jenkins User Permissions:"
echo "✅ Added to docker group"
echo "✅ Sudo access for: docker, terraform, aws, apt, systemctl"
echo "-------------------------------------------"
echo "Docker version:"
docker --version
echo "Terraform version:"
terraform -version
echo "AWS CLI version:"
aws --version
echo "==========================================="

# ---------------------------------------------------------
# Write Info File for Easy Access
# ---------------------------------------------------------
cat > /tmp/jenkins-info.txt << EOF
=====================================================
 Jenkins + Docker + Terraform + AWS CLI Installed
=====================================================
Public URL:  http://$PUBLIC_IP:8080
Private URL: http://$PRIVATE_IP:8080

Jenkins Admin Password:
$JENKINS_PASS

Jenkins User Permissions Configured:
- Added to docker group
- Passwordless sudo for: docker, terraform, aws, apt, systemctl

Installed Versions:
$(docker --version)
$(terraform -version | head -n 1)
$(aws --version)

Useful Commands:
  sudo systemctl status jenkins
  sudo docker ps
  terraform -help
  aws sts get-caller-identity

Test Jenkins user access:
  sudo -u jenkins sudo -n docker --version
  sudo -u jenkins sudo -n terraform -version

Logs: /var/log/jenkins-setup.log

Important: 
1. Log out and log back in for Docker group permissions to take effect!
2. Jenkins can now run docker, terraform, and aws commands without password
=====================================================
EOF

echo "Setup complete! Check /tmp/jenkins-info.txt for details."
echo "Important: Log out and log back in for Docker group permissions to take effect!"