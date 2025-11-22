pipeline {
    agent any

    environment {
        IMAGE = "vivekkashyap043/salesdemo"
        TAG = "${env.GIT_COMMIT.substring(0,7)}"
        K8S_NAMESPACE = "salesdemo"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test (Windows)') {
            steps {
                bat 'mvnw.cmd -B clean test'
                bat 'mvnw.cmd -B -DskipTests package'
            }
        }

        stage('Build Docker Image') {
            steps {
                bat "docker build -t %IMAGE%:%TAG% ."
                bat "docker tag %IMAGE%:%TAG% %IMAGE%:latest"
            }
        }

        stage('Push Image to Docker Hub') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-creds',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    bat "docker login -u %DOCKER_USER% -p %DOCKER_PASS%"
                    bat "docker push %IMAGE%:%TAG%"
                    bat "docker push %IMAGE%:latest"
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                withCredentials([file(credentialsId: 'kubeconfig-file', variable: 'KUBECONFIG')]) {

                    // Update deployment to new image
                    bat """
                    kubectl --kubeconfig=%KUBECONFIG% -n %K8S_NAMESPACE% ^
                    set image deployment/salesdemo-deployment salesdemo=%IMAGE%:%TAG%
                    """

                    // Wait for rollout
                    bat """
                    kubectl --kubeconfig=%KUBECONFIG% -n %K8S_NAMESPACE% ^
                    rollout status deployment/salesdemo-deployment --timeout=120s
                    """
                }
            }
        }
    }

    post {
        success { echo "Successfully deployed %IMAGE%:%TAG%" }
        failure { echo "Pipeline failed!" }
    }
}
