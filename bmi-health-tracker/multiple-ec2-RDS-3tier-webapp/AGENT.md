# BMI Health Tracker - AWS RDS Multi-EC2 3-Tier Architecture

**⚠️ IMPORTANT: This file contains EVERYTHING needed to recreate the entire project from scratch with AWS RDS!**

---

## Overview

A full-stack 3-tier web application for tracking Body Mass Index (BMI), Basal Metabolic Rate (BMR), and daily calorie requirements. The application features trend visualization and stores historical measurement data in AWS RDS PostgreSQL.

**Deployment Target**: Multiple AWS EC2 instances with AWS RDS PostgreSQL Database

---

## Architecture

### 3-Tier AWS Cloud Structure

```
┌─────────────────────────────────────────────────────────────┐
│                        AWS Cloud (VPC)                       │
│                                                              │
│  ┌──────────────────┐  Public Subnet (10.0.1.0/24)         │
│  │                  │  ┌────────────────────────────────┐   │
│  │  Internet Users  ├─►│  Frontend EC2 (Ubuntu 22.04)   │   │
│  │                  │  │  - Nginx Web Server             │   │
│  └──────────────────┘  │  - React App (Production Build) │   │
│           │            │  - Elastic IP                   │   │
│           │            └─────────────┬──────────────────┘   │
│           │                          │                       │
│  ┌────────▼──────────┐  Private Subnet (10.0.2.0/24)       │
│  │  Internet Gateway │               │                       │
│  └───────────────────┘  ┌────────────▼──────────────────┐   │
│                         │  Backend EC2 (Ubuntu 22.04)    │   │
│                         │  - Node.js + Express API       │   │
│                         │  - PM2 Process Manager         │   │
│                         │  - Private IP only             │   │
│                         └─────────────┬──────────────────┘   │
│                                       │                       │
│                          ┌────────────▼──────────────────┐   │
│                          │  AWS RDS PostgreSQL           │   │
│                          │  - Managed Database Service   │   │
│                          │  - Automated Backups          │   │
│                          │  - Multi-AZ Optional          │   │
│                          │  - Private Subnet             │   │
│                          └───────────────────────────────┘   │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### Data Flow

```
Browser → Frontend EC2 (Nginx:80) → Backend EC2 (Express:3000) → AWS RDS PostgreSQL (5432)
   ↑                                                                         │
   └─────────────────────── Response Path ──────────────────────────────────┘
```

### Key Advantages of RDS

1. **Managed Service**: AWS handles backups, patching, scaling
2. **High Availability**: Multi-AZ deployment option for automatic failover
3. **Automated Backups**: Point-in-time recovery
4. **Security**: Encryption at rest and in transit, IAM authentication
5. **Monitoring**: CloudWatch integration for performance metrics
6. **Scalability**: Easy to scale compute and storage independently

---

## Complete Directory Structure

```
multiple-ec2-RDS-3tier-webapp/
├── AGENT.md (this file)
├── DEPLOYMENT_GUIDE.md
├── README.md
├── ARCHITECTURE_DIAGRAM.md
├── PROJECT_SUMMARY.md
│
├── frontend-ec2/
│   ├── src/
│   │   ├── components/
│   │   │   ├── MeasurementForm.jsx
│   │   │   └── TrendChart.jsx
│   │   ├── App.jsx
│   │   ├── main.jsx
│   │   ├── api.js
│   │   └── index.css
│   ├── index.html
│   ├── vite.config.js
│   ├── package.json
│   ├── .env.example
│   ├── .gitignore
│   ├── nginx.conf
│   └── deploy-frontend.sh
│
└── backend-ec2/
    ├── src/
    │   ├── server.js
    │   ├── routes.js
    │   ├── db.js (configured for AWS RDS with SSL)
    │   └── calculations.js
    ├── package.json
    ├── .env.example (RDS endpoint configuration)
    ├── .gitignore
    ├── ecosystem.config.js
    ├── deploy-backend.sh
    └── 001_create_measurements_rds.sql
