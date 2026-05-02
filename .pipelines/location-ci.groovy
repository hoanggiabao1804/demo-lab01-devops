def call(Map params) {
    if (!params.isFromOriginalRepository) {
        echo "Skip pipeline due to changes not come from original repository."
        return
    }

    stage('Run Action') {
        sh '''
        echo "Setup Java and Sonar Cache"
        '''
        
        def action = load '.pipelines/actions/action.groovy'
        action.call()
    }

    stage('Build') {
        sh '''
        mvn clean install \
            -pl location \
            -am \
            -DskipTests \
            -Djacoco.skip=true
        '''
    }

    stage('Run Maven Checkstyle') {
        sh '''
        mvn checkstyle:checkstyle \
        -pl location \
        -am \
        -Dcheckstyle.output.file=reports/checkstyle/location-checkstyle-result.xml
        '''
    }

    stage('Publish Checkstyle') {
        recordIssues(
            tools: [checkStyle(pattern: '**/location-checkstyle-result.xml')]
        )
    }

    stage('Test') {
        sh '''
        mvn clean verify \
        -pl location \
        -am \
        -DskipITs=true
        '''
    }

    stage('Publish Test Result') {
        junit 'location/**/target/surefire-reports/*.xml'
    }

    stage('Publish Coverage Report') {
        publishHTML([
            reportDir: 'location/target/site/jacoco',
            reportFiles: 'index.html',
            reportName: 'Location JaCoCo Coverage',
            keepAll: true,
            alwaysLinkToLastBuild: true
        ])
    }

    stage('Gitleak Scan') {
        sh '''
        echo "Run Gitleaks scan..."
        gitleaks detect \
        --source ./location \
        --no-git \
        --report-path reports/gitleaks/location-gitleaks-report.json \
        --report-format json \
        --exit-code 0
        '''

        def gitleaksUtils = load '.pipelines/utils/gitleaks-utils.groovy'
        gitleaksUtils.jsonToHtml(
            'reports/gitleaks/location-gitleaks-report.json',
            'reports/gitleaks/location-gitleaks-report.html'
        )
    }

    stage('SonarQube Analysis') {
        withSonarQubeEnv('My SonarQube Server') {
            sh '''
            mvn clean verify sonar:sonar \
            -pl location \
            -am \
            -Dsonar.host.url=http://sonarqube:9000 \
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

        snyk test --file=pom.xml --package-manager=maven -d --json > reports/snyk/location-snyk-report.json || true
        '''

        def snykUtils = load '.pipelines/utils/snyk-utils.groovy'
        snykUtils.jsonToHtml(
            'reports/snyk/location-snyk-report.json',
            'reports/snyk/location-snyk-report.html'
        )
    }
}

return this