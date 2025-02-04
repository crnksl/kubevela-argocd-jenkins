pipeline {
    environment {
        DOCKER_IMAGE = "crnksldockerhub/ci-app-example:latest"
    }
    agent {
        kubernetes {
            cloud 'kubernetes'
            yaml """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                app: jenkins-agent
            spec:
              containers:
              - name: docker
                image: docker:20.10.7
                command:
                - cat
                tty: true
                volumeMounts:
                - mountPath: /var/run/docker.sock
                  name: docker-socket
              volumes:
              - name: docker-socket
                hostPath:
                  path: /var/run/docker.sock
            """
        }
    }
    stages {
        stage('Clone Repository') {
            steps {
                checkout([$class: 'GitSCM',
                    branches: [[name: '*/main']],
                    userRemoteConfigs: [[
                        url: 'https://github.com/crnksl/kubevela-argocd-jenkins.git',
                        credentialsId: 'github-credentials'
                    ]]
                ])
            }
        }

        stage('Build Docker Image') {
            steps {
                container('docker') {
                    sh '''
                    docker build -t $DOCKER_IMAGE:$BUILD_NUMBER .
                    docker push $DOCKER_IMAGE:$BUILD_NUMBER
                    '''
                }
            }
        }

        stage('Deploy to KubeVela') {
            steps {
                withCredentials([file(credentialsId: 'kubeconfig', variable: 'KUBECONFIG')]) {
                    sh '''
                    export KUBECONFIG=$KUBECONFIG
                    vela up -f ./vela.yaml
                    '''
                }
            }
        }

        stage('Deploy to ArgoCD') {
            steps {
                withCredentials([file(credentialsId: 'kubeconfig', variable: 'KUBECONFIG')]) {
                    sh '''
                    argocd app sync nginx-jenkins-webapp
                    '''
                }
            }
        }
    }
}
