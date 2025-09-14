pipeline {
    agent any
    tools {
        maven 'maven_3.9.11'
    }
    stages {
        stage('Build') {
            steps {
                sh 'mvn -B -DskipTests clean package'
            }
        }
    }
}