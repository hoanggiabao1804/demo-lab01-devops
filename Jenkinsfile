@Library('my-shared-lib') _

pipeline {
    agent {
        docker {
            image 'maven:3.9.9-eclipse-temurin-17'
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

        stage('Run Custom Action') {
            steps {
                sh '''
                echo "Setup Java and Sonar Cache"
                '''
                setupJDK()
                setupSonarCache()
            }
        }

        // stage('Debug') {
        //     steps {
        //         sh '''
        //         which mvn || echo "Maven not found"
        //         '''
        //     }
        // }

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

        stage('Build & Test') {
            steps {
                sh 'echo "Build & Test phase"'
            }
        }
    }
}