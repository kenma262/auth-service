pipeline {
    agent any
    tools {
        // Configure these tool names in "Manage Jenkins > Global Tool Configuration"
        jdk 'JDK17'          // or 'JDK21' etc.
        maven 'MAVEN_3_9'     // your configured Maven installation name
    }
    options {
        timestamps()
        ansiColor('xterm')
    }
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git rev-parse --short HEAD > .git/shortref'
                script { env.GIT_SHORT_COMMIT = readFile('.git/shortref').trim() }
            }
        }
        stage('Build (Compile)') {
            steps {
                sh 'mvn -B -V -DskipTests clean compile'
            }
        }
        stage('Unit Tests') {
            steps {
                sh 'mvn -B test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
        stage('Package') {
            steps {
                sh 'mvn -B -DskipTests package'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }
    }
    post {
        success { echo "SUCCESS for commit ${env.GIT_SHORT_COMMIT}" }
        failure { echo "FAILED for commit ${env.GIT_SHORT_COMMIT}" }
        always {
            echo 'Cleaning workspace...'
            cleanWs(deleteDirs: true, notFailBuild: true)
        }
    }
}