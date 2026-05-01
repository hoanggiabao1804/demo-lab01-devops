def call(Map params) {
    if (!params.isFromOriginalRepository) {
        echo "Skip pipeline due to changes not come from original repository."
        return
    }

    stage('Build') {
        dir('backoffice') {
            sh '''
            npm ci
            npm run build
            '''
        }
    }

    stage('Code Quality') {
        parallel(
            failFast: true,
            "Lint": {
                dir('backoffice') {
                    sh 'npm run lint'
                }
            },
            "Format Check": {
                dir('backoffice') {
                    sh 'npx prettier --check .'
                }
            }
        )
    }

    stage('Audit') {
        dir('backoffice') {
            sh '''
            npm audit --omit=dev || true
            '''
        }
    }

    stage('Test') {
        dir('backoffice') {
            sh '''
            npm test -- --runInBand --coverage
            '''
        }
    }

    stage('Publish Test Result') {
        junit allowEmptyResults: false, testResults: 'backoffice/test-results/*.xml'
    }

    stage('Publish Coverage Report') {
        publishHTML([
            reportDir: 'backoffice/coverage/lcov-report',
            reportFiles: 'index.html',
            reportName: 'Node Coverage',
            keepAll: true,
            alwaysLinkToLastBuild: true
        ])
    }

    stage('Gitleak Scan') {
        sh '''
        echo "Run Gitleaks scan..."
        gitleaks detect \
        --source ./backoffice \
        --no-git \
        --report-path backoffice-gitleaks-report.json \
        --report-format json \
        --exit-code 0
        '''

        def gitleaksUtils = load '.pipelines/utils/gitleaks-utils.groovy'
        gitleaksUtils.jsonToHtml(
            'backoffice-gitleaks-report.json', 
            'backoffice-gitleaks-report.html'
        )

        publishHTML([
            reportDir: '.',
            reportFiles: 'backoffice-gitleaks-report.html',
            reportName: 'Gitleak Report',
            allowMissing: true,
            alwaysLinkToLastBuild: true,
            keepAll: true
        ])
    }

    stage('SonarQube Analysis') {
        withSonarQubeEnv('My SonarQube Server') {
            dir('backoffice') {
                sh '''
                sonar-scanner \
                -Dsonar.host.url=http://sonarqube:9000
                '''
            }
        }
        timeout(time: 1, unit: 'HOURS') {
            waitForQualityGate abortPipeline: true
        }
    }

    stage('Snyk Scan') {
		dir('backoffice') {
            sh '''
            snyk auth $SNYK_TOKEN

            snyk test -d --json > backoffice-snyk-report.json || true
            '''

            def snykUtils = load '../.pipelines/utils/snyk-utils.groovy'
            snykUtils.jsonToHtml(
                'backoffice-snyk-report.json',
                'backoffice-snyk-report.html'
            )

            publishHTML([
                reportDir: '.',
                reportFiles: 'backoffice-snyk-report.html',
                reportName: 'Snyk Report',
                allowMissing: true,
                alwaysLinkToLastBuild: true,
                keepAll: true
            ])
        }
    }



}

return this