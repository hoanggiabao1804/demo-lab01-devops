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
            -pl inventory \
            -am \
            -DskipTests \
            -Djacoco.skip=true
        '''
    }

    stage('Run Maven Checkstyle') {
        sh '''
        mvn checkstyle:checkstyle \
        -pl inventory \
        -am \
        -Dcheckstyle.output.file=reports/checkstyle/inventory-checkstyle-result.xml
        '''
    }

    stage('Publish Checkstyle') {
        recordIssues(
            tools: [checkStyle(pattern: '**/inventory-checkstyle-result.xml')]
        )
    }

    stage('Test') {
        sh '''
        mvn clean verify \
        -pl inventory \
        -am \
        -DskipITs=true
        '''
    }

    stage('Publish Test Result') {
        junit 'inventory/**/target/surefire-reports/*.xml'
    }

    stage('Publish Coverage Report') {
        publishHTML([
            reportDir: 'inventory/target/site/jacoco',
            reportFiles: 'index.html',
            reportName: 'Inventory JaCoCo Coverage',
            keepAll: true,
            alwaysLinkToLastBuild: true
        ])
    }

    stage('Gitleak Scan') {
        sh '''
        echo "Run Gitleaks scan..."
        gitleaks detect \
        --source ./inventory \
        --no-git \
        --report-path reports/gitleaks/inventory-gitleaks-report.json \
        --report-format json \
        --exit-code 0
        '''

        def gitleaksUtils = load '.pipelines/utils/gitleaks-utils.groovy'
        gitleaksUtils.jsonToHtml(
            'reports/gitleaks/inventory-gitleaks-report.json',
            'reports/gitleaks/inventory-gitleaks-report.html'
        )
    }

    stage('SonarQube Analysis') {
        withSonarQubeEnv('My SonarQube Server') {
            sh '''
            mvn clean verify sonar:sonar \
            -pl inventory \
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
        def jsonReport = 'reports/snyk/inventory-snyk-report.json'
        def htmlReport = 'reports/snyk/inventory-snyk-report.html'
        def snykExitCode = 2

        sh '''
            mvn -B install \
                -pl inventory \
                -am \
                -DskipTests \
                -DskipITs=true \
                -Djacoco.skip=true
        '''

        withCredentials([
            string(
                credentialsId: 'snyk-api-token',
                variable: 'SNYK_TOKEN'
            ),
            string(
                credentialsId: 'snyk-org',
                variable: 'SNYK_ORG'
            )
        ]) {
            snykExitCode = sh(
                returnStatus: true,
                script: '''
                    set -u

                    REVISION="$(mvn -q -N help:evaluate \
                        -Dexpression=revision \
                        -DforceStdout)"

                    snyk test \
                        --file=inventory/pom.xml \
                        --maven-skip-wrapper \
                        --org="$SNYK_ORG" \
                        --json-file-output=reports/snyk/inventory-snyk-report.json \
                        -- \
                        -Drevision="$REVISION"
                '''
            )
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
            echo 'Snyk scanned successfully but found dependency vulnerabilities'
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

        docker build -t 23120022/yas-inventory:$IMAGE_TAG ./inventory

        docker push 23120022/yas-inventory:$IMAGE_TAG
        '''
    }
}

return this