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
        mvn clean package -pl backoffice-bff -am -DskipTests
        '''
    }

    stage('Run Maven Checkstyle') {
        sh '''
        mvn checkstyle:checkstyle \
        -f backoffice-bff \
        -Dcheckstyle.output.file=reports/checkstyle/backoffice-bff-checkstyle-result.xml
        '''
    }

    stage('Publish Checkstyle') {
        recordIssues(
            tools: [checkStyle(pattern: '**/backoffice-bff-checkstyle-result.xml')]
        )
    }

    stage('Test') {
        sh '''
        mvn clean verify \
        -pl backoffice-bff \
        -DskipITs=true
        '''
    }

    stage('Publish Test Result') {
        junit 'backoffice-bff/**/target/surefire-reports/*.xml'
    }

    stage('Publish Coverage Report') {
        publishHTML([
            reportDir: 'backoffice-bff/target/site/jacoco',
            reportFiles: 'index.html',
            reportName: 'Backoffice-bff JaCoCo Coverage',
            keepAll: true,
            alwaysLinkToLastBuild: true
        ])
    }

    stage('Gitleak Scan') {
        sh '''
        echo "Run Gitleaks scan..."
        gitleaks detect \
        --source ./backoffice-bff \
        --no-git \
        --report-path reports/gitleaks/backoffice-bff-gitleaks-report.json \
        --report-format json \
        --exit-code 0
        '''

        def gitleaksUtils = load '.pipelines/utils/gitleaks-utils.groovy'
        gitleaksUtils.jsonToHtml(
            'reports/gitleaks/backoffice-bff-gitleaks-report.json', 
            'reports/gitleaks/backoffice-bff-gitleaks-report.html'
        )
    }

    stage('SonarQube Analysis') {
        withSonarQubeEnv('My SonarQube Server') {
            sh '''
            mvn clean verify sonar:sonar \
            -Dsonar.host.url=http://sonarqube:9000 \
            -f backoffice-bff \
            -DskipITs=true
            '''
        }
        timeout(time: 1, unit: 'HOURS') {
            waitForQualityGate abortPipeline: true
        }
    }

    stage('Snyk Scan') {
        def jsonReport = 'reports/snyk/backoffice-bff-snyk-report.json'
        def htmlReport = 'reports/snyk/backoffice-bff-snyk-report.html'
        def snykExitCode = 2

        sh '''
            mvn -B install \
                -pl backoffice-bff \
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
                        --file=backoffice-bff/pom.xml \
                        --maven-skip-wrapper \
                        --org="$SNYK_ORG" \
                        --json-file-output=reports/snyk/backoffice-bff-snyk-report.json \
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

        docker build -t 23120022/yas-backoffice-bff:$IMAGE_TAG ./backoffice-bff

        docker push 23120022/yas-backoffice-bff:$IMAGE_TAG
        '''
    }
}

return this