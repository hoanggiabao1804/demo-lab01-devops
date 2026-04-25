@Library('my-shared-lib') _

def servicesToBuild = []

pipeline {
    agent {
        docker {
            image '23120022/zakirepo:maven-3.9.14-eclipse-temurin-25-v1.0'
            registryUrl 'https://index.docker.io/v1/'
            registryCredentialsId 'dockerhub_cred'
            args '''
            --network sonar-network 
            -u root 
            -v $HOME/.sonar:/root/.sonar 
            -v $HOME/.owasp:/owasp
            '''
        }
    }

    environment {
        FROM_ORIGINAL_REPOSITORY = "${env.CHANGE_FORK == null || env.BRANCH_NAME == 'main'}"
        NVD_API_KEY = credentials('nvd-api-key')
        SNYK_TOKEN = credentials('snyk-api-token')
        SNYK_CFG_ORG = credentials('snyk-org-id')
    }

    stages {
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
                    ).trim().split("\n")

                    for (file in changedFiles) {
                        if (file == "pom.xml" || file == "Jenkinsfile" || file == ".github/workflows/actions/action.yaml") {
                            servicesToBuild << "all"
                            break
                        }

                        if (file.startsWith("automation-ui/")) {
                            servicesToBuild << "automation-ui"
                        } else if (
							file.startsWith("backoffice-bff/") 
							|| file == ".github/workflows/backoffice-bff-ci.yaml"
							|| file == ".pipelines/backoffice-bff-ci.groovy"
						) {
                            servicesToBuild << "backoffice-bff"
                        } else if (file.startsWith("backoffice/") || file == ".github/workflows/backoffice-ci.yaml") {
                            servicesToBuild << "backoffice"
                        } else if (file.startsWith("cart/") || file == ".github/workflows/cart-ci.yaml") {
                            servicesToBuild << "cart"
                        } else if (file.startsWith("common-library/")) {
                            servicesToBuild << "common-library"
                        } else if (file.startsWith("customer/") || file == ".github/workflows/customer-ci.yaml") {
                            servicesToBuild << "customer"
                        } else if (file.startsWith("delivery/")) {
                            servicesToBuild << "delivery"
                        } else if (file.startsWith("docker/")) {
                            servicesToBuild << "docker"
                        } else if (file.startsWith("inventory/") || file == ".github/workflows/inventory-ci.yaml") {
                            servicesToBuild << "inventory"
                        } else if (file.startsWith("k8s/")) {
                            servicesToBuild << "k8s"
                        } else if (file.startsWith("location/") || file == ".github/workflows/location-ci.yaml") {
                            servicesToBuild << "location"
                        } else if (file.startsWith("media/") || file == ".github/workflows/media-ci.yaml") {
                            servicesToBuild << "media"
                        } else if (file.startsWith("nginx/")) {
                            servicesToBuild << "nginx"
                        } else if (file.startsWith("order/") || file == ".github/workflows/order-ci.yaml") {
                            servicesToBuild << "order"
                        } else if (file.startsWith("payment/") || file == ".github/workflows/payment-ci.yaml") {
                            servicesToBuild << "payment"
                        } else if (file.startsWith("payment-paypal/") || file == ".github/workflows/payment-paypal-ci.yaml") {
                            servicesToBuild << "payment-paypal"
                        } else if (file.startsWith("product/") || file == ".github/workflows/product-ci.yaml") {
                            servicesToBuild << "product"
                        } else if (file.startsWith("promotion/") || file == ".github/workflows/promotion-ci.yaml") {
                            servicesToBuild << "promotion"
                        } else if (file.startsWith("rating/") || file == ".github/workflows/rating-ci.yaml") {
                            servicesToBuild << "rating"
                        } else if (file.startsWith("recommendation/") || file == ".github/workflows/recommendation-ci.yaml") {
                            servicesToBuild << "recommendation"
                        } else if (file.startsWith("sampledata/") || file == ".github/workflows/sampledata-ci.yaml") {
                            servicesToBuild << "sampledata"
                        } else if (file.startsWith("search/") || file == ".github/workflows/search-ci.yaml") {
                            servicesToBuild << "search"
                        } else if (file.startsWith("storefront/") || file == ".github/workflows/storefront-ci.yaml") {
                            servicesToBuild << "storefront"
                        } else if (file.startsWith("storefront-bff/") || file == ".github/workflows/storefront-bff-ci.yaml") {
                            servicesToBuild << "storefront-bff"
                        } else if (file.startsWith("tax/") || file == ".github/workflows/tax-ci.yaml") {
                            servicesToBuild << "tax"
                        } else if (file.startsWith("webhook/") || file == ".github/workflows/webhook-ci.yaml") {
                            servicesToBuild << "webhook"
                        }
                    }
                }
            }
        }

        stage('Debug') {
            steps {
                sh '''
                java -version
                mvn -version
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

        stage ('Run automation-ui pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('automation-ui') }
            }
            steps {
                sh '''
                echo "Automation-ui pipeline..."
                '''
            }
        }

        stage('Run backoffice pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('backoffice') }
            }
            steps {
                sh '''
                echo "Backoffice pipeline..."
                '''
            }
        }

        stage('Run backoffice-bff pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('backoffice-bff') }
            }
            steps {
                script {
					sh '''
					echo "Backoffice-bff pipeline..."

					ls -al ./.pipelines
					'''

					def backoffice_bff = load '.pipelines/backoffice-bff-ci.groovy'

					backoffice_bff.call([
						isFromOriginalRepository: env.FROM_ORIGINAL_REPOSITORY == 'true'
					])
				}
            }
        }

        stage('Run cart pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('cart') }
            }
            steps {
                sh '''
                echo "Cart pipeline..."
                '''
            }
        }

        stage('Run common-library pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('common-library') }
            }
            steps {
                sh '''
                echo "Common-library pipeline..."
                '''
            }
        }

        stage('Run customer pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('customer') }
            }
            steps {
                sh '''
                echo "Customer pipeline..."
                '''
            }
        }

        stage('Run delivery pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('delivery') }
            }
            steps {
                sh '''
                echo "Delivery pipeline..."
                '''
            }
        }

        stage('Run docker pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('docker') }
            }
            steps {
                sh '''
                echo "Docker pipeline..."
                '''
            }
        }

        stage('Run inventory pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('inventory') }
            }
            steps {
                sh '''
                echo "Inventory pipeline..."
                '''
            }
        }

        stage('Run k8s pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('k8s') }
            }
            steps {
                sh '''
                echo "k8s pipeline..."
                '''
            }
        }

        stage('Run location pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('location') }
            }
            steps {
                sh '''
                echo "Location pipeline..."
        	    '''
            }
        }

        stage('Run media pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('media') }
            }
            steps {
                sh '''
                echo "Media pipeline..."
        	    '''
            }
        }

        stage('Run nginx pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('nginx') }
            }
            steps {
                sh '''
                echo "Nginx pipeline..."
        	    '''
            }
        }

        stage('Run order pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('order') }
            }
            steps {
                sh '''
                echo "Order pipeline..."
        	    '''
            }
        }

        stage('Run payment pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('payment') }
            }
            steps {
                sh '''
                echo "Payment pipeline..."
        	    '''
            }
        }

        stage('Run payment-paypal pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('payment-paypal') }
            }
            steps {
                sh '''
                echo "Payment-paypal pipeline..."
        	    '''
            }
        }

        stage('Run product pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('product') }
            }
            steps {
                sh '''
                echo "Product pipeline..."
        	    '''
            }
        }

        stage('Run promotion pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('promotion')}
            }
            steps {
                sh '''
                echo "Promotion pipeline..."
        	    '''
            }
        }

        stage('Run rating pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('rating') }
            }
            steps {
                sh '''
                echo "Rating pipeline..."
        	    '''
            }
        }

        stage('Run recommendation pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('recommendation') }
            }
            steps {
                sh '''
                echo "Recommendation pipeline..."
        	    '''
            }
        }

        stage('Run sampledata pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('sampledata') }
            }
            steps {
                sh '''
                echo "Sampledata pipeline..."
        	    '''
            }
        }

        stage('Run search pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('search') }
            }
            steps {
                sh '''
                echo "Search pipeline..."
        	    '''
            }
        }

        stage('Run storefront pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('storefront') }
            }
            steps {
                sh '''
                echo "Storefront pipeline..."
        	    '''
            }
        }

        stage('Run storefront-bff pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('storefront-bff') }
            }
            steps {
                sh '''
                echo "Storefront-bff pipeline..."
        	    '''
            }
        }

        stage('Run tax pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('tax') }
            }
            steps {
                sh '''
                echo "Tax pipeline..."
        	    '''
            }
        }

        stage('Run webhook pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('webhook') }
            }
            steps {
                sh '''
                echo "Webhook pipeline..."
        	    '''
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

        // stage('Gitleak Scan') {
        //     when {
        //         expression {env.FROM_ORIGINAL_REPOSITORY == 'true'}
        //     }
        //     steps {
        //         script {
        //             sh '''
        //             apt-get update -qq && apt-get install -y -qq curl tar

        //             echo "Download Gitleaks..."
        //             curl -fL https://github.com/gitleaks/gitleaks/releases/download/v8.30.1/gitleaks_8.30.1_linux_x64.tar.gz -o gitleaks.tar.gz

        //             echo "Extract..."
        //             tar -xzf gitleaks.tar.gz

        //             echo "Make executable..."
        //             chmod +x gitleaks

        //             echo "Run Gitleaks scan..."
        //             ./gitleaks detect \
        //             --source . \
        //             --report-path gitleaks-report.json \
        //             --report-format json \
        //             --exit-code 0
        //             '''

        //             sh '''
        //             echo "Debug..."

        //             ls -lah
        //             '''

        //             sh '''
        //             echo "Publish to html..."

        //             cat <<EOF > gitleaks-report.html
        //             <html>
        //             <body>
        //             <pre>
        //             $(cat gitleaks-report.json | sed 's/</\\\\&lt;/g; s/>/\\\\&gt;/g')
        //             </pre>
        //             </body>
        //             </html>
        //             EOF
        //             '''

        //             publishHTML([
        //                 reportDir: '.',
        //                 reportFiles: 'gitleaks-report.html',
        //                 reportName: 'Gitleak Report',
        //                 allowMissing: true,
        //                 alwaysLinkToLastBuild: true,
        //                 keepAll: true
        //             ])

        //             def hasLeak = sh(
        //                 script: '[ -s gitleaks-report.json ]',
        //                 returnStatus: true
        //             )

        //             if (hasLeak == 0) {
        //                 sh '''
        //                 echo "Secrets detected!"
        //                 '''
        //             } else {
        //                 sh '''
        //                 echo "No secrets detected!"
        //                 '''
        //             }
        //         }
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

        // stage('OWASP Dependency Pre-build') {
        //     when {
        //         expression { env.FROM_ORIGINAL_REPOSITORY == 'true' }
        //     }
        //     steps {
        //         sh '''
        //         mvn -B -q clean install -DskipTests
        //         '''
        //     }
        // }

        // stage('OWASP Dependency Check') {
        //     when {
        //         expression { env.FROM_ORIGINAL_REPOSITORY == 'true' }
        //     }
        //     steps {
        //         sh '''
        //         mvn org.owasp:dependency-check-maven:check \
        //         -DnvdApiKey=$NVD_API_KEY \
        //         -Dnvd.api.endpoint=https://services.nvd.nist.gov/rest/json/cves/2.0 \
        //         -Dcisa.enabled=false \
        //         -Dorg.slf4j.simpleLogger.log.org.owasp=debug \
        //         -Dformat=HTML \
        //         -DoutputDirectory=target/dependency-check-report \
        //         -DdataDirectory=/owasp \
        //         -DassemblyAnalyzerEnabled=false \
        //         -DnodeAnalyzerEnabled=false \
        //         -DpyPackageAnalyzerEnabled=false
        //         '''
        //     }
        // }

        // stage('Publish OWASP Report') {
        //     when {
        //         expression { env.FROM_ORIGINAL_REPOSITORY == 'true' }
        //     }
        //     steps {
        //         publishHTML([
        //             reportDir: '.',
        //             reportFiles: '**/target/dependency-check-report.html',
        //             reportName: 'OWASP Dependency Check Report',
        //             allowMissing: true,
        //             alwaysLinkToLastBuild: true,
        //             keepAll: true
        //         ])
        //     }
        // }

        // stage('Snyk Scan') {
        //     steps {
        //         script {
        //             sh '''
        //             curl -Lo snyk https://static.snyk.io/cli/latest/snyk-linux
        //             chmod +x snyk
        //             ./snyk auth $SNYK_TOKEN

        //             ./snyk test --file=pom.xml --package-manager=maven --json > snyk-report.json || true
        //             '''

        //             sh '''
        //             echo "Publish to html..."

        //             cat <<EOF > snyk-report.html
        //             <html>
        //             <body>
        //             <pre>
        //             $(cat snyk-report.json | sed 's/</\\\\&lt;/g; s/>/\\\\&gt;/g')
        //             </pre>
        //             </body>
        //             </html>
        //             EOF
        //             '''

        //             publishHTML([
        //                 reportDir: '.',
        //                 reportFiles: 'snyk-report.html',
        //                 reportName: 'Snyk Report',
        //                 allowMissing: true,
        //                 alwaysLinkToLastBuild: true,
        //                 keepAll: true
        //             ])

        //             def hasVuln = sh(
        //                 script: 'grep -q "vulnerabilities" snyk-report.json',
        //                 returnStatus: true
        //             )

        //             if (hasVuln == 0) {
        //                 error("Snyk vulnerabilities found!")
        //             }
        //         }
        //     }
        // }

        stage('Build & Test') {
            steps {
                sh 'echo "Build & Test phase"'
            }
        }
    }

    // post {
    //     always {
    //         sh 'chmod -R 777 . || true'
    //         deleteDir()
    //     }
    // }
}