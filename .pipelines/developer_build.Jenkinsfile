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
                echo 'Deploy YAS Configuration...'
                
                // sh '''
                //     helm upgrade --install yas-configuration ./k8s/charts/yas-configuration \
                //       --namespace yas-dev \
                //       --create-namespace \
                //       --wait \
                //       --timeout 5m
                // '''
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
                            script: "git ls-remote --heads origin refs/heads/${branch} | awk '{print \\$1}'",
                            returnStdout: true
                        ).trim()

                        if (sha == '') {
                            error "Branch not found on remote origin: ${branch}"
                        }

                        return sha.take(COMMIT_TAG_LENGT)
                    }

                    def summaryLines = []

                    services.each { svc ->
                        def branch = normalizeBranch(params[svc.branchParam])
                        def tag = resolveImageTag(branch)

                        env["${svc.key}_BRANCH_RESOLVED"] = branch
                        env["${svc.keu}_IMAGE_TAG"] = tag

                        summaryLines << "${svc.chart.padRight(18)} branch=${branch.padRight(25)} tag=${tag}"
                    }

                    def summary = summaryLines.join('\n')

                    echo "===== Resolved Image Tags =====\n${summary}"

                    writeFile(
                        file: 'resolved-image-tags.txt'
                        text: summary + '\n'
                    )

                    archiveArtifacts artifacts: 'resolved-image-tags.txt', fingerprint: true
                }
            }
        }
        
        stage('Deploy YAS Applications') {
            steps {
                echo 'helm upgrade --install for each service...'
            }
        }
        
        stage('Show NodePort URLs') {
            steps {
                echo 'kubectl get svc -n yas'
            }
        }
    }
}
