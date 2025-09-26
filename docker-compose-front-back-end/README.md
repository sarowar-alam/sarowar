
# Nginx + Backend App (Docker Compose)

This project demonstrates how to run a simple **Express.js backend** with an **Nginx reverse proxy** using Docker Compose.

## Project Structure
- `backend/` - Node.js Express backend service
- `nginx.conf` - Nginx configuration file
- `index.nginx-debian.html` - Static frontend served by Nginx
- `docker-compose.yml` - Docker Compose configuration
- `combine.ps1` - PowerShell script to combine file contents

## Backend Service
The backend is a simple Express.js application with two endpoints:
- `/api/health` → Returns service status
- `/api/data` → Returns a sample JSON response with timestamp

### Run Locally (without Docker)
```bash
cd backend
npm install
node server.js
```

## Docker Setup
This project uses Docker Compose with two services:
1. **nginx-app** → Serves static frontend and proxies API calls to backend
2. **backend-app** → Node.js Express backend

### Build & Run
```bash
docker-compose up --build
```

### Access
- Frontend: [http://localhost](http://localhost)
- Backend Health: [http://localhost/api/health](http://localhost/api/health)
- Backend Data: [http://localhost/api/data](http://localhost/api/data)

## Nginx Proxy Setup
The Nginx config (`nginx.conf`) proxies `/api/*` requests to the backend:
```nginx
location /api/ {
    proxy_pass http://backend/api/;
}
```
---
✅ Now you can test the backend API through Nginx by clicking the button in the frontend page!
