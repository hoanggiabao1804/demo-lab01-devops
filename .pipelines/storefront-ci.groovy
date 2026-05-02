def call(Map params) {
    if (!params.isFromOriginalRepository) {
        echo "Skip pipeline due to changes not come from original repository."
        return
    }

    stage('Build') {
        dir('storefront') {
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
                dir('storefront') {
                    sh 'npm run lint'
                }
            },
            "Format Check": {
                dir('storefront') {
                    sh 'npx prettier --check .'
                }
            }
        )
    }

    stage('Audit') {
        dir('storefront') {
            sh '''
            npm audit --omit=dev || true
            '''
        }
    }

    stage('Test') {
        dir('storefront') {
            sh '''
            npm test -- --runInBand --coverage
            '''
        }
    }

    stage('Publish Test Result') {
        junit allowEmptyResults: false, testResults: 'storefront/test-results/*.xml'
    }

    stage('Publish Coverage Report') {
        publishHTML([
            reportDir: 'storefront/coverage/lcov-report',
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
        --source ./storefront \
        --no-git \
        --report-path storefront-gitleaks-report.json \
        --report-format json \
        --exit-code 0
        '''

        def gitleaksUtils = load '.pipelines/utils/gitleaks-utils.groovy'
        gitleaksUtils.jsonToHtml(
            'storefront-gitleaks-report.json', 
            'storefront-gitleaks-report.html'
        )

        publishHTML([
            reportDir: '.',
            reportFiles: 'storefront-gitleaks-report.html',
            reportName: 'Gitleak Report',
            allowMissing: true,
            alwaysLinkToLastBuild: true,
            keepAll: true
        ])
    }

    stage('SonarQube Analysis') {
        withSonarQubeEnv('My SonarQube Server') {
            dir('storefront') {
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
		dir('storefront') {
            sh '''
            snyk auth $SNYK_TOKEN

            snyk test -d --json > storefront-snyk-report.json || true
            '''

            def snykUtils = load '.pipelines/utils/snyk-utils.groovy'
            snykUtils.jsonToHtml(
                'storefront-snyk-report.json', 
                'storefront-snyk-report.html'
            )

            publishHTML([
                reportDir: '.',
                reportFiles: 'storefront-snyk-report.html',
                reportName: 'Snyk Report',
                allowMissing: true,
                alwaysLinkToLastBuild: true,
                keepAll: true
            ])
        }
    }
}

return this