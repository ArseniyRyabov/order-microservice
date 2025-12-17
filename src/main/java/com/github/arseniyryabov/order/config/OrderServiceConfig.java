package com.github.arseniyryabov.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class OrderServiceConfig {

    @Bean
    public RestClient userServiceRestClient() {
        return RestClient.builder()
                .baseUrl("http://user-service:8080")
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Bean
    public RestClient productServiceRestClient() {
        return RestClient.builder()
                .baseUrl("http://product-service:8081/api")
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
