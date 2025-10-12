# 🚀 Argo CD NGINX GitOps Project

This repository demonstrates a **GitOps-based deployment pipeline** using **Argo CD** and **Amazon EKS** to manage a Kubernetes NGINX application. The project showcases how Argo CD continuously monitors your Git repository, detects any manifest changes, and automatically applies them to your Kubernetes cluster — ensuring full synchronization between Git and the running environment.

---

## 🌟 Features & Functionalities

### 🧩 1. GitOps-Driven Deployment
- The `nginx-demo` application is automatically deployed using **Argo CD**.
- Any changes committed to the GitHub repository (YAML updates, replica count, etc.) are automatically synced to the cluster.

### ⚙️ 2. Declarative Infrastructure
- Kubernetes resources (Namespace, Deployment, Service) are defined in YAML under `nginx/`.
- Argo CD’s `Application` manifest (`argo-applications/nginx-app.yaml`) declares how to deploy and manage them.

### ☁️ 3. Amazon EKS Cluster Management
- The `aws-cluster.yaml` file defines the EKS cluster configuration using **eksctl**.
- Easily create or manage a Kubernetes cluster in AWS with autoscaling and managed node groups.

### 🛡️ 4. Self-Healing and Auto-Sync
- Argo CD is configured with **self-heal** and **auto-sync**, meaning it restores drifted states automatically.
- Includes retry logic and hard refresh annotations for resilience and instant updates.

### 📦 5. Efficient Deployment Configuration
- Deployment runs with **RollingUpdate** strategy for zero downtime.
- Includes **liveness** and **readiness probes**, resource limits, and restart policies.

### 🧰 6. Multi-Access Setup for Argo CD UI
- Supports **LoadBalancer**, **NodePort**, or **Port Forwarding** modes for flexible access.
- CLI installation instructions provided for both Windows and Linux users.

### 📊 7. Real-Time Synchronization & Testing
- Git-based update testing workflow (e.g., changing replicas count).
- Demonstrates the full GitOps feedback loop (Git → ArgoCD → Kubernetes).

---

## 🪜 Implementation Steps

### 🧱 Phase 1: Create EKS Cluster
```bash
eksctl create cluster -f aws-cluster.yaml --profile ostad
```

### 🚀 Phase 2: Install Argo CD
```bash
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=argocd-server -n argocd --timeout=300s
kubectl get pods -n argocd
```

### 🌐 Phase 3: Expose Argo CD Service
Option 1: LoadBalancer (Cloud)
```bash
kubectl patch svc argocd-server -n argocd -p '{"spec":{"type":"LoadBalancer"}}'
kubectl get svc argocd-server -n argocd -w
```

Option 2: NodePort (On-prem)
```bash
kubectl patch svc argocd-server -n argocd -p '{"spec":{"type":"NodePort"}}'
kubectl get svc argocd-server -n argocd
```

Option 3: Port Forward (Local)
```bash
kubectl port-forward svc/argocd-server -n argocd 8080:443
# Access at: https://localhost:8080
```

### 🔑 Phase 4: Login to Argo CD
```bash
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d
argocd login <ARGOCD_SERVER_URL>:80
# Username: admin
# Password: <from previous command>
```

### 🧠 Phase 5: Deploy NGINX Application via Argo CD
```bash
kubectl apply -f argo-applications/nginx-app.yaml
argocd app get nginx-demo
argocd app sync nginx-demo
kubectl -n nginx-demo get all
```

### 🌍 Phase 6: Test and Port-Forward
```bash
kubectl port-forward -n nginx-demo service/nginx-service 8082:80
# Access http://localhost:8082
```

### 🧪 Phase 7: Validate GitOps Flow
1. Change replicas count or image tag in `nginx/nginx-deployment.yaml`
2. Commit and push to GitHub
```bash
git add .
git commit -m "test: scale nginx replicas"
git push origin main
```
3. Argo CD will detect and automatically synchronize the change!

---

## 📁 Repository Structure

```
.
├── argo-applications/
│   └── nginx-app.yaml          # Argo CD Application definition
├── nginx/
│   ├── nginx-deployment.yaml   # NGINX deployment spec
│   ├── nginx-namespace.yaml    # Namespace definition
│   └── nginx-service.yaml      # Service exposure
├── aws-cluster.yaml            # eksctl configuration for EKS
└── installation.README.md      # Detailed installation instructions
```

---

## 🧩 Summary
✅ Full GitOps implementation using **Argo CD** and **EKS**  
✅ Self-healing deployments with **auto-sync** and **drift correction**  
✅ Declarative infrastructure using YAML manifests  
✅ Cloud-ready setup supporting LoadBalancer, NodePort, or PortForward access  
✅ End-to-end deployment workflow from **Git commit → Cluster deployment**  

---

## 🧑‍💻 Author
**Md. Sarowar Alam**  
Cloud & DevOps Engineer  
📧 Email: sarowar@hotmail.com  
🔗 LinkedIn: [linkedin.com/in/sarowar](https://www.linkedin.com/in/sarowar/)

---

> “If it’s not in Git, it doesn’t exist — GitOps makes deployments predictable, reliable, and fully auditable.”
