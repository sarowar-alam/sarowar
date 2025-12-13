#!/bin/bash

# Cleanup BMI Health Tracker Kubernetes deployment

set -e

NAMESPACE="bmi-health-tracker"

echo "Cleaning up BMI Health Tracker from Kubernetes"
echo "=========================================="
echo "Namespace: $NAMESPACE"
echo ""

read -p "Are you sure you want to delete ALL resources? This cannot be undone! (yes/no) " -r
echo
if [[ ! $REPLY =~ ^yes$ ]]; then
    echo "Cleanup cancelled."
    exit 0
fi

echo "Deleting all resources in namespace $NAMESPACE..."
kubectl delete namespace $NAMESPACE

echo ""
echo "Cleanup complete!"
echo "All resources have been removed from the cluster."
