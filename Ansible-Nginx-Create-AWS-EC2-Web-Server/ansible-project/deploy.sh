#!/bin/bash

# Ansible Deployment Script - Static Configuration
set -e

source ~/ansible-venv/bin/activate

echo "🚀 Starting Ansible Deployment (Static Configuration)"
echo "====================================================="

# Test AWS connectivity
echo "🔍 Testing AWS connectivity..."
ansible localhost -m amazon.aws.aws_caller_info

# Create infrastructure
echo "🏗️ Creating AWS infrastructure..."
ansible-playbook playbooks/01-setup-aws-infrastructure.yml

# Wait for instances to be ready
echo "⏳ Waiting for instances to be ready..."
sleep 30

# Refresh inventory
echo "🔄 Refreshing AWS inventory..."
ansible-inventory -i inventory/aws_ec2.yml --graph

# Configure servers
echo "⚙️ Configuring servers..."
ansible-playbook playbooks/02-configure-servers.yml

echo "✅ Deployment complete!"
echo "📋 Check ~/ansible-project/infrastructure-details.txt for connection info"
