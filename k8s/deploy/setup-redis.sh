#!/bin/bash
set -euo pipefail
set -x

# Read configuration value from cluster-config.yaml file
REDIS_PASSWORD="$(yq -r '.redis.password' ./cluster-config.yaml)"

helm upgrade --install redis oci://registry-1.docker.io/bitnamicharts/redis \
  --namespace redis \
  --create-namespace \
  --set auth.password="$REDIS_PASSWORD" \
  --set volumePermissions.enabled=true
