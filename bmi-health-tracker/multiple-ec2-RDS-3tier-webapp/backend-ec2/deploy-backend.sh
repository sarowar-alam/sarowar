#!/bin/bash

# Backend EC2 Deployment Script for BMI Health Tracker
# This script automates the deployment of the Node.js backend with AWS RDS connection

set -e  # Exit on any error

echo "BMI Health Tracker - Backend Deployment"
echo "=========================================="

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_status() { echo -e "${GREEN}✓${NC} $1"; }
print_error() { echo -e "${RED}✗${NC} $1"; }
print_info() { echo -e "${YELLOW}ℹ${NC} $1"; }

# Get RDS Endpoint
read -p "Enter RDS Endpoint (e.g., bmi-tracker-db.xxxxx.us-east-1.rds.amazonaws.com): " RDS_ENDPOINT
if [ -z "$RDS_ENDPOINT" ]; then
    print_error "RDS Endpoint is required"
    exit 1
fi

read -p "Enter RDS Database Name (default: bmidb): " DB_NAME
DB_NAME=${DB_NAME:-bmidb}

read -p "Enter RDS Master Username (default: bmi_admin): " DB_USER
DB_USER=${DB_USER:-bmi_admin}

read -sp "Enter RDS Master Password: " DB_PASSWORD
echo ""

if [ -z "$DB_PASSWORD" ]; then
    print_error "Database password is required"
    exit 1
fi

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

# Install PM2
if ! command -v pm2 &> /dev/null; then
    print_info "Installing PM2..."
    npm install -g pm2
    print_status "PM2 installed"
else
    print_status "PM2 found"
fi

# Install PostgreSQL client for RDS connection testing
if ! command -v psql &> /dev/null; then
    print_info "Installing PostgreSQL client..."
    sudo apt install postgresql-client -y
    print_status "PostgreSQL client installed"
else
    print_status "PostgreSQL client found"
fi

# Create logs directory
mkdir -p logs

# Install npm dependencies
print_info "Installing npm dependencies..."
npm install --production
print_status "Dependencies installed"

# Create .env file
print_info "Creating .env file..."
cat > .env << EOF
PORT=3000
NODE_ENV=production
DATABASE_URL=postgresql://${DB_USER}:${DB_PASSWORD}@${RDS_ENDPOINT}:5432/${DB_NAME}
FRONTEND_URL=http://FRONTEND_PUBLIC_IP
EOF
print_status ".env file created"

# Test RDS connection
print_info "Testing RDS connection..."
export DATABASE_URL="postgresql://${DB_USER}:${DB_PASSWORD}@${RDS_ENDPOINT}:5432/${DB_NAME}"
if psql "$DATABASE_URL" -c "SELECT 1" > /dev/null 2>&1; then
    print_status "RDS connection successful"
else
    print_error "Failed to connect to RDS"
    print_info "Please check:"
    print_info "  1. RDS security group allows inbound from this EC2"
    print_info "  2. RDS endpoint and credentials are correct"
    print_info "  3. Database exists in RDS instance"
    exit 1
fi

# Start backend with PM2
print_info "Starting backend with PM2..."
pm2 delete bmi-backend 2>/dev/null || true
pm2 start src/server.js --name bmi-backend
pm2 save
print_status "Backend started with PM2"

# Setup PM2 startup
print_info "Configuring PM2 startup..."
sudo env PATH=$PATH:/usr/bin pm2 startup systemd -u $USER --hp $HOME

# Configure firewall
print_info "Configuring firewall..."
sudo ufw allow 3000/tcp
sudo ufw allow OpenSSH
sudo ufw --force enable
print_status "Firewall configured"

echo ""
echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}✓ Backend Deployment Complete!${NC}"
echo -e "${GREEN}================================${NC}"
echo ""
echo "Backend Private IP: $(hostname -I | awk '{print $1}')"
echo "Backend Port: 3000"
echo ""
echo "Verification:"
echo "  pm2 status"
echo "  pm2 logs bmi-backend"
echo "  curl http://localhost:3000/health"
echo ""
