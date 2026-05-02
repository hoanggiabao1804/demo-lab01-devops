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
            -pl product \
            -am \
            -DskipTests \
            -Djacoco.skip=true
        '''
    }

    stage('Run Maven Checkstyle') {
        sh '''
        mvn checkstyle:checkstyle \
        -pl product \
        -am \
        -Dcheckstyle.output.file=reports/checkstyle/product-checkstyle-result.xml
        '''
    }

    stage('Publish Checkstyle') {
        recordIssues(
            tools: [checkStyle(pattern: '**/product-checkstyle-result.xml')]
        )
    }

    stage('Test') {
        sh '''
        mvn clean verify \
        -pl product \
        -am \
        -DskipITs=true
        '''
    }

    stage('Publish Test Result') {
        junit 'product/**/target/surefire-reports/*.xml'
    }

    stage('Publish Coverage Report') {
        publishHTML([
            reportDir: 'product/target/site/jacoco',
            reportFiles: 'index.html',
            reportName: 'Product JaCoCo Coverage',
            keepAll: true,
            alwaysLinkToLastBuild: true
        ])
    }

    stage('Gitleak Scan') {
        sh '''
        echo "Run Gitleaks scan..."
        gitleaks detect \
        --source ./product \
        --no-git \
        --report-path reports/gitleaks/product-gitleaks-report.json \
        --report-format json \
        --exit-code 0
        '''

        def gitleaksUtils = load '.pipelines/utils/gitleaks-utils.groovy'
        gitleaksUtils.jsonToHtml(
            'reports/gitleaks/product-gitleaks-report.json',
            'reports/gitleaks/product-gitleaks-report.html'
        )
    }

    stage('SonarQube Analysis') {
        withSonarQubeEnv('My SonarQube Server') {
            sh '''
            mvn clean verify sonar:sonar \
            -pl product \
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

        snyk test --file=pom.xml --package-manager=maven -d --json > reports/snyk/product-snyk-report.json || true
        '''

        def snykUtils = load '.pipelines/utils/snyk-utils.groovy'
        snykUtils.jsonToHtml(
            'reports/snyk/product-snyk-report.json',
            'reports/snyk/product-snyk-report.html'
        )
    }
}

return this