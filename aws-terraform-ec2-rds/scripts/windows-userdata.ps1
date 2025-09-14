<powershell>
# Enable WinRM for Ansible
Enable-PSRemoting -Force
Set-Item WSMan:\localhost\Client\TrustedHosts * -Force

# Configure WinRM
winrm quickconfig -q
winrm set winrm/config/service '@{AllowUnencrypted="true"}'
winrm set winrm/config/service/auth '@{Basic="true"}'

# Set execution policy
Set-ExecutionPolicy RemoteSigned -Force

# Install AWS CLI
Invoke-WebRequest -Uri "https://awscli.amazonaws.com/AWSCLIV2.msi" -OutFile "$env:TEMP\AWSCLIV2.msi"
Start-Process msiexec.exe -ArgumentList "/i $env:TEMP\AWSCLIV2.msi /quiet /norestart" -Wait

# Install Git Bash
Invoke-WebRequest -Uri "https://github.com/git-for-windows/git/releases/download/v2.43.0.windows.1/Git-2.43.0-64-bit.exe" -OutFile "$env:TEMP\Git-Installer.exe"
Start-Process "$env:TEMP\Git-Installer.exe" -ArgumentList "/VERYSILENT /NORESTART /NOCANCEL /SP- /CLOSEAPPLICATIONS /RESTARTAPPLICATIONS /COMPONENTS=""icons,ext\reg\shellhere,assoc,assoc_sh""" -Wait

# Add Git Bash to system PATH
$gitPath = "C:\Program Files\Git\bin"
$currentPath = [Environment]::GetEnvironmentVariable("PATH", "Machine")
if ($currentPath -notlike "*$gitPath*") {
    $newPath = $currentPath + ";" + $gitPath
    [Environment]::SetEnvironmentVariable("PATH", $newPath, "Machine")
    $env:PATH += ";" + $gitPath
}

# Install SQL Server Management Studio
Invoke-WebRequest -Uri "https://aka.ms/ssmsfullsetup" -OutFile "$env:TEMP\SSMS-Setup.exe"
Start-Process "$env:TEMP\SSMS-Setup.exe" -ArgumentList "/install /quiet /norestart" -Wait

# Create scripts directory
New-Item -ItemType Directory -Path "C:\scripts" -Force

# Verify installations
Write-Output "Installations completed:"
Write-Output "- AWS CLI installed"
Write-Output "- Git Bash installed and added to PATH"
Write-Output "- SSMS installation initiated"
</powershell>