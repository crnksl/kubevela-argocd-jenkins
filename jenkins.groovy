pipeline {
    agent any
    environment {
        DOCKER_IMAGE = "crnksldockerhub/ci-app-example:latest"
    }
    stages {
        stage('Clone Code') {
            steps {
                git 'https://github.com/crnksl/kubevela-argocd-jenkins.git'
            }
        }
        stage('Build Docker Image') {
            steps {
                script {
                    sh 'docker build -t $DOCKER_IMAGE:$BUILD_NUMBER .'
                }
            }
        }
        stage('Push Docker Image') {
            steps {
                script {
                    withDockerRegistry([credentialsId: 'dockerhub-credentials', url: '']) {
                        sh 'docker push $DOCKER_IMAGE:$BUILD_NUMBER'
                    }
                }
            }
        }
        stage('Deploy to KubeVela') {
            steps {
                sh '''
                vela up -f ./vela.yaml
                '''
            }
        }
    }
}
