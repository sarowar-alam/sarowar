# Project Summary - BMI Health Tracker on AWS RDS

**Quick Reference Guide for the Multi-EC2 AWS RDS Architecture**

---

## Project at a Glance

| Aspect | Details |
|--------|---------|
| **Project Name** | BMI Health Tracker - AWS RDS Multi-EC2 |
| **Architecture** | 3-Tier (Frontend → Backend → AWS RDS) |
| **Cloud Provider** | AWS (Amazon Web Services) |
| **Region** | us-east-1 (configurable) |
| **Deployment Type** | Multi-EC2 with Managed Database |
| **Status** | ✅ Production Ready |
| **Version** | 1.0 |

---

## What This Project Does

The BMI Health Tracker is a **full-stack web application** that helps users:
1. Calculate their **Body Mass Index (BMI)** based on weight and height
2. Calculate their **Basal Metabolic Rate (BMR)** considering age and sex
3. Estimate **daily calorie needs** based on activity level
4. Track health metrics over time
5. Visualize **30-day trends** with interactive charts

**Key Features:**
- ✅ Real-time calculations with instant feedback
- ✅ Historical data storage in AWS RDS PostgreSQL
- ✅ Interactive trend charts showing progress
- ✅ Responsive design (mobile, tablet, desktop)
- ✅ RESTful API architecture
- ✅ Secure, production-ready AWS deployment

---

## Why This Architecture?

### 3-Tier Separation

```
Frontend (React)  →  Backend (Node.js)  →  Database (RDS)
  Public Subnet       Private Subnet        Private Subnet
```

**Benefits:**
- **Security**: Database not exposed to internet
- **Scalability**: Each tier can scale independently
- **Maintainability**: Clear separation of concerns
- **Reliability**: Managed RDS with automated backups
- **Cost-Effective**: Free Tier eligible

### Why AWS RDS Instead of Self-Hosted PostgreSQL?

| Feature | Self-Hosted EC2 | AWS RDS |
|---------|----------------|---------|
| Setup Time | 30+ minutes | 10 minutes |
| Backups | Manual setup | Automated |
| Patching | Manual | Automated |
| High Availability | Complex setup | Multi-AZ option |
| Monitoring | CloudWatch + custom | Built-in enhanced monitoring |
| Encryption | Manual SSL config | Automatic at rest + in transit |
| Failover | Manual | Automatic (<2 min) |
| Cost (Free Tier) | $0 | $0 (750 hrs/month) |
| Maintenance Burden | High | Low |

**Decision: RDS** → Less operational overhead, production-grade features out-of-the-box

---

## Technology Stack

### Frontend Layer
```
React 18.2          → UI framework with hooks
Vite 5.0            → Fast build tool and dev server
Chart.js 4.4        → Data visualization library
Axios               → HTTP client for API calls
Nginx               → Production web server
CSS3                → Custom styling (no framework)
```

**Why These Choices:**
- **React**: Popular, component-based, excellent ecosystem
- **Vite**: Fast development experience, optimized builds
- **Chart.js**: Easy to use, responsive charts
- **Nginx**: Industry-standard, high-performance web server

### Backend Layer
```
Node.js 18 LTS      → JavaScript runtime
Express 4.18        → Minimalist web framework
pg 8.10             → PostgreSQL client with SSL support
PM2                 → Process manager for production
CORS                → Cross-origin resource sharing
```

**Why These Choices:**
- **Node.js 18**: LTS release, stable, excellent performance
- **Express**: Mature, flexible, large ecosystem
- **pg**: Official PostgreSQL driver, connection pooling
- **PM2**: Auto-restart, monitoring, zero-downtime reloads

### Database Layer
```
AWS RDS PostgreSQL 14/15  → Managed relational database
db.t3.micro               → 2 vCPU, 1 GB RAM (Free Tier)
20 GB Storage             → SSD, expandable to 64 TB
SSL/TLS Encryption        → Secure connections
Automated Backups         → 7-day retention
```

**Why These Choices:**
- **PostgreSQL**: Robust, ACID compliant, excellent for structured data
- **AWS RDS**: Managed service, reduces operational burden
- **db.t3.micro**: Adequate for small-medium workloads, Free Tier eligible

---

## AWS Infrastructure

