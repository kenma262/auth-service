pipeline {
    agent any
    options { timestamps() }
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Build') {
            steps {
                sh 'echo "Build step (replace with real build)"'
            }
        }
        stage('Test') {
            steps {
                sh 'echo "Test step (replace with real tests)"'
            }
        }
    }
    post {
        success { echo "Pipeline succeeded for ${env.BRANCH_NAME ?: 'main'}" }
        failure { echo "Pipeline failed." }
    }
}