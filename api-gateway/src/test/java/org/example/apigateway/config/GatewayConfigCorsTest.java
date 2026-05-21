package org.example.apigateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayConfigCorsTest {

    // getCorsConfigurations() is protected — subclass to expose it
    private static class InspectableCorsRegistry extends CorsRegistry {
        Map<String, CorsConfiguration> configurations() {
            return getCorsConfigurations();
        }
    }

    private GatewayConfig configWithOrigin(String origin) {
        GatewayConfig config = new GatewayConfig();
        ReflectionTestUtils.setField(config, "allowedOrigin", origin);
        return config;
    }

    private CorsConfiguration applyCors(GatewayConfig config) {
        InspectableCorsRegistry registry = new InspectableCorsRegistry();
        config.addCorsMappings(registry);
        return registry.configurations().get("/**");
    }

    @Test
    void allowedOrigins_containsOnlyConfiguredOrigin() {
        CorsConfiguration cors = applyCors(configWithOrigin("http://localhost:8084"));

        assertThat(cors.getAllowedOrigins()).containsExactly("http://localhost:8084");
    }

    @Test
    void allowedMethods_containsRequiredHttpVerbs() {
        CorsConfiguration cors = applyCors(configWithOrigin("http://localhost:8084"));

        assertThat(cors.getAllowedMethods())
                .containsExactlyInAnyOrder("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }

    @Test
    void corsMapping_coversAllPaths() {
        InspectableCorsRegistry registry = new InspectableCorsRegistry();
        configWithOrigin("http://localhost:8084").addCorsMappings(registry);

        assertThat(registry.configurations()).containsKey("/**");
    }
}
