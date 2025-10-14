# Docker Image Scanner — Jenkins + Trivy

A compact Jenkins pipeline (`image-scanner.gvy`) that automates building a Docker image, running Trivy vulnerability scans, and producing shareable reports.

## What it does (brief)

- Checks out code from a specified Git branch.
- Builds a Docker image tagged as `<image_name>:<BUILD_ID>` (supports service-level Dockerfiles or root Dockerfile).
- Runs Trivy scans and saves results as:
  - `trivy-report.json` (JSON)
  - `trivy-report.sarif` (SARIF)
  - Console table output for quick review
- Converts Trivy JSON into `trivy-report.xlsx` (Excel) with summary sheets (severity, package counts, metadata).
- Generates a lightweight `trivy-report.html` linking to archived artifacts.
- Archives artifacts in Jenkins and enforces forced cleanup of built images.

## Quick prerequisites

- Jenkins server and an agent/node with:
  - Docker CLI and access to Docker daemon
  - Trivy installed and on PATH
  - Python 3 and pip (or preinstalled `pandas` + `openpyxl`)
- Jenkins credential for Git access (the script uses `jenkins-git-creds` by default)

## Installation & Jenkins configuration (step-by-step)

1. Add the repository to Jenkins (or use a Pipeline job):
   - Option A — Pipeline script from SCM:
     - SCM: Git
     - Repository URL: set your repo
     - Branch Specifier: `${branch}`
     - Script Path: `image-scanner.gvy`
   - Option B — Create a Pipeline job and paste `image-scanner.gvy` into the Pipeline script box.

2. Ensure Jenkins has the Git credential configured with the ID used in the script (`jenkins-git-creds`) or update the `credentialsId` inside `image-scanner.gvy` to your credential.

3. Confirm the agent(s) running the pipeline have `docker`, `trivy`, and `python3` available. If using containerized agents, mount the Docker socket or use DinD appropriately.

4. (Optional but recommended) Pre-install Python packages `pandas` and `openpyxl` on the agent to avoid runtime installation issues.

## Pipeline parameters (what to set)

- `branch` — Git branch to checkout (default: `ReleaseCandidate`).
- `git_url` — Git repository URL.
- `dockerfile_path` — Path to the Dockerfile inside the repo (set service folder or `.` for root).
- `image_name` — Image name (should be lowercase).
- `build_method` — `dockerfile_in_path` or `root_dockerfile`.
- `build_context_path` — Docker build context path (default: `.`).

Note: The pipeline uses `params.*` values; review `image-scanner.gvy` if you need different credential IDs or agent labels.

## Artifacts produced

- trivy-report.json — Full JSON output from Trivy
- trivy-report.sarif — SARIF output for security tooling
- trivy-report.xlsx — Excel workbook (All vulnerabilities, Severity_Summary, Package_Summary, Metadata)
- trivy-report.html — Lightweight HTML summary

All artifacts are archived to Jenkins build artifacts and available via `${BUILD_URL}artifact/`.

## Quick troubleshooting

- Docker build fails: verify `dockerfile_path`, `build_context_path`, and Docker daemon access on agent.
- Trivy missing: install Trivy or run on an agent that includes it.
- Excel conversion errors: ensure Python3 and `pandas`/`openpyxl` are installed (the pipeline attempts to install them but preinstallation is more reliable).
- Image cleanup issues: other processes or containers may hold image references; check agent processes.

## Customization ideas

- Fail the build if vulnerabilities of defined severities are detected.
- Push the image to a registry for downstream stages.
- Publish SARIF to code scanning services.

---

## 🧑‍💻 Author
**Md. Sarowar Alam**  
Lead DevOps Engineer, Hogarth Worldwide  
📧 Email: sarowar@hotmail.com  
🔗 LinkedIn: [linkedin.com/in/sarowar](https://www.linkedin.com/in/sarowar/)

---