### Networking Components
```
1x VPC (10.0.0.0/16)
├─ 1x Internet Gateway
├─ 1x NAT Gateway ($32/month)
├─ 3x Subnets
│  ├─ Public Subnet (10.0.1.0/24)        → Frontend EC2
│  ├─ Private Subnet 1 (10.0.2.0/24)     → Backend EC2
│  └─ Private Subnet 2 (10.0.3.0/24)     → RDS (Multi-AZ requires 2)
├─ 2x Route Tables
│  ├─ Public RT: 0.0.0.0/0 → IGW
│  └─ Private RT: 0.0.0.0/0 → NAT Gateway
└─ 3x Security Groups
   ├─ Frontend SG: HTTP(80) ← Internet, SSH(22) ← Your IP
   ├─ Backend SG: API(3000) ← Frontend SG, SSH(22) ← Your IP
   └─ RDS SG: PostgreSQL(5432) ← Backend SG only
```

### Compute & Database
```
Frontend EC2:
  - Type: t2.micro (1 vCPU, 1 GB RAM)
  - OS: Ubuntu 22.04 LTS
  - Public IP: Elastic IP attached
  - Software: Node.js 18, Nginx, React build

Backend EC2:
  - Type: t2.micro (1 vCPU, 1 GB RAM)
  - OS: Ubuntu 22.04 LTS
  - Private IP: 10.0.2.20
  - Software: Node.js 18, PM2, Express

RDS Instance:
  - Class: db.t3.micro (2 vCPU, 1 GB RAM)
  - Engine: PostgreSQL 14 or 15
  - Storage: 20 GB SSD (General Purpose)
  - Endpoint: bmi-tracker-db.xxxxx.us-east-1.rds.amazonaws.com
  - Port: 5432
```

---

## Security Architecture

### Network Security (Defense in Depth)

**Layer 1 - Public Internet:**
- ✅ Only Frontend EC2 exposed
- ✅ HTTPS recommended (can add SSL certificate)
- ✅ Security group restricts to port 80

**Layer 2 - Private Backend:**
- ✅ No public IP address
- ✅ Only accessible from Frontend security group
- ✅ Internet access via NAT Gateway (for updates only)

**Layer 3 - Private Database:**
- ✅ No public endpoint
- ✅ Only accessible from Backend security group
- ✅ SSL/TLS encryption enforced
- ✅ Private subnet with no internet route

### Application Security

**Input Validation:**
- Frontend: React form validation
- Backend: Express middleware validation
- Database: Type constraints and NOT NULL checks

**SQL Injection Protection:**
- Parameterized queries using pg library
- No string concatenation for SQL

**Connection Security:**
- RDS SSL certificates
- Environment-based credentials
- No hardcoded passwords

**Data Security:**
- Encryption at rest (RDS KMS)
- Encryption in transit (SSL/TLS)
- Automated backups (7-day retention)

---

## Cost Analysis

### Monthly Costs

**Year 1 (Free Tier):**
```
Frontend EC2 (t2.micro)        $0.00  ← 750 hrs/month free
Backend EC2 (t2.micro)         $0.00  ← 750 hrs/month free
RDS db.t3.micro                $0.00  ← 750 hrs/month free
RDS Storage (20 GB)            $0.00  ← 20 GB free
NAT Gateway                    $32.85 ← NOT covered by Free Tier
Data Transfer (5 GB out)       $0.00  ← 15 GB/month free
EBS Storage (16 GB)            $0.00  ← 30 GB free
──────────────────────────────────────
TOTAL YEAR 1:                  ~$32/month or $384/year
```

**Year 2+ (Post Free Tier):**
```
Frontend EC2 (t2.micro)        $8.47/month
Backend EC2 (t2.micro)         $8.47/month
RDS db.t3.micro                $25.00/month
RDS Storage (20 GB)            $2.30/month
NAT Gateway                    $32.85/month
Data Transfer (5 GB)           $0.45/month
EBS Storage (16 GB)            $1.60/month
──────────────────────────────────────
TOTAL POST FREE TIER:          ~$83/month or $996/year
```

### Cost Optimization Strategies

**Immediate Savings:**
- Stop NAT Gateway when not developing → Save $33/month
- Use AWS Systems Manager Session Manager instead of SSH → No need for NAT

**Long-Term Savings:**
- Reserved Instances (1-year) → Save 30-40%
- Reserved Instances (3-year) → Save 50-60%
- RDS Reserved Instances → Save ~35%

**Development Savings:**
- Stop instances when not in use → Pay only for running hours
- Use smaller instances for dev/test
- Single-AZ RDS for non-production

