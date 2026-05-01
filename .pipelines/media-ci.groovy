def call(Map params) {
    if (!params.isFromOriginalRepository) {
        echo "Skip pipeline due to changes not come from original repository."
        return
    }

    stage('Build') {
        sh '''
        mvn clean install \
            -pl media \
            -am \
            -DskipTests \
            -Djacoco.skip=true
        '''
    }

    stage('Run Maven Checkstyle') {
        sh '''
        mvn checkstyle:checkstyle \
        -pl media \
        -am \
        -Dcheckstyle.output.file=media-checkstyle-result.xml
        '''
    }

    stage('Publish Checkstyle') {
        recordIssues(
            tools: [checkStyle(pattern: '**/media-checkstyle-result.xml')]
        )
    }

    stage('Test') {
        sh '''
        mvn clean verify \
        -pl media \
        -am \
        -DskipITs=true
        '''
    }

    stage('Publish Test Result') {
        junit 'media/**/target/surefire-reports/*.xml'
    }

    stage('Publish Coverage Report') {
        publishHTML([
            reportDir: 'media/target/site/jacoco',
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
    //     --source ./media \
    //     --no-git \
    //     --report-path media-gitleaks-report.json \
    //     --report-format json \
    //     --exit-code 0
    //     '''

    //     def gitleaksUtils = load '.pipelines/utils/gitleaks-utils.groovy'
    //     gitleaksUtils.jsonToHtml(
    //         'media-gitleaks-report.json',
    //         'media-gitleaks-report.html'
    //     )

    //     publishHTML([
    //         reportDir: '.',
    //         reportFiles: 'media-gitleaks-report.html',
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
    //         -pl media \
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

    //     snyk test --file=pom.xml --package-manager=maven -d --json > media-snyk-report.json || true
    //     '''

    //     def snykUtils = load '.pipelines/utils/snyk-utils.groovy'
    //     snykUtils.jsonToHtml(
    //         'media-snyk-report.json',
    //         'media-snyk-report.html'
    //     )

    //     publishHTML([
    //         reportDir: '.',
    //         reportFiles: 'media-snyk-report.html',
    //         reportName: 'Snyk Report',
    //         allowMissing: true,
    //         alwaysLinkToLastBuild: true,
    //         keepAll: true
    //     ])
    // }
}

return this