# 3-Tier Application Connectivity Configuration

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT BROWSER                          │
│                    http://localhost:5173 (dev)                  │
│                 http://your-domain.com (production)             │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTP Requests
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    TIER 1: FRONTEND (React)                     │
├─────────────────────────────────────────────────────────────────┤
│  Development:                                                   │
│  • Vite Dev Server: Port 5173                                  │
│  • Proxy /api → http://localhost:3000                          │
│                                                                 │
│  Production:                                                    │
│  • Static files served by Nginx                                │
│  • Location: /var/www/bmi-health-tracker                       │
└────────────────────────────┬────────────────────────────────────┘
                             │ API Calls: /api/*
                             │ (Proxied in dev, Nginx in prod)
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│               TIER 2: MIDDLE TIER (Express API)                 │
├─────────────────────────────────────────────────────────────────┤
│  • Port: 3000                                                   │
│  • Base Route: /api                                             │
│  • CORS: Configured per environment                            │
│  • Endpoints:                                                   │
│    - GET  /health                                              │
│    - POST /api/measurements                                    │
│    - GET  /api/measurements                                    │
│    - GET  /api/measurements/trends                            │
└────────────────────────────┬────────────────────────────────────┘
                             │ SQL Queries
                             │ (PostgreSQL Protocol)
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│              TIER 3: BACKEND (PostgreSQL Database)              │
├─────────────────────────────────────────────────────────────────┤
│  • Port: 5432 (default)                                        │
│  • Database: bmidb                                             │
│  • User: bmi_user                                              │
│  • Connection: Pool with 20 max connections                    │
│  • Tables: measurements                                        │
└─────────────────────────────────────────────────────────────────┘
```

---

## Detailed Connectivity Configuration

### 1. Frontend → Middle Tier

#### Development Environment
**File**: `frontend/vite.config.js`
```javascript
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:3000',
      changeOrigin: true
    }
  }
}
```

**How it works:**
- Frontend runs on `http://localhost:5173`
- All requests to `/api/*` are proxied to `http://localhost:3000/api/*`
- Example: `GET /api/measurements` → `http://localhost:3000/api/measurements`

#### Production Environment
**File**: `nginx configuration`
```nginx
location /api/ {
  proxy_pass http://127.0.0.1:3000/api/;
  proxy_http_version 1.1;
  proxy_set_header Host $host;
  proxy_set_header X-Real-IP $remote_addr;
}
```

**How it works:**
- Frontend is static files served by Nginx on port 80/443
- Nginx proxies `/api/*` requests to backend on `http://127.0.0.1:3000`
- Single domain, no CORS issues

---

### 2. Frontend API Client

**File**: `frontend/src/api.js`
```javascript
const api = axios.create({
  baseURL: '/api',              // Relative URL (proxied)
  timeout: 10000,               // 10 second timeout
  headers: {
    'Content-Type': 'application/json'
  }
});
```

**Features:**
- ✅ Request/Response interceptors for error handling
- ✅ 10-second timeout to prevent hanging
- ✅ Automatic JSON content-type headers
- ✅ Centralized error logging

**Usage in Components:**
```javascript
import api from './api';

// POST request
await api.post('/measurements', data);

// GET request
const response = await api.get('/measurements');
```

---

### 3. Middle Tier Configuration

**File**: `backend/src/server.js`

#### Port Configuration
```javascript
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`🚀 Server running on port ${PORT}`);
});
```

#### CORS Configuration
```javascript
const corsOptions = {
  origin: NODE_ENV === 'production' 
    ? process.env.FRONTEND_URL || 'http://localhost'
    : ['http://localhost:5173', 'http://localhost:3000'],
  credentials: true,
  optionsSuccessStatus: 200
};
app.use(cors(corsOptions));
```

**Security:**
- ✅ Development: Allows localhost:5173 (Vite) and localhost:3000
- ✅ Production: Only allows configured FRONTEND_URL
- ✅ Credentials enabled for cookies/auth headers
- ⚠️ In production with Nginx proxy, CORS not needed (same origin)

#### Route Mounting
```javascript
app.use('/api', routes);
```
All routes defined in `routes.js` are prefixed with `/api`

---

### 4. Middle Tier → Database

**File**: `backend/src/db.js`

#### Connection Pool
```javascript
const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  max: 20,                    // Max 20 concurrent connections
  idleTimeoutMillis: 30000,   // Close idle after 30s
  connectionTimeoutMillis: 2000 // Timeout after 2s
});
```

#### Connection String Format
**File**: `backend/.env`
```
DATABASE_URL=postgresql://username:password@host:port/database
DATABASE_URL=postgresql://bmi_user:password@localhost:5432/bmidb
```

**Components:**
- `postgresql://` - Protocol
- `bmi_user` - Database user
- `password` - User password
- `localhost` - Database host (127.0.0.1 in production)
- `5432` - PostgreSQL port
- `bmidb` - Database name

#### Connection Testing
```javascript
pool.query('SELECT NOW()', (err, res) => {
  if (err) {
    console.error('❌ Database connection failed');
    process.exit(1);
  } else {
    console.log('✅ Database connected successfully');
  }
});
```

**Features:**
- ✅ Automatic connection on startup
- ✅ Error handling with process exit on failure
- ✅ Connection pooling for performance
- ✅ Idle connection cleanup

---

## Environment Variables

### Backend (.env)
```env
PORT=3000                                          # API server port
DATABASE_URL=postgresql://user:pass@host:port/db  # PostgreSQL connection
NODE_ENV=production                                # Environment
FRONTEND_URL=http://localhost                      # CORS allowed origin
```

### Frontend
No environment variables needed. Proxy configuration in `vite.config.js` handles routing.

---

## Request Flow Examples

### Example 1: Create Measurement

```
1. User submits form in React
   ↓
2. Frontend: api.post('/measurements', data)
   • URL: /api/measurements (relative)
   ↓
3. Vite Proxy (dev) or Nginx (prod)
   • Forwards to: http://localhost:3000/api/measurements
   ↓
4. Express API: POST /api/measurements
   • Validates data
   • Calculates BMI/BMR
   ↓
5. PostgreSQL Query
   • INSERT INTO measurements (...)
   ↓
6. Response flows back
   • Database → Express → Proxy → React
   • Status: 201 Created
   • Body: { measurement: {...} }
```

### Example 2: Get Measurements

```
1. Component loads: useEffect(() => { load() }, [])
   ↓
2. Frontend: api.get('/measurements')
   • URL: /api/measurements
   ↓
3. Proxy forwards to Express API
   ↓
4. Express: GET /api/measurements
   ↓
5. PostgreSQL Query
   • SELECT * FROM measurements ORDER BY created_at DESC
   ↓
6. Response with data
   • Status: 200 OK
   • Body: { rows: [...] }
   ↓
7. React updates state and re-renders
```

---

## Connection Testing

### Test Frontend → API
```bash
# Development
curl http://localhost:5173/api/measurements

# Production
curl http://your-domain.com/api/measurements
```

### Test API → Database
```bash
# From backend directory
node -e "require('dotenv').config(); const db = require('./src/db'); db.query('SELECT NOW()', (e,r) => console.log(r.rows[0]))"
```

### Test API Health
```bash
curl http://localhost:3000/health
# Expected: {"status":"ok","environment":"development"}
```

---

## Port Summary

| Component          | Port | Protocol | Access       |
|--------------------|------|----------|--------------|
| Frontend (Dev)     | 5173 | HTTP     | localhost    |
| Frontend (Prod)    | 80   | HTTP     | public       |
| Frontend (SSL)     | 443  | HTTPS    | public       |
| API (Express)      | 3000 | HTTP     | localhost    |
| Database (Postgres)| 5432 | TCP      | localhost    |

---

## Security Considerations

### ✅ Implemented
- CORS configured per environment
- Database connection pooling with limits
- Environment-specific configurations
- Request timeout (10s)
- Input validation on API routes
- Error handling without exposing internals

### 🔒 Production Recommendations
1. **Use HTTPS**: Always encrypt traffic with SSL/TLS
2. **Firewall**: Only expose ports 80/443, block 3000 and 5432 externally
3. **Strong DB Password**: Use 16+ character passwords
4. **Environment Variables**: Never commit `.env` to Git
5. **Rate Limiting**: Add rate limiting to API endpoints
6. **SQL Injection**: Using parameterized queries (✅ already implemented)
7. **Content Security Policy**: Add CSP headers to Nginx

---

## Troubleshooting Connectivity

### Frontend can't reach API

**Development:**
```bash
# Check Vite proxy
cat frontend/vite.config.js | grep proxy

# Check API is running
curl http://localhost:3000/health

# Check browser console
# Look for CORS or network errors
```

**Production:**
```bash
# Check Nginx proxy
sudo nginx -t
sudo systemctl status nginx

# Check Nginx logs
sudo tail -f /var/log/nginx/bmi-error.log

# Test API directly
curl http://localhost:3000/api/measurements
```

### API can't reach Database

```bash
# Check PostgreSQL is running
sudo systemctl status postgresql

# Test connection
psql -U bmi_user -d bmidb -h localhost

# Check DATABASE_URL in .env
cat backend/.env | grep DATABASE_URL

# Check backend logs
pm2 logs bmi-backend
```

### CORS Errors

```bash
# Check CORS configuration
grep -A 10 "corsOptions" backend/src/server.js

# In production, CORS shouldn't be needed if using Nginx proxy
# Browser and API should be on same domain
```

---

## Summary

✅ **All 3 tiers properly configured:**

1. **Frontend → Middle Tier**
   - Vite proxy in development
   - Nginx proxy in production
   - Axios client with error handling

2. **Middle Tier → Backend**
   - Connection pool with proper limits
   - Environment-based configuration
   - Health checks and error handling

3. **Security**
   - CORS properly configured
   - Input validation
   - Parameterized SQL queries
   - Timeout protection

The connectivity is **production-ready** and follows best practices for 3-tier application architecture.
