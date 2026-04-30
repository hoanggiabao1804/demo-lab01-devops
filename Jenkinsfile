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
                hostname
                which docker || echo "no docker"
                '''
            }
        }

        stage('Run Action') {
            steps {
                script {
                    sh '''
                    echo "Setup Java and Sonar Cache"
                    '''
                    def action = load '.pipelines/actions/action.groovy'

                    action.call()
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