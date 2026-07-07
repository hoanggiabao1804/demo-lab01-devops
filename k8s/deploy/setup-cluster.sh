#!/bin/bash
set -euo pipefail
set -x

# Add chart repos and update
helm repo add postgres-operator-charts https://opensource.zalando.com/postgres-operator/charts/postgres-operator --force-update
helm repo add strimzi https://strimzi.io/charts/ --force-update
helm repo add akhq https://akhq.io/ --force-update
helm repo add elastic https://helm.elastic.co --force-update
helm repo add grafana https://grafana.github.io/helm-charts --force-update
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts --force-update
helm repo add open-telemetry https://open-telemetry.github.io/opentelemetry-helm-charts --force-update
helm repo add jetstack https://charts.jetstack.io --force-update
helm repo update

# Read configuration value from cluster-config.yaml file
mapfile -t CLUSTER_CONFIG < <(yq -r '.domain, .postgresql.replicas, .postgresql.username,
 .postgresql.password, .kafka.replicas, .zookeeper.replicas,
 .elasticsearch.replicas, .grafana.username, .grafana.password' ./cluster-config.yaml)
DOMAIN="${CLUSTER_CONFIG[0]}"
POSTGRESQL_REPLICAS="${CLUSTER_CONFIG[1]}"
POSTGRESQL_USERNAME="${CLUSTER_CONFIG[2]}"
POSTGRESQL_PASSWORD="${CLUSTER_CONFIG[3]}"
KAFKA_REPLICAS="${CLUSTER_CONFIG[4]}"
ZOOKEEPER_REPLICAS="${CLUSTER_CONFIG[5]}"
ELASTICSEARCH_REPLICAES="${CLUSTER_CONFIG[6]}"
GRAFANA_USERNAME="${CLUSTER_CONFIG[7]}"
GRAFANA_PASSWORD="${CLUSTER_CONFIG[8]}"

# Install the postgres-operator
helm upgrade --install postgres-operator postgres-operator-charts/postgres-operator \
 --create-namespace --namespace postgres
kubectl rollout status deployment/postgres-operator --timeout=180s \
 --namespace postgres

# Đợi Postgres Operator sẵn sàng trước khi tạo Cluster DB
echo "Waiting for postgres-operator to be ready..."
kubectl rollout status deployment/postgres-operator -n postgres --timeout=180s

# Install postgresql
helm upgrade --install postgres ./postgres/postgresql \
--create-namespace --namespace postgres \
--set replicas="$POSTGRESQL_REPLICAS" \
--set username="$POSTGRESQL_USERNAME" \
--set password="$POSTGRESQL_PASSWORD"

# Install pgadmin
pg_admin_hostname="pgadmin.$DOMAIN" yq -i '.hostname=env(pg_admin_hostname)' ./postgres/pgadmin/values.yaml
helm upgrade --install pgadmin ./postgres/pgadmin \
--create-namespace --namespace postgres

# Install strimzi-kafka-operator
helm upgrade --install kafka-operator strimzi/strimzi-kafka-operator \
--create-namespace --namespace kafka
kubectl wait --for=condition=established --timeout=180s crd/kafkas.kafka.strimzi.io
kubectl wait --for=condition=established --timeout=180s crd/kafkaconnects.kafka.strimzi.io
kubectl wait --for=condition=established --timeout=180s crd/kafkaconnectors.kafka.strimzi.io
kubectl rollout status deployment/strimzi-cluster-operator --timeout=180s \
 --namespace kafka

# Đợi Strimzi Operator đăng ký hoàn tất các Custom Resource Definitions (CRDs) vào API Server
echo "Waiting for Strimzi Kafka CRDs to be established..."
kubectl wait --for=condition=Established crd/kafkas.kafka.strimzi.io --timeout=180s
kubectl wait --for=condition=Established crd/kafkaconnects.kafka.strimzi.io --timeout=180s
kubectl wait --for=condition=Established crd/kafkaconnectors.kafka.strimzi.io --timeout=180s

# Build Kafka Connect image with the Debezium PostgreSQL plugin into Minikube.
DEBEZIUM_CONNECT_IMAGE="yas-debezium-connect-postgresql:1.0.1-kafka-4.3.0-debezium-3.3"
minikube image build -t "$DEBEZIUM_CONNECT_IMAGE" ./kafka/kafka-connect

# Install kafka and postgresql connector
helm upgrade --install kafka-cluster ./kafka/kafka-cluster \
--create-namespace --namespace kafka \
--set kafka.replicas="$KAFKA_REPLICAS" \
--set zookeeper.replicas="$ZOOKEEPER_REPLICAS" \
--set postgresql.username="$POSTGRESQL_USERNAME" \
--set postgresql.password="$POSTGRESQL_PASSWORD" \
--set debeziumConnect.image="$DEBEZIUM_CONNECT_IMAGE"

# Install akhq
akhq_hostname="akhq.$DOMAIN" yq -i '.hostname=env(akhq_hostname)' ./kafka/akhq.values.yaml
helm upgrade --install akhq akhq/akhq \
--create-namespace --namespace kafka \
--values ./kafka/akhq.values.yaml

# Install elastic-operator
helm upgrade --install elastic-operator elastic/eck-operator \
 --create-namespace --namespace elasticsearch
kubectl rollout status statefulset/elastic-operator --timeout=180s \
 --namespace elasticsearch

# Đợi Elastic Operator sẵn sàng để tránh lỗi khi deploy cluster ngay sau đó
echo "Waiting for elastic-operator to be ready..."
kubectl rollout status statefulset/elastic-operator -n elasticsearch --timeout=180s

# Install elasticsearch-cluster
helm upgrade --install elasticsearch-cluster ./elasticsearch/elasticsearch-cluster \
--create-namespace --namespace elasticsearch \
--set elasticsearch.replicas="$ELASTICSEARCH_REPLICAES" \
--set kibana.ingress.hostname="kibana.$DOMAIN"

# Install loki (Thêm flag sử dụng Test Schema để tránh lỗi Validate đòi cấu hình lưu trữ dài hạn)
helm upgrade --install loki grafana/loki \
 --create-namespace --namespace observability \
 -f ./observability/loki.values.yaml

# Install tempo
helm upgrade --install tempo grafana/tempo \
--create-namespace --namespace observability \
-f ./observability/tempo.values.yaml

# Install cert manager
helm upgrade --install cert-manager jetstack/cert-manager \
  --namespace cert-manager \
  --create-namespace \
  --version v1.12.0 \
  --set installCRDs=true \
  --set prometheus.enabled=false \
  --set webhook.timeoutSeconds=4 \
  --set admissionWebhooks.certManager.create=true
kubectl rollout status deployment/cert-manager --timeout=180s \
  --namespace cert-manager
kubectl rollout status deployment/cert-manager-cainjector --timeout=180s \
  --namespace cert-manager
kubectl rollout status deployment/cert-manager-webhook --timeout=180s \
  --namespace cert-manager

kubectl rollout status deployment/cert-manager -n cert-manager --timeout=180s
kubectl rollout status deployment/cert-manager-webhook -n cert-manager --timeout=180s
kubectl rollout status deployment/cert-manager-cainjector -n cert-manager --timeout=180s

# Install opentelemetry-operator
helm upgrade --install opentelemetry-operator open-telemetry/opentelemetry-operator \
--create-namespace --namespace observability
kubectl wait --for=condition=established --timeout=180s crd/opentelemetrycollectors.opentelemetry.io
kubectl rollout status deployment/opentelemetry-operator --timeout=180s \
 --namespace observability

# Đợi Webhook của OpenTelemetry Operator hoàn toàn "sống" để xử lý chuyển đổi dữ liệu thành công
echo "Waiting for OpenTelemetry operator webhook service to be ready..."
set +x
kubectl rollout status deployment/opentelemetry-operator -n observability --timeout=180s
kubectl wait --for=condition=Ready pod -n observability \
  -l app.kubernetes.io/name=opentelemetry-operator \
  --timeout=180s

for i in {1..90}; do
  WEBHOOK_ENDPOINT_READY="$(kubectl get endpointslice -n observability \
    -l kubernetes.io/service-name=opentelemetry-operator-webhook \
    -o jsonpath='{.items[0].endpoints[0].conditions.ready}' 2>/dev/null || true)"

  if [ "$WEBHOOK_ENDPOINT_READY" = "true" ]; then
    # The endpoint can appear just before the TLS webhook listener accepts
    # connections through the Service. Give kube-proxy a short settle window.
    sleep 30
    break
  fi

  if [ "$i" -eq 90 ]; then
    echo "Timed out waiting for OpenTelemetry operator webhook server" >&2
    exit 1
  fi

  sleep 2
done
set -x

# Install opentelemetry-collector
helm upgrade --install opentelemetry-collector ./observability/opentelemetry \
--create-namespace --namespace observability

set +x
echo "Waiting for OpenTelemetry collector to be ready..."
kubectl rollout status deployment/opentelemetry-collector -n observability --timeout=180s
set -x

# Install promtail
set +x
echo "Preparing node inotify limits for promtail..."
for NODE in minikube minikube-m02; do
  if minikube ssh -n "$NODE" -- test -e /proc/sys/fs/inotify/max_user_instances 2>/dev/null; then
    minikube ssh -n "$NODE" -- "sudo sh -c 'echo 1024 > /proc/sys/fs/inotify/max_user_instances && echo 1048576 > /proc/sys/fs/inotify/max_user_watches'"
  fi
done
set -x

helm upgrade --install promtail grafana/promtail \
--create-namespace --namespace observability \
--values ./observability/promtail.values.yaml

# Minikube can keep many old container log files under /var/log/pods. Promtail
# may hit the container's default nofile limit while creating file targets, so
# raise it before starting the Promtail process.
set +x
echo "Patching promtail runtime limits and waiting for rollout..."
kubectl patch daemonset promtail -n observability --type='json' -p='[
  {"op":"add","path":"/spec/template/spec/containers/0/command","value":["/bin/sh","-c"]},
  {"op":"replace","path":"/spec/template/spec/containers/0/args","value":["ulimit -n 65536 || ulimit -n 4096 || true; exec /usr/bin/promtail -config.file=/etc/promtail/promtail.yaml"]}
]'
kubectl rollout status daemonset/promtail -n observability --timeout=180s
set -x