```

---

## Tech Stack

### Frontend EC2
- **Runtime**: Node.js 18 LTS (via NVM)
- **Framework**: React 18.2
- **Build Tool**: Vite 5.0
- **HTTP Client**: Axios 1.4
- **Charts**: Chart.js 4.4 + react-chartjs-2 5.2
- **Web Server**: Nginx (reverse proxy)
- **Subnet**: Public (with Elastic IP)

### Backend EC2
- **Runtime**: Node.js 18 LTS (via NVM)
- **Framework**: Express.js 4.18
- **Database Client**: pg 8.10 (PostgreSQL driver with SSL for RDS)
- **Process Manager**: PM2
- **Dependencies**: CORS 2.8, dotenv 16.0, body-parser 1.20
- **Subnet**: Private (no direct internet access)

### AWS RDS Database
- **Engine**: PostgreSQL 14+ or 15+
- **Instance Class**: db.t3.micro (Free Tier) or db.t3.small
- **Storage**: 20 GB GP2 SSD (expandable)
- **Backup**: Automated daily backups (7-day retention)
- **Encryption**: At rest and in transit (SSL/TLS)
- **Subnet**: Private RDS subnet group

---

## Complete File Contents

### Frontend EC2 Files

#### frontend-ec2/package.json
```json
{
  "name": "bmi-health-frontend",
  "version": "1.0.0",
  "private": true,
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview --port 5173"
  },
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "axios": "^1.4.0",
    "chart.js": "^4.4.0",
    "react-chartjs-2": "^5.2.0"
  },
  "devDependencies": {
    "vite": "^5.0.0",
    "@vitejs/plugin-react": "^4.2.0"
  }
}
```

#### frontend-ec2/vite.config.js
```javascript
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    host: '0.0.0.0',
    proxy: {
      '/api': {
        target: process.env.VITE_BACKEND_URL || 'http://localhost:3000',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    sourcemap: false,
    minify: 'terser'
  }
});
```

#### frontend-ec2/index.html
```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <meta name="description" content="BMI and Health Tracker - Track your Body Mass Index, BMR, and daily calorie needs" />
  <title>BMI & Health Tracker</title>
</head>
<body>
  <div id="root"></div>
  <script type="module" src="/src/main.jsx"></script>
</body>
</html>
```

#### frontend-ec2/.env.example
```env
# Backend API URL (Private IP of Backend EC2)
VITE_BACKEND_URL=http://10.0.2.20:3000

# Environment
VITE_NODE_ENV=production
```

#### frontend-ec2/nginx.conf
```nginx
server {
    listen 80;
    server_name _;

    root /var/www/bmi-health-tracker;
    index index.html;

    # Gzip compression
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css text/xml text/javascript application/x-javascript application/xml+rss application/json application/javascript;

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # Frontend static files
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Proxy API requests to backend EC2 (Update with your Backend EC2 Private IP)
    location /api/ {
        proxy_pass http://BACKEND_PRIVATE_IP:3000/api/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Health check endpoint
    location /health {
        proxy_pass http://BACKEND_PRIVATE_IP:3000/health;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
    }

    # Cache static assets
    location ~* \.(jpg|jpeg|png|gif|ico|css|js|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # Deny access to hidden files
    location ~ /\. {
        deny all;
        access_log off;
        log_not_found off;
    }
}
```

#### frontend-ec2/src/main.jsx
```javascript
import React from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import './index.css';

createRoot(document.getElementById('root')).render(<App />);
```

#### frontend-ec2/src/api.js
```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  timeout: 10000, // 10 second timeout
  headers: {
    'Content-Type': 'application/json'
  }
});

// Request interceptor
api.interceptors.request.use(
  config => {
    return config;
  },
  error => {
    console.error('Request error:', error);
    return Promise.reject(error);
  }
);

// Response interceptor for error handling
api.interceptors.response.use(
  response => response,
  error => {
    if (error.response) {
      // Server responded with error status
      console.error('API Error:', error.response.status, error.response.data);
    } else if (error.request) {
      // Request made but no response
      console.error('Network Error: No response from server');
    } else {
      // Something else happened
      console.error('Error:', error.message);
    }
    return Promise.reject(error);
  }
);

export default api;
```

#### frontend-ec2/src/App.jsx
```javascript
import React, { useEffect, useState } from 'react';
import MeasurementForm from './components/MeasurementForm';
import TrendChart from './components/TrendChart';
import api from './api';

