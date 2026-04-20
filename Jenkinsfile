@Library('my-shared-lib') _

pipeline {
    agent {
        docker {
            image 'maven:3.9.14-eclipse-temurin-25'
            args '--network sonar-network -u root -v $HOME/.sonar:/root/.sonar'
        }
    }

    environment {
        FROM_ORIGINAL_REPOSITORY = "${env.CHANGE_FORK == null || env.BRANCH_NAME == 'main'}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Debug') {
            steps {
                sh '''
                java -version
                mvn -version
                '''
            }
        }

        stage('Run Custom Action') {
            steps {
                sh '''
                echo "Setup Java and Sonar Cache"
                '''
                setupJDK()
                setupSonarCache()
            }
        }



        stage('Run Maven Checkstyle') {
            when {
                expression {env.FROM_ORIGINAL_REPOSITORY == 'true'}
            }
            steps {
                sh '''
                mvn checkstyle:checkstyle \
                -f backoffice-bff \
                -Dcheckstyle.output.file=backoffice-bff-checkstyle-result.xml
                '''
            }
        }

        stage('Publish Checkstyle') {
            when {
                expression {env.FROM_ORIGINAL_REPOSITORY == 'true'}
            }
            steps {
                recordIssues(
                    tools: [checkStyle(pattern: '**/backoffice-bff-checkstyle-result.xml')]
                )
            }
        }

        stage('SonarQube Analysis') {
            when {
                expression { env.FROM_ORIGINAL_REPOSITORY == 'true' }
            }
            steps {
                withSonarQubeEnv('My SonarQube Server') {
                    sh '''
                    mvn clean verify sonar:sonar \
                    -Dsonar.host.url=http://sonarqube:9000 \
                    -f backoffice-bff
                    '''
                }
                timeout(time: 1, unit: 'HOURS') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Build & Test') {
            steps {
                sh 'echo "Build & Test phase"'
            }
        }
    }
}