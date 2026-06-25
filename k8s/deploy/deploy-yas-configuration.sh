#!/bin/bash
set -euo pipefail
set -x

# Auto restart when change configmap or secret
helm repo add stakater https://stakater.github.io/stakater-charts --force-update
helm repo update

helm dependency build ../charts/yas-configuration
helm upgrade --install yas-configuration ../charts/yas-configuration \
--namespace yas --create-namespace