export default function App() {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const r = await api.get('/measurements');
      setRows(r.data.rows);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to load measurements');
    } finally {
      setLoading(false);
    }
  };
  
  useEffect(() => { load() }, []);
  
  // Calculate stats
  const latestMeasurement = rows[0];
  const totalMeasurements = rows.length;
  
  return (
    <>
      <header className="app-header">
        <h1>BMI & Health Tracker</h1>
        <p className="app-subtitle">Track your health metrics and reach your fitness goals</p>
      </header>

      <div className="container">
        {/* Add Measurement Card */}
        <div className="card">
          <div className="card-header">
            <h2>📝 Add New Measurement</h2>
          </div>
          <MeasurementForm onSaved={load} />
        </div>

        {/* Stats Cards */}
        {latestMeasurement && (
          <div className="stats-grid">
            <div className="stat-card">
              <span className="stat-value">{latestMeasurement.bmi}</span>
              <span className="stat-label">Current BMI</span>
            </div>
            <div className="stat-card" style={{ background: 'linear-gradient(135deg, #f59e0b 0%, #d97706 100%)' }}>
              <span className="stat-value">{latestMeasurement.bmr}</span>
              <span className="stat-label">BMR (cal)</span>
            </div>
            <div className="stat-card" style={{ background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)' }}>
              <span className="stat-value">{latestMeasurement.daily_calories}</span>
              <span className="stat-label">Daily Calories</span>
            </div>
            <div className="stat-card" style={{ background: 'linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%)' }}>
              <span className="stat-value">{totalMeasurements}</span>
              <span className="stat-label">Total Records</span>
            </div>
          </div>
        )}

        {/* Recent Measurements Card */}
        <div className="card">
          <div className="card-header">
            <h2>📋 Recent Measurements</h2>
          </div>
          {error && <div className="alert alert-error">{error}</div>}
          {loading ? (
            <div className="loading">Loading your data...</div>
          ) : (
            <ul className="measurements-list">
              {rows.length === 0 ? (
                <div className="empty-state">
                  <p>No measurements yet. Add your first one above!</p>
                </div>
              ) : (
                rows.slice(0, 10).map(r => (
                  <li key={r.id} className="measurement-item">
                    <span className="measurement-date">
                      {new Date(r.created_at).toLocaleDateString('en-US', { 
                        month: 'short', 
                        day: 'numeric', 
                        year: 'numeric' 
                      })}
                    </span>
                    <div className="measurement-data">
                      <span className="measurement-badge badge-bmi">
                        BMI: <strong>{r.bmi}</strong> ({r.bmi_category})
                      </span>
                      <span className="measurement-badge badge-bmr">
                        BMR: <strong>{r.bmr}</strong> cal
                      </span>
                      <span className="measurement-badge badge-calories">
                        Daily: <strong>{r.daily_calories}</strong> cal
                      </span>
                    </div>
                  </li>
                ))
              )}
            </ul>
          )}
        </div>

        {/* Trend Chart Card */}
        <div className="card">
          <div className="card-header">
            <h2>📈 30-Day BMI Trend</h2>
          </div>
          <div className="chart-container">
            <TrendChart />
          </div>
        </div>
      </div>
    </>
  );
}
```

#### frontend-ec2/src/components/MeasurementForm.jsx
```javascript
import React, { useState } from 'react';
import api from '../api';

export default function MF({ onSaved }) {
  const [f, sf] = useState({ weightKg: 70, heightCm: 175, age: 30, sex: 'male', activity: 'moderate' });
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);
  
  const sub = async e => {
    e.preventDefault();
    setError(null);
    setSuccess(false);
    setLoading(true);
    try {
      await api.post('/measurements', f);
      setSuccess(true);
      setTimeout(() => setSuccess(false), 3000);
      onSaved && onSaved();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to save measurement');
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <form onSubmit={sub}>
      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">✓ Measurement saved successfully!</div>}
      
      <div className="form-row">
        <div className="form-group">
          <label htmlFor="weight">Weight (kg)</label>
          <input 
            id="weight"
            type="number" 
            value={f.weightKg} 
            onChange={e => sf({ ...f, weightKg: +e.target.value })}
            required
            min="1"
            max="500"
            step="0.1"
            placeholder="70"
          />
        </div>
        
        <div className="form-group">
          <label htmlFor="height">Height (cm)</label>
          <input 
            id="height"
            type="number"
            value={f.heightCm} 
            onChange={e => sf({ ...f, heightCm: +e.target.value })}
            required
            min="1"
            max="300"
            step="0.1"
            placeholder="175"
          />
        </div>
        
        <div className="form-group">
          <label htmlFor="age">Age (years)</label>
          <input 
            id="age"
            type="number"
            value={f.age} 
            onChange={e => sf({ ...f, age: +e.target.value })}
            required
            min="1"
            max="150"
            placeholder="30"
          />
        </div>
      </div>
      
      <div className="form-row">
        <div className="form-group">
          <label htmlFor="sex">Biological Sex</label>
          <select 
            id="sex"
            value={f.sex} 
            onChange={e => sf({ ...f, sex: e.target.value })}
            required
          >
            <option value="male">Male</option>
            <option value="female">Female</option>
          </select>
        </div>
        
        <div className="form-group">
          <label htmlFor="activity">Activity Level</label>
          <select 
            id="activity"
            value={f.activity} 
            onChange=e => sf({ ...f, activity: e.target.value })}
            required
          >
            <option value="sedentary">Sedentary (Little/No Exercise)</option>
            <option value="light">Light (1-3 days/week)</option>
            <option value="moderate">Moderate (3-5 days/week)</option>
            <option value="active">Active (6-7 days/week)</option>
            <option value="very_active">Very Active (2x per day)</option>
          </select>
        </div>
      </div>
      
      <button type="submit" disabled={loading}>
        {loading ? '⏳ Saving...' : '✓ Save Measurement'}
      </button>
    </form>
  );
}
```

#### frontend-ec2/src/components/TrendChart.jsx
```javascript
import React, { useEffect, useState } from 'react';
import { Line } from 'react-chartjs-2';
import api from '../api';
import { Chart as C, CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend } from 'chart.js';

C.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend);

