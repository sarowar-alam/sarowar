# Phase 1: Initial Installation
# 1. Create a dedicated namespace for Argo CD
kubectl create namespace argocd
# 2. Install Argo CD using the official manifest
# This deploys all Argo CD components (server, repo server, controller, etc.)
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
# Use Case: Setting up the core Argo CD platform for GitOps-based Kubernetes deployments.

# Phase 2: Wait for Pods to Be Ready
# 3. Wait for argocd-server pod to be fully ready (critical for next steps)
# This ensures the API server is ready before we try to access it
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=argocd-server -n argocd --timeout=300s
# 4. Verify all pods are running properly
kubectl get pods -n argocd
# Use Case: Ensuring all components are healthy before proceeding with configuration.


# Phase 3: Service Exposure Methods

# Option A: LoadBalancer (Recommended for Cloud Providers)
# 5. Change service type from ClusterIP to LoadBalancer
# This automatically provisions an external load balancer (cloud providers)
kubectl patch svc argocd-server -n argocd -p '{"spec":{"type":"LoadBalancer"}}'
# 6. Wait for the external IP to be assigned
kubectl get svc argocd-server -n argocd -w
# Press Ctrl+C once you see the external IP
# Use Case: When running in cloud environments (AWS, GCP, Azure) and you want automatic external access.

# Option B: NodePort (For On-Prem/Bare Metal)
# Alternative: Use NodePort for on-premise clusters
kubectl patch svc argocd-server -n argocd -p '{"spec":{"type":"NodePort"}}'
kubectl get svc argocd-server -n argocd
# Use Case: Bare metal or on-premise Kubernetes clusters without cloud load balancers.

# Option C: Port Forwarding (Development/Quick Access)
# Simple port forwarding for immediate access (blocks terminal)
kubectl port-forward svc/argocd-server -n argocd 8080:443
# Access at: https://localhost:8080
# Use Case: Development environments or quick testing without changing service types.


# Phase 4: Retrieve Admin Credentials
# 7. Get the initial auto-generated admin password
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d
echo
# Use Case: First-time login to Argo CD web UI. Username is admin, password from above command.

# Phase 5: Argo CD CLI Setup (Optional but Recommended)
# For Windows - Download and install Argo CD CLI
Invoke-WebRequest -Uri https://github.com/argoproj/argo-cd/releases/latest/download/argocd-windows-amd64.exe -OutFile argocd.exe
move .\argocd.exe "C:\Windows\System32"
# Verify installation
argocd version
# Use Case: Command-line management of Argo CD, automation, and advanced operations.


# Set ArgoCD Server Address
# If ArgoCD is running on the same cluster
argocd login af1515d25d46f4a3ab1f6ef26946c561-431926638.ap-south-1.elb.amazonaws.com:80
# WARNING: server certificate had error: tls: failed to verify certificate: x509: certificate signed by unknown authority. Proceed insecurely (y/n)? y
Username: admin
Password:
'admin:login' logged in successfully
# Context 'af1515d25d46f4a3ab1f6ef26946c561-431926638.ap-south-1.elb.amazonaws.com:80' updated

# Or if using port-forward (most common)
kubectl port-forward svc/argocd-server -n argocd 8080:443 &
argocd login localhost:8080
# Or if you have an ingress/LB
argocd login your-argocd-domain.com


# Deployment Commands:
# Apply Argo CD application
kubectl apply -f argo-applications/nginx-app.yaml
# Verify deployment
argocd app get nginx-demo
argocd app sync nginx-demo
# Check resources
kubectl -n nginx-demo get all