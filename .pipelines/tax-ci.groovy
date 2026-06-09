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
        -Dcheckstyle.output.file=reports/checkstyle/tax-checkstyle-result.xml
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
            reportName: 'Tax JaCoCo Coverage',
            keepAll: true,
            alwaysLinkToLastBuild: true
        ])
    }

    stage('Gitleak Scan') {
        sh '''
        echo "Run Gitleaks scan..."
        gitleaks detect \
        --source ./tax \
        --no-git \
        --report-path reports/gitleaks/tax-gitleaks-report.json \
        --report-format json \
        --exit-code 0
        '''

        def gitleaksUtils = load '.pipelines/utils/gitleaks-utils.groovy'
        gitleaksUtils.jsonToHtml(
            'reports/gitleaks/tax-gitleaks-report.json',
            'reports/gitleaks/tax-gitleaks-report.html'
        )
    }

    stage('SonarQube Analysis') {
        withSonarQubeEnv('My SonarQube Server') {
            // sh '''
            // mvn clean test jacoco:report sonar:sonar \
            // -pl tax \
            // -am \
            // -Djacoco.skip.check=true \
            // -Dsonar.host.url=http://sonarqube:9000 \
            // '''
            sh '''
            mvn clean verify sonar:sonar \
            -pl tax \
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

        snyk test --file=pom.xml --package-manager=maven -d --json > reports/snyk/tax-snyk-report.json || true
        '''

        def snykUtils = load '.pipelines/utils/snyk-utils.groovy'
        snykUtils.jsonToHtml(
            'reports/snyk/tax-snyk-report.json',
            'reports/snyk/tax-snyk-report.html'
        )
    }

    stage('Dockerhub Login') {
        withCredentials([usernamePassword(
            credentialsId: 'dockerhub_cred',
            usernameVariable: 'DOCKER_USER',
            passwordVariable: 'DOCKER_PASS'
        )]) {
            sh '''
            echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
            '''
        }
    }

    stage('Build and Push Docker Image') {
        sh '''
        COMMIT_ID=$(git rev-parse --short HEAD)

        if [ "$BRANCH_NAME" = "main" ]; then
            IMAGE_TAG=main
        else
            IMAGE_TAG=$COMMIT_ID
        fi

        echo "Branch name is: '$IMAGE_TAG'"

        docker build -t 23120022/yas-tax:$IMAGE_TAG ./tax

        docker push 23120022/yas-tax:$IMAGE_TAG
        '''
    }
}

return this