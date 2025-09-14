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
                // Auto-detect Maven Wrapper, Maven, or npm
                sh '''
                  set -e
                  if [ -x ./mvnw ]; then
                    ./mvnw -B -q clean package -DskipTests
                  elif command -v mvn >/dev/null 2>&1; then
                    mvn -B -q clean package -DskipTests
                  elif [ -f package.json ]; then
                    if command -v npm >/dev/null 2>&1; then
                      npm ci
                      npm run build
                    else
                      echo "npm not found on PATH"; exit 1
                    fi
                  else
                    echo "No recognized build tool found (Maven or npm)."; exit 1
                  fi
                '''
            }
        }
        stage('Test') {
            steps {
                // Run tests with Maven (wrapper or system) or npm
                sh '''
                  set -e
                  if [ -x ./mvnw ]; then
                    ./mvnw -B test
                  elif command -v mvn >/dev/null 2>&1; then
                    mvn -B test
                  elif [ -f package.json ]; then
                    if command -v npm >/dev/null 2>&1; then
                      npm test -- --ci || npm test
                    else
                      echo "npm not found on PATH"; exit 1
                    fi
                  else
                    echo "No recognized test tool found (Maven or npm)."; exit 1
                  fi
                '''
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