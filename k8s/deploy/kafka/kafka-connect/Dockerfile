FROM quay.io/debezium/connect:3.3 AS debezium

FROM quay.io/strimzi/kafka:1.1.0-kafka-4.3.0

USER root
RUN mkdir -p /opt/kafka/plugins/debezium-connector-postgres
COPY --from=debezium /kafka/connect/debezium-connector-postgres/ /opt/kafka/plugins/debezium-connector-postgres/
RUN chown -R 1001:0 /opt/kafka/plugins/debezium-connector-postgres
USER 1001
