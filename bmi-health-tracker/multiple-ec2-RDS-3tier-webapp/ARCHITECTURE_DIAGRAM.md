# Architecture Diagrams

This document provides visual representations of the BMI Health Tracker AWS RDS Multi-EC2 Architecture.

---

## Table of Contents
1. [High-Level Architecture](#high-level-architecture)
2. [Network Topology](#network-topology)
3. [Security Groups Flow](#security-groups-flow)
4. [Request Flow](#request-flow)
5. [Database Architecture](#database-architecture)
6. [Deployment Flow](#deployment-flow)

---

## High-Level Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                         AWS Cloud (us-east-1)                        │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                    VPC (10.0.0.0/16)                          │ │
│  │                                                                │ │
│  │  ┌─────────────────────────────────────────────────────────┐  │ │
│  │  │         Public Subnet (10.0.1.0/24)                     │  │ │
│  │  │                                                          │  │ │
│  │  │   ┌──────────────────────────────────────┐              │  │ │
│  │  │   │      Frontend EC2 (t2.micro)         │              │  │ │
│  │  │   │  - React App                         │              │  │ │
│  │  │   │  - Nginx Web Server                  │◄─────────────┼──┼─┼──── Internet Users
│  │  │   │  - Elastic IP: XX.XX.XX.XX           │              │  │ │     (HTTP:80)
│  │  │   │  - Port 80 (HTTP)                    │              │  │ │
│  │  │   └───────────────┬──────────────────────┘              │  │ │
│  │  │                   │                                      │  │ │
│  │  └───────────────────┼──────────────────────────────────────┘  │ │
│  │                      │                                          │ │
│  │                      │ API Requests                             │ │
│  │                      │ (proxy_pass)                             │ │
│  │                      ▼                                          │ │
│  │  ┌─────────────────────────────────────────────────────────┐  │ │
│  │  │       Private Subnet 1 (10.0.2.0/24)                    │  │ │
│  │  │                                                          │  │ │
│  │  │   ┌──────────────────────────────────────┐              │  │ │
│  │  │   │      Backend EC2 (t2.micro)          │              │  │ │
│  │  │   │  - Node.js + Express                 │              │  │ │
│  │  │   │  - PM2 Process Manager               │              │  │ │
│  │  │   │  - Private IP: 10.0.2.20             │              │  │ │
│  │  │   │  - Port 3000 (API)                   │              │  │ │
│  │  │   └───────────────┬──────────────────────┘              │  │ │
│  │  │                   │                                      │  │ │
│  │  └───────────────────┼──────────────────────────────────────┘  │ │
│  │                      │                                          │ │
│  │                      │ SQL Queries                              │ │
│  │                      │ (SSL/TLS)                                │ │
│  │                      ▼                                          │ │
│  │  ┌─────────────────────────────────────────────────────────┐  │ │
│  │  │       Private Subnet 2 (10.0.3.0/24)                    │  │ │
│  │  │                                                          │  │ │
│  │  │   ┌──────────────────────────────────────┐              │  │ │
│  │  │   │   AWS RDS PostgreSQL (db.t3.micro)   │              │  │ │
│  │  │   │  - PostgreSQL 14/15                  │              │  │ │
│  │  │   │  - 20 GB Storage                     │              │  │ │
│  │  │   │  - Encrypted at Rest                 │              │  │ │
│  │  │   │  - Automated Backups                 │              │  │ │
│  │  │   │  - Port 5432 (PostgreSQL)            │              │  │ │
│  │  │   └──────────────────────────────────────┘              │  │ │
│  │  │                                                          │  │ │
│  │  └──────────────────────────────────────────────────────────┘  │ │
│  │                                                                │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Network Topology

```
┌───────────────────────────────────────────────────────────────────────┐
│                        Internet (0.0.0.0/0)                           │
└───────────────────────────┬───────────────────────────────────────────┘
                            │
                    ┌───────▼────────┐
                    │  Internet      │
                    │  Gateway       │
                    │  (IGW)         │
                    └───────┬────────┘
                            │
┌───────────────────────────┼───────────────────────────────────────────┐
│                           │        VPC (10.0.0.0/16)                  │
│                           │                                           │
│  ┌────────────────────────▼─────────────────────────────┐             │
│  │        Public Subnet (10.0.1.0/24)                   │             │
│  │  Route: 0.0.0.0/0 → IGW                              │             │
│  │                                                       │             │
│  │  ┌─────────────┐           ┌──────────────┐          │             │
│  │  │  Frontend   │           │  NAT Gateway │          │             │
│  │  │  EC2        │           │  (for private│          │             │
│  │  │  + Elastic  │           │   subnet     │          │             │
│  │  │    IP       │           │   internet)  │          │             │
│  │  └─────────────┘           └──────┬───────┘          │             │
│  │        │                          │                  │             │
│  └────────┼──────────────────────────┼──────────────────┘             │
│           │                          │                                │
│           │ Internal                 │ Outbound                       │
│           │ API Calls                │ Internet                       │
│           │                          │                                │
│  ┌────────▼──────────────────────────┼──────────────────┐             │
│  │     Private Subnet 1 (10.0.2.0/24)                   │             │
│  │     Route: 0.0.0.0/0 → NAT Gateway                   │             │
│  │                                   │                  │             │
│  │  ┌──────────────┐                 │                  │             │
│  │  │  Backend EC2 │                 │                  │             │
│  │  │  - Node.js   │◄────────────────┘                  │             │
│  │  │  - No public │  (can access internet              │             │
│  │  │    IP        │   for npm installs, etc)           │             │
│  │  └──────┬───────┘                                    │             │
│  │         │                                            │             │
│  └─────────┼────────────────────────────────────────────┘             │
│            │ SQL Queries                                              │
│            │ (SSL on port 5432)                                       │
│            │                                                          │
│  ┌─────────▼────────────────────────────────────────────┐             │
│  │     Private Subnet 2 (10.0.3.0/24)                   │             │
│  │     DB Subnet Group (Multi-AZ requires 2+ subnets)   │             │
│  │                                                       │             │
│  │  ┌──────────────────────────┐                        │             │
│  │  │  AWS RDS PostgreSQL      │                        │             │
│  │  │  - No internet access    │                        │             │
│  │  │  - Only accepts from     │                        │             │
│  │  │    Backend Security Group│                        │             │
│  │  └──────────────────────────┘                        │             │
│  │                                                       │             │
│  └───────────────────────────────────────────────────────┘             │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘

Key Components:
• Internet Gateway: Enables public subnet to access internet
• NAT Gateway: Allows private subnet 1 to access internet (for updates)
• Elastic IP: Static public IP for frontend EC2
• Route Tables: Direct traffic appropriately for each subnet
```

---

## Security Groups Flow

```
┌──────────────────────────────────────────────────────────────────────┐
│                        Security Architecture                         │
└──────────────────────────────────────────────────────────────────────┘

  Internet Users
       │
       │ HTTP (Port 80)
       │ Source: 0.0.0.0/0
       ▼
┌─────────────────────────────────────┐
│   Frontend Security Group           │
│   (frontend-sg)                     │
│                                     │
│   Inbound Rules:                    │
│   • Port 80 (HTTP) ← 0.0.0.0/0     │
│   • Port 22 (SSH) ← Your IP        │
│                                     │
│   Outbound Rules:                   │
│   • Port 3000 → Backend SG         │
│   • All traffic → 0.0.0.0/0        │
└─────────────┬───────────────────────┘
              │
              │ API Proxy
              │ Port 3000
              │ Source: Frontend SG
              ▼
┌─────────────────────────────────────┐
│   Backend Security Group            │
│   (backend-sg)                      │
│                                     │
│   Inbound Rules:                    │
│   • Port 3000 (API) ← Frontend SG  │
│   • Port 22 (SSH) ← Your IP        │
│                                     │
│   Outbound Rules:                   │
│   • Port 5432 → RDS SG             │
│   • Port 443 → 0.0.0.0/0 (HTTPS)   │
│   • Port 80 → 0.0.0.0/0 (HTTP)     │
└─────────────┬───────────────────────┘
              │
              │ PostgreSQL Connection
              │ Port 5432 (SSL/TLS)
              │ Source: Backend SG
              ▼
┌─────────────────────────────────────┐
│   RDS Security Group                │
│   (rds-sg)                          │
│                                     │
│   Inbound Rules:                    │
│   • Port 5432 (PostgreSQL)         │
│     ← Backend SG ONLY              │
│                                     │
│   Outbound Rules:                   │
│   • (None required)                 │
└─────────────────────────────────────┘

Security Principles:
✓ Least Privilege: Each tier only accepts from previous tier
✓ No Public Database: RDS not accessible from internet
✓ Private Backend: Backend has no public IP, only via NAT for updates
✓ Source-Based Rules: Security groups reference each other, not IPs
```

---

## Request Flow

### User Submits Health Data

```
1. Browser Request
   ┌─────────────────────────────────────────────────────────────┐
   │ User fills form and clicks "Calculate BMI"                  │
   └────────────────────────────┬────────────────────────────────┘
                                │
                                │ POST /api/measurements
                                │ Body: { weightKg: 70, heightCm: 175, ... }
                                │
                                ▼
   ┌─────────────────────────────────────────────────────────────┐
   │ Frontend EC2 (Nginx)                                        │
   │ - Receives request on port 80                               │
   │ - Nginx proxy_pass configuration:                           │
   │   location /api/ {                                          │
   │     proxy_pass http://10.0.2.20:3000;                       │
   │   }                                                          │
   └────────────────────────────┬────────────────────────────────┘
                                │
                                │ Forwards to Backend
                                │ http://10.0.2.20:3000/api/measurements
                                │
                                ▼
   ┌─────────────────────────────────────────────────────────────┐
   │ Backend EC2 (Node.js Express)                               │
   │ 1. Validates input data                                     │
   │ 2. Calculates BMI:                                          │
   │    bmi = weightKg / (heightCm/100)²                         │
   │ 3. Calculates BMR:                                          │
   │    Male: 10*weight + 6.25*height - 5*age + 5                │
   │    Female: 10*weight + 6.25*height - 5*age - 161            │
   │ 4. Calculates daily calories based on activity              │
   │ 5. Prepares SQL INSERT query                                │
   └────────────────────────────┬────────────────────────────────┘
                                │
                                │ SQL INSERT with SSL
                                │ postgresql://user:pass@rds-endpoint:5432/db
                                │
                                ▼
   ┌─────────────────────────────────────────────────────────────┐
   │ AWS RDS PostgreSQL                                          │
   │ 1. Accepts SSL connection from Backend SG                   │
   │ 2. Validates credentials                                    │
   │ 3. Inserts measurement into 'measurements' table            │
   │ 4. Returns inserted row with generated ID                   │
   └────────────────────────────┬────────────────────────────────┘
                                │
                                │ Database Response
                                │ { id: 123, weight_kg: 70, bmi: 22.9, ... }
                                │
                                ▼
   ┌─────────────────────────────────────────────────────────────┐
   │ Backend EC2                                                 │
   │ - Formats response as JSON                                  │
   │ - Sends HTTP 201 Created                                    │
   └────────────────────────────┬────────────────────────────────┘
                                │
                                │ JSON Response
                                │ { measurement: {...}, success: true }
                                │
                                ▼
   ┌─────────────────────────────────────────────────────────────┐
   │ Frontend EC2 (Nginx)                                        │
   │ - Proxies response back to browser                          │
   └────────────────────────────┬────────────────────────────────┘
                                │
                                │ HTTP Response
                                │
                                ▼
   ┌─────────────────────────────────────────────────────────────┐
   │ User's Browser (React)                                      │
   │ 1. Receives JSON response                                   │
   │ 2. Updates UI with results:                                 │
   │    - BMI value and category                                 │
   │    - BMR value                                              │
   │    - Daily calorie needs                                    │
   │ 3. Refreshes trend chart                                    │
   │ 4. Shows success message                                    │
   └─────────────────────────────────────────────────────────────┘

Total Round-Trip Time: ~200-500ms
```

### Load Historical Data (Chart)

```
   Browser
      │
      │ GET /api/measurements/trends
      ▼
   Frontend EC2 (Nginx)
      │
      │ proxy_pass
      ▼
   Backend EC2
      │
      │ SQL: SELECT date, AVG(bmi) FROM measurements
      │      WHERE created_at >= NOW() - INTERVAL '30 days'
      │      GROUP BY date ORDER BY date
      ▼
   AWS RDS
      │
      │ Returns aggregate data
      ▼
   Backend EC2
      │
      │ Formats for Chart.js
      ▼
   Frontend EC2 (Nginx)
      │
      │ Proxy response
      ▼
   Browser (React)
      │
      │ Renders Chart.js line graph
      └─────► User sees 30-day BMI trend
```

---

## Database Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                    AWS RDS PostgreSQL Instance                       │
│                    bmi-tracker-db.xxxxx.rds.amazonaws.com            │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Instance Specs:                                                     │
│  • Class: db.t3.micro                                                │
│  • CPU: 2 vCPU                                                       │
│  • RAM: 1 GB                                                         │
│  • Storage: 20 GB SSD (expandable to 64 TB)                          │
│  • IOPS: Baseline 3000 IOPS (burstable)                              │
│  • PostgreSQL Version: 14.x or 15.x                                  │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                  Database: bmidb                               │ │
│  │                                                                │ │
│  │  ┌──────────────────────────────────────────────────────────┐ │ │
│  │  │  Table: measurements                                     │ │ │
│  │  │                                                          │ │ │
│  │  │  Columns:                                                │ │ │
│  │  │  • id               SERIAL PRIMARY KEY                   │ │ │
│  │  │  • weight_kg        DECIMAL(5,2) NOT NULL                │ │ │
│  │  │  • height_cm        DECIMAL(5,2) NOT NULL                │ │ │
│  │  │  • age              INTEGER NOT NULL                     │ │ │
│  │  │  • sex              VARCHAR(10) NOT NULL                 │ │ │
│  │  │  • activity         VARCHAR(20) NOT NULL                 │ │ │
│  │  │  • bmi              DECIMAL(5,2)                         │ │ │
│  │  │  • bmi_category     VARCHAR(20)                          │ │ │
│  │  │  • bmr              INTEGER                              │ │ │
│  │  │  • daily_calories   INTEGER                              │ │ │
│  │  │  • created_at       TIMESTAMP DEFAULT NOW()              │ │ │
│  │  │                                                          │ │ │
│  │  │  Indexes:                                                │ │ │
│  │  │  • PRIMARY KEY on id                                     │ │ │
│  │  │  • INDEX on created_at (for trend queries)               │ │ │
│  │  └──────────────────────────────────────────────────────────┘ │ │
│  │                                                                │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  Security Features:                                                  │
│  ✓ Encryption at Rest: AES-256                                       │
│  ✓ Encryption in Transit: SSL/TLS (port 5432)                        │
│  ✓ Network Isolation: Private subnet only                            │
│  ✓ Access Control: Security group (Backend SG only)                  │
│  ✓ Authentication: Username/password (bmi_admin)                     │
│                                                                      │
│  Backup Configuration:                                               │
│  ✓ Automated Backups: Enabled (7-day retention)                      │
│  ✓ Backup Window: 03:00-04:00 UTC                                    │
│  ✓ Maintenance Window: Sun 04:00-05:00 UTC                           │
│  ✓ Point-in-Time Recovery: Enabled (up to 7 days)                    │
│                                                                      │
│  High Availability (Optional):                                       │
│  ○ Multi-AZ Deployment: Can enable for production                    │
│  ○ Automatic Failover: <120 seconds                                  │
│  ○ Standby Replica: In different AZ                                  │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘

Connection String Format:
postgresql://bmi_admin:SecurePassword123@bmi-tracker-db.xxxxx.us-east-1.rds.amazonaws.com:5432/bmidb?sslmode=require

Connection Pool (Backend):
• Min Connections: 2
• Max Connections: 10
• Idle Timeout: 10 seconds
• Connection Timeout: 10 seconds
• SSL Mode: require (rejectUnauthorized: false for AWS certificates)
```

---

## Deployment Flow

```
┌──────────────────────────────────────────────────────────────────────┐
│                      Deployment Sequence                             │
└──────────────────────────────────────────────────────────────────────┘

Phase 1: AWS Infrastructure Setup (15-20 minutes)
├─ Step 1: Create VPC (10.0.0.0/16)
├─ Step 2: Create 3 Subnets
│  ├─ Public: 10.0.1.0/24
│  ├─ Private 1: 10.0.2.0/24 (Backend)
│  └─ Private 2: 10.0.3.0/24 (RDS)
├─ Step 3: Create & attach Internet Gateway
├─ Step 4: Create NAT Gateway in public subnet
├─ Step 5: Configure Route Tables
│  ├─ Public RT: 0.0.0.0/0 → IGW
│  └─ Private RT: 0.0.0.0/0 → NAT Gateway
├─ Step 6: Create 3 Security Groups
│  ├─ Frontend SG: Port 80 ← 0.0.0.0/0, Port 22 ← Your IP
│  ├─ Backend SG: Port 3000 ← Frontend SG, Port 22 ← Your IP
│  └─ RDS SG: Port 5432 ← Backend SG only
└─ Step 7: Create EC2 Key Pair
   └─ Validation: ✓ All resources created and properly configured

Phase 2: Database Setup (10-15 minutes)
├─ Step 1: Create DB Subnet Group
│  └─ Include Private Subnet 1 and 2 (Multi-AZ requirement)
├─ Step 2: Launch RDS PostgreSQL Instance
│  ├─ Instance class: db.t3.micro
│  ├─ Storage: 20 GB
│  ├─ Database name: bmidb
│  ├─ Master username: bmi_admin
│  ├─ Master password: [secure password]
│  ├─ Subnet group: bmi-db-subnet-group
│  ├─ Security group: rds-sg
│  ├─ Public access: No
│  ├─ Encryption: Yes
│  └─ Automated backups: Enabled (7 days)
├─ Wait for "Available" status (5-10 minutes)
└─ Validation: ✓ RDS endpoint accessible from Backend subnet

Phase 3: Backend Deployment (10-15 minutes)
├─ Step 1: Launch Backend EC2 Instance
│  ├─ AMI: Ubuntu 22.04
│  ├─ Instance type: t2.micro
│  ├─ Network: VPC / Private Subnet 1
│  ├─ Security group: backend-sg
│  └─ Key pair: Your key
├─ Step 2: Connect via AWS Systems Manager Session Manager
├─ Step 3: Upload backend-ec2/ files using SCP
├─ Step 4: Run deploy-backend.sh
│  ├─ Installs Node.js 18 via NVM
│  ├─ Installs npm packages
│  ├─ Prompts for RDS endpoint and credentials
│  ├─ Creates .env file with DATABASE_URL
│  ├─ Tests database connection
│  └─ Starts PM2 process manager
├─ Step 5: Run database migration
│  └─ psql $DATABASE_URL < 001_create_measurements_rds.sql
└─ Validation:
   ├─ ✓ curl http://localhost:3000/health returns {"status":"ok"}
   ├─ ✓ pm2 status shows "online" status
   └─ ✓ Database connection successful

Phase 4: Frontend Deployment (10-15 minutes)
├─ Step 1: Launch Frontend EC2 Instance
│  ├─ AMI: Ubuntu 22.04
│  ├─ Instance type: t2.micro
│  ├─ Network: VPC / Public Subnet
│  ├─ Security group: frontend-sg
│  ├─ Auto-assign public IP: Yes
│  └─ Key pair: Your key
├─ Step 2: Allocate Elastic IP and associate
├─ Step 3: Connect via SSH
├─ Step 4: Upload frontend-ec2/ files using SCP
├─ Step 5: Update nginx.conf with backend private IP
├─ Step 6: Run deploy-frontend.sh
│  ├─ Installs Node.js 18 via NVM
│  ├─ Installs npm packages
│  ├─ Builds production React app (npm run build)
│  ├─ Installs Nginx
│  ├─ Copies nginx.conf
│  ├─ Copies build files to /var/www/bmi-frontend
│  └─ Restarts Nginx
└─ Validation:
   ├─ ✓ curl http://localhost returns React app HTML
   ├─ ✓ Browser access http://ELASTIC_IP shows app
   ├─ ✓ API requests work through proxy
   └─ ✓ Can submit measurement and see results

Phase 5: Final Testing (5 minutes)
├─ Test 1: Submit health data via web interface
├─ Test 2: Verify data stored in RDS
├─ Test 3: Check 30-day trend chart loads
├─ Test 4: Test from multiple devices
└─ Validation: ✓ All features working end-to-end

Total Deployment Time: 45-60 minutes
```

---

## Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                          BMI Calculation Flow                       │
└─────────────────────────────────────────────────────────────────────┘

User Input:                      Calculations:                Output:
┌──────────┐                    ┌──────────┐               ┌──────────┐
│ Weight   │                    │   BMI    │               │   BMI:   │
│  70 kg   │──┐              ┌─▶│  22.86   │──┐            │  22.9    │
└──────────┘  │              │  └──────────┘  │            │          │
              │              │                │            │ Category:│
┌──────────┐  │   Backend    │  ┌──────────┐  │   RDS      │ "Normal" │
│ Height   │──┼─────────────▶│  │Category  │  ├──────────▶│          │
│ 175 cm   │  │   Express    │  │ Lookup   │  │  Store     │   BMR:   │
└──────────┘  │              │  └──────────┘  │            │  1705    │
              │              │                │            │          │
┌──────────┐  │              │  ┌──────────┐  │            │ Calories:│
│   Age    │──┤              └─▶│   BMR    │──┤            │  2643    │
│   30     │  │                 │  1705    │  │            │ (moderate│
└──────────┘  │                 └──────────┘  │            │ activity)│
              │                               │            └──────────┘
┌──────────┐  │                 ┌──────────┐  │
│   Sex    │──┤                 │  Daily   │  │
│  Male    │  │                 │ Calories │──┘
└──────────┘  │                 │  2643    │
              │                 └──────────┘
┌──────────┐  │
│ Activity │──┘
│ Moderate │
└──────────┘

Formulas:
• BMI = weight(kg) / (height(m))²
• BMR (Male) = 10*weight + 6.25*height - 5*age + 5
• BMR (Female) = 10*weight + 6.25*height - 5*age - 161
• Daily Calories = BMR * Activity Multiplier
  - Sedentary: 1.2
  - Light: 1.375
  - Moderate: 1.55
  - Active: 1.725
  - Very Active: 1.9
```

---

## Cost Breakdown Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                     Monthly Cost Breakdown                           │
└──────────────────────────────────────────────────────────────────────┘

Free Tier (First 12 Months):
────────────────────────────────────────
Frontend EC2 (t2.micro)     │ $0.00  ✓ (750 hrs/month free)
Backend EC2 (t2.micro)      │ $0.00  ✓ (750 hrs/month free)
RDS db.t3.micro             │ $0.00  ✓ (750 hrs/month free)
RDS Storage (20 GB)         │ $0.00  ✓ (20 GB free)
NAT Gateway                 │ $32.00 ✗ (NOT free)
Data Transfer (5 GB)        │ $0.00  ✓ (15 GB free)
EBS Storage (16 GB EC2)     │ $0.00  ✓ (30 GB free)
Elastic IP (in use)         │ $0.00  ✓ (free when attached)
────────────────────────────────────────
Total (Free Tier):          │ ~$32/month


After Free Tier Expires:
────────────────────────────────────────
Frontend EC2 (t2.micro)     │ $8.47/month
Backend EC2 (t2.micro)      │ $8.47/month
RDS db.t3.micro             │ $25.00/month
RDS Storage (20 GB)         │ $2.30/month
RDS Backup Storage (20 GB)  │ $0.00 (free up to DB size)
NAT Gateway                 │ $32.85/month
Data Transfer Out (5 GB)    │ $0.45/month
EBS Storage (16 GB)         │ $1.60/month
Elastic IP (in use)         │ $0.00
────────────────────────────────────────
Total (Post Free Tier):     │ ~$83/month


Cost Optimization Options:
────────────────────────────────────────
Stop NAT when not developing    Saves ~$33/month
Reserved Instances (1 year)     Saves ~30-40%
Spot Instances (dev/test)       Saves ~70%
Single-AZ RDS (non-prod)        No extra cost
Stop instances when idle        Variable savings
────────────────────────────────────────

Annual Cost Estimate:
Year 1: $384 (12 months * $32 NAT only)
Year 2+: $996 (12 months * $83)
```

---

## Scaling Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                      Future Scaling Options                          │
└──────────────────────────────────────────────────────────────────────┘

Current (Single Instance per Tier):
  [Frontend EC2] → [Backend EC2] → [RDS Single-AZ]

Vertical Scaling (Upgrade instance sizes):
  [Frontend t2.small] → [Backend t2.small] → [RDS db.t3.small]
  Cost: +$25/month, 2x resources

Horizontal Scaling (Add load balancers):
  [ALB] → [Frontend EC2 x2] → [ALB] → [Backend EC2 x2] → [RDS Multi-AZ]
  Cost: +$100/month, Better availability

High Availability (Multi-AZ):
           ┌─[Frontend EC2 - AZ 1]
  [ALB] ──┤
           └─[Frontend EC2 - AZ 2]
                     │
           ┌─[Backend EC2 - AZ 1]
           │         │
  [ALB] ──┤         └─[RDS Multi-AZ]
           │           ├─Primary (AZ 1)
           └─[Backend EC2 - AZ 2]
                       └─Standby (AZ 2)
  Cost: +$150/month, Production-ready

Containerized (Future):
  [ALB] → [ECS Fargate] → [ALB] → [ECS Fargate] → [RDS Aurora]
  Cost: Variable, Auto-scaling
```

---

## Summary

This architecture provides:
- ✅ **Separation of Concerns**: Frontend, Backend, Database on separate systems
- ✅ **Security**: Private subnets, security groups, SSL encryption
- ✅ **Scalability**: Can upgrade instances or add more as needed
- ✅ **Managed Database**: AWS handles backups, patching, high availability
- ✅ **Cost-Effective**: Free Tier eligible (~$32/month initially)
- ✅ **Production-Ready**: Complete monitoring, logging, and backup capabilities

For detailed deployment steps, see [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)
