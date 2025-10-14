# Jenkins CI/CD Pipeline for Docker Image Build & Security Scanning

This repository contains a Jenkins declarative pipeline (`Jenkinsfile`) that automates the process of building a Docker image, running a vulnerability scan with [Trivy](https://trivy.dev), generating reports (JSON, Excel, HTML, SARIF), and cleaning up Docker images after the build.

---

## 🚀 Features

- **Parameterized builds** for flexibility (branch, repo URL, image name, Dockerfile location, etc.)
- **Checkout** source code from Git with credentials management
- **Docker image build** with support for multiple build contexts:
  - `dockerfile_in_path`: Uses `YourService_Folder_Name/Dockerfile`
  - `root_dockerfile`: Uses root-level `Dockerfile`
- **Trivy security scan** with multiple output formats:
  - Console table view
  - JSON report
  - SARIF report
- **Excel report generation** using Python (pandas + openpyxl)
- **HTML summary report**
- **Forced Docker image cleanup** after each build (ensures no dangling images)
- **Post-build artifact archiving** for reports
- **Error handling** with `SUCCESS`, `FAILURE`, and `UNSTABLE` states

---

## 🛠️ Prerequisites

Before running this pipeline, ensure that:

1. **Jenkins requirements**
   - Jenkins installed with Docker and Git plugins
   - Jenkins agent has **Docker** installed and running
   - Jenkins agent has **Python3** + `pip` installed (for Excel report generation)
   - Jenkins has a configured **credential** for Git (replace `jenkins-credentials-manager-id` in the pipeline)

2. **Tools installed on Jenkins agent**
   - [Trivy](https://trivy.dev) (latest version)
   - Python3 with `pandas` and `openpyxl`

   ```bash
   pip3 install pandas openpyxl
   ```

---

## ⚙️ Pipeline Parameters

| Parameter        | Description |
|------------------|-------------|
| **branch**       | Git branch to checkout (default: `your_branch_name`) |
| **git_url**      | Git repository URL (default: `git@github.com:your-git/your-repo.git`) |
| **dockerfile_path** | Path to Dockerfile directory (e.g. `serviceA` → `serviceA/Dockerfile`) |
| **image_name**   | Docker image name (lowercase only, default: `mb-rc-image-service`) |
| **build_method** | Build strategy: `dockerfile_in_path` or `root_dockerfile` |

---

## 📋 Pipeline Stages

1. **Checkout** – Clones source code from GitHub into `source/`
2. **Build Image** – Builds Docker image using selected method (`dockerfile_in_path` or `root_dockerfile`)
3. **Pre-Scan Setup** – Clears Trivy cache
4. **Security Scan** – Runs Trivy scans and generates:
   - Console vulnerability report (table)
   - JSON (`trivy-report.json`)
   - SARIF (`trivy-report.sarif`)
5. **Generate Excel Report** – Converts JSON report into `trivy-report.xlsx` with multiple sheets:
   - All vulnerabilities
   - Severity summary
   - Package summary
   - Metadata
6. **Generate HTML Report** – Creates `trivy-report.html` with summary & download links
7. **Force Image Cleanup** – Removes Docker images (`image_name:BUILD_ID` and `latest` tag)
8. **Post-Build** – Archives reports as Jenkins artifacts

---

## 📊 Reports Generated

After successful (or unstable) builds, the following reports are available as Jenkins artifacts:

- **JSON** → `trivy-report.json`
- **Excel** → `trivy-report.xlsx`
- **HTML** → `trivy-report.html`
- **SARIF** → `trivy-report.sarif`

Download links are also echoed in the Jenkins console logs.

---

## ✅ Example Jenkins Console Links

After success, the console will show links like:

```
All Reports: http://<jenkins-url>/job/<job-name>/<build-id>/artifact/
Excel Report: http://<jenkins-url>/job/<job-name>/<build-id>/artifact/trivy-report.xlsx
HTML Report: http://<jenkins-url>/job/<job-name>/<build-id>/artifact/trivy-report.html
JSON Report: http://<jenkins-url>/job/<job-name>/<build-id>/artifact/trivy-report.json
SARIF Report: http://<jenkins-url>/job/<job-name>/<build-id>/artifact/trivy-report.sarif
```

---

## 🧹 Cleanup

- Docker images are always cleaned up in the **Force Image Cleanup** stage.
- Temporary files (Python scripts, HTML templates) are removed during post-build cleanup.

---

## 📌 Notes

- If Excel or HTML report generation fails, pipeline continues but marks build as **UNSTABLE**.
- Reports are always archived (if generated).
- Customize `jenkins-credentials-manager-id` with your actual Jenkins credential ID for Git access.

---

## 🔒 Security

- Trivy scans are performed locally in Jenkins agent
- No sensitive data is exposed in reports (only vulnerabilities & metadata)
---

## 🧑‍💻 Author
**Md. Sarowar Alam**  
Lead Engineer, Hogarth Worldwide  
📧 Email: sarowar@hotmail.com  
🔗 LinkedIn: [linkedin.com/in/sarowar](https://www.linkedin.com/in/sarowar/)

---