def call(Map params) {
    if (!params.isFromOriginalRepository) {
        echo "Skip pipeline due to changes not come from original repository."
        return
    }

    stage('Build') {
        dir('backoffice') {
            sh '''
            npm ci
            npm run build
            '''
        }
    }

    stage('Code Quality') {
        parallel(
            failFast: true,
            "Lint": {
                dir('backoffice') {
                    sh 'npm run lint'
                }
            },
            "Format Check": {
                dir('backoffice') {
                    sh 'npx prettier --check .'
                }
            }
        )
    }

    stage('Audit') {
        dir('backoffice') {
            sh '''
            npm audit --omit=dev || true
            '''
        }
    }

    stage('Gitleak Scan') {
        sh '''
        echo "Run Gitleaks scan..."
        gitleaks detect \
        --source ./backoffice \
        --no-git \
        --report-path backoffice-gitleaks-report.json \
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
        '  backoffice-gitleaks-report.json > backoffice-gitleaks-report.html
        '''

        publishHTML([
            reportDir: '.',
            reportFiles: 'backoffice-gitleaks-report.html',
            reportName: 'Gitleak Report',
            allowMissing: true,
            alwaysLinkToLastBuild: true,
            keepAll: true
        ])

        def hasLeak = sh(
            script: '[ grep -q "RuleID" backoffice-gitleaks-report.json ]',
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
            -f backoffice
            '''
        }
        timeout(time: 1, unit: 'HOURS') {
            waitForQualityGate abortPipeline: true
        }
    }
}

return this