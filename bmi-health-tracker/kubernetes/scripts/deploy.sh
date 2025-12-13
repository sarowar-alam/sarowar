#!/bin/bash

# Deploy BMI Health Tracker to Kubernetes

set -e

NAMESPACE="bmi-health-tracker"
MANIFESTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../manifests" && pwd)"

echo "Deploying BMI Health Tracker to Kubernetes"
echo "=========================================="
echo "Namespace: $NAMESPACE"
echo "Manifests: $MANIFESTS_DIR"
echo ""

# Check if kubectl is installed
if ! command -v kubectl &> /dev/null; then
    echo "ERROR: kubectl is not installed!"
    exit 1
fi

# Check cluster connection
if ! kubectl cluster-info &> /dev/null; then
    echo "ERROR: Cannot connect to Kubernetes cluster!"
    exit 1
fi

echo "Connected to cluster:"
kubectl cluster-info | head -n 1
echo ""

# Create namespace
echo "[1/10] Creating namespace..."
kubectl apply -f "$MANIFESTS_DIR/namespace.yaml"

# Deploy PostgreSQL
echo "[2/10] Creating PostgreSQL PVC..."
kubectl apply -f "$MANIFESTS_DIR/postgres-pvc.yaml"

echo "[3/10] Creating PostgreSQL secret..."
kubectl apply -f "$MANIFESTS_DIR/postgres-secret.yaml"

echo "[4/10] Deploying PostgreSQL..."
kubectl apply -f "$MANIFESTS_DIR/postgres-deployment.yaml"

echo "[5/10] Creating PostgreSQL service..."
kubectl apply -f "$MANIFESTS_DIR/postgres-service.yaml"

# Wait for PostgreSQL to be ready
echo "[6/10] Waiting for PostgreSQL to be ready..."
kubectl wait --for=condition=available --timeout=120s \
    deployment/postgres -n $NAMESPACE

# Run database migration
echo "[7/10] Running database migration..."
kubectl apply -f "$MANIFESTS_DIR/postgres-init-job.yaml"
kubectl wait --for=condition=complete --timeout=60s \
    job/postgres-init -n $NAMESPACE

# Deploy Backend
echo "[8/10] Deploying backend..."
kubectl apply -f "$MANIFESTS_DIR/backend-configmap.yaml"
kubectl apply -f "$MANIFESTS_DIR/backend-deployment.yaml"
kubectl apply -f "$MANIFESTS_DIR/backend-service.yaml"

# Wait for backend to be ready
kubectl wait --for=condition=available --timeout=120s \
    deployment/bmi-backend -n $NAMESPACE

# Deploy Frontend
echo "[9/10] Deploying frontend..."
kubectl apply -f "$MANIFESTS_DIR/frontend-deployment.yaml"
kubectl apply -f "$MANIFESTS_DIR/frontend-service.yaml"

# Wait for frontend to be ready
kubectl wait --for=condition=available --timeout=120s \
    deployment/bmi-frontend -n $NAMESPACE

# Deploy Ingress (optional)
echo "[10/10] Deploying Ingress..."
if [ -f "$MANIFESTS_DIR/ingress.yaml" ]; then
    kubectl apply -f "$MANIFESTS_DIR/ingress.yaml"
else
    echo "Ingress file not found, skipping..."
fi

echo ""
echo "=========================================="
echo "Deployment Complete!"
echo "=========================================="
echo ""
echo "Check status:"
echo "  kubectl get all -n $NAMESPACE"
echo ""
echo "Get service URL:"
echo "  kubectl get svc frontend-service -n $NAMESPACE"
echo ""
echo "View logs:"
echo "  kubectl logs -f deployment/bmi-backend -n $NAMESPACE"
echo "  kubectl logs -f deployment/bmi-frontend -n $NAMESPACE"
echo ""