**Example Cost Scenarios:**
```
Scenario 1: Production 24/7              ~$83/month
Scenario 2: Dev (8 hrs/day, 5 days/wk)  ~$30/month
Scenario 3: Reserved Instances (1-year)  ~$60/month
Scenario 4: No NAT + Reserved + Stop     ~$35/month
```

---

## Deployment Process

### Time Estimates

| Phase | Duration | Details |
|-------|----------|---------|
| **Infrastructure** | 15-20 min | VPC, subnets, gateways, security groups |
| **RDS Setup** | 10-15 min | DB creation + wait for "Available" |
| **Backend** | 10-15 min | EC2 launch, deployment script, migration |
| **Frontend** | 10-15 min | EC2 launch, build, Nginx configuration |
| **Testing** | 5 min | End-to-end validation |
| **TOTAL** | **45-60 min** | Complete deployment from scratch |

### Deployment Steps (High-Level)

```
1. Prerequisites
   - AWS account
   - AWS CLI configured (optional)
   - SSH key pair
   - 45-60 minutes

2. AWS Infrastructure
   - Create VPC
   - Create 3 subnets
   - Create Internet Gateway + NAT Gateway
   - Configure route tables
   - Create 3 security groups
   ✓ Validation checkpoint

3. RDS Database
   - Create DB subnet group
   - Launch RDS PostgreSQL instance
   - Wait for "Available" status
   ✓ Validation checkpoint

4. Backend EC2
   - Launch instance in private subnet
   - Upload backend files
   - Run deploy-backend.sh
   - Connect to RDS and run migration
   ✓ Validation checkpoint

5. Frontend EC2
   - Launch instance in public subnet
   - Allocate Elastic IP
   - Upload frontend files
   - Run deploy-frontend.sh
   ✓ Validation checkpoint

6. Final Testing
   - Access via browser
   - Submit test measurement
   - Verify data in RDS
   - Check trend chart
   ✓ All features working
```

**Automated Components:**
- `deploy-backend.sh`: Installs Node.js, npm packages, PM2, connects to RDS
- `deploy-frontend.sh`: Installs Node.js, builds React app, configures Nginx

---

## Key Design Decisions

### 1. Why Multi-EC2 Instead of Single EC2?
**Decision:** Separate Frontend and Backend EC2 instances

**Reasoning:**
- Better security (backend in private subnet)
- Independent scaling
- Easier maintenance and debugging
- Real-world production pattern
- Follows AWS Well-Architected Framework

### 2. Why AWS RDS Instead of EC2-Hosted PostgreSQL?
**Decision:** Use managed AWS RDS service

**Reasoning:**
- Automated backups and point-in-time recovery
- Automated patching and maintenance
- Built-in monitoring and alerting
- Easy Multi-AZ high availability
- Reduces operational burden
- Same cost during Free Tier period

### 3. Why NAT Gateway Instead of NAT Instance?
**Decision:** Use managed NAT Gateway

**Reasoning:**
- Highly available (AWS-managed)
- Auto-scaling bandwidth
- No maintenance required
- Better security updates
- Worth $33/month for production

**Alternative:** For dev/test, can use AWS Systems Manager Session Manager instead

### 4. Why PM2 Instead of systemd?
**Decision:** Use PM2 for Node.js process management

**Reasoning:**
- Built for Node.js applications
- Zero-downtime reloads
- Built-in monitoring
- Easy log management
- Cluster mode support
- Better developer experience

### 5. Why Vite Instead of Create React App?
**Decision:** Use Vite for frontend build tool

**Reasoning:**
- Much faster dev server (HMR in <50ms)
- Faster production builds
- Modern ESM-based approach
- Smaller bundle sizes
- Better developer experience
- Actively maintained (CRA deprecated)

---

## API Reference

### Endpoints

**1. Health Check**
```http
GET /health

Response: 200 OK
{
  "status": "ok",
  "environment": "production",
  "database": "AWS RDS PostgreSQL",
  "timestamp": "2025-12-13T12:00:00.000Z"
}
```

**2. Create Measurement**
```http
POST /api/measurements
Content-Type: application/json

Body:
{
  "weightKg": 70,
  "heightCm": 175,
  "age": 30,
  "sex": "male",
  "activity": "moderate"
}

Response: 201 Created
{
  "measurement": {
    "id": 1,
    "weight_kg": 70,
    "height_cm": 175,
    "age": 30,
    "sex": "male",
    "activity": "moderate",
    "bmi": 22.86,
    "bmi_category": "Normal",
    "bmr": 1705,
    "daily_calories": 2643,
    "created_at": "2025-12-13T12:00:00.000Z"
  }
}
```

