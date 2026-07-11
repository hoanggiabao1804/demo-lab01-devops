package com.yas.commonlibrary.kafka.cdc.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.yas.commonlibrary.kafka.cdc.message.Product;
import org.junit.jupiter.api.Test;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;

class BaseKafkaListenerConfigTest {

    @Test
    void kafkaListenerContainerFactory_usesTypedConsumerFactory() {
        KafkaProperties kafkaProperties = new KafkaProperties();
        TestKafkaListenerConfig config = new TestKafkaListenerConfig(kafkaProperties);

        ConcurrentKafkaListenerContainerFactory<String, Product> factory = config.listenerContainerFactory();

        assertNotNull(factory);
        assertNotNull(factory.getConsumerFactory());
    }

    private static class TestKafkaListenerConfig extends BaseKafkaListenerConfig<String, Product> {
        TestKafkaListenerConfig(KafkaProperties kafkaProperties) {
            super(String.class, Product.class, kafkaProperties);
        }

        @Override
        public ConcurrentKafkaListenerContainerFactory<String, Product> listenerContainerFactory() {
            return kafkaListenerContainerFactory();
        }
    }
}
