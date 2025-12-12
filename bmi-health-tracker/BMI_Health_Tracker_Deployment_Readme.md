
# BMI & Health Tracker — Deployment Guide (Ubuntu + Nginx, No Docker)

This guide explains how to deploy the **BMI & Health Tracker** app (React frontend + Node.js backend + PostgreSQL database) on a **fresh Ubuntu EC2 server** using **nginx** as reverse proxy and static hosting.

---

# 1. Update Server & Install Dependencies

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y git build-essential nginx ufw unzip
```

---

# 2. Install Node.js (Using NVM)

```bash
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.5/install.sh | bash
source ~/.bashrc
nvm install --lts
node -v
npm -v
```

---

# 3. Install PostgreSQL

```bash
sudo apt install -y postgresql postgresql-contrib
sudo systemctl enable --now postgresql
```

### Create DB & User

```bash
sudo -u postgres createuser --pwprompt bmi_user
sudo -u postgres createdb -O bmi_user bmidb
```

---

# 4. Upload & Unzip the Project

Example using SCP:

```bash
scp bmi-health-tracker.zip ubuntu@YOUR_SERVER_IP:/home/ubuntu/
```

Unzip:

```bash
unzip bmi-health-tracker.zip -d bmi-health-tracker
cd bmi-health-tracker
```

---

# 5. Configure Backend

```bash
cd backend
cp .env.example .env
nano .env
```

Set:

```
PORT=3000
DATABASE_URL=postgresql://bmi_user:YOURPASSWORD@localhost:5432/bmidb
```

Install backend dependencies:

```bash
npm install
```

Run DB migration:

```bash
sudo -u postgres psql -d bmidb -f migrations/001_create_measurements.sql
```

---

# 6. Run Backend with PM2

```bash
npm install -g pm2
pm2 start src/server.js --name bmi-backend
pm2 save
pm2 startup
```

Run the command PM2 prints to enable auto-start.

---

# 7. Build the Frontend

```bash
cd ../frontend
npm install
npm run build
```

Copy build output to nginx directory:

```bash
sudo mkdir -p /var/www/bmi-frontend
sudo cp -r dist/* /var/www/bmi-frontend/
sudo chown -R www-data:www-data /var/www/bmi-frontend
```

---

# 8. Configure nginx

Create nginx site file:

```bash
sudo nano /etc/nginx/sites-available/bmi
```

Paste:

```
server {
    listen 80;
    server_name YOUR_DOMAIN_OR_IP;

    root /var/www/bmi-frontend;
    index index.html;

    location / {
        try_files $uri /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:3000/api/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
    }
}
```

Enable it:

```bash
sudo ln -s /etc/nginx/sites-available/bmi /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

---

# 9. Enable HTTPS with Certbot (Optional)

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d YOUR_DOMAIN
```

---

# 10. Enable Firewall

```bash
sudo ufw allow OpenSSH
sudo ufw allow 'Nginx Full'
sudo ufw enable
```

---

# 11. Logs & Monitoring

PM2 logs:

```
pm2 logs bmi-backend
```

nginx logs:

```
/var/log/nginx/access.log
/var/log/nginx/error.log
```

---

# 12. Expected Server Structure

```
/var/www/bmi-frontend          # Built React frontend
/home/ubuntu/bmi-health-tracker/backend   # Node backend
```

---

# Deployment Complete 🎉

Your app is now live!  
If you need auto-deployment, HTTPS redirect, or systemd instead of PM2, just ask.
