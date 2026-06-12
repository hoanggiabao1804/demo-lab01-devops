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
        
        stage('Deploy YAS Configuration') {
            steps {
                dir('k8s/deploy') {
                    sh '''
                        echo 'Deploy YAS Configuration...'
                        
                        chmod a+x ./deploy-yas-configuration.sh

                        ./deploy-yas-configuration.sh
                    '''
                }
            }
        }
        
        stage('Resolve Image Tags') {
            steps {
                script {
                    echo 'Resolve branch -> commit id'
                    def DEFAULT_IMAGE_TAG = 'main'
                    def COMMIT_TAG_LENGTH = 7

                    def services = [
                        [key: 'BACKOFFICE_BFF',    chart: 'backoffice-bff',    branchParam: 'BACKOFFICE_BFF_BRANCH'],
                        [key: 'BACKOFFICE_UI',     chart: 'backoffice-ui',     branchParam: 'BACKOFFICE_UI_BRANCH'],
                        [key: 'STOREFRONT_BFF',    chart: 'storefront-bff',    branchParam: 'STOREFRONT_BFF_BRANCH'],
                        [key: 'STOREFRONT_UI',     chart: 'storefront-ui',     branchParam: 'STOREFRONT_UI_BRANCH'],

                        [key: 'CART',              chart: 'cart',              branchParam: 'CART_BRANCH'],
                        [key: 'CUSTOMER',          chart: 'customer',          branchParam: 'CUSTOMER_BRANCH'],
                        [key: 'INVENTORY',         chart: 'inventory',         branchParam: 'INVENTORY_BRANCH'],
                        [key: 'LOCATION',          chart: 'location',          branchParam: 'LOCATION_BRANCH'],
                        [key: 'MEDIA',             chart: 'media',             branchParam: 'MEDIA_BRANCH'],
                        [key: 'ORDER',             chart: 'order',             branchParam: 'ORDER_BRANCH'],
                        [key: 'PAYMENT',           chart: 'payment',           branchParam: 'PAYMENT_BRANCH'],
                        [key: 'PAYMENT_PAYPAL',    chart: 'payment-paypal',    branchParam: 'PAYMENT_PAYPAL_BRANCH'],
                        [key: 'PRODUCT',           chart: 'product',           branchParam: 'PRODUCT_BRANCH'],
                        [key: 'PROMOTION',         chart: 'promotion',         branchParam: 'PROMOTION_BRANCH'],
                        [key: 'RATING',            chart: 'rating',            branchParam: 'RATING_BRANCH'],
                        [key: 'RECOMMENDATION',    chart: 'recommendation',    branchParam: 'RECOMMENDATION_BRANCH'],
                        [key: 'SEARCH',            chart: 'search',            branchParam: 'SEARCH_BRANCH'],
                        [key: 'TAX',               chart: 'tax',               branchParam: 'TAX_BRANCH'],
                        [key: 'WEBHOOK',           chart: 'webhook',           branchParam: 'WEBHOOK_BRANCH'],
                        [key: 'SAMPLEDATA',        chart: 'sampledata',        branchParam: 'SAMPLEDATA_BRANCH']
                    ]

                    def normalizeBranch = { rawBranch -> 
                        def branch = rawBranch == null ? '' : rawBranch.trim();

                        if (branch == '') {
                            return 'main'
                        }

                        branch = branch.replaceFirst(/^origin\\//, '')
                        branch = branch.replaceFirst(/^refs\\/heads\\//, '')

                        return branch
                    }

                    def validateBranch = { branch -> 
                        if (!(branch ==~ /^[A-Za-z0-9._\\/-]+$/)) {
                            error "Invalid branch name: ${branch}"
                        }
                    }

                    def resolveImageTag = { branch ->
                        branch = normalizeBranch(branch)
                        validateBranch(branch)

                        if (branch == 'main') {
                            return DEFAULT_IMAGE_TAG
                        }

                        def sha = sh(
                            script: "git ls-remote --heads origin refs/heads/${branch} | cut -f1",
                            returnStdout: true
                        ).trim()

                        if (sha == '') {
                            error "Branch not found on remote origin: ${branch}"
                        }

                        return sha.take(COMMIT_TAG_LENGTH)
                    }

                    def summaryLines = []
                    def envLines = []

                    services.each { svc ->
                        def branch = normalizeBranch(params[svc.branchParam])
                        def tag = resolveImageTag(branch)

                        summaryLines << "${svc.chart.padRight(18)} branch=${branch.padRight(25)} tag=${tag}"

                        envLines << "${svc.key}_BRANCH_RESOLVED=${branch}"
                        envLines << "${svc.key}_IMAGE_TAG=${tag}"
                    }

                    def summary = summaryLines.join('\n')
                    def envContent = envLines.join('\n') + '\n'

                    echo "===== Resolved Image Tags =====\n${summary}"

                    writeFile(
                        file: 'resolved-image-tags.txt',
                        text: summary + '\n'
                    )

                    writeFile(
                        file: 'resolved-image-tags.env',
                        text: envContent
                    )

                    archiveArtifacts artifacts: 'resolved-image-tags.txt,resolved-image-tags.env', fingerprint: true
                }
            }
        }
        
        stage('Deploy YAS Applications') {
            steps {
                dir('k8s/deploy') {
                    sh '''#!/usr/bin/env bash
                        set -euxo pipefail

                        NAMESPACE="yas"

                        echo "Deploy YAS Applications..."

                        echo "Deploy storefront-bff..."
                        helm dependency build ../charts/storefront-bff
                        helm upgrade --install storefront-bff ../charts/storefront-bff \
                        --namespace "$NAMESPACE" --create-namespace \
                        --set backend.ingress.enabled=false \
                        --set backend.service.type=NodePort \
                        --wait \
                        --timeout 5m

                        BFF_NODE_PORT=$(kubectl get svc storefront-bff \
                        -n "$NAMESPACE" \
                        -o jsonpath='{.spec.ports[0].nodePort}')

                        echo "BFF NodePort: $BFF_NODE_PORT"
                        echo "BFF API URL: http://storefront.yas.local.com:$BFF_NODE_PORT/api"

                        echo "Deploy storefront-ui..."
                        helm dependency build ../charts/storefront-ui
                        helm upgrade --install storefront-ui ../charts/storefront-ui \
                        --namespace "$NAMESPACE" \
                        --create-namespace \
                        --set ui.service.type=NodePort \
                        --set-string 'ui.extraEnvs[0].name=API_BASE_PATH' \
                        --set-string "ui.extraEnvs[0].value=http://storefront.yas.local.com:$BFF_NODE_PORT/api" \
                        --wait \
                        --timeout 5m
                    '''
                }
            }
        }
        
        stage('Show NodePort URLs') {
            steps {
                dir('k8s/deploy') {
                    sh '''
                        set -euo pipefail

                        NAMESPACE="${NAMESPACE:-yas}"
                        DOMAIN=$(yq -r '.domain' ./cluster-config.yaml)
                        APP_HOST="storefront.$DOMAIN"

                        NODE_IP=$(kubectl get nodes \
                        -o jsonpath='{.items[0].status.addresses[?(@.type=="InternalIP")].address}')

                        UI_NODE_PORT=$(kubectl get svc storefront-ui \
                        -o jsonpath='{.spec.ports[?(@.port==3000)].nodePort}')

                        BFF_NODE_PORT=$(kubectl get svc storefront-bff \
                        -n "$NAMESPACE" \
                        -o jsonpath='{.spec.ports[?(@.port==80)].nodePort}')

                        BFF_HEALTH_NODE_PORT=$(kubectl get svc storefront-bff \
                        -n "$NAMESPACE" \
                        -o jsonpath='{.spec.ports[?(@.port==8090)].nodePort}')
                            
                        if [ -z "$UI_NODE_PORT" ]; then
                            echo "ERROR: Cannot find NodePort for storefront-ui port 3000"
                            exit 1
                        fi

                        if [ -z "$BFF_NODE_PORT" ]; then
                            echo "ERROR: Cannot find NodePort for storefront-bff port 80"
                            exit 1
                        fi

                        if [ -z "$BFF_HEALTH_NODE_PORT" ]; then
                            echo "ERROR: Cannot find NodePort for storefront-bff actuator port 8090"
                            exit 1
                        fi

                        UI_URL="http://$APP_HOST:$UI_NODE_PORT"
                        BFF_API_BASE_URL="http://$APP_HOST:$BFF_NODE_PORT/api"
                        BFF_HEALTH_URL="http://$APP_HOST:$BFF_HEALTH_NODE_PORT/actuator/health"

                        cat > "$WORKSPACE/nodeport-urls.txt" <<EOF
===== Developer Access Information =====

Namespace:
$NAMESPACE

Worker/Minikube Node IP:
$NODE_IP

Add this line to /etc/hosts if not exists:
$NODE_IP $APP_HOST

Storefront UI:
$UI_URL

Storefront BFF API base:
$BFF_API_BASE_URL

Storefront BFF health:
$BFF_HEALTH_URL

Kubernetes services:
EOF

                        kubectl get svc -n "$NAMESPACE" storefront-ui storefront-bff >> "$WORKSPACE/nodeport-urls.txt"

                        echo ""
                        cat "$WORKSPACE/nodeport-urls.txt"
                        echo ""

                        echo "Verify BFF health endpoint..."
                        curl -fsS "$BFF_HEALTH_URL" || {
                            echo ""
                            echo "WARNING: BFF health check failed. Check logs:"
                            echo "kubectl logs -n $NAMESPACE deploy/storefront-bff --tail=200"
                            exit 1
                        }
                    '''
                }

                archiveArtifacts artifacts: 'nodeport-urls.txt', fingerprint: true
            }
        }
    }
}
