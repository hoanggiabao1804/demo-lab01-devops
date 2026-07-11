#!/usr/bin/env bash
set -u

NAMESPACE="${NAMESPACE:-staging}"
CURL_IMAGE="${CURL_IMAGE:-curlimages/curl:8.10.1}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-10}"
POD_READY_TIMEOUT="${POD_READY_TIMEOUT:-120s}"
MODE="${1:-full}"
TEST_LABEL="app=istio-authz-test"
LOG_FILE="${LOG_FILE:-k8s/deploy/istio/authorization-policy-test-$(date +%Y%m%d-%H%M%S).log}"

mkdir -p "$(dirname "${LOG_FILE}")"
exec > >(tee "${LOG_FILE}") 2>&1

SOURCES=(
  default
  backoffice-bff
  storefront-bff
  backoffice-ui
  storefront-ui
  cart
  customer
  inventory
  location
  media
  order
  payment
  payment-paypal
  product
  promotion
  rating
  recommendation
  sampledata
  search
  tax
  webhook
)

DESTINATIONS=(
  backoffice-bff
  storefront-bff
  backoffice-ui
  storefront-ui
  cart
  customer
  inventory
  location
  media
  order
  payment
  payment-paypal
  product
  promotion
  rating
  recommendation
  sampledata
  search
  tax
  webhook
)

QUICK_CASES=(
  "order product allow"
  "order cart allow"
  "payment order allow"
  "product media allow"
  "backoffice-bff product allow"
  "storefront-bff cart allow"
  "default product deny"
  "product order deny"
  "order inventory deny"
  "media product deny"
)

declare -A URLS=(
  [backoffice-bff]="http://backoffice-bff/"
  [storefront-bff]="http://storefront-bff/"
  [backoffice-ui]="http://backoffice-ui:3000/"
  [storefront-ui]="http://storefront-ui:3000/"
  [cart]="http://cart/cart/v3/api-docs"
  [customer]="http://customer/customer/v3/api-docs"
  [inventory]="http://inventory/inventory/v3/api-docs"
  [location]="http://location/location/v3/api-docs"
  [media]="http://media/media/v3/api-docs"
  [order]="http://order/order/v3/api-docs"
  [payment]="http://payment/payment/v3/api-docs"
  [payment-paypal]="http://payment-paypal/payment-paypal/v3/api-docs"
  [product]="http://product/product/v3/api-docs"
  [promotion]="http://promotion/promotion/v3/api-docs"
  [rating]="http://rating/rating/v3/api-docs"
  [recommendation]="http://recommendation/recommendation/v3/api-docs"
  [sampledata]="http://sampledata/sampledata/v3/api-docs"
  [search]="http://search/search/v3/api-docs"
  [tax]="http://tax/tax/v3/api-docs"
  [webhook]="http://webhook/webhook/v3/api-docs"
)

declare -A ALLOW_MAP=(
  [backoffice-bff]="*"
  [storefront-bff]="*"
  [backoffice-ui]="backoffice-bff"
  [storefront-ui]="storefront-bff"
  [product]="backoffice-bff storefront-bff cart inventory order promotion rating search recommendation"
  [media]="backoffice-bff storefront-bff cart payment product"
  [customer]="backoffice-bff storefront-bff order rating recommendation"
  [cart]="backoffice-bff storefront-bff order"
  [order]="backoffice-bff storefront-bff payment rating recommendation"
  [location]="backoffice-bff storefront-bff customer inventory tax"
  [inventory]="backoffice-bff storefront-bff"
  [tax]="backoffice-bff storefront-bff order"
  [promotion]="backoffice-bff storefront-bff order"
  [rating]="backoffice-bff storefront-bff product"
  [payment]="backoffice-bff storefront-bff"
  [payment-paypal]="backoffice-bff storefront-bff"
  [search]="backoffice-bff storefront-bff"
  [recommendation]="backoffice-bff storefront-bff"
  [webhook]="backoffice-bff storefront-bff"
  [sampledata]="backoffice-bff storefront-bff"
)

created_pods=()
failures=0
total=0

usage() {
  cat <<EOF
Usage:
  $0 [full|quick]

Environment variables:
  NAMESPACE=staging
  CURL_IMAGE=curlimages/curl:8.10.1
  TIMEOUT_SECONDS=10
  POD_READY_TIMEOUT=120s
  LOG_FILE=k8s/deploy/istio/authorization-policy-test-YYYYMMDD-HHMMSS.log

Examples:
  $0 quick
  $0 full
  LOG_FILE=/tmp/authz-test.log $0 full
EOF
}

cleanup() {
  echo
  echo "Cleaning test pods in namespace ${NAMESPACE}..."
  kubectl delete pod -n "${NAMESPACE}" -l "${TEST_LABEL}" --ignore-not-found=true --wait=false >/dev/null 2>&1 || true
}

trap cleanup EXIT INT TERM

