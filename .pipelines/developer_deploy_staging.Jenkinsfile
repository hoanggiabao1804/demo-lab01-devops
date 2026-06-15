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

        stage('Verify Infrastructure') {
            steps {
                sh '''
                    set -e

                    fail() {
                        echo "❌ $1"
                        exit 1
                    }

                    ok() {
                        echo "✅ $1"
                    }

                    wait_namespace_healthy() {
                        local ns="$1"
                        local timeout_seconds="$2"
                        local sleep_seconds=10
                        local start_time
                        start_time=$(date +%s)

                        echo "===== Check pods in namespace: $ns ====="

                        while true; do
                            kubectl get pods -n "$ns" -o wide

                            local pod_count
                            pod_count=$(kubectl get pods -n "$ns" --no-headers 2>/dev/null | wc -l)

                            if [ "$pod_count" -eq 0 ]; then
                            fail "No pods found in namespace: $ns"
                            fi

                            local pod_lines
                            pod_lines=$(kubectl get pods -n "$ns" \
                            -o jsonpath='{range .items[*]}{.metadata.name}{"|"}{.status.phase}{"|"}{range .status.conditions[?(@.type=="Ready")]}{.status}{end}{"\\n"}{end}')

                            local failed_pods=""
                            local waiting_pods=""

                            while IFS='|' read -r name phase ready; do
                                [ -z "$name" ] && continue

                                case "$phase" in
                                    Succeeded)
                                    # Completed job pod is OK
                                    ;;
                                    Running)
                                    if [ "$ready" != "True" ]; then
                                        waiting_pods="${waiting_pods}\\n${name} phase=${phase} ready=${ready}"
                                    fi
                                    ;;
                                    Failed)
                                    failed_pods="${failed_pods}\\n${name} phase=${phase} ready=${ready}"
                                    ;;
                                    *)
                                    waiting_pods="${waiting_pods}\\n${name} phase=${phase} ready=${ready}"
                                    ;;
                                esac
                            done <<EOF
$pod_lines
EOF

                            if [ -n "$failed_pods" ]; then
                                echo "Failed pods:$failed_pods"
                                fail "Namespace $ns contains Failed pod(s)"
                            fi

                            if [ -z "$waiting_pods" ]; then
                                ok "Namespace $ns is healthy. Running pods are Ready, Completed pods are accepted."
                                return 0
                            fi

                            local now
                            now=$(date +%s)
                            local elapsed=$((now - start_time))

                            if [ "$elapsed" -ge "$timeout_seconds" ]; then
                                echo "Pods still not healthy after ${timeout_seconds}s:"
                                echo "$waiting_pods"
                                kubectl describe pods -n "$ns" || true
                                fail "Timeout waiting for namespace $ns to become healthy"
                            fi

                            echo "Waiting pods in namespace $ns:"
                            echo "$waiting_pods"
                            sleep "$sleep_seconds"
                        done
                    }

                    echo "===== Nodes ====="
                    kubectl get nodes -o wide

                    echo "===== Namespaces ====="
                    kubectl get ns keycloak
                    kubectl get ns redis
                    kubectl get ns postgres
                    kubectl get ns kafka
                    kubectl get ns elasticsearch

                    echo "===== Helm releases ====="
                    helm list -A | grep -E 'keycloak|redis|postgres|kafka|elastic'

                    echo "===== Pods ====="
                    kubectl get pods -n keycloak
                    kubectl get pods -n redis
                    kubectl get pods -n postgres
                    kubectl get pods -n kafka
                    kubectl get pods -n elasticsearch

                    echo "===== Wait namespace healthy ====="
                    wait_namespace_healthy keycloak 600
                    wait_namespace_healthy redis 300
                    wait_namespace_healthy postgres 600
                    wait_namespace_healthy kafka 900
                    wait_namespace_healthy elasticsearch 900

                    echo "Infrastructure looks ready."
                '''
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
