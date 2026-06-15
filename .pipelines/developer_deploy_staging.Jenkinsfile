def services = [
    // [name: 'backoffice-bff',    path: 'backoffice-bff/',    chart: 'backoffice-bff',    type: 'backend',    build: 'mvn clean package -pl backoffice-bff -am -DskipTests'],
    // [name: 'backoffice',        path: 'backoffice/',        chart: 'backoffice-ui',     type: 'ui',         build: 'npm ci && npm run build'],
    [name: 'storefront-bff',    path: 'storefront-bff/',    chart: 'storefront-bff',    type: 'backend',    build: 'mvn clean package -pl storefront-bff -am -DskipTests'],
    [name: 'storefront',        path: 'storefront/',        chart: 'storefront-ui',     type: 'ui',         build: 'npm ci && npm run build'],
    // [name: 'cart',              path: 'cart/',              chart: 'cart',              type: 'backend',    build: 'mvn clean install -pl cart -am -DskipTests -Djacoco.skip=true'],
    // [name: 'customer',          path: 'customer/',          chart: 'customer',          type: 'backend',    build: 'mvn clean install -pl customer -am -DskipTests -Djacoco.skip=true'],
    // [name: 'inventory',         path: 'inventory/',         chart: 'inventory',         type: 'backend',    build: 'mvn clean install -pl inventory -am -DskipTests -Djacoco.skip=true'],
    // [name: 'location',          path: 'location/',          chart: 'location',          type: 'backend',    build: 'mvn clean install -pl location -am -DskipTests -Djacoco.skip=true'],
    // [name: 'media',             path: 'media/',             chart: 'media',             type: 'backend',    build: 'mvn clean install -pl media -am -DskipTests -Djacoco.skip=true'],
    // [name: 'order',             path: 'order/',             chart: 'order',             type: 'backend',    build: 'mvn clean install -pl order -am -DskipTests -Djacoco.skip=true'],
    // [name: 'payment',           path: 'payment/',           chart: 'payment',           type: 'backend',    build: 'mvn clean install -pl payment -am -DskipTests -Djacoco.skip=true'],
    // [name: 'payment-paypal',    path: 'payment-paypal/',    chart: 'payment-paypal',    type: 'backend',    build: 'mvn clean install -pl payment-paypal -am -DskipTests -Djacoco.skip=true'],
    // [name: 'product',           path: 'product/',           chart: 'product',           type: 'backend',    build: 'mvn clean install -pl product -am -DskipTests -Djacoco.skip=true'],
    // [name: 'promotion',         path: 'promotion/',         chart: 'promotion',         type: 'backend',    build: 'mvn clean install -pl promotion -am -DskipTests -Djacoco.skip=true'],
    // [name: 'rating',            path: 'rating/',            chart: 'rating',            type: 'backend',    build: 'mvn clean install -pl rating -am -DskipTests -Djacoco.skip=true'],
    // [name: 'recommendation',    path: 'recommendation/',    chart: 'recommendation',    type: 'backend',    build: 'mvn clean install -pl recommendation -am -DskipTests -Djacoco.skip=true'],
    // [name: 'search',            path: 'search/',            chart: 'search',            type: 'backend',    build: 'mvn clean install -pl search -am -DskipTests -Djacoco.skip=true'],
    // [name: 'tax',               path: 'tax/',               chart: 'tax',               type: 'backend',    build: 'mvn clean install -pl tax -am -DskipTests -Djacoco.skip=true'],
    // [name: 'webhook',           path: 'webhook/',           chart: 'webhook',           type: 'backend',    build: 'mvn clean install -pl webhook -am -DskipTests -Djacoco.skip=true'],
    // [name: 'sampledata',        path: 'sampledata/',        chart: 'sampledata',        type: 'backend',    build: 'mvn clean install -pl sampledata -am -DskipTests -Djacoco.skip=true']
]

def IMAGE_TAG = ''

pipeline {
    agent {
        docker {
            image '23120022/zakirepo:maven-3.9.14-eclipse-temurin-25-v4.0'
            registryUrl 'https://index.docker.io/v1/'
            registryCredentialsId 'dockerhub_cred'
            args '''
            --network sonar-network 
            -u root 
            -v /var/run/docker.sock:/var/run/docker.sock
            -v $HOME/.sonar:/root/.sonar 
            -v $HOME/.owasp:/owasp
            -v $HOME/.npm:/root/.npm
            -v $HOME/.m2:/root/.m2
            '''
        }
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Init') {
            steps {
                script {
                    IMAGE_TAG = ref.replace("refs/tags/", "")
                    echo "Current tag name is: '$IMAGE_TAG'"
                }
            }
        }

        stage('Dockerhub Login') {
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
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub_cred',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    script {
                        services.each { svc -> 
                            def repository = "$DOCKER_USER/yas-${svc.name}"

                            if (svc.type == 'ui') {
                                sh """
                                    cd ./${svc.path}

                                    eval "${svc.build}"

                                    cd ..
                                """
                            } else if (svc.type == 'backend') {
                                sh """
                                    eval "${svc.build}"
                                """
                            }

                            sh """
                                docker build -t $repository:$IMAGE_TAG ./${svc.path}

                                docker push $repository:$IMAGE_TAG
                            """
                        }
                    }
                }
            }
        }

        stage('Checkout to YAS manifest repository') {
            agent {
                label 'built-in'
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
            agent {
                label 'built-in'
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

                            services.each { svc -> 
                                sh """
                                    yq -i '
                                    .${svc.type}.image.repository = "$DOCKER_USER/yas-${svc.name}" |
                                    .${svc.type}.image.tag = "$IMAGE_TAG"
                                    ' staging/${svc.chart}-values.yaml

                                    git add staging/${svc.chart}-values.yaml
                                """
                            }

                            sh """
                                git commit -m "release(${IMAGE_TAG}): Update staging manifest files of services: ${services*.name.collect().join("|")}."
                                git push origin main
                            """    
                        }
                    }
                }
            }
        }
    }
}
