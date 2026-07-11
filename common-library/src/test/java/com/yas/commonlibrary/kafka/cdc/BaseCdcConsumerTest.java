package com.yas.commonlibrary.kafka.cdc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.MessageHeaders;

class BaseCdcConsumerTest {

    private final TestCdcConsumer consumer = new TestCdcConsumer();

    @Test
    void processMessage_withOnlyRecord_invokesConsumer() {
        AtomicReference<String> processedValue = new AtomicReference<>();
        MessageHeaders headers = headersWithReceivedKey("key-1");

        consumer.processRecord("value-1", headers, processedValue::set);

        assertEquals("value-1", processedValue.get());
    }

    @Test
    void processMessage_withKeyAndValue_invokesBiConsumer() {
        AtomicReference<String> processedKey = new AtomicReference<>();
        AtomicReference<String> processedValue = new AtomicReference<>();
        MessageHeaders headers = headersWithReceivedKey("ignored-header-key");

        consumer.processRecord("key-2", "value-2", headers, (key, value) -> {
            processedKey.set(key);
            processedValue.set(value);
        });

        assertEquals("key-2", processedKey.get());
        assertEquals("value-2", processedValue.get());
    }

    private MessageHeaders headersWithReceivedKey(String key) {
        Map<String, Object> values = new HashMap<>();
        values.put(KafkaHeaders.RECEIVED_KEY, key);
        return new MessageHeaders(values);
    }

    private static class TestCdcConsumer extends BaseCdcConsumer<String, String> {
        void processRecord(String value, MessageHeaders headers, java.util.function.Consumer<String> delegate) {
            processMessage(value, headers, delegate);
        }

        void processRecord(String key, String value, MessageHeaders headers,
                           java.util.function.BiConsumer<String, String> delegate) {
            processMessage(key, value, headers, delegate);
        }
    }
}
