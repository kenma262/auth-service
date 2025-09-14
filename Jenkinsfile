pipeline {
  agent any
  options {
    timestamps()
    ansiColor('xterm')
    buildDiscarder(logRotator(numToKeepStr: '20'))
  }
  tools {
    jdk 'JDK_17'
    maven 'MAVEN_3_9'
  }
  environment {
    APP_NAME = 'auth-service'
    // Example: adjust Maven options as needed
    MAVEN_OPTS = '-Dmaven.test.failure.ignore=false'
  }
  parameters {
    booleanParam(name: 'RUN_TESTS', defaultValue: true, description: 'Run unit tests')
  }
  stages {
    stage('Checkout') {
      steps {
        checkout scm
        // If using a simple Pipeline job instead, replace with:
        // git branch: 'main', url: 'https://github.com/your-org/auth-service.git', credentialsId: 'git-creds-id'
      }
    }
    stage('Build') {
      steps {
        sh 'mvn -B -q clean package -DskipTests'
      }
    }
    stage('Test') {
      when { expression { return params.RUN_TESTS } }
      steps {
        sh 'mvn -B test'
      }
    }
    // ...add more stages like Static Analysis or Deploy as needed...
  }
  post {
    always {
      junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
    }
    success {
      archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
    }
    failure {
      echo 'Build failed.'
    }
  }
}

