pipeline {
    agent any

    //tools {
        //jdk 'JDK 17' // Ensure JDK 17 is configured in Jenkins tools
        //maven 'Maven 3.8.4' // Ensure Maven is configured in Jenkins tools
    //}

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean install'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test'
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
        }
        success {
            echo 'Build completed successfully.'
        }
        failure {
            echo 'Build failed.'
        }
    }
}
