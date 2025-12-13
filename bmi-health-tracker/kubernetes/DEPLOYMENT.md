# BMI Health Tracker - Kubernetes Deployment Guide

Complete step-by-step guide to deploy the BMI & Health Tracker application on Kubernetes.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Cluster Setup](#cluster-setup)
3. [Build Docker Images](#build-docker-images)
4. [Configure Secrets](#configure-secrets)
5. [Deploy PostgreSQL](#deploy-postgresql)
6. [Deploy Backend](#deploy-backend)
7. [Deploy Frontend](#deploy-frontend)
8. [Configure Ingress](#configure-ingress)
9. [Verification](#verification)
10. [Troubleshooting](#troubleshooting)
11. [Scaling](#scaling)
12. [Backup & Restore](#backup--restore)

---

## Prerequisites

### Required Tools
- **kubectl** (v1.24+)
- **Docker** (v20.10+)
- **Kubernetes cluster** with:
  - 4 CPU cores minimum
  - 8GB RAM minimum
  - 10GB storage available
  - LoadBalancer support (AWS ELB, GKE LB, or MetalLB for on-premise)

### Supported Kubernetes Platforms
- AWS EKS
- Google GKE
- Azure AKS
- DigitalOcean Kubernetes
- On-premise (kubeadm, k3s, etc.)
- Minikube/Kind (for testing)

### Check Prerequisites

```bash
# Check kubectl
kubectl version --client

# Check cluster connection
kubectl cluster-info

# Check available nodes
kubectl get nodes

# Check storage classes
kubectl get storageclass
```

---

## Cluster Setup

### For AWS EKS

```bash
# Install eksctl
curl --silent --location "https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp
sudo mv /tmp/eksctl /usr/local/bin

# Create cluster
eksctl create cluster \
  --name bmi-cluster \
  --region us-west-2 \
  --nodegroup-name standard-workers \
  --node-type t3.medium \
  --nodes 3 \
  --nodes-min 2 \
  --nodes-max 4 \
  --managed

# Configure kubectl
aws eks update-kubeconfig --name bmi-cluster --region us-west-2
```

### For Google GKE

```bash
# Create cluster
gcloud container clusters create bmi-cluster \
  --num-nodes=3 \
  --machine-type=e2-medium \
  --region=us-central1

# Configure kubectl
gcloud container clusters get-credentials bmi-cluster --region=us-central1
```

### For Minikube (Local Testing)

```bash
# Start minikube
minikube start --cpus=4 --memory=8192

# Enable ingress addon
minikube addons enable ingress
```

---

## Build Docker Images

### Option 1: Automated Build Script

```bash
cd kubernetes/scripts
chmod +x build-images.sh

# Edit the script to set your Docker registry
nano build-images.sh
# Change: REGISTRY="your-docker-registry"
# To: REGISTRY="yourusername"  (for Docker Hub)
# Or: REGISTRY="gcr.io/your-project"  (for GCR)

# Build images
./build-images.sh
```

### Option 2: Manual Build

```bash
# Set your registry
REGISTRY="yourusername"
VERSION="v1.0.0"

# Build backend
docker build \
  -f kubernetes/Dockerfile.backend \
  -t $REGISTRY/bmi-backend:$VERSION \
  .

# Build frontend
docker build \
  -f kubernetes/Dockerfile.frontend \
  -t $REGISTRY/bmi-frontend:$VERSION \
  .

# Push to registry
docker push $REGISTRY/bmi-backend:$VERSION
docker push $REGISTRY/bmi-frontend:$VERSION
```

### Verify Images

```bash
# List images
docker images | grep bmi

# Test backend locally
docker run -p 3000:3000 $REGISTRY/bmi-backend:$VERSION

# Test frontend locally
docker run -p 8080:80 $REGISTRY/bmi-frontend:$VERSION
```

---

## Configure Secrets

### Update PostgreSQL Credentials

```bash
# Edit postgres secret
nano kubernetes/manifests/postgres-secret.yaml
```

**IMPORTANT**: Change these values:

```yaml
stringData:
  POSTGRES_USER: bmi_user
  POSTGRES_PASSWORD: CHANGE_THIS_STRONG_PASSWORD_123!
  POSTGRES_DB: bmidb
  DATABASE_URL: postgresql://bmi_user:CHANGE_THIS_STRONG_PASSWORD_123!@postgres-service:5432/bmidb
```

### Update Backend Configuration

```bash
# Edit backend configmap
nano kubernetes/manifests/backend-configmap.yaml
```

Update with your domain:

```yaml
data:
  NODE_ENV: "production"
  PORT: "3000"
  FRONTEND_URL: "https://bmi.yourdomain.com"
```

### Update Image References

```bash
# Edit backend deployment
nano kubernetes/manifests/backend-deployment.yaml
```

Change image:

```yaml
spec:
  containers:
  - name: backend
    image: yourusername/bmi-backend:v1.0.0  # Update this
```

```bash
# Edit frontend deployment
nano kubernetes/manifests/frontend-deployment.yaml
```

Change image:

```yaml
spec:
  containers:
  - name: frontend
    image: yourusername/bmi-frontend:v1.0.0  # Update this
```

---

## Deploy PostgreSQL

### Step 1: Create Namespace

```bash
kubectl apply -f kubernetes/manifests/namespace.yaml

# Verify
kubectl get namespace bmi-health-tracker
```

### Step 2: Create Persistent Volume Claim

```bash
kubectl apply -f kubernetes/manifests/postgres-pvc.yaml

# Verify PVC is bound
kubectl get pvc -n bmi-health-tracker
```

If PVC is pending, check storage class:

```bash
# List available storage classes
kubectl get storageclass

# If needed, edit PVC to use available storage class
kubectl edit pvc postgres-pvc -n bmi-health-tracker
```

### Step 3: Create Secret

```bash
kubectl apply -f kubernetes/manifests/postgres-secret.yaml

# Verify (values should be base64 encoded)
kubectl get secret postgres-secret -n bmi-health-tracker -o yaml
```

### Step 4: Deploy PostgreSQL

```bash
kubectl apply -f kubernetes/manifests/postgres-deployment.yaml

# Watch deployment
kubectl get pods -n bmi-health-tracker -w
```

Wait for pod to be `Running`:

```
NAME                        READY   STATUS    RESTARTS   AGE
postgres-xxxxxxxxxx-xxxxx   1/1     Running   0          30s
```

### Step 5: Create Service

```bash
kubectl apply -f kubernetes/manifests/postgres-service.yaml

# Verify service
kubectl get svc postgres-service -n bmi-health-tracker
```

### Step 6: Initialize Database

```bash
# Run migration job
kubectl apply -f kubernetes/manifests/postgres-init-job.yaml

# Check job status
kubectl get jobs -n bmi-health-tracker

# View migration logs
kubectl logs job/postgres-init -n bmi-health-tracker
```

Should see: "Database migration completed successfully!"

### Verify PostgreSQL

```bash
# Connect to PostgreSQL pod
kubectl exec -it deployment/postgres -n bmi-health-tracker -- psql -U bmi_user -d bmidb

# Run test query
\dt
SELECT * FROM measurements;
\q
```

---

## Deploy Backend

### Step 1: Create ConfigMap

```bash
kubectl apply -f kubernetes/manifests/backend-configmap.yaml

# Verify
kubectl get configmap backend-config -n bmi-health-tracker -o yaml
```

### Step 2: Deploy Backend

```bash
kubectl apply -f kubernetes/manifests/backend-deployment.yaml

# Watch deployment
kubectl get pods -n bmi-health-tracker -l app=bmi-backend -w
```

Wait for 2 replicas to be running.

### Step 3: Create Service

```bash
kubectl apply -f kubernetes/manifests/backend-service.yaml

# Verify service
kubectl get svc backend-service -n bmi-health-tracker
```

### Verify Backend

```bash
# Check logs
kubectl logs -f deployment/bmi-backend -n bmi-health-tracker

# Port forward to test
kubectl port-forward svc/backend-service 3000:3000 -n bmi-health-tracker

# Test in another terminal
curl http://localhost:3000/health
# Should return: {"status":"ok","timestamp":"..."}
```

---

## Deploy Frontend

### Step 1: Deploy Frontend

```bash
kubectl apply -f kubernetes/manifests/frontend-deployment.yaml

# Watch deployment
kubectl get pods -n bmi-health-tracker -l app=bmi-frontend -w
```

### Step 2: Create Service

```bash
kubectl apply -f kubernetes/manifests/frontend-service.yaml

# Get service (wait for EXTERNAL-IP)
kubectl get svc frontend-service -n bmi-health-tracker -w
```

On cloud providers, an external IP/hostname will be assigned.

### Verify Frontend

```bash
# Get external IP
FRONTEND_IP=$(kubectl get svc frontend-service -n bmi-health-tracker -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

echo "Frontend URL: http://$FRONTEND_IP"

# Or for AWS ELB (hostname instead of IP)
FRONTEND_HOST=$(kubectl get svc frontend-service -n bmi-health-tracker -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')

echo "Frontend URL: http://$FRONTEND_HOST"
```

Open the URL in your browser!

---

## Configure Ingress

For production with custom domain and SSL/TLS:

### Prerequisites

```bash
# Install nginx ingress controller (if not already installed)
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/cloud/deploy.yaml

# Install cert-manager for SSL (optional)
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml
```

### Update Ingress Configuration

```bash
nano kubernetes/manifests/ingress.yaml
```

Change domain:

```yaml
spec:
  rules:
  - host: bmi.yourdomain.com  # Your actual domain
```

### Deploy Ingress

```bash
kubectl apply -f kubernetes/manifests/ingress.yaml

# Get ingress IP
kubectl get ingress bmi-ingress -n bmi-health-tracker
```

### Configure DNS

Point your domain to the Ingress IP/hostname:

```
A Record: bmi.yourdomain.com → <INGRESS_IP>
```

Or for AWS:

```
CNAME Record: bmi.yourdomain.com → <ELB_HOSTNAME>
```

---

## Verification

### Complete Health Check

```bash
# 1. Check all resources
kubectl get all -n bmi-health-tracker

# 2. Check pod status
kubectl get pods -n bmi-health-tracker -o wide

# 3. Check services
kubectl get svc -n bmi-health-tracker

# 4. Check persistent volumes
kubectl get pvc -n bmi-health-tracker

# 5. Test backend health
kubectl run curl --image=curlimages/curl -i --tty --rm \
  -- curl http://backend-service.bmi-health-tracker:3000/health

# 6. Test database connection
kubectl exec -it deployment/bmi-backend -n bmi-health-tracker -- \
  node -e "require('./src/db').query('SELECT 1').then(() => console.log('DB OK')).catch(e => console.error('DB Error:', e))"

# 7. Check logs for errors
kubectl logs deployment/bmi-backend -n bmi-health-tracker --tail=50
kubectl logs deployment/bmi-frontend -n bmi-health-tracker --tail=50
kubectl logs deployment/postgres -n bmi-health-tracker --tail=50
```

### Application Test

1. Open frontend URL in browser
2. Add a measurement
3. Check if it appears in the list
4. Verify calculations (BMI, BMR, calories)
5. Check if 30-day trend chart loads

---

## Troubleshooting

### Pod Not Starting

```bash
# Describe pod
kubectl describe pod <pod-name> -n bmi-health-tracker

# Check events
kubectl get events -n bmi-health-tracker --sort-by='.lastTimestamp'

# Common issues:
# - Image pull error: Check image name and registry credentials
# - CrashLoopBackOff: Check logs
# - Pending: Check resources and PVC binding
```

### Database Connection Issues

```bash
# Check PostgreSQL logs
kubectl logs deployment/postgres -n bmi-health-tracker

# Test connection from backend pod
kubectl exec -it deployment/bmi-backend -n bmi-health-tracker -- \
  nc -zv postgres-service 5432

# Check secret is mounted
kubectl exec deployment/bmi-backend -n bmi-health-tracker -- \
  printenv DATABASE_URL
```

### Service Not Accessible

```bash
# Check service endpoints
kubectl get endpoints -n bmi-health-tracker

# Test service from within cluster
kubectl run debug --image=busybox -i --tty --rm -- \
  wget -O- http://backend-service.bmi-health-tracker:3000/health
```

### Ingress Issues

```bash
# Check ingress controller
kubectl get pods -n ingress-nginx

# Check ingress status
kubectl describe ingress bmi-ingress -n bmi-health-tracker

# Check ingress logs
kubectl logs -n ingress-nginx deployment/ingress-nginx-controller
```

---

## Scaling

### Manual Scaling

```bash
# Scale backend
kubectl scale deployment bmi-backend --replicas=3 -n bmi-health-tracker

# Scale frontend
kubectl scale deployment bmi-frontend --replicas=3 -n bmi-health-tracker

# Verify
kubectl get pods -n bmi-health-tracker
```

### Horizontal Pod Autoscaler (HPA)

```bash
# Enable metrics server (if not installed)
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# Create HPA for backend
kubectl autoscale deployment bmi-backend \
  --cpu-percent=70 \
  --min=2 \
  --max=10 \
  -n bmi-health-tracker

# Create HPA for frontend
kubectl autoscale deployment bmi-frontend \
  --cpu-percent=70 \
  --min=2 \
  --max=10 \
  -n bmi-health-tracker

# Check HPA status
kubectl get hpa -n bmi-health-tracker
```

---

## Backup & Restore

### Backup PostgreSQL Data

```bash
# Create backup job
kubectl run postgres-backup \
  --image=postgres:15-alpine \
  --rm -i --tty \
  --env="PGPASSWORD=$(kubectl get secret postgres-secret -n bmi-health-tracker -o jsonpath='{.data.POSTGRES_PASSWORD}' | base64 -d)" \
  -- pg_dump -h postgres-service.bmi-health-tracker -U bmi_user bmidb > backup-$(date +%Y%m%d).sql
```

### Restore from Backup

```bash
# Restore database
kubectl run postgres-restore \
  --image=postgres:15-alpine \
  --rm -i --tty \
  --env="PGPASSWORD=$(kubectl get secret postgres-secret -n bmi-health-tracker -o jsonpath='{.data.POSTGRES_PASSWORD}' | base64 -d)" \
  -- psql -h postgres-service.bmi-health-tracker -U bmi_user bmidb < backup-20251212.sql
```

---

## Cleanup

### Remove Application

```bash
cd kubernetes/scripts
chmod +x cleanup.sh
./cleanup.sh
```

Or manually:

```bash
# Delete namespace (removes everything)
kubectl delete namespace bmi-health-tracker

# Verify
kubectl get all -n bmi-health-tracker
```

---

## Production Checklist

- [ ] Change all default passwords in secrets
- [ ] Use versioned Docker images (not :latest)
- [ ] Configure resource requests/limits
- [ ] Set up monitoring (Prometheus/Grafana)
- [ ] Configure log aggregation (ELK/Loki)
- [ ] Enable network policies
- [ ] Configure backup automation
- [ ] Set up SSL/TLS with cert-manager
- [ ] Configure DNS with your domain
- [ ] Test disaster recovery procedures
- [ ] Document runbook for operations team

---

## Support

For additional help:
- Check [AGENT.md](AGENT.md) for complete configuration reference
- Review [README.md](README.md) for quick start guide
- Check Kubernetes logs for errors
- Verify all configuration values are correct

