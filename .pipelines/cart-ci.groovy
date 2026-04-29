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
        mvn clean test jacoco:report \
        -pl cart \
        -am \
        -Djacoco.skip=false
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

    stage('Gitleak Scan') {
        sh '''
        echo "Run Gitleaks scan..."
        gitleaks detect \
        --source ./cart \
        --no-git \
        --report-path cart-gitleaks-report.json \
        --report-format json \
        --exit-code 0
        '''

        sh '''
        jq -r '
        if length == 0 then
        "<p>No secrets detected</p>"
        else
        "<html>
        <head>
        <style>
        body { font-family: Arial; padding: 20px; }
        h2 { margin-bottom: 20px; }

        table {
        border-collapse: collapse;
        width: 100%;
        }

        th, td {
        border: 1px solid #ddd;
        padding: 10px;
        text-align: left;
        }

        th {
        background-color: #f4f4f4;
        }

        tr:nth-child(even) {
        background-color: #fafafa;
        }
        </style>
        </head>
        <body>

        <h2>Gitleaks Report</h2>

        <table>
        <thead>
        <tr>
        <th>File</th>
        <th>RuleID</th>
        <th>Secret</th>
        <th>StartLine</th>
        </tr>
        </thead>
        <tbody>
        " +

        (
        [.[] |
            "<tr>" +
            "<td>" + .File + "</td>" +
            "<td>" + .RuleID + "</td>" +
            "<td>" + .Secret + "</td>" +
            "<td>" + (.StartLine | tostring) + "</td>" +
            "</tr>"
        ] | join("")
        )

        + "

        </tbody>
        </table>

        </body>
        </html>
        "
        end
        '  cart-gitleaks-report.json > cart-gitleaks-report.html
        '''

        publishHTML([
            reportDir: '.',
            reportFiles: 'cart-gitleaks-report.html',
            reportName: 'Gitleak Report',
            allowMissing: true,
            alwaysLinkToLastBuild: true,
            keepAll: true
        ])

        def hasLeak = sh(
            script: '[ grep -q "RuleID" cart-gitleaks-report.json ]',
            returnStatus: true
        )

        if (hasLeak == 0) {
            sh '''
            echo "Secrets detected!"
            '''
        } else {
            sh '''
            echo "No secrets detected!"
            '''
        }
    }

    stage('SonarQube Analysis') {
        withSonarQubeEnv('My SonarQube Server') {
            sh '''
            echo "Find Jacoco XML report..."
            find . -name jacoco.xml
            '''

            sh '''
            mvn clean test jacoco:report sonar:sonar \
            -pl cart \
            -am \
            -Djacoco.skip.check=true \
            -Dsonar.host.url=http://sonarqube:9000 \
            '''
        }
        timeout(time: 1, unit: 'HOURS') {
            waitForQualityGate abortPipeline: true
        }
    }

    // stage('OWASP Dependency Pre-build') {
    //     sh '''
    //     mvn -B -q clean install -DskipTests
    //     '''
    // }

    // stage('OWASP Dependency Check') {
    //     sh '''
    //     mvn org.owasp:dependency-check-maven:check \
    //     -pl cart -am \
    //     -DnvdApiKey=$NVD_API_KEY \
    //     -Dnvd.api.endpoint=https://services.nvd.nist.gov/rest/json/cves/2.0 \
    //     -Dcisa.enabled=false \
    //     -Dformat=HTML \
    //     -DoutputDirectory=target/dependency-check-report \
    //     -DdataDirectory=/owasp \
    //     -DassemblyAnalyzerEnabled=false \
    //     -DnodeAnalyzerEnabled=false \
    //     -DpyPackageAnalyzerEnabled=false
    //     '''
    // }

    // stage('Publish OWASP Report') {
    //     publishHTML([
    //         reportDir: '.',
    //         reportFiles: '**/target/dependency-check-report.html',
    //         reportName: 'OWASP Dependency Check Report',
    //         allowMissing: true,
    //         alwaysLinkToLastBuild: true,
    //         keepAll: true
    //     ])
    // }

    stage('Snyk Scan') {
		dir('cart') {
            sh '''
            snyk auth $SNYK_TOKEN

            find . -name "mvnw" -exec chmod +x {} \\;

            snyk test \
            --package-manager=maven \
            -d \
            --json > cart-snyk-report.json || true
            '''

            sh '''
            jq -r '
            if (.vulnerabilities | length) == 0 then
            "<p>No vulnerabilities</p>"
            else
            "<html>
            <head>
            <style>
            body { font-family: Arial; padding: 20px; }
            h2 { margin-bottom: 20px; }

            table {
            border-collapse: collapse;
            width: 100%;
            }

            th, td {
            border: 1px solid #ddd;
            padding: 10px;
            text-align: left;
            }

            th {
            background-color: #f4f4f4;
            }

            tr:nth-child(even) {
            background-color: #fafafa;
            }
            </style>
            </head>
            <body>

            <h2>Snyk Vulnerability Report</h2>

            <table>
            <thead>
            <tr>
            <th>Severity</th>
            <th>Package</th>
            <th>Version</th>
            <th>Title</th>
            <th>Fixed In</th>
            </tr>
            </thead>
            <tbody>
            " +

            (
            [.vulnerabilities[] |
                "<tr>" +
                "<td>" + .severity + "</td>" +
                "<td>" + .packageName + "</td>" +
                "<td>" + .version + "</td>" +
                "<td>" + .title + "</td>" +
                "<td>" + (if .fixedIn then (.fixedIn | join(", ")) else "N/A" end) + "</td>" +
                "</tr>"
            ] | join("")
            )

            + "

            </tbody>
            </table>

            </body>
            </html>
            "
            end
            ' cart-snyk-report.json > cart-snyk-report.html
            '''

            publishHTML([
                reportDir: '.',
                reportFiles: 'cart-snyk-report.html',
                reportName: 'Snyk Report',
                allowMissing: true,
                alwaysLinkToLastBuild: true,
                keepAll: true
            ])

            def hasVuln = sh(
                script: 'grep -q "vulnerabilities" cart-snyk-report.json',
                returnStatus: true
            )

            if (hasVuln == 0) {
                sh '''
                echo "Snyk vulnerabilities found!"
                '''
            } else {
                sh '''
                echo "No vulnerabilites found!"
                '''
            }
        }
    }
}

return this