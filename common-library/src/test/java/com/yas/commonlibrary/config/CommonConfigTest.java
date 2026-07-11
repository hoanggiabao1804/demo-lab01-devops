package com.yas.commonlibrary.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

class CommonConfigTest {

    @Test
    void serviceUrlConfig_recordExposesConfiguredUrls() {
        ServiceUrlConfig serviceUrlConfig = new ServiceUrlConfig("http://media", "http://product");

        assertEquals("http://media", serviceUrlConfig.media());
        assertEquals("http://product", serviceUrlConfig.product());
    }

    @Test
    void corsConfigure_registersCorsMapping() {
        CorsConfig corsConfig = new CorsConfig();
        ReflectionTestUtils.setField(corsConfig, "allowedOrigins", "*");

        WebMvcConfigurer configurer = corsConfig.corsConfigure();

        assertNotNull(configurer);
        configurer.addCorsMappings(new CorsRegistry());
    }
}
