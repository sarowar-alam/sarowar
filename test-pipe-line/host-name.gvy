pipeline {
    agent any
    stages {
        stage('Get Jenkins Hostname') {
            steps {
                script {
                    def hostName = InetAddress.localHost.hostName
                    echo "Jenkins Hostname: ${hostName}"
                }
            }
        }
    }
}
