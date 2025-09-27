#!/bin/bash

# Update system
sudo apt update && sudo apt upgrade -y

# Install Ansible and required packages
sudo apt install -y ansible python3-pip python3-venv python3-full

# Install AWS CLI
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
sudo apt install -y unzip
unzip awscliv2.zip
sudo ./aws/install
rm -rf awscliv2.zip aws/

# Create Python virtual environment for Ansible and boto3
python3 -m venv ~/ansible-venv
source ~/ansible-venv/bin/activate

# Install Python packages in virtual environment
pip install --upgrade pip
pip install boto3 botocore

# Create project structure
mkdir -p ~/ansible-project/{inventory,group_vars,host_vars,roles,playbooks,files,templates}

# Configure AWS credentials
mkdir -p ~/.aws
cat > ~/.aws/credentials << EOF
[default]
aws_access_key_id = YOUR_ACCESS_KEY
aws_secret_access_key = YOUR_SECRET_KEY
region = us-east-1
EOF

chmod 600 ~/.aws/credentials

# Create ansible.cfg with virtual environment support
cat > ~/ansible-project/ansible.cfg << EOF
[defaults]
inventory = inventory/aws_ec2.yml
host_key_checking = False
remote_user = ubuntu
private_key_file = ~/.ssh/aws-key.pem
roles_path = roles
retry_files_enabled = False
timeout = 30
gathering = smart
interpreter_python = /usr/bin/python3

[privilege_escalation]
become = True
become_method = sudo
become_user = root
become_ask_pass = False

[ssh_connection]
ssh_args = -o ControlMaster=auto -o ControlPersist=60s -o PasswordAuthentication=no
pipelining = True
EOF

# Create activation script for easy use
cat > ~/ansible-project/activate-venv.sh << 'EOF'
#!/bin/bash
source ~/ansible-venv/bin/activate
echo "Ansible virtual environment activated"
echo "Run 'deactivate' to exit the virtual environment"
EOF

chmod +x ~/ansible-project/activate-venv.sh

# Create a wrapper script to run ansible with the virtual environment
cat > ~/ansible-project/run-ansible.sh << 'EOF'
#!/bin/bash
source ~/ansible-venv/bin/activate
ansible "\$@"
EOF

chmod +x ~/ansible-project/run-ansible.sh

echo "Ansible master setup complete!"
echo ""
echo "Important: Before running Ansible, you need to:"
echo "1. Update ~/.aws/credentials with your actual AWS credentials"
echo "2. Set up your SSH key: ~/.ssh/aws-key.pem"
echo ""
echo "To use Ansible, run:"
echo "  source ~/ansible-project/activate-venv.sh"
echo "Or use the wrapper: ~/ansible-project/run-ansible.sh"
echo ""
echo "Alternative: If you prefer not to use a virtual environment,"
echo "you can install the packages system-wide with:"
echo "  pip3 install --break-system-packages boto3 botocore"
