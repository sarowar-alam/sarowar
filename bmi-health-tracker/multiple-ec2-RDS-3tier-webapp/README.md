# BMI Health Tracker - AWS Deployment Guide

## 🎯 What You'll Build

A health tracking web application on AWS with:
- **Frontend**: React website accessible from internet
- **Backend**: Node.js API server (secure, private)
- **Database**: AWS RDS PostgreSQL (managed database)

**Total Time**: 45-60 minutes | **Monthly Cost**: ~$32 (Year 1) → ~$83 (Year 2+)

---

## 📋 Before You Start

### What You Need
- [ ] AWS Account (create at [aws.amazon.com](https://aws.amazon.com))
- [ ] AWS Free Tier eligible (for cost savings)
- [ ] Basic understanding of: SSH, command line, web applications
- [ ] Computer with SSH client installed

### What You'll Learn
- ✅ Setting up AWS networking (VPC, subnets)
- ✅ Deploying EC2 instances
- ✅ Configuring AWS RDS database
- ✅ Setting up security groups
- ✅ Deploying a full-stack application

---

## 🏗️ Architecture Simplified

```
┌─────────────────────────────────────────────────────────────┐
│                         Internet                            │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
              ┌────────────────┐
              │  Frontend EC2  │ ← Users access this (has public IP)
              │  (React + Nginx)│
              └────────┬───────┘
                       │
                       ▼
              ┌────────────────┐
              │  Backend EC2   │ ← Private (no public IP)
              │  (Node.js API) │
              └────────┬───────┘
                       │
                       ▼
              ┌────────────────┐
              │  AWS RDS       │ ← Database (private, managed)
              │  (PostgreSQL)  │
              └────────────────┘
```

**Why this design?**
- Frontend is public (users can access)
- Backend is private (more secure)
- Database is private (most secure)
- Each layer can only talk to the next layer

---

## 🚀 Quick Start Deployment (Step-by-Step)

### Phase 1: AWS Infrastructure Setup (20 minutes)

This creates the networking foundation.

#### Step 1.1: Create VPC
1. Go to AWS Console → VPC
2. Click "Create VPC"
3. Enter:
   - Name: `bmi-tracker-vpc`
   - IPv4 CIDR: `10.0.0.0/16`
4. Click "Create VPC"

**✅ Validate Now:**
```bash
# You should see your VPC listed in AWS Console
# Status should show "Available"
```

#### Step 1.2: Create Subnets
Create 3 subnets (one for frontend, two for backend/database):

**Subnet 1 - Public (for Frontend):**
- Name: `bmi-public-subnet`
- VPC: `bmi-tracker-vpc`
- Availability Zone: Choose any (e.g., `us-east-1a`)
- IPv4 CIDR: `10.0.1.0/24`

**Subnet 2 - Private (for Backend):**
- Name: `bmi-private-subnet-1`
- VPC: `bmi-tracker-vpc`
- Availability Zone: Same as Subnet 1 (e.g., `us-east-1a`)
- IPv4 CIDR: `10.0.2.0/24`

**Subnet 3 - Private (for RDS Multi-AZ):**
- Name: `bmi-private-subnet-2`
- VPC: `bmi-tracker-vpc`
- Availability Zone: **Different** from Subnet 1 (e.g., `us-east-1b`)
- IPv4 CIDR: `10.0.3.0/24`

**✅ Validate Now:**
```bash
# In AWS Console → VPC → Subnets
# You should see all 3 subnets with "Available" status
```

#### Step 1.3: Create Internet Gateway
1. Go to VPC → Internet Gateways
2. Click "Create internet gateway"
3. Name: `bmi-igw`
4. Click "Create"
5. Select the gateway → Actions → Attach to VPC
6. Choose `bmi-tracker-vpc`

**✅ Validate Now:**
```bash
# Your IGW should show "Attached" status to bmi-tracker-vpc
```

#### Step 1.4: Create NAT Gateway
1. Go to VPC → NAT Gateways
2. Click "Create NAT gateway"
3. Enter:
   - Name: `bmi-nat-gateway`
   - Subnet: `bmi-public-subnet` (important!)
   - Connectivity type: Public
   - Click "Allocate Elastic IP"
4. Click "Create NAT gateway"
5. Wait 2-3 minutes for status to change to "Available"

**✅ Validate Now:**
```bash
# Status should be "Available" (not "Pending")
# Should have an Elastic IP assigned
```

#### Step 1.5: Configure Route Tables
**Public Route Table:**
1. VPC → Route Tables
2. Find the route table associated with `bmi-tracker-vpc`
3. Rename it: `bmi-public-rt`
4. Go to "Routes" tab → Edit routes → Add route:
   - Destination: `0.0.0.0/0`
   - Target: `bmi-igw` (your Internet Gateway)
5. Save
6. Go to "Subnet associations" → Edit → Associate with `bmi-public-subnet`

**Private Route Table:**
1. Create new route table: `bmi-private-rt`
2. VPC: `bmi-tracker-vpc`
3. Routes tab → Edit routes → Add route:
   - Destination: `0.0.0.0/0`
   - Target: `bmi-nat-gateway` (your NAT Gateway)
4. Save
5. Subnet associations → Associate with:
   - `bmi-private-subnet-1`
   - `bmi-private-subnet-2`

**✅ Validate Now:**
```bash
# Public RT should route 0.0.0.0/0 to Internet Gateway
# Private RT should route 0.0.0.0/0 to NAT Gateway
# Check subnet associations are correct
```

#### Step 1.6: Create Security Groups

**Security Group 1 - Frontend:**
1. VPC → Security Groups → Create security group
2. Name: `frontend-sg`
3. Description: `Frontend EC2 security group`
4. VPC: `bmi-tracker-vpc`
5. Inbound rules:
   - Type: HTTP, Port: 80, Source: `0.0.0.0/0` (anywhere)
   - Type: SSH, Port: 22, Source: `Your IP` (click "My IP")
6. Outbound rules: Leave default (all traffic allowed)

**Security Group 2 - Backend:**
1. Create security group
2. Name: `backend-sg`
3. Description: `Backend EC2 security group`
4. VPC: `bmi-tracker-vpc`
5. Inbound rules:
   - Type: Custom TCP, Port: 3000, Source: `frontend-sg` (select the security group)
   - Type: SSH, Port: 22, Source: `Your IP`
6. Outbound rules: Leave default

**Security Group 3 - RDS:**
1. Create security group
2. Name: `rds-sg`
3. Description: `RDS database security group`
4. VPC: `bmi-tracker-vpc`
5. Inbound rules:
   - Type: PostgreSQL, Port: 5432, Source: `backend-sg` (select the security group)
6. Outbound rules: Leave default

**✅ Validate Now:**
```bash
# All 3 security groups should be listed
# Check inbound rules are correct for each
# frontend-sg: port 80 from anywhere, port 22 from your IP
# backend-sg: port 3000 from frontend-sg, port 22 from your IP
# rds-sg: port 5432 from backend-sg
```

#### Step 1.7: Create EC2 Key Pair
1. EC2 → Key Pairs → Create key pair
2. Name: `bmi-keypair`
3. Type: RSA
4. Format: `.pem` (for Mac/Linux) or `.ppk` (for Windows PuTTY)
5. Click "Create"
6. **Important**: Save the downloaded file securely - you can't download it again!

**✅ Validate Now:**
```bash
# Key pair should be listed in EC2 console
# Downloaded .pem or .ppk file should be saved safely
```

---

### Phase 2: Database Setup (15 minutes)

#### Step 2.1: Create DB Subnet Group
1. Go to RDS → Subnet groups
2. Click "Create DB subnet group"
3. Enter:
   - Name: `bmi-db-subnet-group`
   - Description: `Subnet group for BMI tracker RDS`
   - VPC: `bmi-tracker-vpc`
   - Availability Zones: Select the 2 AZs you used (e.g., us-east-1a, us-east-1b)
   - Subnets: Select `bmi-private-subnet-1` and `bmi-private-subnet-2`
4. Click "Create"

**✅ Validate Now:**
```bash
# DB subnet group should be listed with "Complete" status
# Should show 2 subnets from 2 different AZs
```

#### Step 2.2: Create RDS PostgreSQL Instance
1. Go to RDS → Databases → Create database
2. Choose:
   - **Creation method**: Standard create
   - **Engine**: PostgreSQL
   - **Version**: PostgreSQL 15.x (latest)
   - **Templates**: Free tier (if eligible) or Dev/Test

3. **Settings**:
   - DB instance identifier: `bmi-tracker-db`
   - Master username: `bmi_admin`
   - Master password: Create a strong password (write it down!)
   - Confirm password

4. **Instance configuration**:
   - DB instance class: `db.t3.micro` (Free Tier eligible)

5. **Storage**:
   - Storage type: General Purpose SSD (gp2)
   - Allocated storage: `20` GB
   - Disable storage autoscaling (for cost control)

6. **Connectivity**:
   - VPC: `bmi-tracker-vpc`
   - DB subnet group: `bmi-db-subnet-group`
   - Public access: **No** (important!)
   - VPC security group: Choose existing → `rds-sg`
   - Availability Zone: No preference

7. **Additional configuration**:
   - Initial database name: `bmidb`
   - Backup retention: 7 days
   - Encryption: Enable (recommended)

8. Click "Create database"
9. **Wait 10-15 minutes** for status to change to "Available"

**✅ Validate Now:**
```bash
# Wait until status is "Available" (not "Creating" or "Backing-up")
# Note down the Endpoint (looks like: bmi-tracker-db.xxxxx.us-east-1.rds.amazonaws.com)
# Note down the Port (should be 5432)
```

**Save These Details:**
```
RDS Endpoint: bmi-tracker-db.xxxxx.us-east-1.rds.amazonaws.com
Port: 5432
Database: bmidb
Username: bmi_admin
Password: [your password]
```

---

### Phase 3: Backend EC2 Deployment (15 minutes)

#### Step 3.1: Launch Backend EC2 Instance
1. EC2 → Instances → Launch instances
2. Enter:
   - **Name**: `bmi-backend-server`
   - **AMI**: Ubuntu Server 22.04 LTS (Free tier eligible)
   - **Instance type**: `t2.micro` (Free tier eligible)
   - **Key pair**: `bmi-keypair`
   
3. **Network settings** - Click "Edit":
   - VPC: `bmi-tracker-vpc`
   - Subnet: `bmi-private-subnet-1`
   - Auto-assign public IP: **Disable** (backend should be private)
   - Firewall: Select existing security group → `backend-sg`

4. **Storage**: 8 GB (default is fine)
5. Click "Launch instance"
6. Wait 2 minutes for status: "Running" with "2/2 checks passed"

**✅ Validate Now:**
```bash
# Instance state: Running
# Status checks: 2/2 passed
# No public IP (since it's in private subnet)
# Private IP should be in 10.0.2.x range
```

**Note the Private IP** (e.g., 10.0.2.20) - you'll need this later!

#### Step 3.2: Connect to Backend EC2
Since backend has no public IP, use AWS Systems Manager Session Manager:

1. EC2 → Instances → Select `bmi-backend-server`
2. Click "Connect" button
3. Choose "Session Manager" tab
4. Click "Connect"

**Alternative** (if Session Manager not available):
Use a bastion host or temporarily add a public IP for setup, then remove it.

**✅ Validate Now:**
```bash
# You should now have a terminal connected to the backend server
# Run: whoami
# Should show: ubuntu or ssm-user
```

#### Step 3.3: Upload Backend Files
From your local computer (not on EC2), upload the backend files:

```bash
# Make backend files executable
chmod +x backend-ec2/deploy-backend.sh

# Upload files (replace XX.XX.XX.XX with a bastion host or use AWS transfer)
# If using bastion/jump host:
scp -i bmi-keypair.pem -r backend-ec2/ ubuntu@BASTION_IP:~/

# Then from bastion:
scp -r backend-ec2/ ubuntu@10.0.2.20:~/
```

**Better Option**: Use AWS S3 as intermediate storage:
```bash
# From your computer
cd multiple-ec2-RDS-3tier-webapp
aws s3 mb s3://bmi-deployment-temp-[your-name]
aws s3 cp backend-ec2/ s3://bmi-deployment-temp-[your-name]/backend-ec2/ --recursive

# From backend EC2 (in Session Manager)
aws s3 cp s3://bmi-deployment-temp-[your-name]/backend-ec2/ ~/backend-ec2/ --recursive
```

**✅ Validate Now:**
```bash
# On backend EC2, run:
ls ~/backend-ec2/
# Should see: deploy-backend.sh, package.json, src/, etc.
```

#### Step 3.4: Run Backend Deployment Script
On the backend EC2 instance:

```bash
cd ~/backend-ec2
chmod +x deploy-backend.sh
./deploy-backend.sh
```

**The script will ask you for:**
1. RDS endpoint (paste from Step 2.2)
2. Database name: `bmidb`
3. Database user: `bmi_admin`
4. Database password: [your RDS password]

**What the script does:**
- Installs Node.js 18
- Installs npm packages
- Creates .env file with your RDS credentials
- Tests database connection
- Starts the app with PM2

**✅ Validate Now:**
```bash
# After script finishes, test:
curl http://localhost:3000/health

# Should return:
# {"status":"ok","environment":"production","database":"AWS RDS PostgreSQL"}

# Check PM2 status:
pm2 status

# Should show:
# │ bmi-backend │ 0  │ online │
```

#### Step 3.5: Run Database Migration
Still on backend EC2:

```bash
cd ~/backend-ec2

# Test database connection first
export DATABASE_URL="postgresql://bmi_admin:YOUR_PASSWORD@YOUR_RDS_ENDPOINT:5432/bmidb"
psql $DATABASE_URL -c "SELECT version();"

# Should show PostgreSQL version

# Run migration
psql $DATABASE_URL < 001_create_measurements_rds.sql

# Verify table created
psql $DATABASE_URL -c "\dt"

# Should show: measurements table
```

**✅ Validate Now:**
```bash
# Query the table
psql $DATABASE_URL -c "SELECT COUNT(*) FROM measurements;"

# Should return: count = 0 (empty table, but exists)
```

---

### Phase 4: Frontend EC2 Deployment (15 minutes)

#### Step 4.1: Allocate Elastic IP
1. EC2 → Elastic IPs → Allocate Elastic IP address
2. Click "Allocate"
3. **Note down the Elastic IP** (e.g., 54.123.45.67)

**✅ Validate Now:**
```bash
# Elastic IP should be listed but not associated yet
```

#### Step 4.2: Launch Frontend EC2 Instance
1. EC2 → Instances → Launch instances
2. Enter:
   - **Name**: `bmi-frontend-server`
   - **AMI**: Ubuntu Server 22.04 LTS
   - **Instance type**: `t2.micro`
   - **Key pair**: `bmi-keypair`
   
3. **Network settings** - Click "Edit":
   - VPC: `bmi-tracker-vpc`
   - Subnet: `bmi-public-subnet`
   - Auto-assign public IP: **Enable**
   - Firewall: Select existing → `frontend-sg`

4. Click "Launch instance"
5. Wait for "Running" status

**✅ Validate Now:**
```bash
# Instance state: Running
# Should have BOTH public and private IP
```

#### Step 4.3: Associate Elastic IP
1. EC2 → Elastic IPs
2. Select your Elastic IP → Actions → Associate Elastic IP
3. Instance: `bmi-frontend-server`
4. Click "Associate"

**✅ Validate Now:**
```bash
# Elastic IP should now show "Associated" status
# Try to ping it (may not respond, that's ok):
ping YOUR_ELASTIC_IP
```

#### Step 4.4: Connect to Frontend EC2
```bash
# From your computer
ssh -i bmi-keypair.pem ubuntu@YOUR_ELASTIC_IP

# First time will ask "Are you sure?", type: yes
```

**✅ Validate Now:**
```bash
# You should be logged into the frontend server
# Prompt should show: ubuntu@ip-10-0-1-x:~$
```

#### Step 4.5: Upload Frontend Files
From your local computer (open a new terminal, don't close SSH):

```bash
# From your computer
cd multiple-ec2-RDS-3tier-webapp
chmod +x frontend-ec2/deploy-frontend.sh

# Upload files
scp -i bmi-keypair.pem -r frontend-ec2/ ubuntu@YOUR_ELASTIC_IP:~/
```

**✅ Validate Now:**
```bash
# On frontend EC2, check:
ls ~/frontend-ec2/
# Should see: deploy-frontend.sh, package.json, src/, nginx.conf, etc.
```

#### Step 4.6: Update Nginx Configuration
Before running deployment, update nginx.conf with your backend IP:

```bash
# On frontend EC2
cd ~/frontend-ec2
nano nginx.conf

# Find this line (around line 24):
#     proxy_pass http://10.0.2.20:3000;

# Replace 10.0.2.20 with your actual backend private IP from Step 3.1

# Save: Ctrl+O, Enter, Ctrl+X
```

**✅ Validate Now:**
```bash
# Verify the change:
grep "proxy_pass" nginx.conf
# Should show your correct backend IP
```

#### Step 4.7: Run Frontend Deployment Script
```bash
cd ~/frontend-ec2
chmod +x deploy-frontend.sh
./deploy-frontend.sh
```

**The script will:**
- Install Node.js 18
- Install npm packages
- Build React app
- Install and configure Nginx
- Copy files to /var/www/bmi-frontend
- Start Nginx

**This takes 3-5 minutes** due to npm install and React build.

**✅ Validate Now:**
```bash
# After script finishes, test locally:
curl http://localhost

# Should return HTML with "<title>BMI Health Tracker</title>"

# Check Nginx status:
sudo systemctl status nginx

# Should show: active (running)
```

---

### Phase 5: Final Testing (5 minutes)

#### Step 5.1: Access from Browser
1. Open your web browser
2. Go to: `http://YOUR_ELASTIC_IP`
3. You should see the BMI Health Tracker website!

**✅ Validate Now:**
```bash
# Website should load completely
# No errors in browser console (F12)
# All buttons and forms should be visible
```

#### Step 5.2: Test BMI Calculation
1. Fill in the form:
   - Weight: 70 kg
   - Height: 175 cm
   - Age: 30
   - Sex: Male
   - Activity: Moderate
2. Click "Calculate BMI"
3. Results should appear immediately

**✅ Validate Now:**
```bash
# Should see results like:
# BMI: 22.9 (Normal)
# BMR: ~1705 calories
# Daily Calories: ~2643 calories
```

#### Step 5.3: Verify Data Storage
1. Submit a few more measurements (change the values)
2. Check if the chart shows data

**On backend EC2**, verify data in database:
```bash
psql $DATABASE_URL -c "SELECT * FROM measurements ORDER BY created_at DESC LIMIT 5;"

# Should show your test measurements
```

**✅ Validate Now:**
```bash
# Database should have your test entries
# Chart on website should show the data points
```

#### Step 5.4: Test API Endpoints
```bash
# From your computer or frontend EC2:
curl http://YOUR_ELASTIC_IP/api/health

# Should return: {"status":"ok","environment":"production",...}

curl http://YOUR_ELASTIC_IP/api/measurements

# Should return: {"rows":[...your data...]}
```

**✅ Final Validation Checklist:**
- [ ] Website loads in browser
- [ ] Can submit measurements
- [ ] Results display correctly
- [ ] Data is saved to database
- [ ] Chart displays trend
- [ ] API endpoints respond
- [ ] No errors in browser console
- [ ] Backend PM2 shows "online" status
- [ ] Nginx shows "active" status

---

## 🎉 Congratulations!

You've successfully deployed a 3-tier web application on AWS!

**What you built:**
- ✅ VPC with public and private subnets
- ✅ Internet Gateway and NAT Gateway
- ✅ 3 Security Groups with proper access control
- ✅ Frontend EC2 with React and Nginx
- ✅ Backend EC2 with Node.js and PM2
- ✅ AWS RDS PostgreSQL database
- ✅ Complete health tracking application

**Your application is now accessible at:** `http://YOUR_ELASTIC_IP`

---

## 💰 Cost Tracking

### Current Monthly Cost (Free Tier):
- Frontend EC2: $0 (750 hrs free)
- Backend EC2: $0 (750 hrs free)
- RDS: $0 (750 hrs free)
- **NAT Gateway: ~$32** (not free!)
- **Total: ~$32/month**

### After Free Tier (Year 2+):
- EC2 instances: ~$16/month
- RDS: ~$25/month
- NAT Gateway: ~$32/month
- Storage & transfer: ~$10/month
- **Total: ~$83/month**

### 💡 Cost Saving Tips:
1. **Stop instances when not in use:**
   ```bash
   # Stop instances (doesn't delete, just stops billing)
   AWS Console → EC2 → Select instance → Instance state → Stop
   ```

2. **Stop NAT Gateway when not developing:**
   - Delete NAT Gateway when not needed
   - Recreate when you need to develop
   - Saves $32/month

3. **Use Reserved Instances:**
   - If running 24/7, buy 1-year reserved instances
   - Saves 30-40% on EC2 and RDS costs

---

## 🔧 Troubleshooting Common Issues

### Issue 1: Cannot Access Website
**Symptoms:** Browser shows "This site can't be reached" or timeout

**Check these in order:**
```bash
# 1. Is Frontend EC2 running?
AWS Console → EC2 → Check status

# 2. Is Elastic IP associated?
AWS Console → Elastic IPs → Should show "Associated"

# 3. Is security group correct?
AWS Console → EC2 → bmi-frontend-server → Security tab
Should allow: HTTP (80) from 0.0.0.0/0

# 4. Is Nginx running?
SSH to frontend:
sudo systemctl status nginx
# Should show: active (running)

# If not running:
sudo systemctl start nginx
```

**✅ Test:**
```bash
curl http://YOUR_ELASTIC_IP
# Should return HTML
```

---

### Issue 2: Website Loads But API Fails
**Symptoms:** Website shows but submitting form gives errors

**Check these in order:**
```bash
# 1. Is Backend EC2 running?
# On backend EC2:
pm2 status
# Should show: online

# If not running:
pm2 restart bmi-backend

# 2. Is frontend nginx pointing to correct backend IP?
# On frontend EC2:
grep "proxy_pass" /etc/nginx/sites-available/bmi-frontend
# Should match your backend private IP (10.0.2.x)

# If wrong:
sudo nano /etc/nginx/sites-available/bmi-frontend
# Fix the proxy_pass line
sudo systemctl restart nginx

# 3. Can frontend reach backend?
# On frontend EC2:
curl http://10.0.2.X:3000/health
# Should return: {"status":"ok"}
```

**✅ Test:**
```bash
# From your browser, open developer tools (F12)
# Check Network tab when submitting form
# API calls to /api/measurements should return 200 OK
```

---

### Issue 3: Database Connection Fails
**Symptoms:** Backend logs show "Database connection failed"

**Check these in order:**
```bash
# 1. Is RDS available?
AWS Console → RDS → Check status = "Available"

# 2. Is RDS endpoint correct in .env?
# On backend EC2:
cat ~/backend-ec2/.env
# DATABASE_URL should match your RDS endpoint

# 3. Can backend reach RDS?
# On backend EC2:
export DATABASE_URL="your-database-url-here"
psql $DATABASE_URL -c "SELECT 1;"
# Should return: 1

# 4. Check security group
AWS Console → RDS → bmi-tracker-db → Connectivity
Security group should be: rds-sg
Inbound rule should allow: PostgreSQL (5432) from backend-sg

# 5. Restart backend with correct config
pm2 restart bmi-backend
pm2 logs bmi-backend
# Should show: "Database connected successfully"
```

**✅ Test:**
```bash
curl http://localhost:3000/health
# Should return database: "AWS RDS PostgreSQL"
```

---

### Issue 4: Backend Can't Install Packages
**Symptoms:** npm install fails on backend EC2

**This means NAT Gateway issue:**
```bash
# 1. Is NAT Gateway running?
AWS Console → VPC → NAT Gateways
Status should be: Available

# 2. Is route table correct?
AWS Console → VPC → Route Tables → bmi-private-rt
Routes should have: 0.0.0.0/0 → nat-xxxxx (your NAT Gateway)

# 3. Is subnet associated?
Same route table → Subnet associations
Should include: bmi-private-subnet-1 and bmi-private-subnet-2

# 4. Test internet from backend
# On backend EC2:
ping -c 3 8.8.8.8
# Should get responses (not 100% packet loss)
```

**✅ Test:**
```bash
curl https://www.google.com
# Should return HTML
```

---

### Issue 5: RDS Password Error
**Symptoms:** "password authentication failed"

**Solution:**
```bash
# 1. Reset RDS password in AWS Console
AWS Console → RDS → bmi-tracker-db → Modify
Scroll to "Credentials" → New master password
Save changes

# 2. Update backend .env file
# On backend EC2:
nano ~/backend-ec2/.env
# Update password in DATABASE_URL

# 3. Restart backend
pm2 restart bmi-backend
pm2 logs bmi-backend --lines 20
```

---

### Issue 6: Port 3000 Already in Use
**Symptoms:** Backend won't start, "port already in use"

**Solution:**
```bash
# On backend EC2:

# Find process using port 3000
sudo lsof -i :3000

# Stop PM2 process
pm2 delete bmi-backend

# Or kill specific process
kill -9 <PID>

# Restart
pm2 start ~/backend-ec2/ecosystem.config.js
```

---

## 📊 Monitoring Your Application

### Check Application Health

**Frontend Health:**
```bash
# From anywhere:
curl http://YOUR_ELASTIC_IP/

# Should return HTML with title "BMI Health Tracker"
```

**Backend Health:**
```bash
# From frontend EC2:
curl http://10.0.2.X:3000/health

# Should return:
# {"status":"ok","environment":"production","database":"AWS RDS PostgreSQL"}
```

**Database Health:**
```bash
# On backend EC2:
psql $DATABASE_URL -c "SELECT COUNT(*) FROM measurements;"

# Shows number of records in database
```

### View Logs

**Backend Application Logs:**
```bash
# On backend EC2:
pm2 logs bmi-backend

# Last 50 lines:
pm2 logs bmi-backend --lines 50

# Only errors:
pm2 logs bmi-backend --err
```

**Frontend Web Server Logs:**
```bash
# On frontend EC2:

# Access logs (successful requests):
sudo tail -f /var/log/nginx/bmi-frontend-access.log

# Error logs:
sudo tail -f /var/log/nginx/bmi-frontend-error.log

# Last 100 lines:
sudo tail -100 /var/log/nginx/bmi-frontend-error.log
```

**RDS Database Logs:**
```bash
# In AWS Console:
RDS → bmi-tracker-db → Logs & events
View: Error logs, Slow query logs
```

### AWS CloudWatch Metrics

**Check automatically collected metrics:**
1. Go to CloudWatch → Dashboards
2. Or CloudWatch → Metrics → All metrics
3. Look for:
   - EC2 metrics (CPU, Network, Disk)
   - RDS metrics (Connections, CPU, Storage)

**Set up alarms (recommended):**
```
High CPU Alert:
- Metric: EC2 > CPUUtilization
- Threshold: > 80%
- Duration: 5 minutes
- Action: Email notification

Low Storage Alert:
- Metric: RDS > FreeStorageSpace
- Threshold: < 2 GB
- Action: Email notification
```

---

## 🛠️ Maintenance Tasks

### Daily Checks
```bash
# Check backend is running
pm2 status

# Check database has new data
psql $DATABASE_URL -c "SELECT COUNT(*) FROM measurements WHERE created_at > NOW() - INTERVAL '1 day';"
```

### Weekly Tasks
```bash
# Check RDS backups
AWS Console → RDS → bmi-tracker-db → Maintenance & backups
Verify: Automated backups exist

# Review logs for errors
pm2 logs bmi-backend --lines 1000 | grep -i error
sudo grep -i error /var/log/nginx/bmi-frontend-error.log
```

### Monthly Tasks
```bash
# Update packages on Backend EC2
sudo apt update
sudo apt upgrade -y

# Update Node.js packages
cd ~/backend-ec2
npm outdated
npm update

# Update packages on Frontend EC2
sudo apt update
sudo apt upgrade -y

# Check costs in AWS Console
AWS Console → Billing → Bills
```

---

## 🔄 Making Changes to Your Application

### Update Frontend Code

```bash
# 1. Make changes to frontend-ec2/ files on your computer

# 2. Upload new files
scp -i bmi-keypair.pem -r frontend-ec2/ ubuntu@YOUR_ELASTIC_IP:~/

# 3. On frontend EC2, rebuild
cd ~/frontend-ec2
npm install  # if you added packages
npm run build

# 4. Copy to web directory
sudo cp -r dist/* /var/www/bmi-frontend/

# 5. Restart Nginx
sudo systemctl restart nginx
```

**✅ Test:** Refresh browser (Ctrl+F5 to clear cache)

### Update Backend Code

```bash
# 1. Make changes to backend-ec2/ files on your computer

# 2. Upload new files (via S3 or bastion)
aws s3 cp backend-ec2/src/ s3://bmi-deployment-temp/backend-ec2/src/ --recursive

# 3. On backend EC2, download
aws s3 cp s3://bmi-deployment-temp/backend-ec2/src/ ~/backend-ec2/src/ --recursive

# 4. Install new packages (if any)
cd ~/backend-ec2
npm install

# 5. Restart application
pm2 restart bmi-backend

# 6. Check logs
pm2 logs bmi-backend --lines 20
```

**✅ Test:**
```bash
curl http://localhost:3000/health
```

### Add Database Table/Column

```bash
# 1. Create migration file on your computer
# backend-ec2/002_add_new_column.sql

# 2. Upload to backend EC2

# 3. On backend EC2, run migration
psql $DATABASE_URL < ~/backend-ec2/002_add_new_column.sql

# 4. Verify
psql $DATABASE_URL -c "\d measurements"
```

---

## 📚 Project Files Reference

### What Each File Does

**Frontend Files:**
```
frontend-ec2/
├── src/
│   ├── App.jsx           → Main React component
│   ├── api.js            → API calls to backend
│   ├── index.css         → Styling
│   ├── main.jsx          → React entry point
│   └── components/
│       ├── MeasurementForm.jsx  → Input form
│       └── TrendChart.jsx       → Chart display
├── package.json          → Dependencies list
├── vite.config.js        → Build configuration
├── nginx.conf            → Web server config
└── deploy-frontend.sh    → Automated setup script
```

**Backend Files:**
```
backend-ec2/
├── src/
│   ├── server.js         → Express server setup
│   ├── routes.js         → API endpoint definitions
│   ├── db.js             → RDS database connection
│   └── calculations.js   → BMI/BMR math formulas
├── package.json          → Dependencies
├── ecosystem.config.js   → PM2 process config
├── .env                  → Database credentials (created by script)
├── deploy-backend.sh     → Automated setup script
└── 001_create_measurements_rds.sql  → Database schema
```

---

## 🎓 Learning Resources

### AWS Services Used

**VPC (Virtual Private Cloud):**
- Isolated network for your resources
- Like your own data center in the cloud
- [AWS VPC Docs](https://docs.aws.amazon.com/vpc/)

**EC2 (Elastic Compute Cloud):**
- Virtual servers in the cloud
- Like renting a computer from AWS
- [AWS EC2 Docs](https://docs.aws.amazon.com/ec2/)

**RDS (Relational Database Service):**
- Managed PostgreSQL database
- AWS handles backups, updates, scaling
- [AWS RDS Docs](https://docs.aws.amazon.com/rds/)

**Security Groups:**
- Virtual firewalls
- Control what traffic can reach your servers
- [Security Groups Guide](https://docs.aws.amazon.com/vpc/latest/userguide/VPC_SecurityGroups.html)

### Technology Stack

**React (Frontend):**
- JavaScript library for building UIs
- [React Documentation](https://react.dev/)

**Node.js + Express (Backend):**
- JavaScript runtime for server-side code
- [Node.js Docs](https://nodejs.org/docs/)
- [Express.js Guide](https://expressjs.com/)

**PostgreSQL (Database):**
- Powerful open-source database
- [PostgreSQL Tutorial](https://www.postgresql.org/docs/15/tutorial.html)

**Nginx (Web Server):**
- High-performance web server
- [Nginx Beginner's Guide](http://nginx.org/en/docs/beginners_guide.html)

---

## 📖 Additional Documentation

### Detailed Guides
- **[DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)** - Alternative detailed deployment steps
- **[AGENT.md](AGENT.md)** - Complete project reconstruction guide with all file contents
- **[ARCHITECTURE_DIAGRAM.md](ARCHITECTURE_DIAGRAM.md)** - Visual architecture diagrams and data flow
- **[PROJECT_SUMMARY.md](PROJECT_SUMMARY.md)** - High-level overview and design decisions

### Quick Reference
- **API Endpoints**: See section above
- **Security Groups**: Step 1.6 in deployment
- **Database Schema**: `backend-ec2/001_create_measurements_rds.sql`
- **Cost Tracking**: See cost section above

---

## ⚡ Quick Commands Reference

### EC2 Management
```bash
# Start instance
AWS Console → EC2 → Instance State → Start

# Stop instance
AWS Console → EC2 → Instance State → Stop

# Connect via SSH
ssh -i bmi-keypair.pem ubuntu@YOUR_IP

# Connect via Session Manager (no public IP needed)
AWS Console → EC2 → Connect → Session Manager
```

### Application Management
```bash
# Backend (PM2):
pm2 status              # Check status
pm2 restart all         # Restart all apps
pm2 logs bmi-backend    # View logs
pm2 stop bmi-backend    # Stop app
pm2 start bmi-backend   # Start app

# Frontend (Nginx):
sudo systemctl status nginx    # Check status
sudo systemctl restart nginx   # Restart
sudo systemctl stop nginx      # Stop
sudo systemctl start nginx     # Start
```

### Database Operations
```bash
# Connect to database
psql $DATABASE_URL

# Quick queries
psql $DATABASE_URL -c "SELECT COUNT(*) FROM measurements;"
psql $DATABASE_URL -c "SELECT * FROM measurements ORDER BY created_at DESC LIMIT 5;"

# Inside psql:
\dt              # List tables
\d measurements  # Describe table
\q               # Quit
```

### Network Testing
```bash
# Test internet connectivity
ping -c 3 8.8.8.8
curl https://www.google.com

# Test local ports
curl http://localhost:3000/health
curl http://localhost:80

# Test from another server
curl http://PRIVATE_IP:3000/health
```

---

## 🚨 Emergency Procedures

### Application Down - Quick Recovery

**1. Check backend:**
```bash
# SSH to backend EC2
pm2 status
# If stopped:
pm2 restart bmi-backend
```

**2. Check frontend:**
```bash
# SSH to frontend EC2
sudo systemctl status nginx
# If stopped:
sudo systemctl restart nginx
```

**3. Check database:**
```bash
# AWS Console → RDS
# Status should be "Available"
# If stopped, start it (takes ~5 minutes)
```

### Complete Restart (Nuclear Option)

```bash
# 1. Restart RDS (AWS Console)
RDS → bmi-tracker-db → Actions → Reboot

# 2. Restart Backend EC2
EC2 → bmi-backend-server → Instance state → Reboot

# 3. Restart Frontend EC2
EC2 → bmi-frontend-server → Instance state → Reboot

# Wait 5 minutes, then test:
curl http://YOUR_ELASTIC_IP/api/health
```

---

## ✅ Post-Deployment Checklist

After completing deployment, verify:

- [ ] Frontend accessible in browser: `http://YOUR_ELASTIC_IP`
- [ ] Can submit measurement and see results
- [ ] Data saves to database (check PM2 logs)
- [ ] Chart displays correctly
- [ ] API health endpoint works: `http://YOUR_ELASTIC_IP/api/health`
- [ ] Backend PM2 status: `online`
- [ ] Frontend Nginx status: `active (running)`
- [ ] RDS status: `Available`
- [ ] No errors in browser console (F12)
- [ ] Cost tracking set up in AWS Billing
- [ ] Elastic IP noted and saved
- [ ] RDS endpoint and password saved securely
- [ ] All validation checkpoints passed

**Optional but recommended:**
- [ ] Set up CloudWatch alarms
- [ ] Enable RDS automated backups (should be automatic)
- [ ] Document your specific IPs and endpoints
- [ ] Take EC2 AMI snapshots for easy recovery
- [ ] Set up billing alerts (AWS Billing → Preferences)

---

## 🎯 What's Next?

### Improvements to Consider

**Security:**
1. Add HTTPS with SSL certificate (Let's Encrypt)
2. Implement user authentication (JWT)
3. Add rate limiting to API
4. Enable AWS WAF (Web Application Firewall)

**Features:**
1. User accounts and login
2. Data export (CSV/PDF)
3. Email notifications
4. Goal tracking
5. Multiple user profiles

**Performance:**
1. Add Redis caching
2. Enable RDS read replicas
3. Use CloudFront CDN for frontend
4. Optimize database queries

**Operations:**
1. Set up CI/CD pipeline (GitHub Actions)
2. Add automated testing
3. Implement blue-green deployment
4. Set up log aggregation (CloudWatch Logs Insights)

---

## 🙏 Need Help?

### Common Questions

**Q: How do I add HTTPS?**
A: Use AWS Certificate Manager + Application Load Balancer, or Let's Encrypt on the frontend EC2

**Q: Can I use this for production?**
A: Yes! But add: HTTPS, backups verification, monitoring alerts, and possibly Multi-AZ RDS

**Q: How do I scale this?**
A: Add Application Load Balancers and multiple EC2 instances, or containerize with ECS

**Q: What if I run out of RDS storage?**
A: Enable storage autoscaling in RDS settings, or manually increase storage size

### Troubleshooting Steps

1. Check this README troubleshooting section
2. Review [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) troubleshooting
3. Check AWS Console for resource status
4. Review application logs (PM2 and Nginx)
5. Verify security groups allow correct traffic

---

## 📄 License & Credits

**License:** MIT - Free for personal and commercial use

**Tech Stack Credits:**
- React (Meta/Facebook)
- Node.js (OpenJS Foundation)
- PostgreSQL (PostgreSQL Global Development Group)
- Express.js (OpenJS Foundation)
- Nginx (F5 Networks)

**Architecture:** Based on AWS Well-Architected Framework best practices

---

**Version:** 1.0  
**Last Updated:** December 13, 2025  
**For:** Junior DevOps Engineers & AWS Beginners  
**Status:** ✅ Production Ready with Step-by-Step Validation
