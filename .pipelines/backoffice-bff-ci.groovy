def call(Map params) {
    if (!params.isFromOriginalRepository) {
        echo "Skip pipeline due to changes not come from original repository."
        return
    }

    stage('Run Maven Checkstyle') {
        sh '''
        mvn checkstyle:checkstyle \
        -f backoffice-bff \
        -Dcheckstyle.output.file=backoffice-bff-checkstyle-result.xml
        '''
    }

    stage('Publish Checkstyle') {
        recordIssues(
            tools: [checkStyle(pattern: '**/backoffice-bff-checkstyle-result.xml')]
        )
    }

    stage('Gitleak Scan') {
        sh '''
        echo "Run Gitleaks scan..."
        gitleaks detect \
        --source ./backoffice-bff \
        --no-git \
        --report-path gitleaks-report.json \
        --report-format json \
        --exit-code 0
        '''

        sh '''
        echo "Publish to html..."

        jq -r '
        if length == 0 then
        "<p>No secrets detected 🎉</p>"
        else
        "<table>
        <tr>
            <th>File</th>
            <th>Rule</th>
            <th>Secret</th>
            <th>Line</th>
        </tr>" +
        ( .[] |
            "<tr>
            <td>\\(.File)</td>
            <td>\\(.RuleID)</td>
            <td>\\(.Secret)</td>
            <td>\\(.StartLine)</td>
            </tr>"
        ) +
        "</table>"
        end
        ' gitleaks-report.json > gitleaks-table.html

        cat <<EOF > gitleaks-report.html
        <html>
        <body>
        <h2>Gitleaks Report</h2>
        $(cat gitleaks-table.html)
        </body>
        </html>
        EOF
        '''

        publishHTML([
            reportDir: '.',
            reportFiles: 'gitleaks-report.html',
            reportName: 'Gitleak Report',
            allowMissing: true,
            alwaysLinkToLastBuild: true,
            keepAll: true
        ])

        def hasLeak = sh(
            script: '[ grep -q "RuleID" gitleaks-report.json ]',
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
            mvn clean verify sonar:sonar \
            -Dsonar.host.url=http://sonarqube:9000 \
            -f backoffice-bff
            '''
        }
        timeout(time: 1, unit: 'HOURS') {
            waitForQualityGate abortPipeline: true
        }
    }

    stage('OWASP Dependency Pre-build') {
        sh '''
        mvn -B -q clean install -DskipTests
        '''
    }

    stage('OWASP Dependency Check') {
        sh '''
        mvn org.owasp:dependency-check-maven:check \
        -pl backoffice-bff -am \
        -DnvdApiKey=$NVD_API_KEY \
        -Dnvd.api.endpoint=https://services.nvd.nist.gov/rest/json/cves/2.0 \
        -Dcisa.enabled=false \
        -Dformat=HTML \
        -DoutputDirectory=target/dependency-check-report \
        -DdataDirectory=/owasp \
        -DassemblyAnalyzerEnabled=false \
        -DnodeAnalyzerEnabled=false \
        -DpyPackageAnalyzerEnabled=false
        '''
    }

    stage('Publish OWASP Report') {
        publishHTML([
            reportDir: '.',
            reportFiles: '**/target/dependency-check-report.html',
            reportName: 'OWASP Dependency Check Report',
            allowMissing: true,
            alwaysLinkToLastBuild: true,
            keepAll: true
        ])
    }

    stage('Snyk Scan') {
		sh '''
		snyk auth $SNYK_TOKEN

		snyk test --file=pom.xml --package-manager=maven --json > snyk-report.json || true

        snyk test --file=backoffice-bff/pom.xml --package-manager=maven --json > snyk-backoffice-bff-report.json || true
		'''

        sh '''
        echo "Publish to html..."

        jq -r '
        def color(sev):
        if sev=="critical" then "red"
        elif sev=="high" then "orange"
        elif sev=="medium" then "gold"
        else "green" end;

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
            "<td style=\\"color:" + color(.severity) + "; font-weight:bold\\">" + .severity + "</td>" +
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
        ' snyk-report.json > snyk-report.html
        '''

        sh '''
        jq -r '
        if (.vulnerabilities | length) == 0 then
        "<p>No vulnerabilities 🎉</p>"
        else
        "<table>
        <tr>
            <th>Package</th>
            <th>Severity</th>
            <th>Title</th>
            <th>Version</th>
            <th>Dependency Path</th>
        </tr>" +
        (.vulnerabilities[] |
            "<tr>
            <td>\\(.packageName)</td>
            <td>\\(.severity)</td>
            <td>\\(.title)</td>
            <td>\\(.version)</td>
            <td>\\(.from | join(\" → \"))</td>
            </tr>"
        ) +
        "</table>"
        end
        ' snyk-report.json > snyk-backoffice-bff-table.html

        cat <<EOF > snyk-backoffice-bff-report.html
        <html>
        <head>
        <style>
        body { font-family: Arial; }
        table { border-collapse: collapse; width: 100%; }
        th, td { border: 1px solid #ddd; padding: 8px; }
        th { background-color: #f2f2f2; }
        tr:nth-child(even) { background-color: #f9f9f9; }

        /* severity color */
        .low { color: green; }
        .medium { color: orange; }
        .high { color: red; }
        .critical { color: darkred; font-weight: bold; }
        </style>
        </head>
        <body>

        <h2>Snyk backoffice-bff Report</h2>

        $(cat snyk-backoffice-bff-table.html)

        </body>
        </html>
        EOF
		'''

		publishHTML([
			reportDir: '.',
			reportFiles: 'snyk-report.html,snyk-backoffice-bff-report.html',
			reportName: 'Snyk Report',
			allowMissing: true,
			alwaysLinkToLastBuild: true,
			keepAll: true
		])

		def hasVuln = sh(
			script: 'grep -q "vulnerabilities" snyk-report.json',
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

return this