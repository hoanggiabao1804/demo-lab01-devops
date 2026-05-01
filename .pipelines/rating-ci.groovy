def call(Map params) {
    if (!params.isFromOriginalRepository) {
        echo "Skip pipeline due to changes not come from original repository."
        return
    }

    stage('Build') {
        sh '''
        mvn clean install \
            -pl rating \
            -am \
            -DskipTests \
            -Djacoco.skip=true
        '''
    }

    stage('Run Maven Checkstyle') {
        sh '''
        mvn checkstyle:checkstyle \
        -pl rating \
        -am \
        -Dcheckstyle.output.file=rating-checkstyle-result.xml
        '''
    }

    stage('Publish Checkstyle') {
        recordIssues(
            tools: [checkStyle(pattern: '**/rating-checkstyle-result.xml')]
        )
    }

    stage('Test') {
        sh '''
        mvn clean verify \
        -pl rating \
        -am \
        -DskipITs=true
        '''
    }

    stage('Publish Test Result') {
        junit 'rating/**/target/surefire-reports/*.xml'
    }

    stage('Publish Coverage Report') {
        publishHTML([
            reportDir: 'rating/target/site/jacoco',
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
    //     --source ./rating \
    //     --no-git \
    //     --report-path rating-gitleaks-report.json \
    //     --report-format json \
    //     --exit-code 0
    //     '''

    //     def gitleaksUtils = load '.pipelines/utils/gitleaks-utils.groovy'
    //     gitleaksUtils.jsonToHtml(
    //         'rating-gitleaks-report.json',
    //         'rating-gitleaks-report.html'
    //     )

    //     publishHTML([
    //         reportDir: '.',
    //         reportFiles: 'rating-gitleaks-report.html',
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
    //         -pl rating \
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

    //     snyk test --file=pom.xml --package-manager=maven -d --json > rating-snyk-report.json || true
    //     '''

    //     def snykUtils = load '.pipelines/utils/snyk-utils.groovy'
    //     snykUtils.jsonToHtml(
    //         'rating-snyk-report.json',
    //         'rating-snyk-report.html'
    //     )

    //     publishHTML([
    //         reportDir: '.',
    //         reportFiles: 'rating-snyk-report.html',
    //         reportName: 'Snyk Report',
    //         allowMissing: true,
    //         alwaysLinkToLastBuild: true,
    //         keepAll: true
    //     ])
    // }
}

return this