**3. Get All Measurements**
```http
GET /api/measurements

Response: 200 OK
{
  "rows": [
    {
      "id": 1,
      "weight_kg": 70,
      "bmi": 22.86,
      "created_at": "2025-12-13T12:00:00.000Z"
    },
    ...
  ]
}
```

**4. Get 30-Day Trends**
```http
GET /api/measurements/trends

Response: 200 OK
{
  "rows": [
    {
      "day": "2025-12-01",
      "avg_bmi": 22.5,
      "count": 3
    },
    ...
  ]
}
```

---

## Database Schema

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

**Data Types:**
- `DECIMAL(5,2)`: Max 999.99 (sufficient for kg/cm)
- `INTEGER`: Whole numbers for age, calories
- `VARCHAR`: Variable length strings
- `TIMESTAMP`: Date and time with timezone

**Constraints:**
- Primary key on `id`
- NOT NULL for required fields
- CHECK constraints for data validation
- Index on `created_at` for trend queries

---

## Monitoring & Observability

### Built-in AWS Monitoring

**EC2 CloudWatch Metrics (Automatic):**
- CPU Utilization
- Network In/Out
- Disk Read/Write
- Status Checks

**RDS CloudWatch Metrics (Automatic):**
- CPU Utilization
- Database Connections
- Free Storage Space
- Read/Write IOPS
- Replica Lag (if Multi-AZ)

### Application Monitoring

**PM2 Monitoring (Backend):**
```bash
pm2 status                  # Process status
pm2 monit                   # Real-time monitoring
pm2 logs                    # Application logs
pm2 show bmi-backend        # Detailed info
```

**Nginx Logs (Frontend):**
```bash
sudo tail -f /var/log/nginx/bmi-frontend-access.log
sudo tail -f /var/log/nginx/bmi-frontend-error.log
```

**RDS Logs:**
- Available in AWS Console
- PostgreSQL error logs
- Slow query logs
- General logs

### Recommended Alarms

**Set up CloudWatch Alarms for:**
1. EC2 CPU > 80% for 5 minutes
2. RDS CPU > 80% for 5 minutes
3. RDS Free Storage < 2 GB
4. RDS Database Connections > 80
5. Frontend EC2 Status Check Failed

---

## Troubleshooting Quick Reference

| Problem | Solution |
|---------|----------|
| **Cannot access frontend** | Check security group port 80, verify Elastic IP, check Nginx status |
| **API requests fail** | Check backend PM2 status, verify nginx.conf backend IP, check security group |
| **Database connection error** | Verify RDS is "Available", check security group, test with psql |
| **Backend can't install npm packages** | Check NAT Gateway status, verify route table, test internet: `ping google.com` |
| **Nginx 502 Bad Gateway** | Backend not running or wrong IP in nginx.conf |
| **No data in chart** | Check browser console, verify API endpoints, check database has data |

**Detailed troubleshooting:** See [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) section 8

---

## File Structure Reference

```
multiple-ec2-RDS-3tier-webapp/
│
├── frontend-ec2/                    # Deploy to Frontend EC2
│   ├── package.json                 # Dependencies
│   ├── vite.config.js               # Build configuration
│   ├── index.html                   # HTML entry point
│   ├── nginx.conf                   # Web server config
│   ├── deploy-frontend.sh           # Automated deployment
│   ├── .env.example                 # Environment template
│   └── src/
│       ├── main.jsx                 # React entry point
│       ├── App.jsx                  # Main component (~120 lines)
│       ├── api.js                   # Axios configuration
│       ├── index.css                # Styling (~350 lines)
│       └── components/
│           ├── MeasurementForm.jsx  # Input form (~90 lines)
│           └── TrendChart.jsx       # Chart.js component (~60 lines)
│
├── backend-ec2/                     # Deploy to Backend EC2
│   ├── package.json                 # Dependencies
│   ├── ecosystem.config.js          # PM2 configuration
│   ├── deploy-backend.sh            # Automated deployment
│   ├── .env.example                 # RDS connection template
│   ├── 001_create_measurements_rds.sql  # Database migration
│   └── src/
│       ├── server.js                # Express server entry (~50 lines)
│       ├── routes.js                # API endpoints (~100 lines)
│       ├── db.js                    # RDS connection with SSL (~40 lines)
│       └── calculations.js          # BMI/BMR formulas (~50 lines)
│
├── AGENT.md                         # Complete reconstruction guide (500+ lines)
├── DEPLOYMENT_GUIDE.md              # Step-by-step AWS setup (1000+ lines)
├── README.md                        # Quick start and overview
├── ARCHITECTURE_DIAGRAM.md          # Visual architecture diagrams
└── PROJECT_SUMMARY.md               # This file - quick reference
```

