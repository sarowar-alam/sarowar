# Docker Image Scanner (Trivy) — Jenkins Pipeline

Short description
This Jenkins Groovy pipeline builds a Docker image from a Git repository, runs Trivy vulnerability scans against the built image (table, JSON and SARIF outputs), converts the JSON results into an Excel report, generates a simple HTML summary, and forces cleanup of the built image. The pipeline archives scan artifacts for download from Jenkins.

Table of contents
- [Requirements](#requirements)
- [Parameters](#parameters)
- [High-level flow](#high-level-flow)
- [Pipeline stages (detailed)](#pipeline-stages-detailed)
- [Artifacts produced](#artifacts-produced)
- [Build result behavior and exit codes](#build-result-behavior-and-exit-codes)
- [How to run / trigger](#how-to-run--trigger)
- [Environment & credentials required](#environment--credentials-required)
- [Troubleshooting & common fixes](#troubleshooting--common-fixes)
- [Notes, limitations & suggestions](#notes-limitations--suggestions)
- [Maintainers / Contact](#maintainers--contact)
- [License](#license)

## Requirements
- Jenkins (modern LTS recommended)
- Jenkins job configured to use this Groovy pipeline (Jenkinsfile or pipeline job)
- Agent/node with:
  - Docker CLI + Docker daemon access (required to build and remove images)
  - Trivy (installed and on PATH)
  - Python 3 and pip (or python + pip) to run the Excel conversion script; alternatively ensure `pandas` and `openpyxl` are available on agent
- Jenkins credentials:
  - `jenkins-git-creds` (used for Git checkout; replace if different)
- (Optional) network access from Jenkins agent to install Python packages and to access Docker registry if you push images (this pipeline does not push images by default)
- Common plugins used in most Jenkins installations:
  - Pipeline (workflow-aggregator)
  - Credentials Binding / Credentials
  - (archiveArtifacts is built-in to pipeline steps)

## Parameters
The pipeline defines the following build parameters:

- `branch` (string) — Git branch to checkout. Default: `ReleaseCandidate`
- `git_url` (string) — Git repository URL. Default: `git@github.com:my-git-hub/my-git-repo.git`
- `dockerfile_path` (string) — Path inside the repo where the Dockerfile sits (if not root). Default: `.`
- `image_name` (string) — Name (repository) used for the built image. Default: `xbox-rc-image-service` (should be lowercase)
- `build_method` (choice) — How to select Dockerfile:
  - `dockerfile_in_path` — use Dockerfile at `${dockerfile_path}/Dockerfile`
  - `root_dockerfile` — use the repository root `Dockerfile`
  Default choices: `dockerfile_in_path` or `root_dockerfile`
- `build_context_path` (string) — Path used as Docker build context. Default: `.`

Note: The pipeline references `env.build_context_path` when composing the docker build command; expected variable is `params.build_context_path` — see "Notes, limitations & suggestions" below.

## High-level flow
1. Checkout the Git repo into `source/` using `jenkins-git-creds`.
2. Build the Docker image following the selected `build_method`. Image tag used is `${image_name}:${BUILD_ID}`.
3. Pre-scan cleanup: clears Trivy cache (best-effort).
4. Run Trivy scan:
   - Console/table output (for human-readable logs)
   - `trivy-report.json` (JSON)
   - `trivy-report.sarif` (SARIF)
5. Convert JSON to Excel (`trivy-report.xlsx`) using an inline Python script (uses `pandas` + `openpyxl`)
6. Generate a minimal `trivy-report.html` summary
7. Force removal of the built Docker image (multiple attempts) to clean the agent
8. Post: archive artifacts (`trivy-report.*`), final cleanup verification; prints download URLs on success/failure/unstable outcomes

## Pipeline stages (detailed)
- Checkout
  - Uses `GitSCM` with `RelativeTargetDirectory: source` and `credentialsId: jenkins-git-creds`
  - On failure: sets `currentBuild.result = 'FAILURE'` and aborts

- Build Image
  - Assembles a Docker build command:
    - If `dockerfile_in_path` -> `docker build -f ${dockerfile_path}/Dockerfile -t ${image_name}:${BUILD_ID} ${build_context}`
    - If `root_dockerfile` -> `docker build -f Dockerfile -t ${image_name}:${BUILD_ID} ${build_context}`
  - Uses `dir("source/${params.dockerfile_path}") { sh "cd ../ && ...build..." }`
  - On failure: marks build FAILURE and stops

- Pre-Scan Setup
  - Runs `trivy --clear-cache` (best-effort)

- Security Scan
  - Invokes:
    - `trivy image --format table --exit-code 0 ${image_name}:${BUILD_ID}`
    - `trivy image --format json --output trivy-report.json ${image_name}:${BUILD_ID}`
    - `trivy image --format sarif --output trivy-report.sarif ${image_name}:${BUILD_ID}`
  - On exceptions: catches and sets `currentBuild.result = 'UNSTABLE'` (pipeline continues)

- Generate Excel Report
  - Creates `convert_to_excel.py` that:
    - Reads `trivy-report.json`
    - Extracts vulnerability fields into a DataFrame
    - Writes `trivy-report.xlsx` with sheets: `All_Vulnerabilities`, `Severity_Summary`, `Package_Summary`, `Metadata`
  - Installs Python dependencies (attempts `pip3` / `pip` installs for `pandas` and `openpyxl`)
  - Exports `IMAGE_NAME` environment variable for the script
  - Runs the script; failure will set `currentBuild.result = 'UNSTABLE'` but the pipeline continues

- Generate HTML Report
  - Emits a simple `trivy-report.html` that links the artifacts and shows basic summary information

- Force Image Cleanup
  - Attempts `docker rmi -f ${image_name}:${BUILD_ID}` twice and also attempts `docker rmi -f ${image_name}:latest`
  - Logs warnings if image still present

- Post (always / success / failure / unstable)
  - `always`: archive artifacts `trivy-report.*`, remove temporary files, check for lingering images
  - `success`: prints artifact URLs and status
  - `failure`: prints failure summary and status
  - `unstable`: prints unstable summary and status

## Artifacts produced
- `trivy-report.json` — full Trivy JSON report
- `trivy-report.sarif` — SARIF formatted report suitable for code-scanning tools or code host upload
- `trivy-report.xlsx` — Excel report containing vulnerability list and summary sheets (produced by Python script)
- `trivy-report.html` — a basic HTML summary page
All are archived by `archiveArtifacts artifacts: 'trivy-report.*'` and available at `${BUILD_URL}artifact/`

## Build result behavior and exit codes
- Checkout/Build failures: Pipeline is marked `FAILURE` and aborts at that stage
- Trivy or report generation failures: pipeline marks `UNSTABLE` and continues (attempts to produce and archive whatever reports exist)
- On success: artifacts are archived and console prints direct download links
- Forced cleanup always runs to remove created image(s)

## How to run / trigger
- If your job is parameterized, use "Build with Parameters" in the Jenkins UI and set:
  - `branch`, `git_url`, `dockerfile_path`, `image_name`, `build_method`, `build_context_path`
- Multi-branch or pipeline in repo: ensure Jenkinsfile references this script or the pipeline is configured correctly
- Example values for a build:
  - `branch`: `main`
  - `git_url`: `git@github.com:your-org/your-repo.git`
  - `dockerfile_path`: `YourServiceName` (if Dockerfile is at `YourServiceName/Dockerfile`)
  - `image_name`: `myorg-image-service`
  - `build_method`: `dockerfile_in_path`
  - `build_context_path`: `.`

## Environment & credentials required
- `jenkins-git-creds` credential ID must exist in Jenkins or update the pipeline to use your credential id
- Trivy available on the agent: `trivy --version`
- Docker CLI/daemon available and accessible by Jenkins agent (privileged or Docker-in-Docker)
- Python 3 and pip, or preinstalled `pandas` and `openpyxl` on agent images
- Network access to install Python packages if not preinstalled
- Ensure agent has permission to run `docker` and `trivy` commands (user in docker group or using privileged executor)

## Troubleshooting & common fixes
- "Checkout failed": verify `jenkins-git-creds`, repository URL, and that Jenkins can reach the Git host (SSH keys, firewall).
- "Docker build failed":
  - Ensure Docker daemon is running and accessible to the Jenkins user.
  - Check `dockerfile_path` and `build_context_path` values — they must match repo structure.
  - Run the same `docker build` command locally to reproduce.
- "Trivy not found" or "trivy --version" fails:
  - Install Trivy on the agent, or use an agent Docker image that includes it.
- "Excel conversion failed" or missing `pandas`:
  - Preinstall `pandas` + `openpyxl` on the agent or allow pipeline to install them (may require internet and permissions).
  - Check that Python executable is available as `python3` or `python`.
- Archived artifacts missing:
  - Confirm `archiveArtifacts` ran and that files are present in the workspace when archiving runs (post stage uses `trivy-report.*`).
- Docker image removal fails:
  - The pipeline tries forced removal; however running containers or other processes referencing the image may prevent deletion.

## Notes, limitations & suggestions
- Potential parameter bug:
  - The pipeline builds the Docker command using `${env.build_context_path}` but the parameter is defined as `build_context_path` (a parameter, accessible via `params.build_context_path`). The pipeline author likely intended `params.build_context_path`. Consider updating the Groovy script to use `params.build_context_path` to avoid unexpected empty values.
- Build context & directory chdirs:
  - The `dir("source/${params.dockerfile_path}") { sh "cd ../ && docker build ... " }` logic is unusual: the step changes into `source/<dockerfile_path>` then does `cd ../`, effectively using the repository root as working directory. Verify this matches your expected docker build context and adjust if needed.
- Running Docker on Jenkins agent:
  - Best practice is to use a dedicated build node with Docker, or run builds inside a Docker-in-Docker (DinD) executor image.
- Python package installs:
  - Installing packages at runtime can be flaky (network, permissions). Prefer pre-baked agent images with required Python packages.
- SARIF output:
  - `trivy-report.sarif` allows integration with code scanning platforms that accept SARIF. Consider uploading SARIF to GitHub code scanning or other tools if required.

## Security & best practices
- Do not store secrets in the repository — use Jenkins credentials.
- Limit agent access and avoid running builds with unnecessary privileges.
- Prefer scanning built images before pushing to public registries.
- Consider signing or verifying images before deploying.

## Example console snippet (what the pipeline runs)
A representative Trivy scan command executed by the pipeline:
```bash
trivy image --format json --output trivy-report.json my-image:123
trivy image --format sarif --output trivy-report.sarif my-image:123
```
Excel conversion execution attempt (agent runs):
```bash
python3 convert_to_excel.py
```

## Maintainers / Contact
- Pipeline author: (please add name / team)
- Repo: update the `git_url` parameter to point at your repository and ensure `jenkins-git-creds` is valid

## License
- Add the project license you prefer (e.g., MIT). If none, add a LICENSE file or note repository policy.
