def call(Map params) {
    if (!params.isFromOriginalRepository) {
        echo "Skip pipeline due to changes not come from original repository."
        return
    }

    stage('Build') {
        sh '''
        mvn clean install \
            -pl tax \
            -am \
            -DskipTests \
            -Djacoco.skip=true
        '''
    }

    stage('Run Maven Checkstyle') {
        sh '''
        mvn checkstyle:checkstyle \
        -pl tax \
        -am \
        -Dcheckstyle.output.file=tax-checkstyle-result.xml
        '''
    }

    stage('Publish Checkstyle') {
        recordIssues(
            tools: [checkStyle(pattern: '**/tax-checkstyle-result.xml')]
        )
    }

    stage('Test') {
        sh '''
        mvn clean verify \
        -pl tax \
        -am \
        -DskipITs=true
        '''
    }

    stage('Publish Test Result') {
        junit 'tax/**/target/surefire-reports/*.xml'
    }

    stage('Publish Coverage Report') {
        publishHTML([
            reportDir: 'tax/target/site/jacoco',
            reportFiles: 'index.html',
            reportName: 'JaCoCo Coverage',
            keepAll: true,
            alwaysLinkToLastBuild: true
        ])
    }

    // stage('Gitleak Scan') {
    //     sh '''
    //     echo "Run Gitleaks scan..."
    //     gitleaks detect \
    //     --source ./tax \
    //     --no-git \
    //     --report-path tax-gitleaks-report.json \
    //     --report-format json \
    //     --exit-code 0
    //     '''

    //     def gitleaksUtils = load '.pipelines/utils/gitleaks-utils.groovy'
    //     gitleaksUtils.jsonToHtml(
    //         'tax-gitleaks-report.json',
    //         'tax-gitleaks-report.html'
    //     )

    //     publishHTML([
    //         reportDir: '.',
    //         reportFiles: 'tax-gitleaks-report.html',
    //         reportName: 'Gitleak Report',
    //         allowMissing: true,
    //         alwaysLinkToLastBuild: true,
    //         keepAll: true
    //     ])
    // }

    // stage('SonarQube Analysis') {
    //     withSonarQubeEnv('My SonarQube Server') {
    //         sh '''
    //         mvn clean test jacoco:report sonar:sonar \
    //         -pl tax \
    //         -am \
    //         -Djacoco.skip.check=true \
    //         -Dsonar.host.url=http://sonarqube:9000 \
    //         '''
    //     }
    //     timeout(time: 1, unit: 'HOURS') {
    //         waitForQualityGate abortPipeline: true
    //     }
    // }

    // stage('Snyk Scan') {
    //     sh '''
    //     snyk auth $SNYK_TOKEN

    //     find . -name "mvnw" -exec chmod +x {} \\;

    //     snyk test --file=pom.xml --package-manager=maven -d --json > tax-snyk-report.json || true
    //     '''

    //     def snykUtils = load '.pipelines/utils/snyk-utils.groovy'
    //     snykUtils.jsonToHtml(
    //         'tax-snyk-report.json',
    //         'tax-snyk-report.html'
    //     )

    //     publishHTML([
    //         reportDir: '.',
    //         reportFiles: 'tax-snyk-report.html',
    //         reportName: 'Snyk Report',
    //         allowMissing: true,
    //         alwaysLinkToLastBuild: true,
    //         keepAll: true
    //     ])
    // }
}

return this