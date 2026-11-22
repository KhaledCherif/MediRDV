package com.medirdv.rdvcore.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * RestClient used by rdv-core to call patient-service.
 * The base URL comes from PATIENT_SERVICE_URL (Docker DNS: http://patient-service:8081).
 */
@Configuration
public class PatientClientConfig {

    @Bean
    public RestClient patientRestClient(
            RestClient.Builder builder,
            @Value("${patient-service.base-url}") String baseUrl,
            @Value("${patient-service.connect-timeout:2s}") Duration connectTimeout,
            @Value("${patient-service.read-timeout:3s}") Duration readTimeout) {

        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(connectTimeout)
                .withReadTimeout(readTimeout);

        return builder
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
    }
}
