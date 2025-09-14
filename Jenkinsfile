pipeline {
    agent {
        docker {
            image 'maven:3.9-eclipse-temurin-17'
            args '-v $HOME/.m2:/root/.m2'
            reuseNode true
        }
    }
    options { timestamps() }
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Build') {
            steps {
                sh 'mvn -B -q clean package -DskipTests'
            }
        }
        stage('Test') {
            steps {
                sh 'mvn -B test'
            }
        }
    }
    post {
        always {
            // Publish JUnit results if present (Maven Surefire/Failsafe or other)
            junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml, target/failsafe-reports/*.xml'
        }
        success {
            archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            echo "Pipeline succeeded for ${env.BRANCH_NAME ?: 'main'}"
        }
        failure { echo "Pipeline failed." }
    }
}