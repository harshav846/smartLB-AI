package com.smartlb.loadbalancerservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configures the shared {@link RestTemplate} used by the traffic-proxy layer.
 * Connection and read timeouts are externally configurable via application.yml.
 */
@Configuration
public class RestTemplateConfig {

    @Value("${app.proxy.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Value("${app.proxy.read-timeout-ms:30000}")
    private int readTimeoutMs;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return new RestTemplate(factory);
    }
}

