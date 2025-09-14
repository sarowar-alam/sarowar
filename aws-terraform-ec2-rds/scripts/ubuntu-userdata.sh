#!/bin/bash
# Update system
apt-get update -y
apt-get upgrade -y

# Install basic packages - FIXED LINE
apt-get install -y \
    git \
    unzip \
    curl \
    wget \
    telnet \
    mariadb-client    # ← CORRECTED: Added hyphen

# Install AWS CLI (alternative method for Ubuntu)
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install
rm -rf awscliv2.zip aws/

# Alternative: Install AWS CLI from Ubuntu repos (simpler)
# apt-get install -y awscli

# Create scripts directory
mkdir -p /opt/scripts

# Set hostname
hostnamectl set-hostname ubuntu-server

# Install additional useful packages
apt-get install -y \
    software-properties-common \
    apt-transport-https \
    ca-certificates \
    gnupg \
    lsb-release

# Clean up
apt-get autoremove -y
apt-get clean

echo "Ubuntu userdata script completed successfully"