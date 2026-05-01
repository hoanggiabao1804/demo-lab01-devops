def call(Map params) {
    if (!params.isFromOriginalRepository) {
        echo "Skip pipeline due to changes not come from original repository."
        return
    }

    stage('Build') {
        sh '''
        mvn clean install \
            -pl cart \
            -am \
            -DskipTests \
            -Djacoco.skip=true
        '''
    }

    stage('Run Maven Checkstyle') {
        sh '''
        mvn checkstyle:checkstyle \
        -pl cart \
        -am \
        -Dcheckstyle.output.file=cart-checkstyle-result.xml
        '''
    }

    stage('Publish Checkstyle') {
        recordIssues(
            tools: [checkStyle(pattern: '**/cart-checkstyle-result.xml')]
        )
    }

    stage('Test') {
        sh '''
        mvn clean verify \
        -pl cart \
        -am \
        -DskipITs=true
        '''
    }

    stage('Publish Test Result') {
        junit 'cart/**/target/surefire-reports/*.xml'
    }

    stage('Publish Coverage Report') {
        publishHTML([
            reportDir: 'cart/target/site/jacoco',
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
    //     --source ./cart \
    //     --no-git \
    //     --report-path cart-gitleaks-report.json \
    //     --report-format json \
    //     --exit-code 0
    //     '''

    //     def gitleaksUtils = load '.pipelines/utils/gitleaks-utils.groovy'
    //     gitleaksUtils.jsonToHtml(
    //         'cart-gitleaks-report.json', 
    //         'cart-gitleaks-report.html'
    //     )

    //     publishHTML([
    //         reportDir: '.',
    //         reportFiles: 'cart-gitleaks-report.html',
    //         reportName: 'Gitleak Report',
    //         allowMissing: true,
    //         alwaysLinkToLastBuild: true,
    //         keepAll: true
    //     ])
    // }

    // stage('SonarQube Analysis') {
    //     withSonarQubeEnv('My SonarQube Server') {
    //         sh '''
    //         mvn clean verify sonar:sonar \
    //         -pl cart \
    //         -am \
    //         -Dsonar.host.url=http://sonarqube:9000 \
    //         -DskipITs=true
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

    //     snyk test --file=pom.xml --package-manager=maven -d --json > cart-snyk-report.json || true
    //     '''

    //     def snykUtils = load '.pipelines/utils/snyk-utils.groovy'
    //     snykUtils.jsonToHtml(
    //         'cart-snyk-report.json', 
    //         'cart-snyk-report.html'
    //     )

    //     publishHTML([
    //         reportDir: '.',
    //         reportFiles: 'cart-snyk-report.html',
    //         reportName: 'Snyk Report',
    //         allowMissing: true,
    //         alwaysLinkToLastBuild: true,
    //         keepAll: true
    //     ])
    // }
}

return this