# Install prometheus + grafana (Bổ sung flag tắt cơ chế chặn mật khẩu tường minh assertNoLeakedSecrets)
grafana_hostname="grafana.$DOMAIN" yq -i '.hostname=env(grafana_hostname)' ./observability/prometheus.values.yaml
postgresql_username="$POSTGRESQL_USERNAME" yq -i '.grafana."grafana.ini".database.user=env(postgresql_username)' ./observability/prometheus.values.yaml
postgresql_password="$POSTGRESQL_PASSWORD" yq -i '.grafana."grafana.ini".database.password=env(postgresql_password)' ./observability/prometheus.values.yaml
helm upgrade --install prometheus prometheus-community/kube-prometheus-stack \
 --create-namespace --namespace observability \
-f ./observability/prometheus.values.yaml \
--set grafana.assertNoLeakedSecrets=false

set +x
echo "Waiting for Prometheus stack to be ready..."
kubectl rollout status deployment/prometheus-kube-prometheus-operator -n observability --timeout=180s
kubectl rollout status deployment/prometheus-kube-state-metrics -n observability --timeout=180s
kubectl rollout status deployment/prometheus-grafana -n observability --timeout=180s
kubectl rollout status daemonset/prometheus-prometheus-node-exporter -n observability --timeout=180s
kubectl rollout status statefulset/prometheus-prometheus-kube-prometheus-prometheus -n observability --timeout=240s
kubectl rollout status statefulset/alertmanager-prometheus-kube-prometheus-alertmanager -n observability --timeout=240s
set -x

# Install grafana operator
helm upgrade --install grafana-operator oci://ghcr.io/grafana-operator/helm-charts/grafana-operator \
--version v5.0.2 \
--create-namespace --namespace observability
kubectl rollout status deployment/grafana-operator --timeout=180s \
 --namespace observability

set +x
echo "Waiting for Grafana operator to be ready..."
kubectl rollout status deployment/grafana-operator -n observability --timeout=180s
set -x

# Add datasource and dashboard to grafana
helm upgrade --install grafana ./observability/grafana \
--create-namespace --namespace observability \
--set hostname="grafana.$DOMAIN" \
--set grafana.username="$GRAFANA_USERNAME" \
--set grafana.password="$GRAFANA_PASSWORD" \
--set postgresql.username="$POSTGRESQL_USERNAME" \
--set postgresql.password="$POSTGRESQL_PASSWORD"

helm upgrade --install zookeeper ./zookeeper \
 --namespace zookeeper --create-namespace
