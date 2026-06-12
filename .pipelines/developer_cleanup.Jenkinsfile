def services = [
    [key: 'BACKOFFICE_BFF',    chart: 'backoffice-bff'],
    [key: 'BACKOFFICE_UI',     chart: 'backoffice-ui'],
    [key: 'STOREFRONT_BFF',    chart: 'storefront-bff'],
    [key: 'STOREFRONT_UI',     chart: 'storefront-ui'],

    [key: 'CART',              chart: 'cart'],
    [key: 'CUSTOMER',          chart: 'customer'],
    [key: 'INVENTORY',         chart: 'inventory'],
    [key: 'LOCATION',          chart: 'location'],
    [key: 'MEDIA',             chart: 'media'],
    [key: 'ORDER',             chart: 'order'],
    [key: 'PAYMENT',           chart: 'payment'],
    [key: 'PAYMENT_PAYPAL',    chart: 'payment-paypal'],
    [key: 'PRODUCT',           chart: 'product'],
    [key: 'PROMOTION',         chart: 'promotion'],
    [key: 'RATING',            chart: 'rating'],
    [key: 'RECOMMENDATION',    chart: 'recommendation'],
    [key: 'SEARCH',            chart: 'search'],
    [key: 'TAX',               chart: 'tax'],
    [key: 'WEBHOOK',           chart: 'webhook'],
    [key: 'SAMPLEDATA',        chart: 'sampledata']
]

pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Check CD Agent') {
            steps {
                sh '''
                    set -e
                    whoami
                    kubectl get nodes
                    helm version
                    yq --version
                '''
            }
        }

        stage('Remove Applications') {
            steps {
                script {

                    def NAMESPACE = 'yas'

                    services.each { svc ->
                        sh """
                            helm uninstall ${svc.chart} -n $NAMESPACE --ignore-not-found
                        """
                    }

                    sh """
                        kubectl delete pvc --all -n $NAMESPACE --ignore-not-found
                    """

                    def podPattern = services.collect { it.chart }.join('|')

                    echo "Waiting resources to be deleted..."
                    while kubectl get pods -n yas --no-headers 2>/dev/null | grep -E '${podPattern}' >/dev/null
                    do
                        kubectl get pods -n yas
                        sleep 5
                    done
                }
            }
        }

        stage('Verify') {
            steps {
                sh '''
                    kubectl get pods -n yas
                    kubectl get svc -n yas
                '''
            }
        }
    }
}