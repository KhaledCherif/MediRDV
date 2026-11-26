package com.medirdv.rdvcore.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class PraticienMetadataConfig {
    @Bean
    public RestClient praticienMetadataRestClient(
            RestClient.Builder builder,
            @Value("${praticien-service.rest.base-url}") String baseUrl,
            @Value("${praticien-service.rest.timeout:3s}") Duration timeout) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(timeout).withReadTimeout(timeout);
        return builder.baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
    }
}
