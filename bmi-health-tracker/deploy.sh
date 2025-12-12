#!/bin/bash

# BMI Health Tracker - Deployment Script for AWS EC2 Ubuntu
# This script automates the deployment process

set -e  # Exit on any error

echo "BMI Health Tracker Deployment Script"
echo "========================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if running as root
if [ "$EUID" -eq 0 ]; then 
   echo -e "${RED}Please do not run as root${NC}"
   exit 1
fi

# Function to print colored output
print_status() {
    echo -e "${GREEN}[OK]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_info() {
    echo -e "${YELLOW}ℹ${NC} $1"
}

# Check prerequisites
print_info "Checking prerequisites..."

# Check Node.js
if ! command -v node &> /dev/null; then
    print_error "Node.js is not installed"
    exit 1
fi
print_status "Node.js $(node -v) found"

# Check npm
if ! command -v npm &> /dev/null; then
    print_error "npm is not installed"
    exit 1
fi
print_status "npm $(npm -v) found"

# Check PostgreSQL
if ! command -v psql &> /dev/null; then
    print_error "PostgreSQL is not installed"
    exit 1
fi
print_status "PostgreSQL found"

# Check PM2
if ! command -v pm2 &> /dev/null; then
    print_info "PM2 not found. Installing..."
    npm install -g pm2
fi
print_status "PM2 found"

# Backend Setup
print_info "Setting up backend..."
cd backend

if [ ! -f .env ]; then
    print_error ".env file not found in backend directory"
    print_info "Please create .env file from .env.example"
    exit 1
fi
print_status ".env file exists"

# Install backend dependencies
print_info "Installing backend dependencies..."
npm install --production
print_status "Backend dependencies installed"

# Frontend Setup
print_info "Setting up frontend..."
cd ../frontend

# Install frontend dependencies
print_info "Installing frontend dependencies..."
npm install
print_status "Frontend dependencies installed"

# Build frontend
print_info "Building frontend for production..."
npm run build
print_status "Frontend built successfully"

# Deploy frontend
print_info "Deploying frontend to /var/www/bmi-health-tracker..."
sudo mkdir -p /var/www/bmi-health-tracker
sudo cp -r dist/* /var/www/bmi-health-tracker/
sudo chown -R www-data:www-data /var/www/bmi-health-tracker
print_status "Frontend deployed"

# Start backend with PM2
cd ../backend
print_info "Starting backend with PM2..."
pm2 delete bmi-backend 2>/dev/null || true
pm2 start src/server.js --name bmi-backend
pm2 save
print_status "Backend started with PM2"

# Setup PM2 startup
print_info "Configuring PM2 startup..."
sudo env PATH=$PATH:/usr/bin pm2 startup systemd -u $USER --hp $HOME

echo ""
echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}Deployment Complete!${NC}"
echo -e "${GREEN}================================${NC}"
echo ""
echo "Backend Status:"
pm2 status
echo ""
echo "Next Steps:"
echo "1. Verify nginx configuration: sudo nginx -t"
echo "2. Reload nginx: sudo systemctl reload nginx"
echo "3. Check PM2 logs: pm2 logs bmi-backend"
echo "4. Access your application at: http://YOUR_DOMAIN_OR_IP"
echo ""
