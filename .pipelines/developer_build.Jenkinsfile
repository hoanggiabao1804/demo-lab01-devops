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
        
                    echo "===== Wait pods ready ====="
                    kubectl wait --for=condition=Ready pod --all -n keycloak --field-selector=status.phase!=Completed --timeout=600s
                    kubectl wait --for=condition=Ready pod --all -n redis --field-selector=status.phase!=Completed --timeout=300s
                    kubectl wait --for=condition=Ready pod --all -n postgres --field-selector=status.phase!=Completed --timeout=600s
                    kubectl wait --for=condition=Ready pod --all -n kafka --field-selector=status.phase!=Completed --timeout=900s
                    kubectl wait --for=condition=Ready pod --all -n elasticsearch --field-selector=status.phase!=Completed --timeout=900s
        
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
                echo 'Resolve branch -> commit id'
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
