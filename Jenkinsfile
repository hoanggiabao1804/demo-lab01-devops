def servicesToBuild = []

pipeline {
    agent {
        docker {
            image '23120022/zakirepo:maven-3.9.14-eclipse-temurin-25-v3.0'
            registryUrl 'https://index.docker.io/v1/'
            registryCredentialsId 'dockerhub_cred'
            args '''
            --network sonar-network 
            -u root 
            -v $HOME/.sonar:/root/.sonar 
            -v $HOME/.owasp:/owasp
            -v $HOME/.npm:/root/.npm
            -v $HOME/.m2:/root/.m2
            '''
        }
    }

    environment {
        FROM_ORIGINAL_REPOSITORY = "${env.CHANGE_FORK == null || env.BRANCH_NAME == 'main'}"
        NVD_API_KEY = credentials('nvd-api-key')
        SNYK_TOKEN = credentials('snyk-api-token')
    }

    stages {
        stage('Initialize reports storage') {
            steps {
                sh '''
                mkdir -p reports/gitleaks
                mkdir -p reports/snyk
                mkdir -p reports/checkstyle
                '''
            }
        }

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
                        } else if (file.startsWith("backoffice/") 
                            || file == ".github/workflows/backoffice-ci.yaml"
                            || file == ".pipelines/backoffice-ci.groovy"
                        ) {
                            servicesToBuild << "backoffice"
                        } else if (file.startsWith("cart/") 
                            || file == ".github/workflows/cart-ci.yaml"
                            || file == ".pipelines/cart-ci.groovy"
                        ) {
                            servicesToBuild << "cart"
                        } else if (file.startsWith("common-library/")) {
                            servicesToBuild << "common-library"
                        } else if (
                            file.startsWith("customer/") 
                            || file == ".github/workflows/customer-ci.yaml"
                            || file == ".pipelines/customer-ci.groovy"
                        ) {
                            servicesToBuild << "customer"
                        } else if (file.startsWith("delivery/")) {
                            servicesToBuild << "delivery"
                        } else if (file.startsWith("docker/")) {
                            servicesToBuild << "docker"
                        } else if (
                            file.startsWith("inventory/") 
                            || file == ".github/workflows/inventory-ci.yaml"
                            || file == ".pipelines/inventory-ci.groovy"
                    ) {
                            servicesToBuild << "inventory"
                        } else if (file.startsWith("k8s/")) {
                            servicesToBuild << "k8s"
                        } else if (
                            file.startsWith("location/") 
                            || file == ".github/workflows/location-ci.yaml"
                            || file == ".pipelines/location-ci.groovy"                        
                        ) {
                            servicesToBuild << "location"
                        } else if (
                            file.startsWith("media/") 
                            || file == ".github/workflows/media-ci.yaml"
                            || file == ".pipelines/media-ci.groovy"    
                        ) {
                            servicesToBuild << "media"
                        } else if (file.startsWith("nginx/")) {
                            servicesToBuild << "nginx"
                        } else if (
                            file.startsWith("order/") 
                            || file == ".github/workflows/order-ci.yaml"
                            || file == ".pipelines/order-ci.groovy"    
                        ) {
                            servicesToBuild << "order"
                        } else if (
                            file.startsWith("payment/") 
                            || file == ".github/workflows/payment-ci.yaml"
                            || file == ".pipelines/payment-ci.groovy"    
                        ) {
                            servicesToBuild << "payment"
                        } else if (
                            file.startsWith("payment-paypal/") 
                            || file == ".github/workflows/payment-paypal-ci.yaml"
                            || file == ".pipelines/payment-paypal-ci.groovy"    
                        ) {
                            servicesToBuild << "payment-paypal"
                        } else if (
                            file.startsWith("product/") 
                            || file == ".github/workflows/product-ci.yaml"
                            || file == ".pipelines/product-ci.groovy"    
                        ) {
                            servicesToBuild << "product"
                        } else if (
                            file.startsWith("promotion/") 
                            || file == ".github/workflows/promotion-ci.yaml"
                            || file == ".pipelines/promotion-ci.groovy"
                        ) {
                            servicesToBuild << "promotion"
                        } else if (
                            file.startsWith("rating/") 
                            || file == ".github/workflows/rating-ci.yaml"
                            || file == ".pipelines/rating-ci.groovy"    
                        ) {
                            servicesToBuild << "rating"
                        } else if (
                            file.startsWith("recommendation/") 
                            || file == ".github/workflows/recommendation-ci.yaml"
                            || file == ".pipelines/recommendation-ci.groovy"    
                        ) {
                            servicesToBuild << "recommendation"
                        } else if (
                            file.startsWith("sampledata/") 
                            || file == ".github/workflows/sampledata-ci.yaml"
                            || file == ".pipelines/sampledata-ci.groovy"    
                        ) {
                            servicesToBuild << "sampledata"
                        } else if (
                            file.startsWith("search/") 
                            || file == ".github/workflows/search-ci.yaml"
                            || file == ".pipelines/search-ci.groovy"    
                        ) {
                            servicesToBuild << "search"
                        } else if (
                            file.startsWith("storefront/") 
                            || file == ".github/workflows/storefront-ci.yaml"
                            || file == ".pipelines/storefront-ci.groovy"    
                        ) {
                            servicesToBuild << "storefront"
                        } else if (
                            file.startsWith("storefront-bff/") 
                            || file == ".github/workflows/storefront-bff-ci.yaml"
                            || file == ".pipelines/storefront-bff-ci.groovy"    
                        ) {
                            servicesToBuild << "storefront-bff"
                        } else if (
                            file.startsWith("tax/") 
                            || file == ".github/workflows/tax-ci.yaml"
                            || file == ".pipelines/tax-ci.groovy"    
                        ) {
                            servicesToBuild << "tax"
                        } else if (
                            file.startsWith("webhook/") 
                            || file == ".github/workflows/webhook-ci.yaml"
                            || file == ".pipelines/webhook-ci.groovy"
                        ) {
                            servicesToBuild << "webhook"
                        }
                    }
                }
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
                script {
                    sh '''
                    echo "Backoffice pipeline..."
                    '''

                    def backoffice = load '.pipelines/backoffice-ci.groovy'

                    backoffice.call([
                        isFromOriginalRepository: env.FROM_ORIGINAL_REPOSITORY == 'true'
                    ])
                }
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
                script {
                    sh '''
                    echo "Cart pipeline..."
					'''

					def cart = load '.pipelines/cart-ci.groovy'

					cart.call([
						isFromOriginalRepository: env.FROM_ORIGINAL_REPOSITORY == 'true'
					])
				}
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
                script {
                    sh '''
                    echo "Customer pipeline..."
					'''

					def customer = load '.pipelines/customer-ci.groovy'

					customer.call([
						isFromOriginalRepository: env.FROM_ORIGINAL_REPOSITORY == 'true'
					])
				}
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
                script {
                    sh '''
                    echo "Inventory pipeline..."
					'''

					def inventory = load '.pipelines/inventory-ci.groovy'

					inventory.call([
						isFromOriginalRepository: env.FROM_ORIGINAL_REPOSITORY == 'true'
					])
				}
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
                script {
                    sh '''
                    echo "Location pipeline..."
					'''

					def location = load '.pipelines/location-ci.groovy'

					location.call([
						isFromOriginalRepository: env.FROM_ORIGINAL_REPOSITORY == 'true'
					])
				}
            }
        }

        stage('Run media pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('media') }
            }
            steps {
                script {
                    sh '''
                    echo "Media pipeline..."
					'''

					def media = load '.pipelines/media-ci.groovy'

					media.call([
						isFromOriginalRepository: env.FROM_ORIGINAL_REPOSITORY == 'true'
					])
				}
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
                script {
                    sh '''
                    echo "Order pipeline..."
					'''

					def order = load '.pipelines/order-ci.groovy'

					order.call([
						isFromOriginalRepository: env.FROM_ORIGINAL_REPOSITORY == 'true'
					])
				}
            }
        }

        stage('Run payment pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('payment') }
            }
            steps {
                script {
                    sh '''
                    echo "Payment pipeline..."
					'''

					def payment = load '.pipelines/payment-ci.groovy'

					payment.call([
						isFromOriginalRepository: env.FROM_ORIGINAL_REPOSITORY == 'true'
					])
				}
            }
        }

        stage('Run payment-paypal pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('payment-paypal') }
            }
            steps {
                script {
                    sh '''
                    echo "Payment-paypal pipeline..."
					'''

					def paymentPaypal = load '.pipelines/payment-paypal-ci.groovy'

					paymentPaypal.call([
						isFromOriginalRepository: env.FROM_ORIGINAL_REPOSITORY == 'true'
					])
				}
            }
        }

        stage('Run product pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('product') }
            }
            steps {
                script {
                    sh '''
                    echo "Product pipeline..."
					'''

					def product = load '.pipelines/product-ci.groovy'

					product.call([
						isFromOriginalRepository: env.FROM_ORIGINAL_REPOSITORY == 'true'
					])
				}
            }
        }

        stage('Run promotion pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('promotion')}
            }
            steps {
                script {
                    sh '''
                    echo "Promotion pipeline..."
					'''

					def promotion = load '.pipelines/promotion-ci.groovy'

					promotion.call([
						isFromOriginalRepository: env.FROM_ORIGINAL_REPOSITORY == 'true'
					])
				}
            }
        }

        stage('Run rating pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('rating') }
            }
            steps {
                script {
                    sh '''
                    echo "Rating pipeline..."
					'''

					def rating = load '.pipelines/rating-ci.groovy'

					rating.call([
						isFromOriginalRepository: env.FROM_ORIGINAL_REPOSITORY == 'true'
					])
				}
            }
        }

        stage('Run recommendation pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('recommendation') }
            }
            steps {
                script {
                    sh '''
                    echo "Recommendation pipeline..."
					'''

					def recommendation = load '.pipelines/recommendation-ci.groovy'

					recommendation.call([
						isFromOriginalRepository: env.FROM_ORIGINAL_REPOSITORY == 'true'
					])
				}
            }
        }

        stage('Run sampledata pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('sampledata') }
            }
            steps {
                script {
                    sh '''
                    echo "Sampledata pipeline..."
					'''

					def sampledata = load '.pipelines/sampledata-ci.groovy'

					sampledata.call([
						isFromOriginalRepository: env.FROM_ORIGINAL_REPOSITORY == 'true'
					])
				}
            }
        }

        stage('Run search pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('search') }
            }
            steps {
                script {
                    sh '''
                    echo "Search pipeline..."
					'''

					def search = load '.pipelines/search-ci.groovy'

					search.call([
						isFromOriginalRepository: env.FROM_ORIGINAL_REPOSITORY == 'true'
					])
				}
            }
        }

        stage('Run storefront pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('storefront') }
            }
            steps {
                script {
                    sh '''
                    echo "Storefront pipeline..."
                    '''

                    def storefront = load '.pipelines/storefront-ci.groovy'

                    storefront.call([
                        isFromOriginalRepository: env.FROM_ORIGINAL_REPOSITORY == 'true'
                    ])
                }
            }
        }

        stage('Run storefront-bff pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('storefront-bff') }
            }
            steps {
                script {
					sh '''
					echo "Storefront-bff pipeline..."
					'''

					def storefront_bff = load '.pipelines/storefront-bff-ci.groovy'

					storefront_bff.call([
						isFromOriginalRepository: env.FROM_ORIGINAL_REPOSITORY == 'true'
					])
				}
            }
        }

        stage('Run tax pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('tax') }
            }
            steps {
                script {
                    sh '''
                    echo "Tax pipeline..."
					'''

					def tax = load '.pipelines/tax-ci.groovy'

					tax.call([
						isFromOriginalRepository: env.FROM_ORIGINAL_REPOSITORY == 'true'
					])
				}
            }
        }

        stage('Run webhook pipeline') {
            when {
                expression { servicesToBuild.contains('all') || servicesToBuild.contains('webhook') }
            }
            steps {
                script {
                    sh '''
                    echo "Webhook pipeline..."
					'''

					def webhook = load '.pipelines/webhook-ci.groovy'

					webhook.call([
						isFromOriginalRepository: env.FROM_ORIGINAL_REPOSITORY == 'true'
					])
				}
            }
        }

        stage('Publish Gitleaks Reports') {
            steps {
                sh 'echo "Publish Gitleaks reports..."'

                // publishHTML([
                //     reportDir: '.',
                //     reportFiles: 'reports/gitleaks/*gitleaks-report.html',
                //     reportName: 'Gitleak Report',
                //     allowMissing: true,
                //     alwaysLinkToLastBuild: true,
                //     keepAll: true
                // ])

                sh '''
                mkdir -p reports/gitleaks/merged

                echo "<html><body><h1>Gitleaks Reports</h1><ul>" > reports/gitleaks/merged/index.html

                for f in reports/gitleaks/*gitleaks-report.html; do
                    name=$(basename $f)
                    cp $f reports/gitleaks/merged/
                    echo "<li><a href='$name'>$name</a></li>" >> reports/gitleaks/merged/index.html
                done

                echo "</ul></body></html>" >> reports/gitleaks/merged/index.html
                '''

                publishHTML([
                    reportDir: 'reports/gitleaks/merged',
                    reportFiles: 'index.html',
                    reportName: 'Gitleaks Report',
                    allowMissing: true,
                    alwaysLinkToLastBuild: true,
                    keepAll: true
                ])
            }
        }

        stage('Publish Snyk Reports') {
            steps {
                sh 'echo "Publish Snyk reports..."'

                // publishHTML([
                //     reportDir: '.',
                //     reportFiles: 'reports/snyk/*snyk-report.html',
                //     reportName: 'Snyk Report',
                //     allowMissing: true,
                //     alwaysLinkToLastBuild: true,
                //     keepAll: true
                // ])

                sh '''
                mkdir -p reports/snyk/merged

                echo "<html><body><h1>Snyk Reports</h1><ul>" > reports/snyk/merged/index.html

                for f in reports/snyk/*snyk-report.html; do
                    name=$(basename $f)
                    cp $f reports/snyk/merged/
                    echo "<li><a href='$name'>$name</a></li>" >> reports/snyk/merged/index.html
                done

                echo "</ul></body></html>" >> reports/snyk/merged/index.html
                '''

                publishHTML([
                    reportDir: 'reports/snyk/merged',
                    reportFiles: 'index.html',
                    reportName: 'Snyk Report',
                    allowMissing: true,
                    alwaysLinkToLastBuild: true,
                    keepAll: true
                ])
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