**Total Lines of Code:** ~2,000+ lines (application code + documentation)

---

## Next Steps After Deployment

### Immediate (Day 1)
- [ ] Verify all validation checkpoints passed
- [ ] Test from multiple devices/browsers
- [ ] Submit test measurements
- [ ] Check CloudWatch metrics

### Short Term (Week 1)
- [ ] Set up CloudWatch alarms
- [ ] Configure RDS backup window (off-peak)
- [ ] Document any custom configurations
- [ ] Plan SSL certificate (HTTPS)

### Medium Term (Month 1)
- [ ] Monitor costs and usage
- [ ] Consider Reserved Instances if running 24/7
- [ ] Implement additional features
- [ ] Set up CI/CD pipeline

### Long Term (Quarter 1)
- [ ] Evaluate Multi-AZ RDS for production
- [ ] Consider Auto Scaling Groups
- [ ] Implement user authentication
- [ ] Plan for horizontal scaling

---

## Learning Outcomes

By completing this project, you will have learned:

**AWS Services:**
- ✅ VPC networking (subnets, route tables, gateways)
- ✅ EC2 instance management
- ✅ AWS RDS managed databases
- ✅ Security groups and network security
- ✅ Elastic IPs and NAT Gateways

**DevOps Practices:**
- ✅ Infrastructure as Code principles
- ✅ Deployment automation with shell scripts
- ✅ Process management with PM2
- ✅ Nginx reverse proxy configuration
- ✅ SSL/TLS certificate management

**Full-Stack Development:**
- ✅ React frontend with Vite
- ✅ Express.js REST API
- ✅ PostgreSQL database design
- ✅ API design and documentation
- ✅ Error handling and validation

**Production Best Practices:**
- ✅ Security-first architecture
- ✅ Environment-based configuration
- ✅ Logging and monitoring
- ✅ Backup and disaster recovery
- ✅ Cost optimization

---

## Resources

### Documentation
- [AGENT.md](AGENT.md) - Complete file contents and reconstruction guide
- [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) - Step-by-step deployment with validations
- [ARCHITECTURE_DIAGRAM.md](ARCHITECTURE_DIAGRAM.md) - Visual diagrams
- [README.md](README.md) - Quick start guide

### External Resources
- [AWS Free Tier](https://aws.amazon.com/free/)
- [AWS RDS User Guide](https://docs.aws.amazon.com/rds/)
- [Express.js Documentation](https://expressjs.com/)
- [React Documentation](https://react.dev/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

---

## Support & Contributions

### Getting Help
1. Check [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) Troubleshooting section
2. Verify all validation checkpoints passed
3. Check AWS Console for resource status
4. Review application logs (PM2 and Nginx)

### Future Enhancements
- User authentication (JWT)
- Email notifications
- Export data functionality
- Mobile app (React Native)
- CI/CD pipeline (GitHub Actions)
- Containerization (Docker/ECS)

---

## Conclusion

This project demonstrates a **production-ready, scalable, secure 3-tier web application** deployed on AWS using:
- **Frontend EC2** with React and Nginx
- **Backend EC2** with Node.js and Express
- **AWS RDS PostgreSQL** managed database

The architecture follows AWS best practices for:
- Network security (public/private subnets)
- Defense in depth (multiple security layers)
- High availability (can enable Multi-AZ)
- Cost optimization (Free Tier eligible)
- Operational excellence (automated backups, monitoring)

**Total Deployment Time:** 45-60 minutes  
**Monthly Cost:** ~$32 (Year 1) → ~$83 (Year 2+)  
**Scalability:** Can handle 100s of concurrent users with current setup  
**Status:** ✅ Production Ready

---

**Version:** 1.0  
**Last Updated:** December 13, 2025  
**Maintainer:** Your Name  
**License:** MIT
