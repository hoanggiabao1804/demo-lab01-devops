@Library('my-shared-lib') _

pipeline {
    agent {
        docker {
            image 'maven:3.9.14-eclipse-temurin-25'
            args '--network sonar-network -u root -v $HOME/.sonar:/root/.sonar -v $HOME/.owasp:/owasp'
        }
    }

    environment {
        FROM_ORIGINAL_REPOSITORY = "${env.CHANGE_FORK == null || env.BRANCH_NAME == 'main'}"
        NVD_API_KEY = credentials('nvd-api-key')
    }

    stages {
        // stage('Checkout') {
        //     steps {
        //         checkout scm
        //     }
        // }

        stage('Detect Changes') {
            steps {
                script {
                    sh '''
                    git config --global --add safe.directory '*'
                    '''

                    echo "env.CHANGE_TARGET: '${env.CHANGE_TARGET}'"

                    def base = env.CHANGE_TARGET ?: "main"
                    sh "git fetch origin ${base}:refs/remotes/origin/${base}"

                    def changedFiles = sh(
                        script: "git diff refs/remotes/origin/${base}...HEAD --name-only",
                        returnStdout: true
                    ).trim()

                    echo "Changed files:\n${changedFiles}"

                    def paths = [
                        "backoffice-bff/",
                        "pom.xml",
                        ".github/workflows/actions/action.yaml",
                        ".github/workflows/backoffice-bff-ci.yaml",
                        "Jenkinsfile"
                    ]

                    def shouldRun = changedFiles.split("\n").any { file ->
                        paths.any { p ->
                            file.startsWith(p) || file == p
                        }
                    }

                    if (!shouldRun) {
                        currentBuild.result = 'NOT_BUILT'
                        error("No relevant changes -> skip pipeline")
                    }
                }
            }
        }

        stage('Debug') {
            steps {
                sh '''
                java -version
                mvn -version
                echo -n "$NVD_API_KEY" | wc -c
                '''
            }
        }

        stage('Run Custom Action') {
            steps {
                sh '''
                echo "Setup Java and Sonar Cache"
                '''
                setupJDK()
                setupSonarCache()
            }
        }

        // stage('Run Maven Checkstyle') {
        //     when {
        //         expression {env.FROM_ORIGINAL_REPOSITORY == 'true'}
        //     }
        //     steps {
        //         sh '''
        //         mvn checkstyle:checkstyle \
        //         -f backoffice-bff \
        //         -Dcheckstyle.output.file=backoffice-bff-checkstyle-result.xml
        //         '''
        //     }
        // }

        // stage('Publish Checkstyle') {
        //     when {
        //         expression {env.FROM_ORIGINAL_REPOSITORY == 'true'}
        //     }
        //     steps {
        //         recordIssues(
        //             tools: [checkStyle(pattern: '**/backoffice-bff-checkstyle-result.xml')]
        //         )
        //     }
        // }

        // stage('SonarQube Analysis') {
        //     when {
        //         expression { env.FROM_ORIGINAL_REPOSITORY == 'true' }
        //     }
        //     steps {
        //         withSonarQubeEnv('My SonarQube Server') {
        //             sh '''
        //             mvn clean verify sonar:sonar \
        //             -Dsonar.host.url=http://sonarqube:9000 \
        //             -f backoffice-bff
        //             '''
        //         }
        //         timeout(time: 1, unit: 'HOURS') {
        //             waitForQualityGate abortPipeline: true
        //         }
        //     }
        // }

        stage('OWASP Dependency Pre-build') {
            when {
                expression { env.FROM_ORIGINAL_REPOSITORY == 'true' }
            }
            steps {
                sh '''
                mvn -B -q clean install -DskipTests
                '''
            }
        }

        stage('OWASP Dependency Check') {
            when {
                expression { env.FROM_ORIGINAL_REPOSITORY == 'true' }
            }
            steps {
                sh '''
                mvn org.owasp:dependency-check-maven:check \
                -DnvdApiKey=$NVD_API_KEY \
                -Dnvd.api.endpoint=https://services.nvd.nist.gov/rest/json/cves/2.0 \
                -Dcisa.enabled=false \
                -Dorg.slf4j.simpleLogger.log.org.owasp=debug \
                -Dformat=HTML \
                -DoutputDirectory=target/dependency-check-report \
                -DdataDirectory=/owasp \
                -DassemblyAnalyzerEnabled=false \
                -DnodeAnalyzerEnabled=false \
                -DpyPackageAnalyzerEnabled=false
                '''
            }
        }

        stage('Publish OWASP Report') {
            when {
                expression { env.FROM_ORIGINAL_REPOSITORY == 'true' }
            }
            steps {
                sh '''
                echo "=== DEBUG FILES ==="
                find . -name "*dependency-check*"
                '''
                archiveArtifacts artifacts: 'target/dependency-check-report/**/*', fingerprint: true
            }
        }

        stage('Build & Test') {echo "=== DEBUG FILES ==="
find . -name "*dependency-check*"
            steps {
                sh 'echo "Build & Test phase"'
            }
        }
    }
}