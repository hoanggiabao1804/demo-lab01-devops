def services = [
    [name: 'backoffice-bff',    path: 'backoffice-bff/'],
    [name: 'backoffice',        path: 'backoffice/'],
    [name: 'storefront-bff',    path: 'storefront-bff/'],
    [name: 'storefront',        path: 'storefront/'],
    [name: 'cart',              path: 'cart/'],
    [name: 'customer',          path: 'customer/'],
    [name: 'inventory',         path: 'inventory/'],
    [name: 'location',          path: 'location/'],
    [name: 'media',             path: 'media/'],
    [name: 'order',             path: 'order/'],
    [name: 'payment',           path: 'payment/'],
    [name: 'payment-paypal',    path: 'payment-paypal/'],
    [name: 'product',           path: 'product/'],
    [name: 'promotion',         path: 'promotion/'],
    [name: 'rating',            path: 'rating/'],
    [name: 'recommendation',    path: 'recommendation/'],
    [name: 'search',            path: 'search/'],
    [name: 'tax',               path: 'tax/'],
    [name: 'webhook',           path: 'webhook/'],
    [name: 'sampledata',        path: 'sampledata/']
]

def servicesToDeploy = []

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
                        serviceToDeploy = services*.name
                    } else {
                        services.each { svc -> 
                            if (changedFiles.any { it.startsWith(svc.path) }) {
                                serviceToDeploy << svc.name
                            }
                        }
                    }

                    def changedServices = serviceToDeploy.collect().join("\n");
                    
                    echo "Changed service: $changedServices\n"
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
                        sh '''
                            COMMIT_ID=$(git rev-parse --short HEAD)
                            IMAGE_TAG=$COMMIT_ID
                        '''

                        echo "Current commit id is: '$IMAGE_TAG'"

                        // serviceToDeploy.each { svc -> 
                        //     sh """
                        //         docker build -t $DOCKER_USER/yas-$svc:$IMAGE_TAG ./$svc
                        //         docker tag $DOCKER_USER/yas-$svc:$IMAGE_TAG $DOCKER_USER/yas-$svc:main

                        //         docker push $DOCKER_USER/yas-$svc:$IMAGE_TAG
                        //         docker push $DOCKER_USER/yas-$svc:main
                        //     """
                        // }
                }
            }
        }

        stage('Checkout to YAS manifest repository') {
            steps {
                sh """
                    git clone https://github.com/hoanggiabao1804/yas-helmchart-k8s.git
                    
                    cd yas-helmchart-k8s/

                    git checkout main
                """
            }
        }

        stage('Update Deployment') {
            steps {
                echo "Updating Deployment..."
            }
        }
    }
}