export default function TC() {
  const [d, sd] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  useEffect(() => {
    setLoading(true);
    api.get('/measurements/trends')
      .then(r => {
        console.log('Trend data:', r.data);
        const rows = r.data.rows;
        if (rows && rows.length > 0) {
          sd({
            labels: rows.map(x => new Date(x.day).toLocaleDateString()),
            datasets: [{
              label: 'Average BMI',
              data: rows.map(x => parseFloat(x.avg_bmi)),
              borderColor: 'rgb(75, 192, 192)',
              backgroundColor: 'rgba(75, 192, 192, 0.2)',
              tension: 0.1
            }]
          });
        } else {
          setError(null); // Clear error if no data
        }
      })
      .catch(err => {
        console.error('Failed to load trends:', err);
        console.error('Error details:', err.response?.data);
        setError('Failed to load trend data');
      })
      .finally(() => setLoading(false));
  }, []);
  
  if (loading) return <div className="loading">Loading chart...</div>;
  if (error) return <div className="alert alert-error">{error}</div>;
  if (!d) return <div className="empty-state"><p>No trend data available yet. Add measurements over multiple days to see trends!</p></div>;
  
  return <Line data={d} options={{
    responsive: true,
    plugins: {
      legend: { position: 'top' },
      title: { display: true, text: '30-Day BMI Trend' }
    }
  }} />;
}
```

#### frontend-ec2/src/index.css
```css
:root {
  --primary: #4f46e5;
  --primary-dark: #4338ca;
  --primary-light: #818cf8;
  --secondary: #10b981;
  --danger: #ef4444;
  --warning: #f59e0b;
  --gray-50: #f9fafb;
  --gray-100: #f3f4f6;
  --gray-200: #e5e7eb;
  --gray-300: #d1d5db;
  --gray-600: #4b5563;
  --gray-700: #374151;
  --gray-800: #1f2937;
  --gray-900: #111827;
  --shadow-sm: 0 1px 2px 0 rgb(0 0 0 / 0.05);
  --shadow: 0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1);
  --shadow-md: 0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1);
  --shadow-lg: 0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1);
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen',
    'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  min-height: 100vh;
  color: var(--gray-800);
  line-height: 1.6;
}

.app-header {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  padding: 1.5rem 0;
  box-shadow: var(--shadow-md);
  margin-bottom: 2rem;
  border-bottom: 3px solid var(--primary);
}

.app-header h1 {
  color: var(--primary);
  font-size: 2.5rem;
  font-weight: 700;
  text-align: center;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
}

.app-header h1::before {
  content: "💪";
  font-size: 2.5rem;
}

.app-subtitle {
  text-align: center;
  color: var(--gray-600);
  font-size: 1rem;
  margin-top: 0.5rem;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1.5rem 3rem;
}

.card {
  background: white;
  border-radius: 16px;
  padding: 2rem;
  box-shadow: var(--shadow-lg);
  margin-bottom: 2rem;
  transition: transform 0.2s, box-shadow 0.2s;
}

.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 20px 25px -5px rgb(0 0 0 / 0.1), 0 8px 10px -6px rgb(0 0 0 / 0.1);
}

.card-header {
  border-bottom: 2px solid var(--gray-100);
  padding-bottom: 1rem;
  margin-bottom: 1.5rem;
}