contains_word() {
  local haystack="$1"
  local needle="$2"
  [[ " ${haystack} " == *" ${needle} "* ]]
}

expected_for() {
  local source="$1"
  local destination="$2"
  local allowed_sources="${ALLOW_MAP[$destination]:-}"

  if [[ "${allowed_sources}" == "*" ]]; then
    echo "allow"
  elif contains_word "${allowed_sources}" "${source}"; then
    echo "allow"
  else
    echo "deny"
  fi
}

pod_name_for() {
  local source="$1"
  echo "authz-curl-${source}" | tr '_' '-'
}

ensure_pod() {
  local source="$1"
  local pod
  pod="$(pod_name_for "${source}")"

  if kubectl get pod -n "${NAMESPACE}" "${pod}" >/dev/null 2>&1; then
    return 0
  fi

  echo "Creating test pod ${pod} with serviceAccount=${source}..."
  if ! kubectl run "${pod}" -n "${NAMESPACE}" \
    --image="${CURL_IMAGE}" \
    --restart=Never \
    --labels="${TEST_LABEL}" \
    --overrides="{\"spec\":{\"serviceAccountName\":\"${source}\"}}" \
    -- sleep 3600 >/dev/null; then
    echo "Failed to create pod ${pod}" >&2
    return 1
  fi

  created_pods+=("${pod}")
  if ! kubectl wait -n "${NAMESPACE}" --for=condition=Ready "pod/${pod}" --timeout="${POD_READY_TIMEOUT}" >/dev/null; then
    echo "Pod ${pod} did not become Ready within ${POD_READY_TIMEOUT}" >&2
    return 1
  fi
}

run_case() {
  local source="$1"
  local destination="$2"
  local expected="$3"
  local url="${URLS[$destination]}"
  local pod
  local output
  local exit_code
  local status
  local body
  local actual

  pod="$(pod_name_for "${source}")"
  if ! ensure_pod "${source}"; then
    total=$((total + 1))
    failures=$((failures + 1))
    printf 'FAIL  %-16s -> %-16s expected=%-5s actual=%-5s http=%s\n' "${source}" "${destination}" "${expected}" "pod-error" "n/a"
    return
  fi

  output="$(kubectl exec -n "${NAMESPACE}" "${pod}" -c "${pod}" -- sh -c "body=\$(mktemp); status=\$(curl -sS -m ${TIMEOUT_SECONDS} -o \"\$body\" -w '%{http_code}' '${url}' || true); printf '%s\n' \"\$status\"; cat \"\$body\"; rm -f \"\$body\"" 2>&1)"
  exit_code=$?

  status="$(printf '%s\n' "${output}" | sed -n '1p')"
  body="$(printf '%s\n' "${output}" | sed '1d')"

  if [[ ${exit_code} -ne 0 ]]; then
    actual="error"
  elif printf '%s' "${body}" | grep -q "RBAC: access denied"; then
    actual="deny"
  else
    actual="allow"
  fi

  total=$((total + 1))

  if [[ "${actual}" == "${expected}" ]]; then
    printf 'PASS  %-16s -> %-16s expected=%-5s actual=%-5s http=%s\n' "${source}" "${destination}" "${expected}" "${actual}" "${status}"
  else
    failures=$((failures + 1))
    printf 'FAIL  %-16s -> %-16s expected=%-5s actual=%-5s http=%s\n' "${source}" "${destination}" "${expected}" "${actual}" "${status}"
    printf '      url: %s\n' "${url}"
    printf '      first response lines:\n'
    printf '%s\n' "${body}" | sed -n '1,5p' | sed 's/^/        /'
  fi
}

run_quick() {
  local case_def
  local source
  local destination
  local expected

  for case_def in "${QUICK_CASES[@]}"; do
    read -r source destination expected <<<"${case_def}"
    run_case "${source}" "${destination}" "${expected}"
  done
}

run_full() {
  local source
  local destination
  local expected

  for source in "${SOURCES[@]}"; do
    for destination in "${DESTINATIONS[@]}"; do
      expected="$(expected_for "${source}" "${destination}")"
      run_case "${source}" "${destination}" "${expected}"
    done
  done
}

if [[ "${MODE}" == "-h" || "${MODE}" == "--help" ]]; then
  usage
  exit 0
fi

if [[ "${MODE}" != "full" && "${MODE}" != "quick" ]]; then
  usage
  exit 2
fi

echo "Testing Istio AuthorizationPolicy in namespace ${NAMESPACE} (${MODE} mode)"
echo "This script creates temporary curl pods labeled ${TEST_LABEL} and removes them at the end."
echo "Writing output to ${LOG_FILE}"
echo

if [[ "${MODE}" == "quick" ]]; then
  run_quick
else
  run_full
fi

echo
echo "Summary: total=${total}, failures=${failures}"

if [[ ${failures} -gt 0 ]]; then
  exit 1
fi
