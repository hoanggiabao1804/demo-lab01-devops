def call(Map params) {
    if (!params.isFromOriginalRepository) {
        echo "Skip pipeline due to changes not come from original repository."
        return
    }

    stage('Build') {
        sh '''
        mvn clean package -pl storefront-bff -am -DskipTests
        '''
    }

    stage('Run Maven Checkstyle') {
        sh '''
        mvn checkstyle:checkstyle \
        -f storefront-bff \
        -Dcheckstyle.output.file=storefront-bff-checkstyle-result.xml
        '''
    }

    stage('Publish Checkstyle') {
        recordIssues(
            tools: [checkStyle(pattern: '**/storefront-bff-checkstyle-result.xml')]
        )
    }

    stage('Test') {
        sh '''
        mvn clean verify \
        -pl storefront-bff \
        -DskipITs=true
        '''
    }

    stage('Publish Test Result') {
        junit 'storefront-bff/**/target/surefire-reports/*.xml'
    }

    stage('Publish Coverage Report') {
        publishHTML([
            reportDir: 'storefront-bff/target/site/jacoco',
            reportFiles: 'index.html',
            reportName: 'JaCoCo Coverage',
            keepAll: true,
            alwaysLinkToLastBuild: true
        ])
    }

    stage('Gitleak Scan') {
        sh '''
        echo "Run Gitleaks scan..."
        gitleaks detect \
        --source ./storefront-bff \
        --no-git \
        --report-path storefront-bff-gitleaks-report.json \
        --report-format json \
        --exit-code 0
        '''

        def gitleaksUtils = load '.pipelines/utils/gitleaks-utils.groovy'
        gitleaksUtils.jsonToHtml(
            'storefront-bff-gitleaks-report.json', 
            'storefront-bff-gitleaks-report.html'
        )

        publishHTML([
            reportDir: '.',
            reportFiles: 'storefront-bff-gitleaks-report.html',
            reportName: 'Gitleak Report',
            allowMissing: true,
            alwaysLinkToLastBuild: true,
            keepAll: true
        ])
    }

    stage('SonarQube Analysis') {
        withSonarQubeEnv('My SonarQube Server') {
            sh '''
            mvn clean verify sonar:sonar \
            -Dsonar.host.url=http://sonarqube:9000 \
            -f storefront-bff \
            -DskipITs=true
            '''
        }
        timeout(time: 1, unit: 'HOURS') {
            waitForQualityGate abortPipeline: true
        }
    }

    stage('Snyk Scan') {
		sh '''
		snyk auth $SNYK_TOKEN

        find . -name "mvnw" -exec chmod +x {} \\;

        snyk test --file=pom.xml --package-manager=maven -d --json > storefront-bff-snyk-report.json || true
		'''

        def snykUtils = load '.pipelines/utils/snyk-utils.groovy'
        snykUtils.jsonToHtml(
            'storefront-bff-snyk-report.json', 
            'storefront-bff-snyk-report.html'
        )

		publishHTML([
			reportDir: '.',
			reportFiles: 'storefront-bff-snyk-report.html',
			reportName: 'Snyk Report',
			allowMissing: true,
			alwaysLinkToLastBuild: true,
			keepAll: true
		])
    }
}

return this