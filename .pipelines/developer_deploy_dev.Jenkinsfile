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

        // stage('Debug') {
        //     steps {
        //         script {
        //             def branch = sh(
        //                 script: 'git rev-parse --abbrev-ref HEAD',
        //                 returnStdout: true
        //             ).trim()

        //             echo "branch = '${branch}'"

        //             CURRENT_BRANCH = branch

        //             echo "CURRENT_BRANCH = '${CURRENT_BRANCH}'"

        //             echo "BRANCH_NAME = '${env.BRANCH_NAME}'"
        //             echo "GIT_BRANCH = '${env.GIT_BRANCH}'"

        //             def current = sh(
        //                 script: 'git branch --show-current',
        //                 returnStdout: true
        //             ).trim()

        //             echo "current = '${current}'"
        //         }
        //     }
        // }
        
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
                                git commit -m "feat(manifest): Update manifest files of services: ${servicesToDeploy*.name.collect().join("|")}."
                                git push origin main
                            """    
                        }
                    }
                }
            }
        }
    }
}
