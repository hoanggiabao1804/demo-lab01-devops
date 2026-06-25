#!/bin/bash
set -euo pipefail
set -x

# Read configuration value from cluster-config.yaml file
mapfile -t KEYCLOAK_CONFIG < <(yq -r '.domain,
  .postgresql.username, .postgresql.password,
  .keycloak.bootstrapAdmin.username, .keycloak.bootstrapAdmin.password,
  .keycloak.backofficeRedirectUrl, .keycloak.storefrontRedirectUrl' ./cluster-config.yaml)
DOMAIN="${KEYCLOAK_CONFIG[0]}"
POSTGRESQL_USERNAME="${KEYCLOAK_CONFIG[1]}"
POSTGRESQL_PASSWORD="${KEYCLOAK_CONFIG[2]}"
BOOTSTRAP_ADMIN_USERNAME="${KEYCLOAK_CONFIG[3]}"
BOOTSTRAP_ADMIN_PASSWORD="${KEYCLOAK_CONFIG[4]}"
KEYCLOAK_BACKOFFICE_REDIRECT_URL="${KEYCLOAK_CONFIG[5]}"
KEYCLOAK_STOREFRONT_REDIRECT_URL="${KEYCLOAK_CONFIG[6]}"

# Install CRD keycloak
kubectl create namespace keycloak --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -f https://raw.githubusercontent.com/keycloak/keycloak-k8s-resources/26.0.2/kubernetes/keycloaks.k8s.keycloak.org-v1.yml
kubectl apply -f https://raw.githubusercontent.com/keycloak/keycloak-k8s-resources/26.0.2/kubernetes/keycloakrealmimports.k8s.keycloak.org-v1.yml
kubectl apply -f https://raw.githubusercontent.com/keycloak/keycloak-k8s-resources/26.0.2/kubernetes/kubernetes.yml -n keycloak

kubectl rollout status deployment/keycloak-operator -n keycloak --timeout=180s

# Install keycloak
helm upgrade --install keycloak ./keycloak/keycloak \
--namespace keycloak \
--set hostname="identity.$DOMAIN" \
--set postgresql.username="$POSTGRESQL_USERNAME" \
--set postgresql.password="$POSTGRESQL_PASSWORD" \
--set bootstrapAdmin.username="$BOOTSTRAP_ADMIN_USERNAME" \
--set bootstrapAdmin.password="$BOOTSTRAP_ADMIN_PASSWORD" \
--set backofficeRedirectUrl="$KEYCLOAK_BACKOFFICE_REDIRECT_URL" \
--set storefrontRedirectUrl="$KEYCLOAK_STOREFRONT_REDIRECT_URL"
