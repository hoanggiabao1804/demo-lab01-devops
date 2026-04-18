pipeline {
    agent any

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
                echo "Run custom action"
                '''
            }
        }

        stage('Checkstyle') {
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

        stages('Build & Test') {
            steps {
                sh 'echo "Build & Test phase"'
            }
        }
    }
}