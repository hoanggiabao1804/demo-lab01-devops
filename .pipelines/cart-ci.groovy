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
            -pl cart \
            -am \
            -DskipTests \
            -Djacoco.skip=true
        '''
    }

    // stage('Run Maven Checkstyle') {
    //     sh '''
    //     mvn checkstyle:checkstyle \
    //     -pl cart \
    //     -am \
    //     -Dcheckstyle.output.file=reports/checkstyle/cart-checkstyle-result.xml
    //     '''
    // }

    // stage('Publish Checkstyle') {
    //     recordIssues(
    //         tools: [checkStyle(pattern: '**/cart-checkstyle-result.xml')]
    //     )
    // }

    // stage('Test') {
    //     sh '''
    //     mvn clean verify \
    //     -pl cart \
    //     -am \
    //     -DskipITs=true
    //     '''
    // }

    // stage('Publish Test Result') {
    //     junit 'cart/**/target/surefire-reports/*.xml'
    // }

    // stage('Publish Coverage Report') {
    //     publishHTML([
    //         reportDir: 'cart/target/site/jacoco',
    //         reportFiles: 'index.html',
    //         reportName: 'Cart JaCoCo Coverage',
    //         keepAll: true,
    //         alwaysLinkToLastBuild: true
    //     ])
    // }

    // stage('Gitleak Scan') {
    //     sh '''
    //     echo "Run Gitleaks scan..."
    //     gitleaks detect \
    //     --source ./cart \
    //     --no-git \
    //     --report-path reports/gitleaks/cart-gitleaks-report.json \
    //     --report-format json \
    //     --exit-code 0
    //     '''

    //     def gitleaksUtils = load '.pipelines/utils/gitleaks-utils.groovy'
    //     gitleaksUtils.jsonToHtml(
    //         'reports/gitleaks/cart-gitleaks-report.json', 
    //         'reports/gitleaks/cart-gitleaks-report.html'
    //     )
    // }

    // stage('Gitleak Scan') {
    //     sh '''
    //     echo "Run Gitleaks scan..."
    //     gitleaks detect \
    //     --source ./cart \

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

    //     snyk test --file=pom.xml --package-manager=maven -d --json > reports/snyk/cart-snyk-report.json || true
    //     '''

    //     def snykUtils = load '.pipelines/utils/snyk-utils.groovy'
    //     snykUtils.jsonToHtml(
    //         'reports/snyk/cart-snyk-report.json', 
    //         'reports/snyk/cart-snyk-report.html'
    //     )
    // }

    stage('Snyk Scan') {
        def jsonReport = 'reports/snyk/cart-snyk-report.json'
        def htmlReport = 'reports/snyk/cart-snyk-report.html'
        def snykExitCode = 2

        sh '''
            mvn -B install \
                -pl cart \
                -am \
                -DskipTests \
                -DskipITs=true \
                -Djacoco.skip=true
        '''

        sh '''
            echo "Debugging #1"

            mvn help:evaluate \
                -Dexpression=settings.localRepository \
                -q \
                -DforceStdout
        '''

        sh '''
            echo "Debugging #2"

            mvn -B \
                -f cart/pom.xml \
                dependency:tree \
                -Drevision=1.0-SNAPSHOT
        '''



        withCredentials([
            string(
                credentialsId: 'snyk-api-token',
                variable: 'SNYK_TOKEN'
            )
        ]) {
            withEnv(['SNYK_ORG=baozakison123']) {
                snykExitCode = sh(
                    returnStatus: true,
                    script: '''
                        set -u

                        REVISION="$(mvn -q -N help:evaluate \
                            -Dexpression=revision \
                            -DforceStdout)"

                        snyk test \
                            --file=cart/pom.xml \
                            --package-manager=maven \
                            --org="$SNYK_ORG" \
                            --project-name="yas-cart" \
                            --json-file-output=reports/snyk/cart-snyk-report.json \
                            -- \
                            -Drevision="$REVISION"
                    '''
                )
            }
        }

        if (snykExitCode != 0 && snykExitCode != 1) {
            error("Snyk failed to scan, exit code: ${snykExitCode}")
        }

        if (!fileExists(jsonReport)) {
            error("Cannot find Snyk JSON report: ${jsonReport}")
        }

        def snykUtils = load '.pipelines/utils/snyk-utils.groovy'

        snykUtils.jsonToHtml(
            jsonReport,
            htmlReport
        )

        if (snykExitCode == 1) {
            unstable('Snyk scanned successfully but found dependency vulnerabilities')
        } else {
            echo 'Snyk scanned successfully and did not found any dependency vulnerability.'
        }
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

        docker build -t 23120022/yas-cart:$IMAGE_TAG ./cart

        docker push 23120022/yas-cart:$IMAGE_TAG
        '''
    }
}

return this