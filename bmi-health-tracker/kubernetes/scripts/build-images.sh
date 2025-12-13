#!/bin/bash

# Build and push Docker images for Kubernetes deployment

set -e

# Configuration
REGISTRY="your-docker-registry"  # Change this to your Docker registry
VERSION="latest"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

echo "Building BMI Health Tracker Docker Images"
echo "=========================================="
echo "Registry: $REGISTRY"
echo "Version: $VERSION"
echo "Project Root: $PROJECT_ROOT"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "ERROR: Docker is not running!"
    exit 1
fi

# Build Backend Image
echo "[1/2] Building backend image..."
docker build \
    -f "$PROJECT_ROOT/kubernetes/Dockerfile.backend" \
    -t "$REGISTRY/bmi-backend:$VERSION" \
    "$PROJECT_ROOT"

echo "Backend image built: $REGISTRY/bmi-backend:$VERSION"
echo ""

# Build Frontend Image
echo "[2/2] Building frontend image..."
docker build \
    -f "$PROJECT_ROOT/kubernetes/Dockerfile.frontend" \
    -t "$REGISTRY/bmi-frontend:$VERSION" \
    "$PROJECT_ROOT"

echo "Frontend image built: $REGISTRY/bmi-frontend:$VERSION"
echo ""

# Push images
echo "Pushing images to registry..."
read -p "Do you want to push images to registry? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Pushing backend image..."
    docker push "$REGISTRY/bmi-backend:$VERSION"
    
    echo "Pushing frontend image..."
    docker push "$REGISTRY/bmi-frontend:$VERSION"
    
    echo ""
    echo "Images pushed successfully!"
else
    echo "Skipping image push."
fi

echo ""
echo "Build complete!"
echo "Backend: $REGISTRY/bmi-backend:$VERSION"
echo "Frontend: $REGISTRY/bmi-frontend:$VERSION"
