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
        apt-get update -qq && apt-get install -y -qq curl tar && apt-get install -y -qq jq

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
		curl -Lo snyk https://static.snyk.io/cli/latest/snyk-linux
		chmod +x snyk
		./snyk auth $SNYK_TOKEN

		./snyk test --file=pom.xml --package-manager=maven --json > snyk-report.json || true

        ./snyk test --file=backoffice-bff/pom.xml --package-manager=maven --json > snyk-backoffice-bff-report.json || true
		'''

		sh '''
		echo "Publish to html..."

		cat <<EOF > snyk-report.html
		<html>
		<body>
		<pre>
		$(cat snyk-report.json | sed 's/</\\\\&lt;/g; s/>/\\\\&gt;/g')
		</pre>
		</body>
		</html>
		EOF
        '''

        sh '''
        cat <<EOF > snyk-backoffice-bff-report.html
		<html>
		<body>
		<pre>
		$(cat snyk-backoffice-bff-report.json | sed 's/</\\\\&lt;/g; s/>/\\\\&gt;/g')
		</pre>
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