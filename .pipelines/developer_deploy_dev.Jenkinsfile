def services = [
    [name: 'backoffice-bff',    path: 'backoffice-bff/',    chart: 'backoffice-bff',    type: 'backend'],
    [name: 'backoffice',        path: 'backoffice/',        chart: 'backoffice-ui',     type: 'ui'],
    [name: 'storefront-bff',    path: 'storefront-bff/',    chart: 'storefront-bff',    type: 'backend'],
    [name: 'storefront',        path: 'storefront/',        chart: 'storefront-ui',     type: 'ui'],
    [name: 'cart',              path: 'cart/',              chart: 'cart',              type: 'backend'],
    [name: 'customer',          path: 'customer/',          chart: 'customer',          type: 'backend'],
    [name: 'inventory',         path: 'inventory/',         chart: 'inventory',         type: 'backend'],
    [name: 'location',          path: 'location/',          chart: 'location',          type: 'backend'],
    [name: 'media',             path: 'media/',             chart: 'media',             type: 'backend'],
    [name: 'order',             path: 'order/',             chart: 'order',             type: 'backend'],
    [name: 'payment',           path: 'payment/',           chart: 'payment',           type: 'backend'],
    [name: 'payment-paypal',    path: 'payment-paypal/',    chart: 'payment-paypal',    type: 'backend'],   
    [name: 'product',           path: 'product/',           chart: 'product',           type: 'backend'],
    [name: 'promotion',         path: 'promotion/',         chart: 'promotion',         type: 'backend'],
    [name: 'rating',            path: 'rating/',            chart: 'rating',            type: 'backend'],
    [name: 'recommendation',    path: 'recommendation/',    chart: 'recommendation',    type: 'backend'],
    [name: 'search',            path: 'search/',            chart: 'search',            type: 'backend'],
    [name: 'tax',               path: 'tax/',               chart: 'tax',               type: 'backend'],
    [name: 'webhook',           path: 'webhook/',           chart: 'webhook',           type: 'backend'],
    [name: 'sampledata',        path: 'sampledata/',        chart: 'sampledata',        type: 'backend']
]

def servicesToDeploy = []

def CURRENT_BRANCH = ''
def IMAGE_TAG = ''

pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Init') {
            steps {
                script {
                    CURRENT_BRANCH = env.GIT_BRANCH.replaceFirst(/^origin\//, '')

                    IMAGE_TAG = sh(
                        script: "git rev-parse --short HEAD",
                        returnStdout: true
                    ).trim()

                    echo "Current branch: '${CURRENT_BRANCH}'"
                    echo "Current commit ID is: '$IMAGE_TAG'"
                }
            }
        }

        stage('Detect Changes') {
            steps {
                script {
                    def previousCommit = sh(
                        script: "git rev-parse HEAD~1",
                        returnStdout: true
                    ).trim()

                    def changedFiles = sh(
                        script: "git diff --name-only ${previousCommit} ${env.GIT_COMMIT}",
                        returnStdout: true
                    ).trim().split("\n")

                    if (changedFiles.any { it.startsWith('common-library') }) {
                        servicesToDeploy = services*.name
                    } else {
                        services.each { svc -> 
                            if (changedFiles.any { it.startsWith(svc.path) }) {
                                servicesToDeploy << svc
                            }
                        }
                    }

                    def changedServices = servicesToDeploy*.name.collect().join("\n");
                    
                    echo "Changed service: $changedServices\n"

                    if (servicesToDeploy.isEmpty()) {
                        echo "No services changed"
                    }
                }
            }
        }

        stage('Dockerhub Login') {
            when {
                expression { CURRENT_BRANCH == "main" && !servicesToDeploy.isEmpty() }
            }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub_cred',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh '''
                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                    '''
                }
            }
        }

        stage('Build and Push Docker Image') {
            when {
                expression { CURRENT_BRANCH == "main" && !servicesToDeploy.isEmpty() }
            }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub_cred',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    script {
                        servicesToDeploy.each { svc -> 
                            def repository = "$DOCKER_USER/yas-${svc.name}"

                            sh """
                                docker build -t $repository:$IMAGE_TAG ./${svc.path}
                                docker tag $repository:$IMAGE_TAG $repository:main

                                docker push $repository:$IMAGE_TAG
                                docker push $repository:main
                            """
                        }
                    }
                }
            }
        }

        stage('Checkout to YAS manifest repository') {
            when {
                expression { CURRENT_BRANCH == "main" && !servicesToDeploy.isEmpty() }
            }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'github_cred',
                    usernameVariable: 'GITHUB_USER',
                    passwordVariable: 'GITHUB_TOKEN'
                )]) {
                    sh """
                        [ -d "yas-helmchart-k8s" ] && rm -rf yas-helmchart-k8s/
                        
                        git clone https://${GITHUB_USER}:${GITHUB_TOKEN}@github.com/hoanggiabao1804/yas-helmchart-k8s.git
                    """
                }

            }
        }

        stage('Update Deployment') {
            when {
                expression { CURRENT_BRANCH == "main" && !servicesToDeploy.isEmpty() }
            }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub_cred',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    script {
                        dir('yas-helmchart-k8s') {
                            sh """
                                git config --global user.name "Jenkins CI"
                                git config --global user.email "jenkins@example.com"
                                git checkout main
                            """

                            echo "Updating Deployment..."

                            servicesToDeploy.each { svc -> 
                                sh """
                                    yq -i '
                                    .${svc.type}.image.repository = "$DOCKER_USER/yas-${svc.name}" |
                                    .${svc.type}.image.tag = "$IMAGE_TAG"
                                    ' dev/${svc.chart}-values.yaml

                                    git add dev/${svc.chart}-values.yaml
                                """
                            }

                            sh """
                                git commit -m "feat(dev-manifest): Update dev manifest files of services: ${servicesToDeploy*.name.collect().join("|")}."
                                git push origin main
                            """    
                        }
                    }
                }
            }
        }
    }
}
