package com.yas.commonlibrary.kafka.cdc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.retrytopic.SameIntervalTopicReuseStrategy;

class RetrySupportDqlTest {

    @Test
    void annotationDefaultValues_areAvailableAtRuntime() throws Exception {
        Method method = RetryFixture.class.getDeclaredMethod("defaultRetry");
        RetrySupportDql annotation = method.getAnnotation(RetrySupportDql.class);

        assertNotNull(annotation);
        assertEquals("4", annotation.attempts());
        assertEquals("true", annotation.autoCreateTopics());
        assertEquals("", annotation.listenerContainerFactory());
        assertEquals(SameIntervalTopicReuseStrategy.SINGLE_TOPIC, annotation.sameIntervalTopicReuseStrategy());
        assertEquals(0, annotation.exclude().length);
        assertNotNull(annotation.kafkaBackoff());
        assertEquals(6000, annotation.backoff().value());
    }

    @Test
    void annotationCustomValues_overrideDefaults() throws Exception {
        Method method = RetryFixture.class.getDeclaredMethod("customRetry");
        RetrySupportDql annotation = method.getAnnotation(RetrySupportDql.class);

        assertEquals("7", annotation.attempts());
        assertEquals("false", annotation.autoCreateTopics());
        assertEquals("customFactory", annotation.listenerContainerFactory());
        assertArrayEquals(new Class[]{IllegalArgumentException.class}, annotation.exclude());
    }

    private static class RetryFixture {
        @RetrySupportDql
        void defaultRetry() {
        }

        @RetrySupportDql(
            attempts = "7",
            autoCreateTopics = "false",
            listenerContainerFactory = "customFactory",
            exclude = IllegalArgumentException.class
        )
        void customRetry() {
        }
    }
}
