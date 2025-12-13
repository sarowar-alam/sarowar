# BMI Health Tracker - AWS RDS Multi-EC2 Deployment Guide

**Complete step-by-step deployment guide with validation checkpoints after each step**

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [AWS Infrastructure Setup](#aws-infrastructure-setup)
3. [AWS RDS PostgreSQL Setup](#aws-rds-postgresql-setup)
4. [Backend EC2 Deployment](#backend-ec2-deployment)
5. [Frontend EC2 Deployment](#frontend-ec2-deployment)
6. [Post-Deployment Verification](#post-deployment-verification)
7. [Troubleshooting Guide](#troubleshooting-guide)

---

## Prerequisites

### Required Tools
- AWS Account with appropriate permissions
- SSH client (PuTTY for Windows, Terminal for Mac/Linux)
- Basic knowledge of AWS EC2, VPC, and RDS

### Local Machine Requirements
- Git (optional, for version control)
- Text editor for editing configuration files

### AWS Service Limits
- At least 2 Elastic IPs available
- Sufficient EC2 instance limits (2x t2.micro)
- RDS instance limit (1x db.t3.micro)

---

## AWS Infrastructure Setup

### Step 1: Create VPC and Networking

#### 1.1 Create VPC

1. Navigate to **AWS Console → VPC → Your VPCs**
2. Click **Create VPC**
3. Configure:
   - **Name**: `bmi-tracker-vpc`
   - **IPv4 CIDR block**: `10.0.0.0/16`
   - **IPv6 CIDR block**: No IPv6 CIDR block
   - **Tenancy**: Default
4. Click **Create VPC**

#### ✅ VALIDATION CHECKPOINT 1: VPC Created

```bash
# In AWS Console: VPC → Your VPCs
☑ VPC Name: bmi-tracker-vpc
☑ State: Available
☑ IPv4 CIDR: 10.0.0.0/16
☑ DNS hostnames: Enabled
☑ DNS resolution: Enabled
```

**VPC ID**: _________________ (Note this down)

---

#### 1.2 Create Subnets

**Public Subnet (for Frontend EC2):**

1. Navigate to **VPC → Subnets**
2. Click **Create subnet**
3. Configure:
   - **VPC**: bmi-tracker-vpc
   - **Subnet name**: `bmi-public-subnet`
   - **Availability Zone**: us-east-1a (or your preferred AZ)
   - **IPv4 CIDR block**: `10.0.1.0/24`
4. Click **Create subnet**
5. Select the subnet → **Actions → Edit subnet settings**
6. Enable **Auto-assign public IPv4 address**

**Private Subnet 1 (for Backend EC2):**

1. Click **Create subnet**
2. Configure:
   - **VPC**: bmi-tracker-vpc
   - **Subnet name**: `bmi-private-subnet-1`
   - **Availability Zone**: us-east-1a
   - **IPv4 CIDR block**: `10.0.2.0/24`
3. Click **Create subnet**

**Private Subnet 2 (for RDS - different AZ):**

1. Click **Create subnet**
2. Configure:
   - **VPC**: bmi-tracker-vpc
   - **Subnet name**: `bmi-private-subnet-2`
   - **Availability Zone**: **us-east-1b** (MUST be different AZ for RDS)
   - **IPv4 CIDR block**: `10.0.3.0/24`
3. Click **Create subnet**

#### ✅ VALIDATION CHECKPOINT 2: Subnets Created

```bash
# In AWS Console: VPC → Subnets

Public Subnet:
☑ Name: bmi-public-subnet
☑ CIDR: 10.0.1.0/24
☑ AZ: us-east-1a
☑ Auto-assign public IPv4: Yes

Private Subnet 1:
☑ Name: bmi-private-subnet-1
☑ CIDR: 10.0.2.0/24
☑ AZ: us-east-1a
☑ Auto-assign public IPv4: No

Private Subnet 2:
☑ Name: bmi-private-subnet-2
☑ CIDR: 10.0.3.0/24
☑ AZ: us-east-1b (DIFFERENT from Subnet 1)
☑ Auto-assign public IPv4: No
```

---

#### 1.3 Create Internet Gateway

1. Navigate to **VPC → Internet Gateways**
2. Click **Create internet gateway**
3. **Name**: `bmi-igw`
4. Click **Create internet gateway**
5. Select the IGW → **Actions → Attach to VPC**
6. Select `bmi-tracker-vpc`
7. Click **Attach internet gateway**

#### ✅ VALIDATION CHECKPOINT 3: Internet Gateway

```bash
# In AWS Console: VPC → Internet Gateways
☑ Name: bmi-igw
☑ State: Attached
☑ VPC: bmi-tracker-vpc
```

---

#### 1.4 Create NAT Gateway (for Backend EC2 updates)

1. Navigate to **VPC → NAT Gateways**
2. Click **Create NAT gateway**
3. Configure:
   - **Name**: `bmi-nat-gateway`
   - **Subnet**: bmi-public-subnet (MUST be public subnet)
4. Click **Allocate Elastic IP**
5. Click **Create NAT gateway**
6. Wait for status to become **Available** (2-3 minutes)

#### ✅ VALIDATION CHECKPOINT 4: NAT Gateway

```bash
# In AWS Console: VPC → NAT Gateways
☑ Name: bmi-nat-gateway
☑ State: Available
☑ Subnet: bmi-public-subnet
☑ Elastic IP: Allocated and associated
```

**NAT Gateway ID**: _________________ (Note this down)

---

#### 1.5 Configure Route Tables

**Public Route Table:**

1. Navigate to **VPC → Route Tables**
2. Find the main route table for bmi-tracker-vpc
3. Rename it to `bmi-public-rt`
4. Select it → **Routes tab** → **Edit routes**
5. Add route:
   - **Destination**: `0.0.0.0/0`
   - **Target**: Internet Gateway (bmi-igw)
6. Click **Save changes**
7. Go to **Subnet associations tab** → **Edit subnet associations**
8. Select `bmi-public-subnet`
9. Click **Save associations**

**Private Route Table:**

1. Click **Create route table**
2. **Name**: `bmi-private-rt`
3. **VPC**: bmi-tracker-vpc
4. Click **Create route table**
5. Select it → **Routes tab** → **Edit routes**
6. Add route:
   - **Destination**: `0.0.0.0/0`
   - **Target**: NAT Gateway (bmi-nat-gateway)
7. Click **Save changes**
8. Go to **Subnet associations tab** → **Edit subnet associations**
9. Select `bmi-private-subnet-1` and `bmi-private-subnet-2`
10. Click **Save associations**

#### ✅ VALIDATION CHECKPOINT 5: Route Tables

```bash
# In AWS Console: VPC → Route Tables

Public Route Table (bmi-public-rt):
☑ Route: 10.0.0.0/16 → local
☑ Route: 0.0.0.0/0 → Internet Gateway (bmi-igw)
☑ Associated Subnet: bmi-public-subnet

Private Route Table (bmi-private-rt):
☑ Route: 10.0.0.0/16 → local
☑ Route: 0.0.0.0/0 → NAT Gateway (bmi-nat-gateway)
☑ Associated Subnets: bmi-private-subnet-1, bmi-private-subnet-2
```

---

### Step 2: Create Security Groups

#### 2.1 Frontend Security Group

1. Navigate to **EC2 → Security Groups**
2. Click **Create security group**
3. Configure:
   - **Name**: `bmi-frontend-sg`
   - **Description**: Security group for frontend EC2 (Nginx)
   - **VPC**: bmi-tracker-vpc

**Inbound Rules:**
```
Type        Protocol  Port  Source          Description
HTTP        TCP       80    0.0.0.0/0       Allow HTTP from internet
HTTPS       TCP       443   0.0.0.0/0       Allow HTTPS from internet
SSH         TCP       22    YOUR_IP/32      SSH access from your IP
```

**Outbound Rules:**
```
Type        Protocol  Port Range  Destination         Description
Custom TCP  TCP       3000        bmi-backend-sg      API calls to backend
HTTP        TCP       80          0.0.0.0/0           Package downloads
HTTPS       TCP       443         0.0.0.0/0           Package downloads
```

4. Click **Create security group**

#### 2.2 Backend Security Group

1. Click **Create security group**
2. Configure:
   - **Name**: `bmi-backend-sg`
   - **Description**: Security group for backend EC2 (Node.js API)
   - **VPC**: bmi-tracker-vpc

**Inbound Rules:**
```
Type        Protocol  Port  Source              Description
Custom TCP  TCP       3000  bmi-frontend-sg     API requests from frontend
SSH         TCP       22    YOUR_IP/32          SSH access from your IP
```

**Outbound Rules:**
```
Type        Protocol  Port Range  Destination     Description
PostgreSQL  TCP       5432        bmi-rds-sg      Database connections
HTTP        TCP       80          0.0.0.0/0       Package downloads
HTTPS       TCP       443         0.0.0.0/0       Package downloads
```

3. Click **Create security group**

#### 2.3 RDS Security Group

1. Click **Create security group**
2. Configure:
   - **Name**: `bmi-rds-sg`
   - **Description**: Security group for RDS PostgreSQL
   - **VPC**: bmi-tracker-vpc

**Inbound Rules:**
```
Type        Protocol  Port  Source              Description
PostgreSQL  TCP       5432  bmi-backend-sg      Allow backend to access RDS
```

**Outbound Rules:**
```
Type         Protocol  Port Range  Destination  Description
All traffic  All       All         0.0.0.0/0    (Default)
```

3. Click **Create security group**

#### ✅ VALIDATION CHECKPOINT 6: Security Groups

```bash
# In AWS Console: EC2 → Security Groups

Frontend SG (bmi-frontend-sg):
☑ Inbound: HTTP (80) from 0.0.0.0/0
☑ Inbound: HTTPS (443) from 0.0.0.0/0
☑ Inbound: SSH (22) from YOUR_IP/32
☑ Outbound: TCP (3000) to bmi-backend-sg
☑ Outbound: HTTP/HTTPS to 0.0.0.0/0

Backend SG (bmi-backend-sg):
☑ Inbound: TCP (3000) from bmi-frontend-sg
☑ Inbound: SSH (22) from YOUR_IP/32
☑ Outbound: PostgreSQL (5432) to bmi-rds-sg
☑ Outbound: HTTP/HTTPS to 0.0.0.0/0

RDS SG (bmi-rds-sg):
☑ Inbound: PostgreSQL (5432) from bmi-backend-sg
☑ Outbound: All traffic to 0.0.0.0/0
```

**CRITICAL**: Ensure security group sources/destinations use **Security Group IDs**, not IP addresses!

---

### Step 3: Create EC2 Key Pair

1. Navigate to **EC2 → Key Pairs**
2. Click **Create key pair**
3. Configure:
   - **Name**: `bmi-tracker-key`
   - **Key pair type**: RSA
   - **Private key file format**: .pem (for Mac/Linux) or .ppk (for PuTTY/Windows)
4. Click **Create key pair**
5. **Save the downloaded file securely!** (You cannot download it again)

6. Set permissions (Mac/Linux):
   ```bash
   chmod 400 bmi-tracker-key.pem
   ```

#### ✅ VALIDATION CHECKPOINT 7: Key Pair

```bash
# In AWS Console: EC2 → Key Pairs
☑ Name: bmi-tracker-key
☑ Type: RSA
☑ File downloaded and saved securely

# Local machine
☑ File permissions: 400 (Mac/Linux)
☑ File location: ___________________
```

---

## AWS RDS PostgreSQL Setup

### Step 1: Create DB Subnet Group

1. Navigate to **RDS → Subnet groups**
2. Click **Create DB subnet group**
3. Configure:
   - **Name**: `bmi-rds-subnet-group`
   - **Description**: Subnet group for BMI Tracker RDS
   - **VPC**: bmi-tracker-vpc
4. **Add subnets**:
   - Select **us-east-1a** → Add `bmi-private-subnet-2` (10.0.3.0/24)
   - Select **us-east-1b** → If you created another subnet, add it
   - **NOTE**: You need at least 2 subnets in different AZs
5. Click **Create**

#### ✅ VALIDATION CHECKPOINT 8: DB Subnet Group

```bash
# In AWS Console: RDS → Subnet groups
☑ Name: bmi-rds-subnet-group
☑ VPC: bmi-tracker-vpc
☑ Status: Complete
☑ Subnets: At least 2 subnets in different AZs
```

---

### Step 2: Create RDS PostgreSQL Instance

1. Navigate to **RDS → Databases**
2. Click **Create database**
3. **Engine options:**
   - Engine type: **PostgreSQL**
   - Version: **PostgreSQL 14.x or 15.x** (latest stable)

4. **Templates:**
   - Select **Free tier** (for testing) or **Production** (for live deployment)

5. **Settings:**
   - **DB instance identifier**: `bmi-tracker-db`
   - **Master username**: `bmi_admin`
   - **Master password**: (Create a strong password)
   - **Confirm password**: (Re-enter password)

6. **DB instance class:**
   - **Burstable classes**: `db.t3.micro` (Free Tier) or `db.t3.small`

7. **Storage:**
   - **Storage type**: General Purpose SSD (gp2)
   - **Allocated storage**: 20 GB
   - **Enable storage autoscaling**: Yes
   - **Maximum storage threshold**: 100 GB

8. **Availability & durability:**
   - **Multi-AZ deployment**: No (Free Tier) / Yes (Production)

9. **Connectivity:**
   - **Virtual private cloud (VPC)**: bmi-tracker-vpc
   - **DB subnet group**: bmi-rds-subnet-group
   - **Public access**: **No** (Important for security!)
   - **VPC security group**: Choose existing → `bmi-rds-sg`
   - **Availability Zone**: No preference (or choose one)

10. **Database authentication:**
    - **Database authentication options**: Password authentication

11. **Additional configuration:**
    - **Initial database name**: `bmidb` (Important!)
    - **DB parameter group**: default.postgres14 (or 15)
    - **Backup retention period**: 7 days
    - **Enable encryption**: Yes
    - **Enable Enhanced monitoring**: Optional (No for Free Tier)
    - **Enable auto minor version upgrade**: Yes

12. Click **Create database**

13. **Wait for creation** (5-10 minutes)

#### ✅ VALIDATION CHECKPOINT 9: RDS Instance Created

```bash
# In AWS Console: RDS → Databases
☑ DB identifier: bmi-tracker-db
☑ Status: Available (wait if "Creating")
☑ Engine: PostgreSQL 14.x or 15.x
☑ Instance class: db.t3.micro
☑ Storage: 20 GB
☑ VPC: bmi-tracker-vpc
☑ Subnet group: bmi-rds-subnet-group
☑ Security group: bmi-rds-sg
☑ Public access: No
☑ Encryption: Enabled
```

**RDS Endpoint**: ____________________________________________
(Copy from: Connectivity & security → Endpoint)

**Example**: `bmi-tracker-db.c9xx1234567.us-east-1.rds.amazonaws.com`

---

### Step 3: Run Database Migration

Since RDS doesn't allow direct file uploads, we'll run the migration from the Backend EC2 after it's deployed.

**For now, document your RDS connection string:**

```bash
DATABASE_URL Format:
postgresql://bmi_admin:YOUR_PASSWORD@RDS_ENDPOINT:5432/bmidb

Example:
postgresql://bmi_admin:MyStrongPass123!@bmi-tracker-db.c9xx1234567.us-east-1.rds.amazonaws.com:5432/bmidb
```

**Connection String**: _________________________________________________________

#### ✅ VALIDATION CHECKPOINT 10: RDS Connection Info

```bash
☑ Master Username: bmi_admin
☑ Master Password: (Stored securely)
☑ Database Name: bmidb
☑ Endpoint: (Copied and saved)
☑ Port: 5432
☑ Full DATABASE_URL: (Constructed and saved)
```

---

## Backend EC2 Deployment

### Step 1: Launch Backend EC2 Instance

1. Navigate to **EC2 → Instances**
2. Click **Launch instances**
3. **Name**: `bmi-backend-ec2`
4. **Application and OS Images (AMI)**:
   - **Quick Start**: Ubuntu
   - **AMI**: Ubuntu Server 22.04 LTS (64-bit x86)
5. **Instance type**: `t2.micro` (Free Tier eligible)
6. **Key pair**: `bmi-tracker-key`
7. **Network settings**:
   - **VPC**: bmi-tracker-vpc
   - **Subnet**: bmi-private-subnet-1 (10.0.2.0/24)
   - **Auto-assign public IP**: **Disable** (Private instance)
   - **Firewall (security groups)**: Select existing → `bmi-backend-sg`
8. **Configure storage**: 8 GB gp2 (default)
9. Click **Launch instance**
10. Wait for instance state: **Running**

#### ✅ VALIDATION CHECKPOINT 11: Backend EC2 Launched

```bash
# In AWS Console: EC2 → Instances
☑ Name: bmi-backend-ec2
☑ Instance State: Running
☑ Status Checks: 2/2 checks passed (wait 2-3 minutes)
☑ Instance Type: t2.micro
☑ AMI: Ubuntu Server 22.04 LTS
☑ VPC: bmi-tracker-vpc
☑ Subnet: bmi-private-subnet-1
☑ Public IPv4 address: — (None, as expected)
☑ Private IPv4 address: (Note this down)
☑ Security group: bmi-backend-sg
☑ Key pair: bmi-tracker-key
```

**Backend Private IP**: ___________________  
(Example: `10.0.2.15`)

---

### Step 2: Connect to Backend EC2

Since Backend EC2 is in a private subnet, you'll need to connect through:

**Option A: AWS Systems Manager Session Manager (Recommended)**

1. Navigate to **EC2 → Instances → bmi-backend-ec2**
2. Click **Connect**
3. Choose **Session Manager** tab
4. Click **Connect**

**Option B: SSH via Bastion Host (if you have one)**

```bash
ssh -i bmi-tracker-key.pem ubuntu@BASTION_IP
ssh ubuntu@BACKEND_PRIVATE_IP
```

**Option C: Temporarily enable public IP for setup (not recommended for production)**

#### ✅ VALIDATION CHECKPOINT 12: Connected to Backend EC2

```bash
# You should see Ubuntu prompt
ubuntu@ip-10-0-2-15:~$

# Verify internet connectivity through NAT
ping -c 3 google.com
☑ Packets transmitted and received (via NAT Gateway)
```

---

### Step 3: Upload Backend Files

**From your local machine:**

```bash
# If using Session Manager, upload files via S3 or use git clone
# If using SSH via bastion:

# Create directory
ssh -i bmi-tracker-key.pem ubuntu@BACKEND_IP "mkdir -p ~/bmi-backend/src"

# Upload all backend files
scp -i bmi-tracker-key.pem -r backend-ec2/* ubuntu@BACKEND_IP:~/bmi-backend/
```

**Alternative: Clone from Git repository**

```bash
# On Backend EC2
git clone YOUR_REPO_URL bmi-backend
cd bmi-backend
```

#### ✅ VALIDATION CHECKPOINT 13: Files Uploaded

```bash
# On Backend EC2
ls -la ~/bmi-backend/

☑ src/ directory exists
☑ src/server.js
☑ src/routes.js
☑ src/db.js
☑ src/calculations.js
☑ package.json
☑ .env.example
☑ ecosystem.config.js
☑ deploy-backend.sh
☑ 001_create_measurements_rds.sql
```

---

### Step 4: Run Backend Deployment Script

```bash
cd ~/bmi-backend
chmod +x deploy-backend.sh
./deploy-backend.sh
```

**The script will:**
1. Update system packages
2. Install Node.js via NVM
3. Install PM2
4. Install PostgreSQL client
5. Create logs directory
6. Install npm dependencies
7. Prompt for RDS connection details
8. Create .env file
9. Test RDS connection
10. Start backend with PM2
11. Configure firewall

**When prompted, enter:**
- RDS Endpoint: `bmi-tracker-db.xxxxx.us-east-1.rds.amazonaws.com`
- Database Name: `bmidb`
- Master Username: `bmi_admin`
- Master Password: (Your RDS password)

#### ✅ VALIDATION CHECKPOINT 14: Backend Deployment Complete

```bash
# Verify Node.js
node -v
☑ v18.x.x

# Verify PM2
pm2 --version
☑ Installed

# Check PM2 status
pm2 status
☑ bmi-backend status: online
☑ uptime: > 0s
☑ restarts: 0

# Test backend locally
curl http://localhost:3000/health
☑ Response: {"status":"ok","environment":"production","database":"AWS RDS PostgreSQL",...}

# Check logs
pm2 logs bmi-backend --lines 20
☑ "✅ Database connected successfully at: ..."
☑ "🚀 Server running on port 3000"
☑ No errors
```

---

### Step 5: Run Database Migration on RDS

```bash
# On Backend EC2
cd ~/bmi-backend

# Test RDS connection first
psql "$(cat .env | grep DATABASE_URL | cut -d'=' -f2)" -c "SELECT version();"
☑ Should show PostgreSQL version

# Run migration
psql "$(cat .env | grep DATABASE_URL | cut -d'=' -f2)" -f 001_create_measurements_rds.sql

# Verify table created
psql "$(cat .env | grep DATABASE_URL | cut -d'=' -f2)" -c "\dt"
☑ Should show "measurements" table

# Check table structure
psql "$(cat .env | grep DATABASE_URL | cut -d'=' -f2)" -c "\d measurements"
☑ Should show all columns
```

#### ✅ VALIDATION CHECKPOINT 15: RDS Database Ready

```bash
# Verify measurements table
psql "$(cat .env | grep DATABASE_URL | cut -d'=' -f2)" -c "SELECT COUNT(*) FROM measurements;"
☑ Returns: count = 0 (empty table)

# Test insert
psql "$(cat .env | grep DATABASE_URL | cut -d'=' -f2)" -c "INSERT INTO measurements (weight_kg, height_cm, age, sex, bmi, bmi_category) VALUES (70, 175, 30, 'male', 22.9, 'Normal');"
☑ INSERT successful

# Verify insert
psql "$(cat .env | grep DATABASE_URL | cut -d'=' -f2)" -c "SELECT * FROM measurements;"
☑ Shows 1 row

# Test API endpoint
curl -X POST http://localhost:3000/api/measurements \
  -H "Content-Type: application/json" \
  -d '{"weightKg":75,"heightCm":180,"age":25,"sex":"male","activity":"moderate"}'
☑ Returns: {"measurement":{...}}

# Verify data saved
curl http://localhost:3000/api/measurements
☑ Returns: {"rows":[...]} with 2 measurements
```

**✅ Backend EC2 fully operational and connected to RDS!**

---

## Frontend EC2 Deployment

### Step 1: Launch Frontend EC2 Instance

1. Navigate to **EC2 → Instances**
2. Click **Launch instances**
3. **Name**: `bmi-frontend-ec2`
4. **Application and OS Images (AMI)**:
   - **Quick Start**: Ubuntu
   - **AMI**: Ubuntu Server 22.04 LTS (64-bit x86)
5. **Instance type**: `t2.micro`
6. **Key pair**: `bmi-tracker-key`
7. **Network settings**:
   - **VPC**: bmi-tracker-vpc
   - **Subnet**: bmi-public-subnet (10.0.1.0/24)
   - **Auto-assign public IP**: **Enable**
   - **Firewall (security groups)**: Select existing → `bmi-frontend-sg`
8. **Configure storage**: 8 GB gp2
9. Click **Launch instance**
10. Wait for instance state: **Running**

### Step 2: Allocate and Associate Elastic IP

1. Navigate to **EC2 → Elastic IPs**
2. Click **Allocate Elastic IP address**
3. Click **Allocate**
4. Select the new Elastic IP
5. **Actions → Associate Elastic IP address**
6. **Instance**: bmi-frontend-ec2
7. Click **Associate**

#### ✅ VALIDATION CHECKPOINT 16: Frontend EC2 Launched

```bash
# In AWS Console: EC2 → Instances
☑ Name: bmi-frontend-ec2
☑ Instance State: Running
☑ Status Checks: 2/2 checks passed
☑ Instance Type: t2.micro
☑ Subnet: bmi-public-subnet
☑ Auto-assign Public IP: Yes
☑ Elastic IP: Associated
☑ Security group: bmi-frontend-sg

# Note these IPs:
Public IP (Elastic): ___________________
Private IP: ___________________
```

---

### Step 3: Connect to Frontend EC2

```bash
# From your local machine
ssh -i bmi-tracker-key.pem ubuntu@FRONTEND_ELASTIC_IP
```

#### ✅ VALIDATION CHECKPOINT 17: Connected to Frontend EC2

```bash
# You should see Ubuntu prompt
ubuntu@ip-10-0-1-10:~$

# Verify internet connectivity
ping -c 3 google.com
☑ Packets transmitted and received
```

---

### Step 4: Upload Frontend Files

```bash
# From your local machine

# Create directory
ssh -i bmi-tracker-key.pem ubuntu@FRONTEND_ELASTIC_IP "mkdir -p ~/frontend/src/components"

# Upload all frontend files
scp -i bmi-tracker-key.pem -r frontend-ec2/* ubuntu@FRONTEND_ELASTIC_IP:~/frontend/
```

#### ✅ VALIDATION CHECKPOINT 18: Frontend Files Uploaded

```bash
# On Frontend EC2
ls -la ~/frontend/

☑ src/ directory
☑ src/components/MeasurementForm.jsx
☑ src/components/TrendChart.jsx
☑ src/App.jsx
☑ src/main.jsx
☑ src/api.js
☑ src/index.css
☑ index.html
☑ vite.config.js
☑ package.json
☑ nginx.conf
☑ deploy-frontend.sh
```

---

### Step 5: Update Configuration with Backend IP

```bash
# On Frontend EC2
cd ~/frontend

# Update nginx.conf with Backend Private IP
sed -i "s/BACKEND_PRIVATE_IP/BACKEND_PRIVATE_IP_HERE/g" nginx.conf

# Example:
sed -i "s/BACKEND_PRIVATE_IP/10.0.2.15/g" nginx.conf

# Verify
grep "proxy_pass" nginx.conf
☑ Should show: proxy_pass http://10.0.2.15:3000/api/;
```

#### ✅ VALIDATION CHECKPOINT 19: Configuration Updated

```bash
# Verify nginx config
cat nginx.conf | grep proxy_pass
☑ Shows correct backend private IP (10.0.2.x)
☑ No placeholder text "BACKEND_PRIVATE_IP" remaining
```

---

### Step 6: Run Frontend Deployment Script

```bash
cd ~/frontend
chmod +x deploy-frontend.sh
./deploy-frontend.sh
```

**When prompted, enter:**
- Backend EC2 Private IP: `10.0.2.15` (your actual backend IP)

**The script will:**
1. Update system packages
2. Install Node.js via NVM
3. Install Nginx
4. Install npm dependencies
5. Build frontend (npm run build)
6. Deploy to /var/www/bmi-health-tracker
7. Configure Nginx with backend IP
8. Test Nginx configuration
9. Restart Nginx
10. Configure firewall

#### ✅ VALIDATION CHECKPOINT 20: Frontend Deployment Complete

```bash
# Verify Node.js
node -v
☑ v18.x.x

# Verify Nginx
sudo systemctl status nginx
☑ active (running)

# Test Nginx config
sudo nginx -t
☑ nginx: configuration file /etc/nginx/nginx.conf test is successful

# Check deployed files
ls -la /var/www/bmi-health-tracker/
☑ index.html
☑ assets/ directory
☑ Files owned by www-data:www-data

# Test frontend locally
curl http://localhost
☑ Returns HTML content

# Test API proxy locally
curl http://localhost/api/health
☑ Returns: {"status":"ok",...}

# Check Nginx logs
sudo tail -20 /var/log/nginx/bmi-frontend-error.log
☑ No critical errors
```

---

### Step 7: Test External Access

```bash
# From your local machine (not EC2)
curl http://FRONTEND_ELASTIC_IP

☑ Should return HTML content
```

**Open in browser:**
```
http://FRONTEND_ELASTIC_IP
```

#### ✅ VALIDATION CHECKPOINT 21: Frontend Externally Accessible

```bash
# In browser: http://FRONTEND_ELASTIC_IP

☑ Page loads successfully
☑ Styling applied correctly
☑ No 404 errors
☑ No console errors (F12 → Console)
```

---

## Post-Deployment Verification

### End-to-End Functionality Test

#### 1. Test Frontend UI

```bash
# Open in browser
http://FRONTEND_ELASTIC_IP

☑ Page loads
☑ Header shows "BMI & Health Tracker"
☑ Measurement form visible
☑ All form fields present
```

#### 2. Test API Connectivity

```bash
# From your local machine
curl http://FRONTEND_ELASTIC_IP/api/health

☑ Returns: {"status":"ok","environment":"production","database":"AWS RDS PostgreSQL",...}
```

#### 3. Test Measurement Submission

**In browser:**

1. Fill out the measurement form:
   - Weight: 70 kg
   - Height: 175 cm
   - Age: 30
   - Sex: Male
   - Activity: Moderate
2. Click "Save Measurement"
3. Success message should appear
4. Measurement should appear in "Recent Measurements" list

#### 4. Test Data Persistence

```bash
# From your local machine
curl http://FRONTEND_ELASTIC_IP/api/measurements

☑ Returns: {"rows":[...]} with measurements
☑ Latest measurement visible
☑ BMI calculated correctly (70/(1.75*1.75) = 22.9)
```

#### 5. Test Trend Chart

**In browser:**

1. Scroll to "30-Day BMI Trend" section
2. If you have measurements, chart should display
3. If no data, should show "No trend data available"

#### ✅ FINAL VALIDATION: Complete System Check

```bash
# Complete System Checklist

Infrastructure:
☑ VPC created and configured
☑ 3 subnets created (1 public, 2 private)
☑ Internet Gateway attached
☑ NAT Gateway operational
☑ Route tables configured
☑ 3 Security groups with correct rules

RDS:
☑ PostgreSQL instance running
☑ Status: Available
☑ Database "bmidb" created
☑ Table "measurements" exists
☑ Accessible from Backend EC2 only

Backend EC2:
☑ Instance running in private subnet
☑ Node.js installed
☑ PM2 running backend
☑ Connected to RDS successfully
☑ API endpoints responding
☑ Health check: OK

Frontend EC2:
☑ Instance running in public subnet
☑ Elastic IP associated
☑ Nginx running
☑ React app built and deployed
☑ Proxying to backend correctly
☑ Externally accessible

End-to-End:
☑ Can access frontend from internet
☑ Can submit measurements
☑ Data saves to RDS
☑ Data retrieval works
☑ Charts display (when data available)
☑ No console errors
☑ All API endpoints functional
```

### Application URLs

```
Frontend URL: http://FRONTEND_ELASTIC_IP
Backend Health: http://FRONTEND_ELASTIC_IP/api/health (via proxy)
RDS Endpoint: (Private, accessible only from Backend EC2)
```

### Connection Flow Verification

```
Your Browser
    ↓ HTTP Request
Frontend EC2 (Nginx on port 80)
    ↓ Proxy /api/* requests
Backend EC2 (Express on port 3000)
    ↓ Database queries
AWS RDS PostgreSQL (port 5432)
    ↓ Results
Backend EC2
    ↓ JSON Response
Frontend EC2
    ↓ HTTP Response
Your Browser (displays data)
```

---

## Troubleshooting Guide

### Issue 1: Cannot Connect to RDS from Backend EC2

**Symptoms:**
- Backend logs show "Database connection failed"
- psql command hangs or times out

**Checks:**
```bash
# 1. Verify RDS is running
# AWS Console: RDS → Databases → bmi-tracker-db
☐ Status should be "Available"

# 2. Check security groups
# Backend EC2 SG should allow outbound to RDS SG on port 5432
# RDS SG should allow inbound from Backend EC2 SG on port 5432

# 3. Test network connectivity
telnet RDS_ENDPOINT 5432
# Should connect (press Ctrl+C to exit)

# 4. Verify DATABASE_URL in .env
cat ~/bmi-backend/.env
# Should have correct format:
# postgresql://bmi_admin:PASSWORD@RDS_ENDPOINT:5432/bmidb

# 5. Check RDS endpoint
# Must use endpoint from AWS Console, not IP address

# 6. Verify database name
# Should be "bmidb" (lowercase, specified during RDS creation)
```

**Solutions:**
- Fix security group rules (use SG IDs, not IP ranges)
- Verify RDS endpoint copied correctly
- Check password doesn't contain special characters that need escaping
- Ensure RDS is in "Available" state

---

### Issue 2: Frontend Cannot Reach Backend

**Symptoms:**
- API requests fail with "Network Error"
- 502 Bad Gateway errors
- Console shows "Failed to load measurements"

**Checks:**
```bash
# On Frontend EC2

# 1. Test backend connectivity
curl http://BACKEND_PRIVATE_IP:3000/health
# Should return JSON

# 2. Check nginx config
sudo cat /etc/nginx/sites-available/bmi-frontend | grep proxy_pass
# Should show correct backend IP

# 3. Check backend is running
ssh to Backend EC2
pm2 status
# bmi-backend should be "online"

# 4. Verify security groups
# Frontend SG → Outbound → TCP 3000 to Backend SG
# Backend SG → Inbound → TCP 3000 from Frontend SG

# 5. Test from frontend EC2
curl http://BACKEND_PRIVATE_IP:3000/api/measurements
# Should return {"rows":[...]}
```

**Solutions:**
- Update nginx.conf with correct backend private IP
- Restart nginx: `sudo systemctl restart nginx`
- Verify backend PM2 is running: `pm2 restart bmi-backend`
- Check firewall rules allow port 3000

---

### Issue 3: Public Cannot Access Frontend

**Symptoms:**
- Cannot open http://FRONTEND_ELASTIC_IP in browser
- Connection timeout
- "Site can't be reached"

**Checks:**
```bash
# 1. Verify Elastic IP associated
# AWS Console: EC2 → Elastic IPs
☐ Should be associated with bmi-frontend-ec2

# 2. Check security group
# AWS Console: EC2 → Security Groups → bmi-frontend-sg
☐ Inbound: HTTP (80) from 0.0.0.0/0
☐ Inbound: HTTPS (443) from 0.0.0.0/0

# 3. Verify Nginx is running
ssh to Frontend EC2
sudo systemctl status nginx
☐ Should be "active (running)"

# 4. Test locally on frontend EC2
curl http://localhost
# Should return HTML

# 5. Check firewall
sudo ufw status
☐ Should allow: 80/tcp, 443/tcp, 22/tcp
```

**Solutions:**
- Add HTTP inbound rule to frontend security group
- Restart Nginx: `sudo systemctl restart nginx`
- Check Nginx logs: `sudo tail -50 /var/log/nginx/error.log`
- Verify frontend EC2 is in public subnet
- Check route table has route to Internet Gateway

---

### Issue 4: Measurements Not Saving

**Symptoms:**
- Form submission shows error
- Success message appears but data doesn't show
- Console errors

**Checks:**
```bash
# On Backend EC2

# 1. Check PM2 logs
pm2 logs bmi-backend --lines 50
# Look for errors

# 2. Test database connection
psql "$(cat .env | grep DATABASE_URL | cut -d'=' -f2)" -c "SELECT COUNT(*) FROM measurements;"
# Should return a number

# 3. Test API directly
curl -X POST http://localhost:3000/api/measurements \
  -H "Content-Type: application/json" \
  -d '{"weightKg":70,"heightCm":175,"age":30,"sex":"male","activity":"moderate"}'
# Should return measurement object

# 4. Check RDS metrics
# AWS Console: RDS → Databases → bmi-tracker-db → Monitoring
☐ Check for connection errors
☐ Check CPU and memory usage
```

**Solutions:**
- Restart backend: `pm2 restart bmi-backend`
- Check RDS connection hasn't timed out
- Verify table schema is correct
- Check backend logs for SQL errors

---

### Issue 5: NAT Gateway Not Working (Backend Can't Update)

**Symptoms:**
- Backend EC2 can't download packages
- `apt update` fails
- `npm install` times out

**Checks:**
```bash
# 1. Verify NAT Gateway status
# AWS Console: VPC → NAT Gateways
☐ Status: Available
☐ Elastic IP associated

# 2. Check route table
# Private route table should have:
☐ Route: 0.0.0.0/0 → NAT Gateway

# 3. Verify subnet association
# Private subnets should be associated with private route table

# 4. Test from backend EC2
ping -c 3 8.8.8.8
# Should work
```

**Solutions:**
- Recreate NAT Gateway if failed
- Update private route table
- Ensure NAT Gateway is in public subnet
- Check NAT Gateway isn't accidentally deleted

---

### Quick Reference Commands

#### Frontend EC2
```bash
# Nginx
sudo systemctl status nginx
sudo systemctl restart nginx
sudo nginx -t
sudo tail -f /var/log/nginx/bmi-frontend-error.log

# Application
ls -la /var/www/bmi-health-tracker/
curl http://localhost
curl http://localhost/api/health
```

#### Backend EC2
```bash
# PM2
pm2 status
pm2 logs bmi-backend
pm2 restart bmi-backend
pm2 stop bmi-backend
pm2 delete bmi-backend

# API Testing
curl http://localhost:3000/health
curl http://localhost:3000/api/measurements
curl -X POST http://localhost:3000/api/measurements \
  -H "Content-Type: application/json" \
  -d '{"weightKg":70,"heightCm":175,"age":30,"sex":"male","activity":"moderate"}'

# Database
psql "$DATABASE_URL" -c "SELECT * FROM measurements LIMIT 5;"
psql "$DATABASE_URL" -c "\dt"  # List tables
```

#### RDS Connection (from Backend EC2)
```bash
# Connect
psql "$(grep DATABASE_URL .env | cut -d'=' -f2)"

# Quick queries
psql "$DATABASE_URL" -c "SELECT COUNT(*) FROM measurements;"
psql "$DATABASE_URL" -c "SELECT * FROM measurements ORDER BY created_at DESC LIMIT 10;"
```

---

## Cost Optimization Tips

1. **Use Free Tier**: Stick with t2.micro for EC2 and db.t3.micro for RDS in first 12 months
2. **Stop NAT Gateway**: If not actively developing, stop NAT Gateway to save ~$32/month
3. **Use S3 for Static Assets**: Move images/videos to S3 if you add them later
4. **Enable RDS Autoscaling**: Let storage grow only when needed
5. **Schedule Instances**: Stop EC2 instances when not in use (dev/test environments)
6. **Monitor Costs**: Set up AWS Budgets alerts

---

## Next Steps

1. **Setup Custom Domain**:
   - Register domain (Route 53 or external)
   - Point A record to Frontend Elastic IP
   - Configure SSL certificate (Let's Encrypt/AWS Certificate Manager)

2. **Enable HTTPS**:
   - Install Certbot on Frontend EC2
   - Obtain SSL certificate
   - Update Nginx config for HTTPS

3. **Setup Monitoring**:
   - Enable CloudWatch alarms
   - Configure SNS notifications
   - Set up log aggregation

4. **Implement CI/CD**:
   - GitHub Actions or AWS CodePipeline
   - Automated deployments
   - Rolling updates

5. **Add Authentication**:
   - JWT tokens
   - User accounts
   - Protected API endpoints

---

**Deployment Guide Version**: 1.0  
**Last Updated**: December 13, 2025  
**Status**: ✅ Complete with validation checkpoints

**🎉 Congratulations! Your BMI Health Tracker is now deployed on AWS with RDS!**
