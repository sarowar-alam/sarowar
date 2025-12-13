#!/bin/bash

# Frontend EC2 Deployment Script for BMI Health Tracker
# This script automates the deployment of the React frontend

set -e  # Exit on any error

echo "BMI Health Tracker - Frontend Deployment"
echo "==========================================="

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_status() { echo -e "${GREEN}✓${NC} $1"; }
print_error() { echo -e "${RED}✗${NC} $1"; }
print_info() { echo -e "${YELLOW}ℹ${NC} $1"; }

# Get Backend EC2 Private IP
read -p "Enter Backend EC2 Private IP (e.g., 10.0.2.20): " BACKEND_IP
if [ -z "$BACKEND_IP" ]; then
    print_error "Backend IP is required"
    exit 1
fi

print_info "Backend IP: $BACKEND_IP"

# Update system packages
print_info "Updating system packages..."
sudo apt update && sudo apt upgrade -y
print_status "System updated"

# Install Node.js via NVM
if ! command -v node &> /dev/null; then
    print_info "Installing Node.js via NVM..."
    curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
    export NVM_DIR="$HOME/.nvm"
    [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"
    nvm install 18
    nvm use 18
    print_status "Node.js installed"
else
    print_status "Node.js $(node -v) found"
fi

# Install Nginx
if ! command -v nginx &> /dev/null; then
    print_info "Installing Nginx..."
    sudo apt install nginx -y
    print_status "Nginx installed"
else
    print_status "Nginx found"
fi

# Install dependencies
print_info "Installing npm dependencies..."
npm install
print_status "Dependencies installed"

# Build frontend
print_info "Building frontend for production..."
npm run build
print_status "Frontend built successfully"

# Deploy to web directory
print_info "Deploying frontend to /var/www/bmi-health-tracker..."
sudo mkdir -p /var/www/bmi-health-tracker
sudo cp -r dist/* /var/www/bmi-health-tracker/
sudo chown -R www-data:www-data /var/www/bmi-health-tracker
print_status "Frontend files deployed"

# Configure Nginx
print_info "Configuring Nginx..."
sudo cp nginx.conf /etc/nginx/sites-available/bmi-frontend

# Replace backend IP in nginx config
sudo sed -i "s/BACKEND_PRIVATE_IP/$BACKEND_IP/g" /etc/nginx/sites-available/bmi-frontend

# Enable site
sudo ln -sf /etc/nginx/sites-available/bmi-frontend /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default

# Test Nginx configuration
print_info "Testing Nginx configuration..."
sudo nginx -t
print_status "Nginx configuration valid"

# Restart Nginx
print_info "Restarting Nginx..."
sudo systemctl restart nginx
sudo systemctl enable nginx
print_status "Nginx restarted and enabled"

# Configure firewall
print_info "Configuring firewall..."
sudo ufw allow 'Nginx Full'
sudo ufw allow OpenSSH
sudo ufw --force enable
print_status "Firewall configured"

echo ""
echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}✓ Frontend Deployment Complete!${NC}"
echo -e "${GREEN}================================${NC}"
echo ""
echo "Frontend URL: http://$(curl -s ifconfig.me)"
echo ""
echo "Verification:"
echo "  sudo systemctl status nginx"
echo "  curl http://localhost"
echo ""
