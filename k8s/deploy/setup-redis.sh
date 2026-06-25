#!/bin/bash
set -euo pipefail
set -x

# Read configuration value from cluster-config.yaml file
REDIS_PASSWORD="$(yq -r '.redis.password' ./cluster-config.yaml)"

helm upgrade --install redis \
  --set auth.password="$REDIS_PASSWORD" \
  --set volumePermissions.enabled=true \
  --set volumePermissions.image.registry=registry-1.docker.io \
  --set volumePermissions.image.repository=bitnami/os-shell \
  --set volumePermissions.image.tag=latest \
  oci://registry-1.docker.io/bitnamicharts/redis -n redis --create-namespace