.card-header h2 {
  color: var(--gray-800);
  font-size: 1.5rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

form {
  display: grid;
  gap: 1.5rem;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1.5rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

label {
  font-weight: 600;
  color: var(--gray-700);
  font-size: 0.875rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

input,
select {
  padding: 0.75rem 1rem;
  border: 2px solid var(--gray-200);
  border-radius: 8px;
  font-size: 1rem;
  transition: all 0.2s;
  background: var(--gray-50);
  font-family: inherit;
}

input:focus,
select:focus {
  outline: none;
  border-color: var(--primary);
  background: white;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

input:hover,
select:hover {
  border-color: var(--gray-300);
}

button {
  padding: 0.875rem 2rem;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: var(--shadow);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
  background: linear-gradient(135deg, var(--primary-dark) 0%, var(--primary) 100%);
}

button:active:not(:disabled) {
  transform: translateY(0);
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.alert {
  padding: 1rem 1.25rem;
  border-radius: 8px;
  margin-bottom: 1.5rem;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-weight: 500;
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.alert-error {
  background-color: #fef2f2;
  color: #991b1b;
  border: 1px solid #fecaca;
}

.alert-error::before {
  content: "⚠️";
  font-size: 1.25rem;
}

.alert-success {
  background-color: #f0fdf4;
  color: #166534;
  border: 1px solid #bbf7d0;
}

.alert-success::before {
  content: "✓";
  font-size: 1.25rem;
  font-weight: bold;
}

.loading {
  text-align: center;
  padding: 3rem;
  color: var(--gray-600);
}

.loading::after {
  content: "";
  display: inline-block;
  width: 2rem;
  height: 2rem;
  border: 3px solid var(--gray-200);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-left: 1rem;
  vertical-align: middle;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.measurements-list {
  list-style: none;
  display: grid;
  gap: 1rem;
}

.measurement-item {
  background: var(--gray-50);
  padding: 1.25rem;
  border-radius: 10px;
  border-left: 4px solid var(--primary);
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 1rem;
  align-items: center;
  transition: all 0.2s;
}

.measurement-item:hover {
  background: white;
  box-shadow: var(--shadow-md);
  transform: translateX(4px);
}

.measurement-date {
  font-weight: 600;
  color: var(--primary);
  font-size: 0.875rem;
}

.measurement-data {
  display: flex;
  gap: 1.5rem;
  flex-wrap: wrap;
  align-items: center;
}

.measurement-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.375rem 0.75rem;
  background: white;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 600;
  box-shadow: var(--shadow-sm);
}

.badge-bmi {
  color: var(--primary);
}

.badge-bmr {
  color: #f59e0b;
}

.badge-calories {
  color: var(--secondary);
}

.empty-state {
  text-align: center;
  padding: 3rem;
  color: var(--gray-600);
  background: var(--gray-50);
  border-radius: 12px;
  border: 2px dashed var(--gray-300);
}

.empty-state::before {
  content: "📊";
  display: block;
  font-size: 3rem;
  margin-bottom: 1rem;
}

.chart-container {
  padding: 1.5rem;
  background: var(--gray-50);
  border-radius: 12px;
  min-height: 300px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.stat-card {
  background: linear-gradient(135deg, var(--primary-light) 0%, var(--primary) 100%);
  color: white;
  padding: 1.5rem;
  border-radius: 12px;
  text-align: center;
  box-shadow: var(--shadow);
}

.stat-value {
  font-size: 2rem;
  font-weight: 700;
  display: block;
  margin-bottom: 0.25rem;
}

.stat-label {
  font-size: 0.875rem;
  opacity: 0.9;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

@media (max-width: 768px) {
  .app-header h1 {
    font-size: 2rem;
  }
  
  .card {
    padding: 1.5rem;
  }
  
  .form-row {
    grid-template-columns: 1fr;
  }
  
  .measurement-item {
    grid-template-columns: 1fr;
    text-align: center;
  }
  
  .measurement-data {
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .container {
    padding: 0 1rem 2rem;
  }
  
  .card {
    padding: 1rem;
    border-radius: 12px;
  }
}
```

---

### Backend EC2 Files

#### backend-ec2/package.json
```json
{
  "name": "bmi-health-backend",
  "version": "1.0.0",
  "main": "src/server.js",
  "scripts": {
    "start": "node src/server.js",
    "dev": "nodemon src/server.js"
  },
  "dependencies": {
    "cors": "^2.8.5",
    "dotenv": "^16.0.0",
    "express": "^4.18.2",
    "pg": "^8.10.0",
    "body-parser": "^1.20.2"
  },
  "devDependencies": {
    "nodemon": "^3.0.1"
  }
}
```

#### backend-ec2/.env.example
```env
# Server Configuration
PORT=3000
NODE_ENV=production

# AWS RDS PostgreSQL Database Connection
# Format: postgresql://username:password@rds-endpoint:port/database
DATABASE_URL=postgresql://bmi_admin:YOUR_STRONG_PASSWORD@bmi-tracker-db.xxxxx.us-east-1.rds.amazonaws.com:5432/bmidb

# Frontend Configuration (for CORS)
FRONTEND_URL=http://FRONTEND_PUBLIC_IP
```

#### backend-ec2/src/server.js
```javascript
require('dotenv').config();
const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const routes = require('./routes');

const app = express();
const PORT = process.env.PORT || 3000;
const NODE_ENV = process.env.NODE_ENV || 'development';

// CORS configuration
const corsOptions = {
  origin: NODE_ENV === 'production' 
    ? process.env.FRONTEND_URL || 'http://localhost'
    : ['http://localhost:5173', 'http://localhost:3000'],
  credentials: true,
  optionsSuccessStatus: 200
};

app.use(cors(corsOptions));
app.use(bodyParser.json());

// Listen on all interfaces (0.0.0.0) to accept connections from frontend EC2
app.listen(PORT, '0.0.0.0', () => {
  console.log(`🚀 Server running on port ${PORT}`);
  console.log(`📊 Environment: ${NODE_ENV}`);
  console.log(`🔗 API available at: http://0.0.0.0:${PORT}/api`);
  console.log(`🗄️  Database: AWS RDS PostgreSQL`);
});

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ 
    status: 'ok', 
    environment: NODE_ENV,
    database: 'AWS RDS PostgreSQL',
    timestamp: new Date().toISOString()
  });
});

// API routes
app.use('/api', routes);

// 404 handler
app.use((req, res) => {
  res.status(404).json({ error: 'Route not found' });
});

// Error handler
app.use((err, req, res, next) => {
  console.error('Server error:', err);
  res.status(500).json({ error: 'Internal server error' });
});
```

#### backend-ec2/src/db.js
```javascript
const { Pool } = require('pg');

// PostgreSQL connection pool configuration for AWS RDS
const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  max: 20, // Maximum number of clients in the pool
  idleTimeoutMillis: 30000, // Close idle clients after 30 seconds
  connectionTimeoutMillis: 10000, // Increased timeout for RDS (10 seconds)
  // SSL configuration for AWS RDS
  ssl: process.env.NODE_ENV === 'production' ? {
    rejectUnauthorized: false // Required for AWS RDS SSL connections
  } : false
});

// Handle pool errors
pool.on('error', (err, client) => {
  console.error('Unexpected error on idle PostgreSQL client:', err);
  process.exit(-1);
});

// Test connection on startup
pool.query('SELECT NOW()', (err, res) => {
  if (err) {
    console.error('❌ Database connection failed:', err.message);
    console.error('Check your DATABASE_URL in .env file');
    console.error('Ensure RDS instance is accessible from this EC2');
    process.exit(1);
  } else {
    console.log('✅ Database connected successfully at:', res.rows[0].now);
    console.log('✅ Connected to AWS RDS PostgreSQL');
  }
});

module.exports = {
  query: (text, params) => pool.query(text, params),
  pool
};
```

#### backend-ec2/src/routes.js
```javascript
const express = require('express');
const router = express.Router();
const db = require('./db');
const { calculateMetrics } = require('./calculations');

// POST /api/measurements - Create new measurement
router.post('/measurements', async (req, res) => {
  try {
    const { weightKg, heightCm, age, sex, activity } = req.body;
    
    // Validation
    if (!weightKg || !heightCm || !age || !sex) {
      return res.status(400).json({ error: 'Missing required fields' });
    }
    if (weightKg <= 0 || heightCm <= 0 || age <= 0) {
      return res.status(400).json({ error: 'Invalid values: must be positive numbers' });
    }
    
    const m = calculateMetrics({ weightKg, heightCm, age, sex, activity });
    const q = `INSERT INTO measurements (weight_kg,height_cm,age,sex,activity_level,bmi,bmi_category,bmr,daily_calories,created_at)
    VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,now()) RETURNING *`;
    const v = [weightKg, heightCm, age, sex, activity, m.bmi, m.bmiCategory, m.bmr, m.dailyCalories];
    const r = await db.query(q, v);
    res.status(201).json({ measurement: r.rows[0] });
  } catch (e) {
    console.error('Error creating measurement:', e);
    res.status(500).json({ error: e.message || 'Failed to create measurement' });
  }
});

// GET /api/measurements - Get all measurements
router.get('/measurements', async (req, res) => {
  try {
    const r = await db.query('SELECT * FROM measurements ORDER BY created_at DESC');
    res.json({ rows: r.rows });
  } catch (e) {
    console.error('Error fetching measurements:', e);
    res.status(500).json({ error: 'Failed to fetch measurements' });
  }
});

// GET /api/measurements/trends - Get 30-day BMI trends
router.get('/measurements/trends', async (req, res) => {
  try {
    const q = `SELECT date_trunc('day',created_at) AS day, AVG(bmi) AS avg_bmi 
    FROM measurements
    WHERE created_at > now() - interval '30 days' 
    GROUP BY day 
    ORDER BY day`;
    const r = await db.query(q);
    res.json({ rows: r.rows });
  } catch (e) {
    console.error('Error fetching trends:', e);
    res.status(500).json({ error: 'Failed to fetch trends' });
  }
});

module.exports = router;
```

#### backend-ec2/src/calculations.js
```javascript
function bmiCategory(b) {
  if (b < 18.5) return 'Underweight';
  if (b < 25) return 'Normal';
  if (b < 30) return 'Overweight';
  return 'Obese';
}

function calculateMetrics({ weightKg, heightCm, age, sex, activity }) {
  const h = heightCm / 100;
  const bmi = +(weightKg / (h * h)).toFixed(1);
  
  let bmr = sex === 'male'
    ? 10 * weightKg + 6.25 * heightCm - 5 * age + 5
    : 10 * weightKg + 6.25 * heightCm - 5 * age - 161;
  
  const mult = {
    sedentary: 1.2,
    light: 1.375,
    moderate: 1.55,
    active: 1.725,
    very_active: 1.9
  }[activity] || 1.2;
  
  return {
    bmi,
    bmiCategory: bmiCategory(bmi),
    bmr: Math.round(bmr),
    dailyCalories: Math.round(bmr * mult)
  };
}

module.exports = { calculateMetrics };
```

#### backend-ec2/ecosystem.config.js
```javascript
module.exports = {
  apps: [{
    name: 'bmi-backend',
    script: './src/server.js',
    cwd: '/home/ubuntu/bmi-backend',
    instances: 1,
    autorestart: true,
    watch: false,
    max_memory_restart: '500M',
    env: {
      NODE_ENV: 'production',
      PORT: 3000
    },
    error_file: './logs/err.log',
    out_file: './logs/out.log',
    log_file: './logs/combined.log',
    time: true,
    merge_logs: true,
    log_date_format: 'YYYY-MM-DD HH:mm:ss Z'
  }]
};
```

#### backend-ec2/001_create_measurements_rds.sql
```sql
-- BMI Health Tracker Database Migration for AWS RDS
-- Version: 001
-- Description: Create measurements table
-- Target: AWS RDS PostgreSQL
-- Date: 2025-12-13

-- Create measurements table
CREATE TABLE IF NOT EXISTS measurements (
  id SERIAL PRIMARY KEY,
  weight_kg NUMERIC(5,2) NOT NULL CHECK (weight_kg > 0 AND weight_kg < 1000),
  height_cm NUMERIC(5,2) NOT NULL CHECK (height_cm > 0 AND height_cm < 300),
  age INTEGER NOT NULL CHECK (age > 0 AND age < 150),
  sex VARCHAR(10) NOT NULL CHECK (sex IN ('male', 'female')),
  activity_level VARCHAR(30) CHECK (activity_level IN ('sedentary', 'light', 'moderate', 'active', 'very_active')),
  bmi NUMERIC(4,1) NOT NULL,
  bmi_category VARCHAR(30),
  bmr INTEGER,
  daily_calories INTEGER,
  created_at TIMESTAMPTZ DEFAULT now() NOT NULL
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_measurements_created_at ON measurements(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_measurements_bmi ON measurements(bmi);

-- Add comments for documentation
COMMENT ON TABLE measurements IS 'Stores user health measurements including BMI, BMR, and calorie data';
COMMENT ON COLUMN measurements.weight_kg IS 'Weight in kilograms';
COMMENT ON COLUMN measurements.height_cm IS 'Height in centimeters';
COMMENT ON COLUMN measurements.age IS 'Age in years';
COMMENT ON COLUMN measurements.sex IS 'Biological sex (male/female)';
COMMENT ON COLUMN measurements.activity_level IS 'Physical activity level';
COMMENT ON COLUMN measurements.bmi IS 'Body Mass Index';
COMMENT ON COLUMN measurements.bmi_category IS 'BMI category (Underweight/Normal/Overweight/Obese)';
COMMENT ON COLUMN measurements.bmr IS 'Basal Metabolic Rate in calories';
COMMENT ON COLUMN measurements.daily_calories IS 'Daily calorie needs based on activity';

-- Display confirmation
SELECT 'Migration 001 completed successfully - measurements table created' AS status;

-- Show table structure
\d measurements
```

---

## AWS RDS Configuration

### RDS Instance Specifications

**Recommended Configuration:**
- **Engine**: PostgreSQL 14.x or 15.x
- **Instance Class**: db.t3.micro (Free Tier) or db.t3.small (Production)
- **Storage**: 20 GB GP2 SSD, Autoscaling enabled up to 100 GB
- **Multi-AZ**: No (Free Tier) / Yes (Production for HA)
- **Backup Retention**: 7 days
- **Encryption**: Enabled (at rest and in transit)

### Security Group Rules for RDS

**Inbound Rules:**
```
Type: PostgreSQL
Protocol: TCP
Port: 5432
Source: Backend EC2 Security Group (sg-xxxxxx)
Description: Allow Backend EC2 to access RDS
```

**Outbound Rules:**
```
Type: All traffic
Destination: 0.0.0.0/0
```

### Database Credentials

```
Master Username: bmi_admin
Master Password: <Your-Strong-Password>
Initial Database Name: bmidb
Port: 5432
Endpoint: bmi-tracker-db.xxxxx.us-east-1.rds.amazonaws.com
```

### RDS Subnet Group

Create DB Subnet Group with at least 2 private subnets in different Availability Zones:
- Private Subnet 1: 10.0.3.0/24 (AZ: us-east-1a)
- Private Subnet 2: 10.0.4.0/24 (AZ: us-east-1b)

---

## API Endpoints

### Health Check
- **GET** `/health`
- Returns: `{ status: 'ok', environment: 'production', database: 'AWS RDS PostgreSQL', timestamp: '...' }`

### Create Measurement
- **POST** `/api/measurements`
- Body: `{ weightKg, heightCm, age, sex, activity }`
- Returns: `{ measurement: {...} }`

### Get All Measurements
- **GET** `/api/measurements`
- Returns: `{ rows: [...] }`

### Get 30-Day Trends
- **GET** `/api/measurements/trends`
- Returns: `{ rows: [{ day, avg_bmi }] }`

---

## Health Calculation Formulas

### BMI (Body Mass Index)
```
BMI = weight_kg / (height_m)²
```

### BMI Categories
- Underweight: BMI < 18.5
- Normal: 18.5 ≤ BMI < 25
- Overweight: 25 ≤ BMI < 30
- Obese: BMI ≥ 30

### BMR (Basal Metabolic Rate) - Mifflin-St Jeor
```
Male:   BMR = 10 × weight + 6.25 × height - 5 × age + 5
Female: BMR = 10 × weight + 6.25 × height - 5 × age - 161
```

### Daily Calories
```
Daily Calories = BMR × Activity Multiplier

Activity Multipliers:
- Sedentary (little/no exercise): 1.2
- Light (1-3 days/week): 1.375
- Moderate (3-5 days/week): 1.55
- Active (6-7 days/week): 1.725
- Very Active (2x per day): 1.9
```

---

## Quick Recreation Steps

If you only have this AGENT.md file:

1. **Create Directory Structure**
   ```bash
   mkdir -p multiple-ec2-RDS-3tier-webapp/{frontend-ec2/src/components,backend-ec2/src}
   cd multiple-ec2-RDS-3tier-webapp
   ```

2. **Copy All File Contents**
   - Copy each file's content from sections above
   - Create files with exact names and paths
   - Pay attention to file extensions

3. **Make Scripts Executable**
   ```bash
   chmod +x frontend-ec2/deploy-frontend.sh
   chmod +x backend-ec2/deploy-backend.sh
   ```

4. **Follow DEPLOYMENT_GUIDE.md** for complete AWS setup

---

## AWS Resource Summary

### EC2 Instances (2 Total)

1. **Frontend EC2**
   - Ubuntu 22.04 LTS
   - t2.micro (1 vCPU, 1 GB RAM)
   - Public subnet with Elastic IP
   - Nginx, Node.js, React build

2. **Backend EC2**
   - Ubuntu 22.04 LTS
   - t2.micro (1 vCPU, 1 GB RAM)
   - Private subnet
   - Node.js, Express, PM2

### RDS Database (1 Instance)

- PostgreSQL 14/15
- db.t3.micro (2 vCPUs, 1 GB RAM)
- 20 GB storage
- Private subnet group
- SSL/TLS enabled

### Networking

- 1 VPC (10.0.0.0/16)
- 1 Internet Gateway
- 1 NAT Gateway (for backend updates)
- 3 Subnets:
  - Public subnet for Frontend
  - Private subnet for Backend
  - Private subnet for RDS
- 3 Security Groups:
  - Frontend SG (HTTP/HTTPS from internet)
  - Backend SG (TCP:3000 from Frontend)
  - RDS SG (PostgreSQL:5432 from Backend)
- 1 Elastic IP (Frontend)

---

## Security Features

- ✅ SSL/TLS encryption for RDS connections
- ✅ Backend in private subnet (no direct internet access)
- ✅ RDS in private subnet (only accessible from Backend EC2)
- ✅ Security Group-based access control
- ✅ SQL Injection Protection (parameterized queries)
- ✅ Environment-based CORS
- ✅ Input validation (frontend, backend, database)
- ✅ Request timeouts
- ✅ Connection pool limits
- ✅ Error sanitization
- ✅ Credentials in environment variables

---

## Cost Estimate (Monthly)

**AWS Free Tier (First 12 Months):**
- EC2 t2.micro x2: $0 (750 hours/month free)
- RDS db.t3.micro: $0 (750 hours/month free)
- 20 GB storage: $0 (20 GB free)
- NAT Gateway: ~$32/month (not free)
- Elastic IP: $0 (if attached)
- Data transfer: Minimal cost

**Total (Free Tier)**: ~$32/month (NAT Gateway only)

**After Free Tier:**
- EC2 t2.micro x2: ~$16/month
- RDS db.t3.micro: ~$25/month
- Storage 20 GB: ~$5/month
- NAT Gateway: ~$32/month
- Data transfer: ~$5/month

**Total (Post Free Tier)**: ~$83/month

---

##Deployment Order

1. **AWS RDS** - Create and configure PostgreSQL database
2. **Backend EC2** - Deploy Node.js API with RDS connection
3. **Frontend EC2** - Deploy React app with backend proxy

This ensures each tier is functional before depending services are deployed.

---

## Troubleshooting

### RDS Connection Issues
```bash
# Test from Backend EC2
psql "postgresql://bmi_admin:PASSWORD@RDS_ENDPOINT:5432/bmidb"

# Check security group allows Backend → RDS on port 5432
# Verify RDS is in "Available" state
# Confirm DATABASE_URL in .env is correct
```

### Backend Cannot Reach RDS
- Check Backend EC2 security group has outbound to RDS
- Check RDS security group has inbound from Backend
- Verify RDS endpoint in DATABASE_URL
- Test: `telnet RDS_ENDPOINT 5432`

### Frontend Cannot Reach Backend
- Verify backend is running: `pm2 status`
- Check nginx config has correct backend private IP
- Test: `curl http://BACKEND_IP:3000/health`

---

**Last Updated:** December 13, 2025  
**Version:** 1.0 - AWS RDS Multi-EC2 Edition  
**Status:** ✅ Production Ready - Can recreate entire project from this file
