pipeline {
    agent any

    environment {
        // Define environment variables
        MVN_HOME = '/opt/maven'  // Adjust this if your Maven is installed elsewhere
        JAVA_HOME = '/usr/lib/jvm/java-17-openjdk'
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out the repository...'
                checkout scm  // Checkout the code from the repository
            }
        }

        stage('Build') {
            steps {
                echo 'Building the project...'
                script {
                    // Run Maven to clean, compile and package the application
                    sh "${MVN_HOME}/bin/mvn clean package -DskipTests"
                }
            }
        }

        stage('Test') {
            steps {
                echo 'Running unit tests...'
                script {
                    // Run Maven to execute tests
                    sh "${MVN_HOME}/bin/mvn test"
                }
            }
        }

        stage('Docker Build') {
            steps {
                echo 'Building Docker image...'
                script {
                    // Optional: Build Docker image if needed
                   // sh 'docker build -t your-image-name .'
                }
            }
        }

        stage('Deploy') {
            steps {
                echo 'Deploying the application...'
                // Optionally deploy to a staging or production server
                // For example, you could use SSH or Kubernetes deployment
                sh 'echo "Deploying application"'
            }
        }
    }

    post {
        always {
            echo 'Pipeline execution finished.'
        }
        success {
            echo 'Pipeline was successful.'
        }
        failure {
            echo 'Pipeline failed.'
        }
    }
}
