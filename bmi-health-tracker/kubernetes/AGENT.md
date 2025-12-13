# BMI Health Tracker - Kubernetes Complete Reconstruction Guide

This document contains everything needed to deploy the BMI Health Tracker on Kubernetes from scratch, even if all other files are lost.

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Complete File Structure](#complete-file-structure)
4. [All Kubernetes Manifests](#all-kubernetes-manifests)
5. [Docker Images](#docker-images)
6. [Deployment Scripts](#deployment-scripts)
7. [Configuration Guide](#configuration-guide)
8. [Deployment Instructions](#deployment-instructions)
9. [Monitoring & Operations](#monitoring--operations)
10. [Troubleshooting](#troubleshooting)

---

## Overview

**Application**: BMI & Health Tracker  
**Architecture**: 3-tier microservices  
**Components**:
- Frontend: React 18 + Vite (Nginx container)
- Backend: Node.js 18 + Express (2 replicas)
- Database: PostgreSQL 15 (StatefulSet with PVC)

**Resource Requirements**:
- CPU: 4 cores minimum
- Memory: 8GB minimum
- Storage: 10GB minimum
- Kubernetes: v1.24+

---

## Architecture

### Kubernetes Architecture

```
┌──────────────────── Kubernetes Cluster ────────────────────┐
│                                                              │
│  ┌────────────────────────────────────────────────┐         │
│  │              Ingress Controller                 │         │
│  │    (Optional - nginx-ingress + cert-manager)    │         │
│  └──────────────────┬──────────────┬───────────────┘         │
│                     │              │                         │
│     ┌───────────────▼──────┐  ┌────▼──────────────┐         │
│     │   Frontend Service   │  │  Backend Service   │         │
│     │   Type: LoadBalancer │  │   Type: ClusterIP  │         │
│     │   Port: 80           │  │   Port: 3000       │         │
│     └───────────────┬──────┘  └────┬──────────────┘         │
│                     │              │                         │
│     ┌───────────────▼──────┐  ┌────▼──────────────┐         │
│     │  Frontend Deployment │  │ Backend Deployment │         │
│     │    Replicas: 2       │  │   Replicas: 2      │         │
│     │    Nginx Alpine      │  │   Node 18 Alpine   │         │
│     │    Memory: 256Mi     │  │   Memory: 512Mi    │         │
│     │    CPU: 200m         │  │   CPU: 500m        │         │
│     └──────────────────────┘  └────┬──────────────┘         │
│                                    │                         │
│                               ┌────▼──────────────┐          │
│                               │ PostgreSQL Service│          │
│                               │  Type: ClusterIP  │          │
│                               │  Port: 5432       │          │
│                               └────┬──────────────┘          │
│                                    │                         │
│                               ┌────▼──────────────┐          │
│                               │ PostgreSQL Deploy │          │
│                               │   Replicas: 1     │          │
│                               │   Postgres 15     │          │
│                               │   Memory: 512Mi   │          │
│                               │   CPU: 500m       │          │
│                               └────┬──────────────┘          │
│                                    │                         │
│                               ┌────▼──────────────┐          │
│                               │ PersistentVolume  │          │
│                               │  Claim (5Gi)      │          │
│                               │  StorageClass:gp2 │          │
│                               └───────────────────┘          │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### Network Flow

```
User Request
    │
    ▼
Ingress (HTTPS:443)
    │
    ├──► /api/* ──► Backend Service:3000 ──► Backend Pod ──► PostgreSQL:5432
    │
    └──► /* ──► Frontend Service:80 ──► Frontend Pod (Nginx) ──► Serve React App
```

---

## Complete File Structure

```
kubernetes/
├── manifests/
│   ├── namespace.yaml              # Create bmi-health-tracker namespace
│   ├── postgres-pvc.yaml           # 5Gi persistent storage for database
│   ├── postgres-secret.yaml        # Database credentials
│   ├── postgres-deployment.yaml    # PostgreSQL deployment
│   ├── postgres-service.yaml       # PostgreSQL ClusterIP service
│   ├── postgres-init-job.yaml      # Database migration job
│   ├── backend-configmap.yaml      # Backend environment variables
│   ├── backend-deployment.yaml     # Backend Node.js deployment
│   ├── backend-service.yaml        # Backend ClusterIP service
│   ├── frontend-deployment.yaml    # Frontend React deployment
│   ├── frontend-service.yaml       # Frontend LoadBalancer service
│   └── ingress.yaml                # Optional Ingress with SSL/TLS
├── scripts/
│   ├── build-images.sh             # Build and push Docker images
│   ├── deploy.sh                   # Deploy everything to K8s
│   └── cleanup.sh                  # Remove all resources
├── Dockerfile.backend              # Multi-stage backend image
├── Dockerfile.frontend             # Multi-stage frontend image with Nginx
├── nginx.conf                      # Nginx config for React SPA
├── README.md                       # Quick start guide
├── DEPLOYMENT.md                   # Detailed deployment steps
└── AGENT.md                        # This file - complete reference
```

---

## All Kubernetes Manifests

### 1. Namespace (namespace.yaml)

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: bmi-health-tracker
  labels:
    name: bmi-health-tracker
    app: bmi-health-tracker
```

**Purpose**: Isolate all BMI app resources in dedicated namespace

---

### 2. PostgreSQL PVC (postgres-pvc.yaml)

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-pvc
  namespace: bmi-health-tracker
  labels:
    app: postgres
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 5Gi
  storageClassName: gp2  # Change based on provider: gp2(AWS), standard(GKE), managed-premium(Azure)
```

**Purpose**: Persistent storage for PostgreSQL data  
**Storage Classes**:
- AWS EKS: `gp2` or `gp3`
- GKE: `standard` or `standard-rwo`
- Azure AKS: `managed-premium`
- On-premise: Check available classes with `kubectl get storageclass`

---

### 3. PostgreSQL Secret (postgres-secret.yaml)

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: postgres-secret
  namespace: bmi-health-tracker
type: Opaque
stringData:
  POSTGRES_USER: bmi_user
  POSTGRES_PASSWORD: SecurePassword123!  # MUST CHANGE THIS!
  POSTGRES_DB: bmidb
  DATABASE_URL: postgresql://bmi_user:SecurePassword123!@postgres-service:5432/bmidb  # MUST CHANGE PASSWORD!
```

**CRITICAL**: Change passwords before deployment!

**Purpose**: Store sensitive database credentials  
**Security**: Values are base64 encoded when stored

---

### 4. PostgreSQL Deployment (postgres-deployment.yaml)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres
  namespace: bmi-health-tracker
  labels:
    app: postgres
spec:
  replicas: 1  # Single replica for database
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgres:15-alpine
        ports:
        - containerPort: 5432
          name: postgres
        env:
        - name: POSTGRES_USER
          valueFrom:
            secretKeyRef:
              name: postgres-secret
              key: POSTGRES_USER
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: postgres-secret
              key: POSTGRES_PASSWORD
        - name: POSTGRES_DB
          valueFrom:
            secretKeyRef:
              name: postgres-secret
              key: POSTGRES_DB
        - name: PGDATA
          value: /var/lib/postgresql/data/pgdata
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          exec:
            command:
            - pg_isready
            - -U
            - bmi_user
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          exec:
            command:
            - pg_isready
            - -U
            - bmi_user
          initialDelaySeconds: 5
          periodSeconds: 10
      volumes:
      - name: postgres-storage
        persistentVolumeClaim:
          claimName: postgres-pvc
```

**Purpose**: Run PostgreSQL database  
**Image**: Official PostgreSQL 15 Alpine (lightweight)  
**Probes**: Health checks ensure database is ready

---

### 5. PostgreSQL Service (postgres-service.yaml)

```yaml
apiVersion: v1
kind: Service
metadata:
  name: postgres-service
  namespace: bmi-health-tracker
  labels:
    app: postgres
spec:
  type: ClusterIP  # Internal only
  ports:
  - port: 5432
    targetPort: 5432
    protocol: TCP
    name: postgres
  selector:
    app: postgres
```

**Purpose**: Internal DNS for database access  
**Access**: `postgres-service.bmi-health-tracker.svc.cluster.local:5432`

---

### 6. PostgreSQL Init Job (postgres-init-job.yaml)

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: postgres-init
  namespace: bmi-health-tracker
  labels:
    app: postgres-init
spec:
  template:
    metadata:
      labels:
        app: postgres-init
    spec:
      restartPolicy: OnFailure
      containers:
      - name: init-db
        image: postgres:15-alpine
        env:
        - name: PGHOST
          value: postgres-service
        - name: PGPORT
          value: "5432"
        - name: PGUSER
          valueFrom:
            secretKeyRef:
              name: postgres-secret
              key: POSTGRES_USER
        - name: PGPASSWORD
          valueFrom:
            secretKeyRef:
              name: postgres-secret
              key: POSTGRES_PASSWORD
        - name: PGDATABASE
          valueFrom:
            secretKeyRef:
              name: postgres-secret
              key: POSTGRES_DB
        command:
        - /bin/sh
        - -c
        - |
          # Wait for PostgreSQL to be ready
          until pg_isready -h $PGHOST -p $PGPORT -U $PGUSER; do
            echo "Waiting for PostgreSQL to be ready..."
            sleep 2
          done
          
          # Run the migration
          psql -h $PGHOST -U $PGUSER -d $PGDATABASE <<'EOF'
          CREATE TABLE IF NOT EXISTS measurements (
            id SERIAL PRIMARY KEY,
            weight_kg NUMERIC(5,2) NOT NULL CHECK (weight_kg > 0 AND weight_kg <= 500),
            height_cm NUMERIC(5,2) NOT NULL CHECK (height_cm > 0 AND height_cm <= 300),
            age INTEGER NOT NULL CHECK (age > 0 AND age <= 150),
            sex VARCHAR(10) NOT NULL CHECK (sex IN ('male', 'female')),
            activity_level VARCHAR(20) NOT NULL CHECK (activity_level IN ('sedentary', 'light', 'moderate', 'active', 'very_active')),
            bmi NUMERIC(5,2) NOT NULL,
            bmi_category VARCHAR(20),
            bmr INTEGER,
            daily_calories INTEGER,
            created_at TIMESTAMPTZ DEFAULT NOW()
          );
          
          CREATE INDEX IF NOT EXISTS idx_measurements_created_at ON measurements(created_at DESC);
          CREATE INDEX IF NOT EXISTS idx_measurements_bmi ON measurements(bmi);
          
          COMMENT ON TABLE measurements IS 'Health measurements including BMI, BMR, and daily calorie needs';
          COMMENT ON COLUMN measurements.bmi IS 'Body Mass Index calculated from weight and height';
          COMMENT ON COLUMN measurements.bmr IS 'Basal Metabolic Rate in calories';
          COMMENT ON COLUMN measurements.daily_calories IS 'Daily calorie needs based on BMR and activity level';
          EOF
          
          echo "Database migration completed successfully!"
```

**Purpose**: Initialize database schema automatically  
**Type**: Kubernetes Job (runs once)

---

### 7. Backend ConfigMap (backend-configmap.yaml)

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: backend-config
  namespace: bmi-health-tracker
  labels:
    app: bmi-backend
data:
  NODE_ENV: "production"
  PORT: "3000"
  FRONTEND_URL: "http://your-domain.com"  # CHANGE THIS!
```

**Purpose**: Non-sensitive backend configuration  
**CHANGE**: Update FRONTEND_URL to your actual domain/IP

---

### 8. Backend Deployment (backend-deployment.yaml)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: bmi-backend
  namespace: bmi-health-tracker
  labels:
    app: bmi-backend
spec:
  replicas: 2  # High availability
  selector:
    matchLabels:
      app: bmi-backend
  template:
    metadata:
      labels:
        app: bmi-backend
    spec:
      containers:
      - name: backend
        image: your-registry/bmi-backend:latest  # CHANGE THIS!
        ports:
        - containerPort: 3000
          name: http
        env:
        - name: NODE_ENV
          valueFrom:
            configMapKeyRef:
              name: backend-config
              key: NODE_ENV
        - name: PORT
          valueFrom:
            configMapKeyRef:
              name: backend-config
              key: PORT
        - name: FRONTEND_URL
          valueFrom:
            configMapKeyRef:
              name: backend-config
              key: FRONTEND_URL
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: postgres-secret
              key: DATABASE_URL
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /health
            port: 3000
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /health
            port: 3000
          initialDelaySeconds: 5
          periodSeconds: 10
```

**Purpose**: Run Node.js backend API  
**CHANGE**: Update image to your Docker registry  
**Replicas**: 2 for high availability

---

### 9. Backend Service (backend-service.yaml)

```yaml
apiVersion: v1
kind: Service
metadata:
  name: backend-service
  namespace: bmi-health-tracker
  labels:
    app: bmi-backend
spec:
  type: ClusterIP  # Internal only
  ports:
  - port: 3000
    targetPort: 3000
    protocol: TCP
    name: http
  selector:
    app: bmi-backend
```

**Purpose**: Internal load balancer for backend pods

---

### 10. Frontend Deployment (frontend-deployment.yaml)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: bmi-frontend
  namespace: bmi-health-tracker
  labels:
    app: bmi-frontend
spec:
  replicas: 2  # High availability
  selector:
    matchLabels:
      app: bmi-frontend
  template:
    metadata:
      labels:
        app: bmi-frontend
    spec:
      containers:
      - name: frontend
        image: your-registry/bmi-frontend:latest  # CHANGE THIS!
        ports:
        - containerPort: 80
          name: http
        resources:
          requests:
            memory: "128Mi"
            cpu: "100m"
          limits:
            memory: "256Mi"
            cpu: "200m"
        livenessProbe:
          httpGet:
            path: /
            port: 80
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /
            port: 80
          initialDelaySeconds: 5
          periodSeconds: 10
```

**Purpose**: Run React frontend with Nginx  
**CHANGE**: Update image to your Docker registry

---

### 11. Frontend Service (frontend-service.yaml)

```yaml
apiVersion: v1
kind: Service
metadata:
  name: frontend-service
  namespace: bmi-health-tracker
  labels:
    app: bmi-frontend
spec:
  type: LoadBalancer  # Public access
  ports:
  - port: 80
    targetPort: 80
    protocol: TCP
    name: http
  selector:
    app: bmi-frontend
```

**Purpose**: External load balancer for public access  
**Type Options**:
- `LoadBalancer`: AWS/GKE/Azure (gets external IP)
- `NodePort`: On-premise (access via node IP:port)
- `ClusterIP`: Use with Ingress only

---

### 12. Ingress (ingress.yaml)

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: bmi-ingress
  namespace: bmi-health-tracker
  annotations:
    kubernetes.io/ingress.class: nginx
    cert-manager.io/cluster-issuer: letsencrypt-prod  # For SSL
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/proxy-body-size: "10m"
spec:
  rules:
  - host: your-domain.com  # CHANGE THIS!
    http:
      paths:
      - path: /api
        pathType: Prefix
        backend:
          service:
            name: backend-service
            port:
              number: 3000
      - path: /health
        pathType: Prefix
        backend:
          service:
            name: backend-service
            port:
              number: 3000
      - path: /
        pathType: Prefix
        backend:
          service:
            name: frontend-service
            port:
              number: 80
  tls:
  - hosts:
    - your-domain.com  # CHANGE THIS!
    secretName: bmi-tls-secret
```

**Purpose**: Route traffic with SSL/TLS  
**Prerequisites**: Nginx Ingress Controller + cert-manager  
**CHANGE**: Update domain name

---

## Docker Images

### Backend Dockerfile (Dockerfile.backend)

```dockerfile
# Multi-stage build for Node.js backend
FROM node:18-alpine AS builder

WORKDIR /app

# Copy package files
COPY backend/package*.json ./

# Install dependencies
RUN npm ci --only=production

# Copy application code
COPY backend/src ./src
COPY backend/migrations ./migrations

# Production stage
FROM node:18-alpine

WORKDIR /app

# Copy dependencies and code from builder
COPY --from=builder /app /app

# Create non-root user
RUN addgroup -g 1001 -S nodejs && \
    adduser -S nodejs -u 1001 && \
    chown -R nodejs:nodejs /app

USER nodejs

EXPOSE 3000

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s \
  CMD node -e "require('http').get('http://localhost:3000/health', (r) => {process.exit(r.statusCode === 200 ? 0 : 1)})"

CMD ["node", "src/server.js"]
```

**Build**: `docker build -f kubernetes/Dockerfile.backend -t your-registry/bmi-backend:v1.0.0 .`

---

### Frontend Dockerfile (Dockerfile.frontend)

```dockerfile
# Multi-stage build for React frontend
FROM node:18-alpine AS builder

WORKDIR /app

# Copy package files
COPY frontend/package*.json ./

# Install dependencies
RUN npm ci

# Copy application code
COPY frontend/ ./

# Build production bundle
RUN npm run build

# Production stage with Nginx
FROM nginx:alpine

# Copy custom nginx config
COPY kubernetes/nginx.conf /etc/nginx/conf.d/default.conf

# Copy built files from builder
COPY --from=builder /app/dist /usr/share/nginx/html

# Create non-root user
RUN addgroup -g 1001 -S nginx-user && \
    adduser -S nginx-user -u 1001 && \
    chown -R nginx-user:nginx-user /usr/share/nginx/html && \
    chown -R nginx-user:nginx-user /var/cache/nginx && \
    chown -R nginx-user:nginx-user /var/log/nginx && \
    chown -R nginx-user:nginx-user /etc/nginx/conf.d && \
    touch /var/run/nginx.pid && \
    chown -R nginx-user:nginx-user /var/run/nginx.pid

USER nginx-user

EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s \
  CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
```

**Build**: `docker build -f kubernetes/Dockerfile.frontend -t your-registry/bmi-frontend:v1.0.0 .`

---

### Nginx Configuration (nginx.conf)

```nginx
server {
    listen 80;
    server_name _;
    root /usr/share/nginx/html;
    index index.html;

    # Gzip compression
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css text/xml text/javascript application/javascript application/xml+rss application/json;

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # Serve static files with caching
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
        try_files $uri =404;
    }

    # SPA routing - serve index.html for all routes
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Health check endpoint
    location /health {
        access_log off;
        return 200 "healthy\n";
        add_header Content-Type text/plain;
    }
}
```

**Purpose**: Serve React SPA with proper routing and caching

---

## Deployment Scripts

### Build Images (scripts/build-images.sh)

```bash
#!/bin/bash
set -e

REGISTRY="your-docker-registry"
VERSION="latest"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

echo "Building BMI Health Tracker Docker Images"
echo "Registry: $REGISTRY"
echo "Version: $VERSION"

# Build Backend
docker build \
    -f "$PROJECT_ROOT/kubernetes/Dockerfile.backend" \
    -t "$REGISTRY/bmi-backend:$VERSION" \
    "$PROJECT_ROOT"

# Build Frontend
docker build \
    -f "$PROJECT_ROOT/kubernetes/Dockerfile.frontend" \
    -t "$REGISTRY/bmi-frontend:$VERSION" \
    "$PROJECT_ROOT"

# Push images
read -p "Push to registry? (y/n) " -n 1 -r
if [[ $REPLY =~ ^[Yy]$ ]]; then
    docker push "$REGISTRY/bmi-backend:$VERSION"
    docker push "$REGISTRY/bmi-frontend:$VERSION"
fi

echo "Build complete!"
```

**Usage**: `chmod +x build-images.sh && ./build-images.sh`

---

### Deploy Script (scripts/deploy.sh)

```bash
#!/bin/bash
set -e

NAMESPACE="bmi-health-tracker"
MANIFESTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../manifests" && pwd)"

echo "Deploying BMI Health Tracker"

# Create namespace
kubectl apply -f "$MANIFESTS_DIR/namespace.yaml"

# Deploy PostgreSQL
kubectl apply -f "$MANIFESTS_DIR/postgres-pvc.yaml"
kubectl apply -f "$MANIFESTS_DIR/postgres-secret.yaml"
kubectl apply -f "$MANIFESTS_DIR/postgres-deployment.yaml"
kubectl apply -f "$MANIFESTS_DIR/postgres-service.yaml"

kubectl wait --for=condition=available --timeout=120s deployment/postgres -n $NAMESPACE

# Initialize database
kubectl apply -f "$MANIFESTS_DIR/postgres-init-job.yaml"
kubectl wait --for=condition=complete --timeout=60s job/postgres-init -n $NAMESPACE

# Deploy Backend
kubectl apply -f "$MANIFESTS_DIR/backend-configmap.yaml"
kubectl apply -f "$MANIFESTS_DIR/backend-deployment.yaml"
kubectl apply -f "$MANIFESTS_DIR/backend-service.yaml"

kubectl wait --for=condition=available --timeout=120s deployment/bmi-backend -n $NAMESPACE

# Deploy Frontend
kubectl apply -f "$MANIFESTS_DIR/frontend-deployment.yaml"
kubectl apply -f "$MANIFESTS_DIR/frontend-service.yaml"

kubectl wait --for=condition=available --timeout=120s deployment/bmi-frontend -n $NAMESPACE

# Deploy Ingress (optional)
if [ -f "$MANIFESTS_DIR/ingress.yaml" ]; then
    kubectl apply -f "$MANIFESTS_DIR/ingress.yaml"
fi

echo "Deployment Complete!"
kubectl get all -n $NAMESPACE
```

**Usage**: `chmod +x deploy.sh && ./deploy.sh`

---

### Cleanup Script (scripts/cleanup.sh)

```bash
#!/bin/bash
set -e

NAMESPACE="bmi-health-tracker"

read -p "Delete ALL resources? (yes/no) " -r
if [[ $REPLY =~ ^yes$ ]]; then
    kubectl delete namespace $NAMESPACE
    echo "Cleanup complete!"
fi
```

**Usage**: `chmod +x cleanup.sh && ./cleanup.sh`

---

## Configuration Guide

### Required Changes Before Deployment

1. **Docker Registry** (3 files):
   - `scripts/build-images.sh`: Line 3
   - `manifests/backend-deployment.yaml`: Line 18
   - `manifests/frontend-deployment.yaml`: Line 18

2. **Database Password** (1 file):
   - `manifests/postgres-secret.yaml`: Lines 8-10

3. **Frontend URL** (1 file):
   - `manifests/backend-configmap.yaml`: Line 11

4. **Domain Name** (1 file, if using Ingress):
   - `manifests/ingress.yaml`: Lines 12, 33

5. **Storage Class** (1 file):
   - `manifests/postgres-pvc.yaml`: Line 11 (if not using AWS)

---

## Deployment Instructions

### Step 1: Prerequisites

```bash
# Verify kubectl
kubectl version --client
kubectl cluster-info

# Verify Docker
docker --version
docker login  # Login to your registry
```

### Step 2: Build Images

```bash
cd kubernetes/scripts

# Edit build-images.sh - change REGISTRY variable
nano build-images.sh

# Build and push
chmod +x build-images.sh
./build-images.sh
```

### Step 3: Configure Secrets

```bash
# Edit postgres secret
nano ../manifests/postgres-secret.yaml
# Change POSTGRES_PASSWORD and DATABASE_URL password

# Edit backend config
nano ../manifests/backend-configmap.yaml
# Change FRONTEND_URL

# Edit backend deployment
nano ../manifests/backend-deployment.yaml
# Change image to your registry

# Edit frontend deployment
nano ../manifests/frontend-deployment.yaml
# Change image to your registry
```

### Step 4: Deploy

```bash
# Deploy everything
chmod +x deploy.sh
./deploy.sh

# Wait for all pods to be ready
kubectl get pods -n bmi-health-tracker -w
```

### Step 5: Get Access URL

```bash
# Get LoadBalancer IP
kubectl get svc frontend-service -n bmi-health-tracker

# Or if using Ingress
kubectl get ingress bmi-ingress -n bmi-health-tracker
```

### Step 6: Verify

```bash
# Check all resources
kubectl get all -n bmi-health-tracker

# Test backend health
kubectl run curl --image=curlimages/curl -i --rm --restart=Never \
  -- curl http://backend-service.bmi-health-tracker:3000/health

# Check logs
kubectl logs -f deployment/bmi-backend -n bmi-health-tracker
```

---

## Monitoring & Operations

### Check Application Status

```bash
# All resources
kubectl get all -n bmi-health-tracker

# Pod details
kubectl describe pod <pod-name> -n bmi-health-tracker

# Resource usage
kubectl top pods -n bmi-health-tracker
kubectl top nodes
```

### View Logs

```bash
# Backend logs
kubectl logs -f deployment/bmi-backend -n bmi-health-tracker

# Frontend logs
kubectl logs -f deployment/bmi-frontend -n bmi-health-tracker

# PostgreSQL logs
kubectl logs -f deployment/postgres -n bmi-health-tracker

# Previous logs (if pod crashed)
kubectl logs deployment/bmi-backend --previous -n bmi-health-tracker
```

### Scale Application

```bash
# Manual scaling
kubectl scale deployment bmi-backend --replicas=5 -n bmi-health-tracker
kubectl scale deployment bmi-frontend --replicas=5 -n bmi-health-tracker

# Auto-scaling
kubectl autoscale deployment bmi-backend \
  --cpu-percent=70 --min=2 --max=10 \
  -n bmi-health-tracker
```

### Database Operations

```bash
# Connect to database
kubectl exec -it deployment/postgres -n bmi-health-tracker -- \
  psql -U bmi_user -d bmidb

# Backup database
kubectl exec deployment/postgres -n bmi-health-tracker -- \
  pg_dump -U bmi_user bmidb > backup-$(date +%Y%m%d).sql

# Restore database
kubectl exec -i deployment/postgres -n bmi-health-tracker -- \
  psql -U bmi_user bmidb < backup-20251212.sql
```

---

## Troubleshooting

### Pod Not Starting

```bash
# Check pod status
kubectl get pods -n bmi-health-tracker

# Describe pod
kubectl describe pod <pod-name> -n bmi-health-tracker

# Check events
kubectl get events -n bmi-health-tracker --sort-by='.lastTimestamp'

# Common issues:
# - ImagePullBackOff: Check image name and credentials
# - CrashLoopBackOff: Check logs
# - Pending: Check resources and PVC
```

### Database Connection Issues

```bash
# Check PostgreSQL is running
kubectl get pods -l app=postgres -n bmi-health-tracker

# Test connection from backend
kubectl exec -it deployment/bmi-backend -n bmi-health-tracker -- \
  nc -zv postgres-service 5432

# Check DATABASE_URL
kubectl exec deployment/bmi-backend -n bmi-health-tracker -- \
  printenv DATABASE_URL
```

### Service Not Accessible

```bash
# Check service
kubectl get svc -n bmi-health-tracker

# Check endpoints
kubectl get endpoints -n bmi-health-tracker

# Test from within cluster
kubectl run debug --image=busybox -i --rm --restart=Never -- \
  wget -O- http://frontend-service.bmi-health-tracker
```

### Performance Issues

```bash
# Check resource usage
kubectl top pods -n bmi-health-tracker

# Check resource limits
kubectl describe deployment bmi-backend -n bmi-health-tracker

# Increase resources if needed
kubectl set resources deployment bmi-backend \
  --requests=cpu=500m,memory=512Mi \
  --limits=cpu=1000m,memory=1Gi \
  -n bmi-health-tracker
```

---

## Production Checklist

- [ ] Changed all default passwords
- [ ] Used versioned Docker images (not :latest)
- [ ] Configured resource requests and limits
- [ ] Set up monitoring (Prometheus/Grafana)
- [ ] Configured log aggregation
- [ ] Enabled network policies
- [ ] Configured automated backups
- [ ] Set up SSL/TLS
- [ ] Configured DNS
- [ ] Tested disaster recovery
- [ ] Documented runbook

---

## Complete Reconstruction Steps

If all files are lost except this AGENT.md:

1. Create directory structure
2. Copy all manifest files from this document
3. Copy Dockerfiles and nginx.conf
4. Copy scripts and make executable
5. Update configuration (registry, passwords, domain)
6. Build Docker images
7. Deploy to Kubernetes
8. Verify deployment

All necessary code and configuration is in this single document!

---

**End of Kubernetes AGENT.md**
