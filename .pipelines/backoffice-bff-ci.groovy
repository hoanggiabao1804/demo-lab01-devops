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
        apt-get update -qq && apt-get install -y -qq curl tar

        echo "Download Gitleaks..."
        curl -fL https://github.com/gitleaks/gitleaks/releases/download/v8.30.1/gitleaks_8.30.1_linux_x64.tar.gz -o gitleaks.tar.gz

        echo "Extract..."
        tar -xzf gitleaks.tar.gz

        echo "Make executable..."
        chmod +x gitleaks

        echo "Run Gitleaks scan..."
        ./gitleaks detect \
        --source ./backoffice-bff \
        --no-git \
        --report-path gitleaks-report.json \
        --report-format json \
        --exit-code 0
        '''

        sh '''
        echo "Debug..."

        ls -lah
        '''

        sh '''
        echo "Publish to html..."

        cat <<EOF > gitleaks-report.html
        <html>
        <body>
        <pre>
        $(cat gitleaks-report.json | sed 's/</\\\\&lt;/g; s/>/\\\\&gt;/g')
        </pre>
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
            script: '[ -s gitleaks-report.json ]',
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
}

return this