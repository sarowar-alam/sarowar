# Automate_Certificate_Renewal

A Jenkins-driven automation suite to check, renew, distribute, and notify about SSL/TLS certificates (Let's Encrypt via Posh-ACME), across AWS and Windows/Linux servers. The project combines Jenkins pipeline(s), PowerShell scripts, and Python utilities to manage the certificate lifecycle and deploy updates to IIS, Jenkins, and Zabbix services.

---

## Features

- Certificate validity checks
  - Uses `ssl.ps1` to query a domain's certificate and return days remaining.

- Certificate creation / renewal
  - `backup-create-cert.ps1` uses Posh-ACME (Route53 plugin) to request or renew certificates and emits a clear status (`[RESULT] NEW_CERT` / `[RESULT] REUSED_CERT`).

- AWS integration
  - Start EC2 instances when needed (`start-mainline-rc.py`).
  - Re-import updated certs into AWS Certificate Manager across regions (`update-aws-certificate.py`).
  - Upload zipped certificate artifacts to S3 and generate presigned URLs for secure sharing.

- Server deployments
  - Update Windows IIS servers remotely and manage bindings/cleanup (`update-iis-robust.ps1`).
  - Convert PFX → JKS and deploy to Jenkins on Windows (`update-jenkins-windows.ps1`).
  - Transfer and deploy JKS to Linux Jenkins via SSH (`update-jenkins-linux.py`).
  - Transfer and deploy certs to Zabbix servers and restart services (`update-zabbix-certificate.py`).

- Notifications
  - Email team recipients with S3 presigned download link via SES (`send_certificate_email.py`).
  - Failure notifications are sent from the pipeline post-failure block (dynamic SES Python script).

- Cleanup and robust error handling
  - Workspace cleanup in `post` stages, retries and explicit checks in scripts, and safe rollback/backups for critical files.

---

## Repository layout (key files)

- `automated-certificate-update.gvy` / `automate-certificate.gvy` — Jenkins pipeline(s)
- `backup-create-cert.ps1` — Create/renew cert via Posh-ACME
- `ssl.ps1` — Returns days until expiry for a host
- `start-mainline-rc.py` — Start EC2 instances and wait for status checks
- `update-aws-certificate.py` — Reimport certs into ACM
- `update-iis-robust.ps1` — Robust remote IIS certificate update
- `update-jenkins-windows.ps1` — PFX→JKS conversion and Jenkins deployment (Windows)
- `update-jenkins-linux.py` — Transfer and deploy JKS to Linux Jenkins
- `update-zabbix-certificate.py` — Deploy certs to Zabbix servers
- `send_certificate_email.py` — Zip certs, upload to S3, send SES email

---

## Quickstart / Implementation Guide

This guide assumes an operator with Jenkins admin access and AWS access to Route53, EC2, S3, ACM, and SES.

1. Prerequisites
   - Jenkins server with a Windows-capable agent (PowerShell, Python, and access to `%LOCALAPPDATA%`).
   - Python 3.x on the Jenkins agent and target Linux servers.
   - PowerShell 5.1+ on Windows agents and target servers.
   - Posh-ACME module installed on the Windows agent where certificates will be produced.
   - AWS CLI/boto3 available in the Python environment or proper IAM credentials configured.
   - 7-Zip installed on agent machine if zipping via `7z.exe` is used.
   - `keytool` (JDK) available where PFX→JKS conversion is performed.
   - Paramiko Python package on agent if using SSH transfers from Python scripts.

2. Jenkins setup
   - Create Jenkins credentials for AWS and machine accounts:
     - AWS credentials for actions (EC2 start, S3 upload, ACM import, SES send). Use Jenkins Credentials (username/password or AWS-specific plugin).
     - Windows remote admin credentials used for PowerShell remoting to IIS targets.
     - Jenkins service jks/pfx passwords stored as `secretText` or `username/password` according to your preference.

   - Copy/Import the pipeline (`automated-certificate-update.gvy`) into a Jenkins job (Pipeline job) or create a folder job. Update the `parameters` and `environment` blocks to reflect your domains, IPs, bucket names, and credential IDs.

3. Configure script paths and file locations
   - Ensure the `SCRIPTS` paths in the pipeline point to the correct relative locations in the workspace or absolute paths.
   - Ensure Posh-ACME stores certs under `%LOCALAPPDATA%\Posh-ACME\LE_PROD` as expected.

4. IAM / AWS Configuration
   - Ensure the AWS account used has permissions for EC2 (Start/Describe), Route53 (if using DNS validation), ACM (ImportCertificate), S3 (PutObject), and SES (SendEmail).
   - If using Route53 DNS validation, the AWS credentials must have access to the hosted zone.

5. Test locally and in dev
   - Run certificate check scripts (`ssl.ps1`) locally against a staging domain to verify output format (should print a single integer of days left).
   - Run `backup-create-cert.ps1` on a Windows machine with Posh-ACME configured to test certificate issuance.
   - Test the Python scripts (`start-mainline-rc.py`, `update-aws-certificate.py`, etc.) with test credentials and smaller scopes.

6. Dry-run and rollouts
   - Consider adding a `--dry-run` flag to scripts for non-destructive tests.
   - Run pipeline in a non-production branch or Jenkins folder first to confirm end-to-end behavior.

---

  ## Flow diagram

  If your Git hosting supports Mermaid (e.g., GitHub) the diagram below will render. A plain-text ASCII fallback follows for other environments.

  ```mermaid
  flowchart TD
    start([Start Jenkins Job])
    choose{Select DOMAIN}
    check["Check Certificate Validity\n(ssl.ps1)"]
    renew["Create / Renew Certificate"]
    acm["Update AWS ACM"]
    deploy["Deploy to Hosts"]
    iis[IIS]
    jwin["Jenkins - Windows"]
    jlin["Jenkins - Linux"]
    zbx[Zabbix]
    email["Upload ZIP to S3 & send email"]
    cleanup["Post: Cleanup workspace"]
    result{"Success or Failure"}
    finish([Finish])
    fail["Send Failure Email & Logs"]

    start --> choose
    choose --> check
    check -- "needs renewal" --> renew
    check -- "valid" --> finish
    renew --> acm
    acm --> deploy
    deploy --> iis
    deploy --> jwin
    deploy --> jlin
    deploy --> zbx
    acm --> email
    deploy --> cleanup
    cleanup --> result
    result -- "Success" --> finish
    result -- "Failure" --> fail
  ```

  ASCII fallback:

  ```
  Start Jenkins Job
    └─> Select DOMAIN
     └─> Check Certificate Validity (ssl.ps1)
      ├─ if valid ----> Finish
      └─ if needs renewal ---> Create/Renew Cert (backup-create-cert.ps1)
                   └─> Update ACM (update-aws-certificate.py)
                     ├─> Deploy to Hosts
                     │     ├─> IIS (update-iis-robust.ps1)
                     │     ├─> Jenkins Windows (update-jenkins-windows.ps1)
                     │     ├─> Jenkins Linux (update-jenkins-linux.py)
                     │     └─> Zabbix (update-zabbix-certificate.py)
                     └─> Upload ZIP to S3 & send email (send_certificate_email.py)
                       └─> Post: Cleanup workspace
                         └─> Success or Failure
                           ├─> Finish
                           └─> Send Failure Email & Logs
  ```


## Configuration checklist

- [ ] Replace placeholder domain/IP/bucket names in the pipeline with real values.
- [ ] Add correct Jenkins credential IDs into the pipeline's `environment` block.
- [ ] Put required PEM keys on the Jenkins box or secure key store for SSH operations.
- [ ] Ensure `LOCALAPPDATA` has the Posh-ACME artifacts if generating certs on that box.
- [ ] Confirm SES sender and verified identities are present in the AWS region used.

---

## Security & best-practices

- Avoid passing AWS keys on the command line; prefer Jenkins credential bindings and environment injection.
- Use IAM least-privilege policies for the credentials used by the pipeline.
- Rotate PFX/JKS passwords periodically and store them in Jenkins Credentials as `secretText`/`usernamePassword`.
- Audit pipeline runs and S3 objects holding certificate zips; consider using short-lived presigned links only.

---

## Troubleshooting tips

- If expiry parsing fails: run `ssl.ps1` manually and confirm it prints a single integer line with days left.
- If Posh-ACME fails with rate-limit errors: check the `backup-create-cert.ps1` output for Let's Encrypt rate-limit messages and try staging or wait.
- If remote IIS update fails: ensure WinRM/PowerShell remoting is enabled on the target and the remote admin credentials are correct.
- If Jenkins restart fails after JKS replacement: inspect Jenkins logs for plugin/key errors; ensure keystore passwords match.

---

## Next improvements (recommended)

- Add a README with step-by-step environment provisioning scripts (e.g., install Posh-ACME, Python requirements file).
- Add unit tests or smoke tests for the Python scripts.
- Add a `--dry-run` mode for pipeline and scripts.
- Standardize output formats (JSON) for scripts to make parsing robust.

---

## License

Choose and add a license (e.g., MIT) before publishing to a public Git repo.

---

If you want, I can now:
- Validate the pipeline script names vs files and fix mismatches automatically.
- Add a small `requirements.txt` for Python dependencies (paramiko, boto3).
- Add a `dry-run` toggle to scripts.

Tell me which one you'd like next and I'll update the todo list and implement it.