# BMI Health Tracker - Detailed Architecture Documentation

**Project**: BMI Health Tracker  
**Architecture Type**: 3-Tier AWS Cloud Application  
**Version**: 1.0  
**Last Updated**: December 13, 2025

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [System Components](#system-components)
3. [Network Architecture](#network-architecture)
4. [Security Architecture](#security-architecture)
5. [Data Architecture](#data-architecture)
6. [Application Architecture](#application-architecture)
7. [Deployment Architecture](#deployment-architecture)
8. [Scalability & High Availability](#scalability--high-availability)
9. [Monitoring & Logging](#monitoring--logging)
10. [Disaster Recovery](#disaster-recovery)

---

## Architecture Overview

### High-Level Architecture

The BMI Health Tracker is built using a **3-tier architecture** pattern deployed on AWS infrastructure:

```
Presentation Tier (Frontend)  →  Application Tier (Backend)  →  Data Tier (Database)
     React + Nginx                 Node.js + Express              AWS RDS PostgreSQL
     Public Subnet                 Private Subnet                 Private Subnet
```

### Architectural Principles

**1. Separation of Concerns**
- Each tier has a distinct responsibility
- Frontend handles presentation and user interaction
- Backend handles business logic and API
- Database handles data persistence

**2. Defense in Depth**
- Multiple layers of security
- Network isolation using VPC subnets
- Security groups at each layer
- Encrypted data in transit and at rest

**3. Scalability by Design**
- Each tier can scale independently
- Stateless application design
- Connection pooling for database
- Ready for horizontal scaling

**4. Managed Services**
- AWS RDS for database (reduces operational overhead)
- AWS-managed infrastructure (EC2, VPC, security)
- Automated backups and patching

**5. Cost Optimization**
- Free Tier eligible resources
- Right-sized instances (t2.micro, db.t3.micro)
- NAT Gateway only when needed
- Storage optimization

---

## System Components

### 1. Frontend Tier (Presentation Layer)

**Purpose**: Serve user interface and handle user interactions

**Components**:
- **Web Server**: Nginx
  - Role: Serve static files, reverse proxy to backend
  - Port: 80 (HTTP)
  - Configuration: `/etc/nginx/sites-available/bmi-frontend`
  
- **React Application**: 
  - Framework: React 18.2 with Vite 5.0
  - Components:
    - `App.jsx`: Main application component
    - `MeasurementForm.jsx`: User input form
    - `TrendChart.jsx`: Data visualization with Chart.js
  - State Management: React Hooks (useState, useEffect)
  - API Client: Axios for HTTP requests
  
- **Static Assets**:
  - CSS: Custom styling (~350 lines)
  - Built files: Optimized bundle in `/var/www/bmi-frontend`

**Deployment**:
- EC2 Instance: t2.micro (1 vCPU, 1 GB RAM)
- OS: Ubuntu 22.04 LTS
- Location: Public subnet (10.0.1.0/24)
- Public Access: Via Elastic IP
- Process Management: systemd (Nginx service)

**Responsibilities**:
- Render user interface
- Client-side validation
- Make API calls to backend
- Display results and charts
- Handle user errors gracefully

---

### 2. Backend Tier (Application Layer)

**Purpose**: Handle business logic, API endpoints, and database operations

**Components**:
- **Application Server**: Node.js 18 LTS
  - Runtime: JavaScript execution environment
  - Package Manager: npm
  - Process Manager: PM2 (auto-restart, monitoring)
  
- **Web Framework**: Express.js 4.18
  - Middleware:
    - `cors`: Cross-Origin Resource Sharing
    - `express.json()`: Parse JSON payloads
    - Error handling middleware
  
- **Application Modules**:
  - `server.js`: Express server setup and configuration
  - `routes.js`: API endpoint definitions
  - `db.js`: PostgreSQL connection pool with SSL
  - `calculations.js`: BMI and BMR calculation logic

**API Endpoints**:
```
GET  /health                   → Health check
POST /api/measurements         → Create new measurement
GET  /api/measurements         → Get all measurements
GET  /api/measurements/trends  → Get 30-day aggregated data
```

**Deployment**:
- EC2 Instance: t2.micro (1 vCPU, 1 GB RAM)
- OS: Ubuntu 22.04 LTS
- Location: Private subnet (10.0.2.0/24)
- No Public IP: Internet access via NAT Gateway
- Process Management: PM2 ecosystem
- Port: 3000 (internal only)

**Responsibilities**:
- Validate input data
- Calculate BMI (Body Mass Index)
- Calculate BMR (Basal Metabolic Rate)
- Calculate daily calorie needs
- Store data in database
- Retrieve and aggregate data
- Handle errors and edge cases

---

### 3. Data Tier (Database Layer)

**Purpose**: Persist application data with reliability and security

**Database**: AWS RDS PostgreSQL 15

**Configuration**:
- Instance Class: db.t3.micro (2 vCPU, 1 GB RAM)
- Storage: 20 GB General Purpose SSD (gp2)
- Engine: PostgreSQL 15.x
- Multi-AZ: Disabled (can enable for HA)
- Backup Retention: 7 days
- Maintenance Window: Sunday 04:00-05:00 UTC

**Database Schema**:
```sql
CREATE TABLE measurements (
    id SERIAL PRIMARY KEY,
    weight_kg DECIMAL(5,2) NOT NULL CHECK (weight_kg > 0),
    height_cm DECIMAL(5,2) NOT NULL CHECK (height_cm > 0),
    age INTEGER NOT NULL CHECK (age > 0 AND age < 150),
    sex VARCHAR(10) NOT NULL CHECK (sex IN ('male', 'female')),
    activity VARCHAR(20) NOT NULL,
    bmi DECIMAL(5,2),
    bmi_category VARCHAR(20),
    bmr INTEGER,
    daily_calories INTEGER,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_measurements_created_at ON measurements(created_at);
```

**Connection Configuration**:
- Connection Type: SSL/TLS encrypted
- Connection Pool: 2-10 connections
- Connection Timeout: 10 seconds
- SSL Mode: `require` (with AWS RDS certificate)

**Deployment**:
- Location: Private subnet group (10.0.2.0/24, 10.0.3.0/24)
- Availability Zones: 2 (for Multi-AZ capability)
- Endpoint: `bmi-tracker-db.xxxxx.us-east-1.rds.amazonaws.com:5432`
- No Public Access: Only accessible from Backend SG

**Responsibilities**:
- Store measurement records
- Enforce data integrity constraints
- Provide data aggregation for trends
- Automated backups and recovery
- Maintain data consistency

---

## Network Architecture

### Virtual Private Cloud (VPC)

**VPC Configuration**:
- **CIDR Block**: 10.0.0.0/16 (65,536 IP addresses)
- **DNS Hostnames**: Enabled
- **DNS Resolution**: Enabled
- **Region**: us-east-1 (configurable)

**Design Rationale**:
- Large enough for growth (65k IPs)
- Private IP range (RFC 1918)
- Easy to remember and manage

---

### Subnets

**1. Public Subnet**
- **Name**: bmi-public-subnet
- **CIDR**: 10.0.1.0/24 (256 IPs)
- **Availability Zone**: us-east-1a
- **Purpose**: Host internet-facing resources
- **Resources**: Frontend EC2, NAT Gateway
- **Route**: 0.0.0.0/0 → Internet Gateway

**2. Private Subnet 1**
- **Name**: bmi-private-subnet-1
- **CIDR**: 10.0.2.0/24 (256 IPs)
- **Availability Zone**: us-east-1a
- **Purpose**: Host application servers
- **Resources**: Backend EC2
- **Route**: 0.0.0.0/0 → NAT Gateway

**3. Private Subnet 2**
- **Name**: bmi-private-subnet-2
- **CIDR**: 10.0.3.0/24 (256 IPs)
- **Availability Zone**: us-east-1b (different AZ)
- **Purpose**: Database high availability
- **Resources**: RDS standby (if Multi-AZ enabled)
- **Route**: 0.0.0.0/0 → NAT Gateway

**Subnet Design Benefits**:
- Clear separation of concerns
- Multiple AZs for high availability
- Allows for horizontal scaling
- Network isolation for security

---

### Gateways

**1. Internet Gateway (IGW)**
- **Name**: bmi-igw
- **Purpose**: Enable internet access for public subnet
- **Attached to**: bmi-tracker-vpc
- **Function**: 
  - Provides internet connectivity
  - Performs NAT for Elastic IPs
  - Stateless (tracks no connection info)

**2. NAT Gateway**
- **Name**: bmi-nat-gateway
- **Location**: bmi-public-subnet
- **Elastic IP**: Auto-allocated
- **Purpose**: Enable private subnets to access internet
- **Use Cases**:
  - Download OS updates
  - Install npm packages
  - Connect to external APIs
- **Cost**: ~$32/month + data transfer
- **High Availability**: Single AZ (can create per-AZ for HA)

**Gateway Selection Rationale**:
- NAT Gateway vs NAT Instance: Managed, higher bandwidth, no maintenance
- IGW for direct internet access (public subnet only)
- Separation allows security control

---

### Route Tables

**1. Public Route Table (bmi-public-rt)**
```
Destination         Target
10.0.0.0/16        local
0.0.0.0/0          igw-xxxxx (Internet Gateway)
```
- **Associated Subnets**: bmi-public-subnet
- **Purpose**: Route internet traffic to IGW

**2. Private Route Table (bmi-private-rt)**
```
Destination         Target
10.0.0.0/16        local
0.0.0.0/0          nat-xxxxx (NAT Gateway)
```
- **Associated Subnets**: bmi-private-subnet-1, bmi-private-subnet-2
- **Purpose**: Route internet traffic through NAT

**Route Table Logic**:
- `local` routes enable VPC-internal communication
- `0.0.0.0/0` (default route) directs internet-bound traffic
- Private subnets cannot receive inbound internet traffic
- NAT allows outbound only (one-way)

---

### Elastic IP

**Configuration**:
- **Associated With**: Frontend EC2
- **Purpose**: Static public IP address
- **Benefits**:
  - Survives EC2 stop/start
  - Can reassociate to different instance
  - No cost when associated
  - Enables DNS mapping

**Use Case**:
- Users access application via this IP
- Can later add domain name (e.g., bmi.example.com)
- Consistent access point

---

## Security Architecture

### Defense in Depth Strategy

The architecture implements multiple layers of security:

**Layer 1: Network Isolation**
- VPC provides network boundary
- Private subnets isolate backend and database
- No direct internet access to sensitive components

**Layer 2: Security Groups (Stateful Firewalls)**
- Instance-level firewalls
- Whitelist approach (deny by default)
- Source-based rules (reference other security groups)

**Layer 3: Application Security**
- Input validation on frontend and backend
- SQL injection protection (parameterized queries)
- Environment-based configuration
- No hardcoded credentials

**Layer 4: Data Security**
- SSL/TLS for all connections
- Encrypted data at rest (RDS)
- Encrypted data in transit
- Secure credential storage

---

### Security Groups

**1. Frontend Security Group (frontend-sg)**

**Purpose**: Control access to frontend web server

**Inbound Rules**:
```
Protocol  Port   Source              Purpose
HTTP      80     0.0.0.0/0          Allow web access from internet
SSH       22     YOUR_IP/32         Admin access (your IP only)
```

**Outbound Rules**:
```
Protocol  Port   Destination         Purpose
TCP       3000   backend-sg          API calls to backend
All       All    0.0.0.0/0          Internet access (OS updates, etc.)
```

**Security Considerations**:
- Port 80 open to world (required for website)
- SSH restricted to specific IP
- Can only reach backend on port 3000
- HTTPS recommended (add ALB with SSL)

---

**2. Backend Security Group (backend-sg)**

**Purpose**: Protect application server

**Inbound Rules**:
```
Protocol  Port   Source              Purpose
TCP       3000   frontend-sg         API requests from frontend only
SSH       22     YOUR_IP/32          Admin access
```

**Outbound Rules**:
```
Protocol  Port   Destination         Purpose
TCP       5432   rds-sg              Database queries
HTTPS     443    0.0.0.0/0           npm packages, OS updates
HTTP      80     0.0.0.0/0           Package mirrors
```

**Security Considerations**:
- No direct internet access (inbound)
- Can only be accessed from frontend
- Can reach RDS on PostgreSQL port
- Internet access for package installation

---

**3. RDS Security Group (rds-sg)**

**Purpose**: Maximum protection for database

**Inbound Rules**:
```
Protocol    Port   Source              Purpose
PostgreSQL  5432   backend-sg          Database queries from backend only
```

**Outbound Rules**:
```
None required (RDS doesn't initiate connections)
```

**Security Considerations**:
- Most restrictive security group
- Only backend can connect
- No internet access (inbound or outbound)
- Cannot be accessed from frontend directly
- Cannot be accessed from internet

---

### Data Encryption

**1. Data in Transit**

**Frontend ↔ User**:
- Currently: HTTP (port 80)
- Recommended: HTTPS with SSL certificate
- Implementation: Add Application Load Balancer with ACM certificate

**Frontend ↔ Backend**:
- Protocol: HTTP (internal VPC traffic)
- Security: Protected by security groups and private subnet
- Enhancement: Can use HTTPS with self-signed cert

**Backend ↔ RDS**:
- Protocol: PostgreSQL with SSL/TLS
- Configuration: `ssl: { rejectUnauthorized: false }`
- Certificate: AWS RDS certificate authority
- Encryption: TLS 1.2 or higher

**2. Data at Rest**

**RDS Storage**:
- Encryption: AES-256
- Key Management: AWS KMS
- Encrypted: Database files, automated backups, read replicas, snapshots
- Configuration: Enabled at RDS creation (cannot be changed later)

**EC2 Storage**:
- EBS volumes: Can enable encryption
- Snapshots: Inherit encryption from volume
- Recommendation: Enable for production

---

### Authentication & Authorization

**Current Implementation**:
- No user authentication (single-user application)
- Database authentication via username/password
- RDS credentials stored in environment variables

**Future Enhancements**:
- JWT-based user authentication
- Multi-user support with user accounts
- Role-based access control (RBAC)
- OAuth2 integration (Google, Facebook login)
- API key authentication for API access

---

### IAM (Identity and Access Management)

**EC2 Instance Roles** (not currently implemented):
- Backend EC2 can assume role to access:
  - S3 for file uploads
  - CloudWatch for logging
  - Systems Manager for secure credential storage
  
**Benefits of IAM Roles**:
- No credentials in code or environment files
- Automatic credential rotation
- Fine-grained permissions
- Audit trail via CloudTrail

---

## Data Architecture

### Data Flow

**1. User Submits Measurement**

```
User Browser
    │
    ├─ Fills form (weight, height, age, sex, activity)
    ├─ Client-side validation
    │
    ▼
POST /api/measurements (JSON payload)
    │
    ▼
Frontend EC2 (Nginx)
    │
    ├─ Receives HTTP request
    ├─ Proxy to backend (proxy_pass)
    │
    ▼
Backend EC2 (Express)
    │
    ├─ Parse JSON body
    ├─ Server-side validation
    ├─ Calculate BMI = weight / (height/100)²
    ├─ Determine BMI category
    ├─ Calculate BMR (Mifflin-St Jeor formula)
    ├─ Calculate daily calories = BMR × activity factor
    │
    ▼
SQL INSERT INTO measurements
    │
    ▼
AWS RDS PostgreSQL
    │
    ├─ Validate constraints
    ├─ Insert record
    ├─ Generate ID (SERIAL)
    ├─ Set created_at timestamp
    │
    ▼
Return inserted row
    │
    ▼
Backend formats response
    │
    ▼
Frontend displays results
    │
    ▼
User sees BMI, BMR, calories
```

**2. User Views Trend Chart**

```
User Browser
    │
    ▼
GET /api/measurements/trends
    │
    ▼
Frontend EC2 (Nginx) → Backend EC2 (Express)
    │
    ▼
SQL: SELECT 
       DATE(created_at) as day,
       AVG(bmi) as avg_bmi,
       COUNT(*) as count
     FROM measurements
     WHERE created_at >= NOW() - INTERVAL '30 days'
     GROUP BY DATE(created_at)
     ORDER BY day
    │
    ▼
AWS RDS PostgreSQL
    │
    ├─ Aggregate data by day
    ├─ Calculate average BMI per day
    │
    ▼
Return aggregated data
    │
    ▼
Backend formats for Chart.js
    │
    ▼
Frontend renders line chart
    │
    ▼
User sees 30-day trend
```

---

### Database Design

**Table: measurements**

**Columns**:
- `id`: SERIAL PRIMARY KEY → Auto-incrementing unique identifier
- `weight_kg`: DECIMAL(5,2) → Up to 999.99 kg
- `height_cm`: DECIMAL(5,2) → Up to 999.99 cm
- `age`: INTEGER → Whole number
- `sex`: VARCHAR(10) → 'male' or 'female'
- `activity`: VARCHAR(20) → Activity level description
- `bmi`: DECIMAL(5,2) → Calculated BMI value
- `bmi_category`: VARCHAR(20) → 'Underweight', 'Normal', 'Overweight', 'Obese'
- `bmr`: INTEGER → Basal Metabolic Rate (calories)
- `daily_calories`: INTEGER → Total daily calorie needs
- `created_at`: TIMESTAMP → Record creation time

**Constraints**:
- NOT NULL: weight_kg, height_cm, age, sex, activity
- CHECK: weight_kg > 0, height_cm > 0
- CHECK: age > 0 AND age < 150
- CHECK: sex IN ('male', 'female')

**Indexes**:
- PRIMARY KEY on `id` (automatic)
- INDEX on `created_at` (for trend queries)

**Design Rationale**:
- Store both input and calculated values (denormalization for performance)
- Timestamps enable historical tracking
- Constraints ensure data integrity
- Index on created_at speeds up trend queries

---

### Data Validation

**1. Frontend Validation** (First Line of Defense)
```javascript
// Client-side validation in React
- Weight: Required, > 0, < 500 kg
- Height: Required, > 0, < 300 cm
- Age: Required, > 0, < 150 years
- Sex: Required, 'male' or 'female'
- Activity: Required, one of predefined options
```

**2. Backend Validation** (Second Line of Defense)
```javascript
// Server-side validation in Express
- Type checking (number vs string)
- Range validation
- Required field validation
- SQL injection prevention (parameterized queries)
```

**3. Database Constraints** (Final Line of Defense)
```sql
-- Database-level constraints
CHECK (weight_kg > 0)
CHECK (height_cm > 0)
CHECK (age > 0 AND age < 150)
CHECK (sex IN ('male', 'female'))
NOT NULL constraints
```

**Three-Layer Validation Benefits**:
- Better user experience (immediate feedback)
- Prevent invalid API calls
- Guarantee data integrity
- Defense against malicious inputs

---

### Calculation Logic

**1. BMI (Body Mass Index)**
```javascript
Formula: BMI = weight(kg) / (height(m))²

Example:
Weight: 70 kg
Height: 175 cm = 1.75 m
BMI = 70 / (1.75)² = 70 / 3.0625 = 22.86

Categories:
< 18.5          Underweight
18.5 - 24.9     Normal
25.0 - 29.9     Overweight
≥ 30.0          Obese
```

**2. BMR (Basal Metabolic Rate)**
```javascript
Formula: Mifflin-St Jeor Equation

For Males:
BMR = (10 × weight_kg) + (6.25 × height_cm) - (5 × age) + 5

For Females:
BMR = (10 × weight_kg) + (6.25 × height_cm) - (5 × age) - 161

Example (30-year-old male, 70kg, 175cm):
BMR = (10 × 70) + (6.25 × 175) - (5 × 30) + 5
BMR = 700 + 1093.75 - 150 + 5
BMR = 1648.75 ≈ 1649 calories/day
```

**3. Daily Calorie Needs**
```javascript
Formula: BMR × Activity Factor

Activity Factors:
Sedentary (little/no exercise):        1.2
Lightly Active (1-3 days/week):        1.375
Moderately Active (3-5 days/week):     1.55
Very Active (6-7 days/week):           1.725
Extra Active (athlete/physical job):   1.9

Example:
BMR = 1649
Activity = Moderately Active (1.55)
Daily Calories = 1649 × 1.55 = 2555.95 ≈ 2556 calories/day
```

---

## Application Architecture

### Frontend Application

**Technology Stack**:
- **Framework**: React 18.2
- **Build Tool**: Vite 5.0
- **State Management**: React Hooks (useState, useEffect)
- **HTTP Client**: Axios
- **Charting**: Chart.js 4.4
- **Styling**: Custom CSS

**Component Structure**:
```
App.jsx (Main Container)
    │
    ├─── MeasurementForm.jsx
    │       ├─ Input fields (weight, height, age, sex, activity)
    │       ├─ Form validation
    │       ├─ Submit handler
    │       └─ Error display
    │
    ├─── Results Display (inline)
    │       ├─ BMI card
    │       ├─ BMR card
    │       └─ Calories card
    │
    └─── TrendChart.jsx
            ├─ Fetch trend data (useEffect)
            ├─ Chart.js line chart
            └─ 30-day data visualization
```

**State Management**:
```javascript
const [measurements, setMeasurements] = useState(null);
const [loading, setLoading] = useState(false);
const [error, setError] = useState(null);

// On form submit:
1. setLoading(true)
2. Make API call
3. setMeasurements(response)
4. setLoading(false)
5. Handle errors with setError()
```

**API Integration**:
```javascript
// api.js - Axios configuration
const api = axios.create({
    baseURL: process.env.VITE_BACKEND_URL || '/api',
    headers: {
        'Content-Type': 'application/json'
    }
});

// Usage in components
api.post('/measurements', data)
api.get('/measurements')
api.get('/measurements/trends')
```

**Build Process**:
```bash
npm run build
    │
    ├─ Vite bundles React app
    ├─ Tree-shaking (remove unused code)
    ├─ Minification
    ├─ Code splitting
    │
    ▼
Output: dist/ directory
    ├─ index.html
    ├─ assets/
    │   ├─ index-[hash].js (bundled JavaScript)
    │   └─ index-[hash].css (bundled CSS)
    └─ (copied to /var/www/bmi-frontend)
```

---

### Backend Application

**Technology Stack**:
- **Runtime**: Node.js 18 LTS
- **Framework**: Express.js 4.18
- **Database Client**: pg (node-postgres) 8.10
- **Process Manager**: PM2
- **Middleware**: cors, express.json()

**Application Structure**:
```
server.js (Entry Point)
    │
    ├─ Load environment variables (.env)
    ├─ Initialize Express app
    ├─ Configure middleware (CORS, JSON parser)
    ├─ Mount routes
    ├─ Error handling middleware
    └─ Start server on port 3000
    
routes.js (API Endpoints)
    │
    ├─ POST /api/measurements
    │       ├─ Validate input
    │       ├─ Calculate BMI (calculations.js)
    │       ├─ Calculate BMR (calculations.js)
    │       ├─ Calculate calories
    │       ├─ INSERT into database
    │       └─ Return result
    │
    ├─ GET /api/measurements
    │       ├─ SELECT all records
    │       └─ Return JSON array
    │
    └─ GET /api/measurements/trends
            ├─ SELECT with GROUP BY date
            ├─ Aggregate by day
            └─ Return 30-day data

db.js (Database Connection)
    │
    ├─ Create connection pool
    │       ├─ Min: 2 connections
    │       ├─ Max: 10 connections
    │       ├─ SSL: enabled (for RDS)
    │       └─ Timeout: 10 seconds
    │
    ├─ Test connection on startup
    └─ Export query function

calculations.js (Business Logic)
    │
    ├─ calculateBMI(weight, height)
    ├─ getBMICategory(bmi)
    ├─ calculateBMR(weight, height, age, sex)
    └─ calculateDailyCalories(bmr, activity)
```

**Connection Pooling**:
```javascript
// Why connection pooling?
1. Reuse database connections (faster)
2. Limit concurrent connections to RDS
3. Handle connection failures gracefully
4. Automatic connection recycling

// Configuration:
{
    min: 2,           // Keep 2 connections alive
    max: 10,          // Allow up to 10 concurrent
    idleTimeoutMillis: 10000,  // Close idle after 10s
    connectionTimeoutMillis: 10000  // Fail after 10s
}
```

**Process Management (PM2)**:
```javascript
// ecosystem.config.js
module.exports = {
    apps: [{
        name: 'bmi-backend',
        script: './src/server.js',
        instances: 1,         // Single instance (can scale to 'max')
        autorestart: true,    // Auto-restart on crash
        watch: false,         // Don't watch files in production
        max_memory_restart: '500M',  // Restart if memory exceeds 500MB
        env: {
            NODE_ENV: 'production',
            PORT: 3000
        },
        error_file: 'logs/err.log',
        out_file: 'logs/out.log',
        time: true            // Prefix logs with timestamp
    }]
};

// PM2 Features:
- Auto-restart on crash
- Log management
- Process monitoring
- Zero-downtime reloads
- Cluster mode support
```

---

### Nginx Configuration

**Purpose**: Web server and reverse proxy

**Configuration File**: `/etc/nginx/sites-available/bmi-frontend`

```nginx
server {
    listen 80 default_server;
    listen [::]:80 default_server;
    
    root /var/www/bmi-frontend;
    index index.html;
    
    server_name _;
    
    # Serve static files
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    # Proxy API requests to backend
    location /api/ {
        proxy_pass http://10.0.2.20:3000;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    # Health check endpoint
    location /health {
        proxy_pass http://10.0.2.20:3000/health;
    }
}
```

**Key Features**:

**1. Static File Serving**
- Serves React build files from `/var/www/bmi-frontend`
- `try_files` directive handles client-side routing
- Falls back to `index.html` for SPA routes

**2. Reverse Proxy**
- Forwards `/api/*` requests to backend
- Adds proxy headers (real IP, forwarded for, etc.)
- Maintains HTTP/1.1 connection

**3. Performance**
- Gzip compression (inherited from main config)
- Client-side caching headers
- Keep-alive connections

**4. Security**
- Hides backend implementation
- Can add rate limiting
- Can add authentication at proxy level

---

## Deployment Architecture

### Deployment Strategy

**Type**: Manual deployment with automated scripts

**Phases**:
1. Infrastructure provisioning (manual via AWS Console)
2. Application deployment (automated via shell scripts)
3. Validation (automated checkpoints)

---

### Deployment Scripts

**1. Backend Deployment (deploy-backend.sh)**

**What it does**:
```bash
1. Update system packages
2. Install Node.js 18 via NVM
3. Install npm dependencies
4. Prompt for RDS credentials
5. Create .env file
6. Install PostgreSQL client (psql)
7. Test database connection
8. Start application with PM2
9. Validate health endpoint
```

**Key Features**:
- Idempotent (can run multiple times)
- Validates each step
- Interactive prompts for sensitive data
- No hardcoded credentials

---

**2. Frontend Deployment (deploy-frontend.sh)**

**What it does**:
```bash
1. Update system packages
2. Install Node.js 18 via NVM
3. Install npm dependencies
4. Build React application (npm run build)
5. Install Nginx
6. Copy nginx.conf to sites-available
7. Create symbolic link to sites-enabled
8. Copy build files to /var/www/bmi-frontend
9. Set correct permissions
10. Restart Nginx
11. Validate website accessible
```

**Key Features**:
- Automates entire frontend setup
- Handles Nginx configuration
- Validates deployment success
- Production-optimized build

---

### Deployment Order

**Critical**: Must deploy in this order

```
1. AWS Infrastructure
   ├─ VPC and subnets
   ├─ Internet Gateway
   ├─ NAT Gateway
   ├─ Route tables
   ├─ Security groups
   └─ EC2 key pair

2. AWS RDS Database
   ├─ DB subnet group
   ├─ Create RDS instance
   └─ Wait for "Available" status

3. Backend EC2
   ├─ Launch instance
   ├─ Upload files
   ├─ Run deploy-backend.sh
   ├─ Run database migration
   └─ Validate health endpoint

4. Frontend EC2
   ├─ Launch instance
   ├─ Associate Elastic IP
   ├─ Upload files
   ├─ Update nginx.conf with backend IP
   ├─ Run deploy-frontend.sh
   └─ Validate website loads

5. End-to-End Testing
   ├─ Submit test measurement
   ├─ Verify data in database
   └─ Check trend chart
```

**Why this order?**
- Database must exist before backend connects
- Backend must be running before frontend can proxy to it
- Infrastructure must exist before deploying applications

---

### Configuration Management

**Environment Variables**:

**Backend (.env)**:
```bash
NODE_ENV=production
PORT=3000
DATABASE_URL=postgresql://user:pass@rds-endpoint:5432/bmidb
```

**Frontend (.env)**:
```bash
VITE_BACKEND_URL=http://BACKEND_IP:3000
```

**Security Best Practices**:
- Never commit `.env` files to git
- Use `.env.example` as template
- Restrict file permissions (chmod 600 .env)
- Rotate credentials regularly
- Use AWS Secrets Manager for production

---

## Scalability & High Availability

### Current Architecture Limitations

**Single Points of Failure**:
- Single Frontend EC2 instance
- Single Backend EC2 instance
- Single-AZ RDS (if Multi-AZ not enabled)
- Single NAT Gateway

**Current Capacity**:
- ~100-500 concurrent users (estimate)
- Limited by t2.micro instances
- Database connection pool limit: 10

---

### Horizontal Scaling Strategy

**Phase 1: Add Load Balancers**

```
                    ┌─── Frontend EC2 #1
Internet → ALB #1 ──┤
                    └─── Frontend EC2 #2

                    ┌─── Backend EC2 #1
ALB #1 → ALB #2 ────┤
                    └─── Backend EC2 #2

ALB #2 → RDS (Multi-AZ)
```

**Changes Required**:
1. Create Application Load Balancers
2. Create Target Groups
3. Configure health checks
4. Update security groups
5. Enable sticky sessions (if needed)

**Benefits**:
- No single point of failure
- Automatic failover
- Can scale instances independently
- Health check-based routing

**Cost**: +$16/month per ALB

---

**Phase 2: Auto Scaling**

```
Auto Scaling Group (Frontend)
    ├─ Min: 2 instances
    ├─ Desired: 2 instances
    ├─ Max: 5 instances
    └─ Scale based on:
        ├─ CPU > 70% (scale out)
        └─ CPU < 30% (scale in)

Auto Scaling Group (Backend)
    ├─ Min: 2 instances
    ├─ Desired: 2 instances
    ├─ Max: 10 instances
    └─ Scale based on:
        ├─ CPU > 70% (scale out)
        ├─ Active connections > 1000
        └─ Custom CloudWatch metrics
```

**Benefits**:
- Automatic capacity adjustment
- Handle traffic spikes
- Cost optimization (scale down when idle)

---

**Phase 3: Multi-AZ & Multi-Region**

**Multi-AZ**:
```
Region: us-east-1
    ├─ AZ 1 (us-east-1a)
    │   ├─ Frontend EC2 instances
    │   ├─ Backend EC2 instances
    │   └─ RDS Primary
    │
    └─ AZ 2 (us-east-1b)
        ├─ Frontend EC2 instances
        ├─ Backend EC2 instances
        └─ RDS Standby (automatic failover)
```

**Multi-Region** (Future):
```
Primary Region: us-east-1
    └─ Full stack deployment

Secondary Region: us-west-2
    └─ Read-only replica or active-active

Route 53 (DNS):
    └─ Geolocation routing
        ├─ East Coast → us-east-1
        └─ West Coast → us-west-2
```

**Benefits**:
- 99.99% availability SLA
- Disaster recovery
- Reduced latency (geo-routing)
- Compliance (data residency)

---

### Vertical Scaling Strategy

**Current**: t2.micro, db.t3.micro

**Upgrade Path**:

**Level 1: Small**
- Frontend: t2.small (2 vCPU, 2 GB RAM) → +$8/mo
- Backend: t2.small (2 vCPU, 2 GB RAM) → +$8/mo
- RDS: db.t3.small (2 vCPU, 2 GB RAM) → +$25/mo
- **Total**: +$41/month
- **Capacity**: ~500-1000 users

**Level 2: Medium**
- Frontend: t3.medium (2 vCPU, 4 GB RAM) → +$30/mo
- Backend: t3.medium (2 vCPU, 4 GB RAM) → +$30/mo
- RDS: db.t3.medium (2 vCPU, 4 GB RAM) → +$60/mo
- **Total**: +$120/month
- **Capacity**: ~1000-5000 users

**Level 3: Large**
- Frontend: t3.large (2 vCPU, 8 GB RAM)
- Backend: t3.large (2 vCPU, 8 GB RAM)
- RDS: db.r5.large (2 vCPU, 16 GB RAM, memory-optimized)
- **Total**: +$300/month
- **Capacity**: ~5000-10000 users

---

### Database Scaling

**Read Replicas**:
```
Primary RDS (us-east-1a)
    ├─ Handles: All writes, some reads
    │
    ├─── Read Replica #1 (us-east-1a)
    │       └─ Handles: Trend queries, reporting
    │
    └─── Read Replica #2 (us-east-1b)
            └─ Handles: Analytics, exports
```

**Application Changes**:
```javascript
// Write operations → Primary
await primaryDB.query('INSERT INTO measurements...');

// Read operations → Replicas
await replicaDB.query('SELECT * FROM measurements...');
```

**Benefits**:
- Offload read traffic from primary
- Improved query performance
- Can promote replica to primary (failover)

**Cost**: ~$25/month per replica (db.t3.micro)

---

**Connection Pooling at Scale**:

**Current**: Single pool (2-10 connections)

**Scaled**: PgBouncer connection pooler
```
Backend Instances (10+)
    ↓ (thousands of connections)
PgBouncer (pooling layer)
    ↓ (100 connections)
RDS PostgreSQL
```

**Benefits**:
- Reduce RDS connection count
- Faster connection reuse
- Better resource utilization

---

### Caching Strategy

**Level 1: Application-level Cache**
```javascript
// In-memory cache with TTL
const cache = new Map();

app.get('/api/measurements/trends', async (req, res) => {
    const cacheKey = 'trends-30d';
    
    // Check cache
    if (cache.has(cacheKey)) {
        return res.json(cache.get(cacheKey));
    }
    
    // Query database
    const data = await db.query('SELECT...');
    
    // Cache for 5 minutes
    cache.set(cacheKey, data);
    setTimeout(() => cache.delete(cacheKey), 5 * 60 * 1000);
    
    res.json(data);
});
```

**Benefits**:
- Reduces database queries
- Faster response times
- No additional infrastructure

**Limitations**:
- Cache not shared between instances
- Memory consumption

---

**Level 2: Redis Cache**
```
Backend Instances
    ↓
Redis ElastiCache (managed)
    ↓ (on cache miss)
RDS PostgreSQL
```

**Use Cases**:
- Cache trend data (updates infrequently)
- Cache user sessions (future feature)
- Rate limiting counters
- Real-time analytics

**Implementation**:
```javascript
const redis = require('redis');
const client = redis.createClient({
    host: 'redis.xxxxx.cache.amazonaws.com',
    port: 6379
});

// Cache trend data
const cachedData = await client.get('trends-30d');
if (cachedData) {
    return JSON.parse(cachedData);
}

const data = await db.query('SELECT...');
await client.setex('trends-30d', 300, JSON.stringify(data));
```

**Cost**: ~$13/month (cache.t2.micro)

---

**Level 3: CloudFront CDN**
```
User Browser
    ↓
CloudFront Edge Locations (global)
    ↓ (on cache miss)
Frontend EC2 + ALB
```

**Cached Content**:
- Static files (JS, CSS, images)
- HTML pages (short TTL)
- API responses (with appropriate headers)

**Benefits**:
- Reduced latency (edge locations)
- Reduced load on origin
- HTTPS included (free SSL cert)
- DDoS protection

**Cost**: Pay per GB transferred (~$0.085/GB)

---

## Monitoring & Logging

### CloudWatch Metrics

**Automatically Collected**:

**EC2 Metrics**:
- CPUUtilization (%)
- NetworkIn/NetworkOut (bytes)
- DiskReadOps/DiskWriteOps
- StatusCheckFailed
- Frequency: Every 5 minutes (default)

**RDS Metrics**:
- CPUUtilization (%)
- DatabaseConnections (count)
- FreeableMemory (bytes)
- FreeStorageSpace (bytes)
- ReadLatency/WriteLatency (ms)
- ReadThroughput/WriteThroughput (bytes/sec)
- Frequency: Every 1 minute

**Enhanced Monitoring** (RDS):
- OS-level metrics (enable for $3/month)
- Process list
- Memory allocation
- I/O statistics

---

### Custom Metrics

**Application Metrics to Track**:

```javascript
// Backend application
const cloudwatch = new AWS.CloudWatch();

// Track API response times
app.use((req, res, next) => {
    const start = Date.now();
    res.on('finish', () => {
        const duration = Date.now() - start;
        
        cloudwatch.putMetricData({
            Namespace: 'BMI-Tracker',
            MetricData: [{
                MetricName: 'APIResponseTime',
                Value: duration,
                Unit: 'Milliseconds',
                Dimensions: [{
                    Name: 'Endpoint',
                    Value: req.path
                }]
            }]
        });
    });
    next();
});

// Track measurements created
await db.query('INSERT INTO measurements...');
cloudwatch.putMetricData({
    Namespace: 'BMI-Tracker',
    MetricData: [{
        MetricName: 'MeasurementsCreated',
        Value: 1,
        Unit: 'Count'
    }]
});
```

**Business Metrics**:
- Daily active users
- Measurements per day
- Average session duration
- Error rates
- Database query times

---

### Logging Strategy

**1. Application Logs**

**Backend (PM2)**:
```bash
# Logs location
~/backend-ec2/logs/
    ├── out.log    # Standard output
    ├── err.log    # Standard error
    └── (rotated automatically)

# View logs
pm2 logs bmi-backend
pm2 logs --lines 100
pm2 logs --err  # errors only
```

**Log Format**:
```
[2025-12-13 10:30:45] INFO: Server started on port 3000
[2025-12-13 10:31:02] INFO: POST /api/measurements - 201 - 45ms
[2025-12-13 10:31:15] ERROR: Database connection failed: timeout
```

---

**2. Web Server Logs**

**Nginx**:
```bash
# Access log
/var/log/nginx/bmi-frontend-access.log
Format: IP - - [timestamp] "request" status bytes "referer" "user-agent"

Example:
54.123.45.67 - - [13/Dec/2025:10:30:45 +0000] "GET / HTTP/1.1" 200 1234 "-" "Mozilla/5.0..."

# Error log
/var/log/nginx/bmi-frontend-error.log
Format: timestamp [level] process: message

Example:
2025/12/13 10:30:45 [error] 12345: *1 connect() failed (111: Connection refused) while connecting to upstream
```

---

**3. Database Logs**

**RDS PostgreSQL**:
```
Available via AWS Console:
    RDS → bmi-tracker-db → Logs & events

Log Types:
    ├─ error/postgres.log    # Errors and warnings
    ├─ slowquery/postgres.log  # Slow queries (if enabled)
    └─ general/postgres.log   # All queries (if enabled)
```

**Enable Slow Query Log**:
```sql
-- Set threshold to 1 second
ALTER DATABASE bmidb SET log_min_duration_statement = 1000;

-- Slow queries will be logged
```

---

**4. Centralized Logging (CloudWatch Logs)**

**Architecture**:
```
EC2 Instances
    ├─ CloudWatch Agent
    │   ├─ Collects application logs
    │   ├─ Collects system logs
    │   └─ Streams to CloudWatch
    │
    ▼
CloudWatch Log Groups
    ├─ /aws/ec2/frontend/nginx
    ├─ /aws/ec2/backend/pm2
    ├─ /aws/ec2/backend/application
    └─ /aws/rds/bmi-tracker-db/error
```

**Benefits**:
- Centralized log viewing
- Log retention policies
- Log queries and filtering
- Alarms based on log patterns
- Integration with Lambda for processing

**Implementation**:
```bash
# Install CloudWatch Agent
wget https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb
sudo dpkg -i amazon-cloudwatch-agent.deb

# Configure
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-config-wizard

# Start
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
    -a fetch-config \
    -m ec2 \
    -s \
    -c file:/opt/aws/amazon-cloudwatch-agent/etc/config.json
```

**Cost**: ~$0.50/GB ingested + $0.03/GB stored

---

### Alerting

**Recommended CloudWatch Alarms**:

**1. High CPU**
```
Metric: CPUUtilization
Threshold: > 80%
Duration: 5 minutes
Action: SNS email notification
```

**2. Low Storage (RDS)**
```
Metric: FreeStorageSpace
Threshold: < 2 GB
Duration: 5 minutes
Action: SNS email + create RDS snapshot
```

**3. High Database Connections**
```
Metric: DatabaseConnections
Threshold: > 80 (out of max 100)
Duration: 5 minutes
Action: SNS email
```

**4. API Errors**
```
Metric: Custom metric (5xx errors)
Threshold: > 10 in 5 minutes
Action: SNS email + page on-call engineer
```

**5. RDS Replication Lag** (if Multi-AZ)
```
Metric: ReplicaLag
Threshold: > 60 seconds
Action: SNS email
```

**SNS Setup**:
```bash
1. Create SNS Topic: bmi-tracker-alerts
2. Create Subscription: email to admin@example.com
3. Confirm subscription via email
4. Create CloudWatch Alarms pointing to SNS topic
```

---

## Disaster Recovery

### Backup Strategy

**1. RDS Automated Backups**

**Configuration**:
- Retention: 7 days (configurable up to 35 days)
- Backup Window: 03:00-04:00 UTC (low traffic time)
- Snapshots: Daily full backups
- Transaction Logs: Continuous (point-in-time recovery)

**Point-in-Time Recovery**:
```
Can restore to any second within retention period

Example:
Database corrupted at: 2025-12-13 10:30:00
Can restore to:         2025-12-13 10:29:59
```

**Manual Snapshots**:
```bash
# Create before major changes
AWS Console → RDS → bmi-tracker-db → Actions → Take snapshot
Name: bmi-db-before-migration-20251213
```

---

**2. EC2 AMI Snapshots**

**Create AMI (Amazon Machine Image)**:
```bash
AWS Console → EC2 → Instances → Select instance
Actions → Image and templates → Create image

Frontend AMI: bmi-frontend-20251213
Backend AMI: bmi-backend-20251213
```

**Benefits**:
- Capture entire instance state
- Quick recovery (launch from AMI)
- Can copy across regions

**Recommendation**: Create weekly AMIs

---

**3. Application Code Backup**

**Git Repository**:
```bash
# All code should be in Git
git push origin main

# Tag releases
git tag -a v1.0 -m "Production release"
git push origin v1.0
```

**S3 Backup**:
```bash
# Backup deployment files
aws s3 sync backend-ec2/ s3://bmi-backup/backend/ --exclude "node_modules/*"
aws s3 sync frontend-ec2/ s3://bmi-backup/frontend/ --exclude "node_modules/*"
```

---

### Recovery Procedures

**Scenario 1: Frontend EC2 Failure**

**Recovery Steps**:
```bash
1. Launch new EC2 instance from AMI (if available)
   OR
   Launch new instance and run deploy-frontend.sh

2. Associate Elastic IP to new instance

3. Verify website loads

Time: 5-10 minutes
Data Loss: None (RDS unaffected)
```

---

**Scenario 2: Backend EC2 Failure**

**Recovery Steps**:
```bash
1. Launch new EC2 instance from AMI
   OR
   Launch new instance and run deploy-backend.sh

2. Update frontend nginx.conf with new backend IP

3. Restart Nginx on frontend

4. Verify API endpoints work

Time: 10-15 minutes
Data Loss: None (RDS unaffected)
```

---

**Scenario 3: RDS Failure**

**Recovery Steps**:
```bash
If Multi-AZ enabled:
    1. Automatic failover to standby
    2. No action required
    3. Downtime: < 2 minutes

If Single-AZ:
    1. AWS automatically recovers instance
    2. Or restore from latest automated backup
    3. Update DNS/endpoint if changed
    4. Update backend .env if needed
    5. Restart backend application
    
Time: 10-30 minutes
Data Loss: Up to 5 minutes (transaction logs)
```

---

**Scenario 4: Complete Region Failure**

**Recovery Steps** (if multi-region setup):
```bash
1. Update Route 53 to point to secondary region
2. Promote read replica to primary (if exists)
3. Update application configurations
4. Verify all services operational

Time: 15-30 minutes
Data Loss: Depends on replication lag
```

**Prevention**: Implement multi-region architecture

---

### RTO and RPO

**Current Architecture**:

**RTO (Recovery Time Objective)**:
- Frontend failure: 10 minutes
- Backend failure: 15 minutes
- Database failure: 30 minutes
- Complete disaster: 2-4 hours

**RPO (Recovery Point Objective)**:
- RDS: 5 minutes (transaction log backup frequency)
- EC2: Up to 1 week (if using weekly AMIs)
- Application code: 0 (Git repository)

**With Multi-AZ RDS**:
- RTO: < 2 minutes (automatic failover)
- RPO: 0 minutes (synchronous replication)

---

## Summary

This architecture provides:

✅ **Scalability**
- Horizontal scaling capability
- Vertical scaling options
- Database scaling strategies

✅ **Security**
- Defense in depth
- Network isolation
- Encryption at rest and in transit
- Least privilege access

✅ **Reliability**
- Automated backups
- Multi-AZ capability
- Health monitoring
- Disaster recovery plans

✅ **Maintainability**
- Clear separation of concerns
- Automated deployment scripts
- Centralized logging
- Well-documented architecture

✅ **Cost Efficiency**
- Free Tier eligible
- Right-sized resources
- Cost optimization strategies
- Pay-as-you-grow model

---

## Next Steps

**For Production**:
1. Enable Multi-AZ RDS for high availability
2. Add Application Load Balancers
3. Implement Auto Scaling
4. Enable enhanced monitoring
5. Set up CloudWatch alarms
6. Implement CloudFront CDN
7. Add HTTPS with SSL certificate
8. Set up CI/CD pipeline
9. Implement automated testing
10. Create runbooks for common scenarios

**For Further Learning**:
- Container orchestration (ECS, EKS)
- Infrastructure as Code (Terraform, CloudFormation)
- Microservices architecture
- Event-driven architecture
- Serverless computing (Lambda, API Gateway)

---

**Document Version**: 1.0  
**Architecture Version**: 1.0  
**Last Review**: December 13, 2025  
**Next Review**: March 13, 2026
