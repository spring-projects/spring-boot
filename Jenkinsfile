pipeline {
    agent any
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }
    
    environment {
        SONAR_HOST = 'http://15.206.213.78:9000'
        SONAR_TOKEN = credentials('sonar-token')
        GITHUB_TOKEN = credentials('archana-sonar')
        SERVICE_NAME = "${env.JOB_NAME.split('-')[0]}"
        DOCKER_IMAGE = "ongrid-scan-${env.BUILD_NUMBER}"
        PROJECT_KEY = "ongrid-${env.JOB_NAME}"
    }
    
    stages {
        stage('📥 Checkout') {
            steps {
                script {
                    echo "Checking out code from: ${env.GIT_URL}"
                }
                checkout scm
            }
        }
        
        stage('🐳 Docker Build') {
            steps {
                script {
                    echo "Building Docker image: ${DOCKER_IMAGE}"
                    sh 'docker build -f SonarqubeDockerfile -t ${DOCKER_IMAGE} .'
                }
            }
        }
        
        stage('🔨 Build + 🔍 Scan (Inside Docker)') {
            steps {
                script {
                    echo "Running Gradle build and SonarQube scan inside container..."
                    sh '''
                        docker run --rm \
                          -e SONAR_HOST=${SONAR_HOST} \
                          -e SONAR_TOKEN=${SONAR_TOKEN} \
                          -e SONAR_PROJECT_KEY=${PROJECT_KEY} \
                          ${DOCKER_IMAGE}
                    '''
                }
            }
        }
        
        stage('⏱️ Quality Gate Check') {
            steps {
                script {
                    echo "Checking Quality Gate status for project: ${PROJECT_KEY}"
                    
                    def qgStatus = ''
                    def attempts = 0
                    def maxAttempts = 12
                    def waitSeconds = 5
                    
                    timeout(time: 5, unit: 'MINUTES') {
                        while (attempts < maxAttempts) {
                            try {
                                qgStatus = sh(
                                    script: '''
                                        curl -s -u "${SONAR_TOKEN}": \
                                        "${SONAR_HOST}/api/qualitygates/project_status?projectKey=${PROJECT_KEY}" \
                                        | jq -r '.projectStatus.status // "UNKNOWN"'
                                    ''',
                                    returnStdout: true
                                ).trim()
                                
                                echo "QG Check attempt ${attempts + 1}/${maxAttempts}: Status = ${qgStatus}"
                                
                                if (qgStatus in ['OK', 'ERROR']) {
                                    break
                                }
                                
                                if (attempts < maxAttempts - 1) {
                                    sleep(waitSeconds)
                                }
                                attempts++
                            } catch (Exception e) {
                                echo "Error checking QG status: ${e.message}"
                                attempts++
                                sleep(waitSeconds)
                            }
                        }
                    }
                    
                    if (qgStatus != 'OK') {
                        error("❌ Quality Gate FAILED - Status: ${qgStatus}")
                    } else {
                        echo "✅ Quality Gate PASSED"
                    }
                }
            }
        }
        
        stage('📤 Post GitHub Status') {
            steps {
                script {
                    def status = currentBuild.result == 'SUCCESS' ? 'success' : 'failure'
                    def description = currentBuild.result == 'SUCCESS' ? '✅ Code Quality Check Passed' : '❌ Code Quality Check Failed'
                    
                    try {
                        sh '''
                            REPO_NAME=$(basename ${GIT_URL} .git)
                            
                            curl -X POST \
                              -H "Authorization: token ${GITHUB_TOKEN}" \
                              -H "Accept: application/vnd.github.v3+json" \
                              https://api.github.com/repos/HELPIIndia/${REPO_NAME}/statuses/${GIT_COMMIT} \
                              -d "{\\"state\\":\\"${status}\\",\\"description\\":\\"${description}\\",\\"context\\":\\"SonarQube/QualityGate\\",\\"target_url\\":\\"${SONAR_HOST}/dashboard?id=${PROJECT_KEY}\\"}"
                        '''
                        echo "GitHub status posted successfully"
                    } catch (Exception e) {
                        echo "⚠️ Warning: Could not post GitHub status: ${e.message}"
                    }
                }
            }
        }
    }
    
    post {
        always {
            script {
                echo "Cleaning up Docker image: ${DOCKER_IMAGE}"
                sh 'docker rmi ${DOCKER_IMAGE} || true && docker system prune -f'
            }
        }
        
        success {
            echo "✅ Pipeline completed successfully"
        }
        
        failure {
            echo "❌ Pipeline failed"
        }
    }
}
