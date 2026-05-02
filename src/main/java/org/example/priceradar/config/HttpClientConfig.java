package org.example.priceradar.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpClientConfig {

    @Bean
    public PoolingHttpClientConnectionManager connectionManager() {
        PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
        manager.setMaxTotal(100);        // максимум з'єднань всього
        manager.setDefaultMaxPerRoute(20); // максимум з'єднань на один хост
        return manager;
    }

    @Bean
    public CloseableHttpClient httpClient(PoolingHttpClientConnectionManager connectionManager) {
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();
    }
}