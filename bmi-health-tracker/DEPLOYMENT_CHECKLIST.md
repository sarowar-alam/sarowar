# Pre-Deployment Checklist

## ✅ Before Deploying to AWS EC2 Ubuntu

### 1. AWS EC2 Setup
- [ ] EC2 instance launched (Ubuntu 22.04 LTS)
- [ ] Security group configured:
  - [ ] SSH (22) - Your IP
  - [ ] HTTP (80) - 0.0.0.0/0
  - [ ] HTTPS (443) - 0.0.0.0/0
- [ ] Key pair downloaded and secured (chmod 400)
- [ ] Elastic IP assigned (optional but recommended)
- [ ] SSH connection tested

### 2. Server Prerequisites
- [ ] Server updated: `sudo apt update && sudo apt upgrade -y`
- [ ] Essential tools installed: git, curl, wget, build-essential
- [ ] Node.js installed (v18+ via NVM)
- [ ] PostgreSQL installed and running
- [ ] Nginx installed
- [ ] UFW firewall configured

### 3. Database Configuration
- [ ] PostgreSQL user created (`bmi_user`)
- [ ] Database created (`bmidb`)
- [ ] Strong password set (16+ characters)
- [ ] Local connection tested
- [ ] Migration SQL verified

### 4. Backend Configuration
- [ ] `.env` file created from `.env.example`
- [ ] `DATABASE_URL` configured correctly
- [ ] `PORT` set to 3000
- [ ] `NODE_ENV` set to production
- [ ] `FRONTEND_URL` configured (if needed for CORS)
- [ ] Dependencies will install without errors

### 5. Frontend Configuration
- [ ] `vite.config.js` present with proxy config
- [ ] All dependencies listed in `package.json`
- [ ] Build process tested locally
- [ ] No hardcoded API URLs

### 6. File Permissions
- [ ] `.env` file secured (chmod 600)
- [ ] Shell scripts executable (chmod +x *.sh)
- [ ] Application owned by correct user

### 7. Nginx Configuration
- [ ] Nginx config file prepared
- [ ] Server name set (domain or IP)
- [ ] Proxy pass to backend:3000 configured
- [ ] Static file serving configured
- [ ] SSL certificate obtained (optional)

### 8. PM2 Setup
- [ ] PM2 installed globally
- [ ] ecosystem.config.js configured
- [ ] Logs directory created
- [ ] Startup script configured

### 9. Security Checklist
- [ ] Firewall (UFW) enabled
- [ ] Only necessary ports open
- [ ] SSH secured (disable password auth, use keys)
- [ ] Database not accessible from outside
- [ ] `.env` file not in git
- [ ] Strong passwords used everywhere
- [ ] CORS properly configured

### 10. Testing Before Production
- [ ] Local build successful
- [ ] Backend starts without errors
- [ ] Database connection works
- [ ] API endpoints respond correctly
- [ ] Frontend builds without errors
- [ ] No console errors in browser

## 📋 Deployment Steps

1. **Upload Code**
   ```bash
   git clone <repo> OR scp files to server
   ```

2. **Setup Database**
   ```bash
   ./setup-database.sh
   ```

3. **Deploy Application**
   ```bash
   ./deploy.sh
   ```

4. **Configure Nginx**
   ```bash
   sudo ln -s /etc/nginx/sites-available/bmi-health-tracker /etc/nginx/sites-enabled/
   sudo nginx -t
   sudo systemctl reload nginx
   ```

5. **Verify Deployment**
   - [ ] Backend health check: `curl http://localhost:3000/health`
   - [ ] Frontend accessible via browser
   - [ ] Can create measurement
   - [ ] Measurements display correctly
   - [ ] Trend chart loads
   - [ ] PM2 shows app running: `pm2 status`
   - [ ] Nginx logs show no errors

## 🔍 Post-Deployment Verification

### Backend Tests
```bash
# Health check
curl http://localhost:3000/health

# Get measurements
curl http://YOUR_IP/api/measurements

# Check PM2 status
pm2 status
pm2 logs bmi-backend --lines 50
```

### Frontend Tests
- [ ] Open http://YOUR_IP in browser
- [ ] Check browser console (F12) for errors
- [ ] Submit a test measurement
- [ ] Verify data saves and displays
- [ ] Check trend chart renders

### Database Tests
```bash
# Connect to database
psql -U bmi_user -d bmidb -h localhost

# Check table exists
\dt

# Check data
SELECT COUNT(*) FROM measurements;

# Exit
\q
```

### Log Monitoring
```bash
# Backend logs
pm2 logs bmi-backend

# Nginx access logs
sudo tail -f /var/log/nginx/bmi-access.log

# Nginx error logs
sudo tail -f /var/log/nginx/bmi-error.log

# PostgreSQL logs
sudo tail -f /var/log/postgresql/postgresql-*-main.log
```

## 🚨 Rollback Plan

If deployment fails:

1. **Stop PM2 process**
   ```bash
   pm2 stop bmi-backend
   ```

2. **Restore previous version**
   ```bash
   git checkout <previous-commit>
   npm install
   pm2 restart bmi-backend
   ```

3. **Database rollback** (if needed)
   ```bash
   # Restore from backup
   psql -U bmi_user -d bmidb -h localhost < backup.sql
   ```

## 📝 Notes

- Always test in development before deploying
- Keep backups of database before major changes
- Monitor logs after deployment
- Document any custom configurations
- Keep credentials secure and never commit to git

## ✅ Sign-Off

Deployment Date: _______________
Deployed By: _______________
Version: _______________
Status: [ ] Success [ ] Failed [ ] Partial

Notes:
_________________________________
_________________________________
_________________________________
