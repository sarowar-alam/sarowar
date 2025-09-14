pipeline {
    agent any
    stages {
        stage('Get Hostname via PowerShell') {
            steps {
                script {
                    // Run PowerShell command
                    def hostName = powershell(script: "hostname", returnStdout: true).trim()
                    echo "Jenkins Hostname (via PowerShell): ${hostName}"
                }
            }
        }
    }
}
