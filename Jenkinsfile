pipeline {
    agent { label 'docker-agent-alpine' }
    tools {
        maven 'maven_3.9.11'
    }
    stages {
        stage('Debug Node') {
            steps {
                sh '''
                echo "Running on node: $NODE_NAME"
                echo "Node labels: $NODE_LABELS"
                '''
                }
            }
        stage('Build') {
            steps {
                sh 'mvn -B -DskipTests clean package'
            }
        }
        stage('Test') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
    }
}