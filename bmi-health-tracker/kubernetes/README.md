# BMI Health Tracker - Kubernetes Deployment

Deploy the BMI & Health Tracker application on any Kubernetes cluster (AWS EKS, GKE, Azure AKS, or on-premise).

## Quick Start

### Prerequisites
- Kubernetes cluster (v1.24+)
- kubectl configured
- Docker for building images
- 5GB+ storage available

### Deploy in 3 Steps

```bash
# 1. Build and push Docker images
cd kubernetes/scripts
chmod +x build-images.sh
./build-images.sh

# 2. Update configuration
# Edit manifests/postgres-secret.yaml - Change passwords
# Edit manifests/backend-configmap.yaml - Set your domain
# Edit manifests/backend-deployment.yaml - Update image registry
# Edit manifests/frontend-deployment.yaml - Update image registry

# 3. Deploy to cluster
chmod +x deploy.sh
./deploy.sh
```

## Architecture

```
┌─────────────────────────────────────────────────┐
│                   Ingress                        │
│          (Optional - with SSL/TLS)               │
└────────────┬──────────────┬─────────────────────┘
             │              │
     ┌───────▼──────┐  ┌────▼─────────┐
     │   Frontend   │  │   Backend    │
     │  Service:80  │  │ Service:3000 │
     └───────┬──────┘  └────┬─────────┘
             │              │
     ┌───────▼──────┐  ┌────▼─────────┐
     │   Frontend   │  │   Backend    │
     │ Deployment   │  │  Deployment  │
     │  (2 replicas)│  │ (2 replicas) │
     └──────────────┘  └────┬─────────┘
                            │
                       ┌────▼─────────┐
                       │  PostgreSQL  │
                       │ Service:5432 │
                       └────┬─────────┘
                            │
                       ┌────▼─────────┐
                       │  PostgreSQL  │
                       │  Deployment  │
                       │   + PVC 5GB  │
                       └──────────────┘
```

## Directory Structure

```
kubernetes/
├── manifests/              # Kubernetes YAML manifests
│   ├── namespace.yaml
│   ├── postgres-pvc.yaml
│   ├── postgres-secret.yaml
│   ├── postgres-deployment.yaml
│   ├── postgres-service.yaml
│   ├── postgres-init-job.yaml
│   ├── backend-configmap.yaml
│   ├── backend-deployment.yaml
│   ├── backend-service.yaml
│   ├── frontend-deployment.yaml
│   ├── frontend-service.yaml
│   └── ingress.yaml
├── scripts/                # Deployment scripts
│   ├── build-images.sh     # Build Docker images
│   ├── deploy.sh           # Deploy to cluster
│   └── cleanup.sh          # Remove all resources
├── Dockerfile.backend      # Backend container image
├── Dockerfile.frontend     # Frontend container image
├── nginx.conf              # Nginx configuration for frontend
├── README.md               # This file
├── DEPLOYMENT.md           # Detailed deployment guide
└── AGENT.md                # Complete reconstruction guide
```

## Key Features

- **High Availability**: 2 replicas for frontend and backend
- **Auto-scaling ready**: Configure HPA for dynamic scaling
- **Health checks**: Liveness and readiness probes
- **Persistent storage**: PostgreSQL with 5GB PVC
- **Security**: Non-root containers, resource limits
- **SSL/TLS**: Ingress with cert-manager support
- **Cloud agnostic**: Works on any Kubernetes cluster

## Access Application

After deployment, get the external IP:

```bash
# LoadBalancer service
kubectl get svc frontend-service -n bmi-health-tracker

# Or with Ingress
kubectl get ingress bmi-ingress -n bmi-health-tracker
```

## Monitoring

```bash
# Check all resources
kubectl get all -n bmi-health-tracker

# View logs
kubectl logs -f deployment/bmi-backend -n bmi-health-tracker
kubectl logs -f deployment/bmi-frontend -n bmi-health-tracker
kubectl logs -f deployment/postgres -n bmi-health-tracker

# Check pod status
kubectl describe pod <pod-name> -n bmi-health-tracker
```

## Cleanup

```bash
cd kubernetes/scripts
./cleanup.sh
```

## Documentation

- **[DEPLOYMENT.md](DEPLOYMENT.md)** - Detailed step-by-step deployment guide
- **[AGENT.md](AGENT.md)** - Complete Kubernetes configuration reference

## Support

For issues or questions, refer to the detailed guides in the documentation folder.
