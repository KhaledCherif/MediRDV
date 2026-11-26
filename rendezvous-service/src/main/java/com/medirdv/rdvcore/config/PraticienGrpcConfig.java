package com.medirdv.rdvcore.config;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.medirdv.praticien.grpc.PraticienServiceGrpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * gRPC channel toward praticien-service (Suivi 2).
 * Address comes from PRATICIEN_GRPC_HOST / PRATICIEN_GRPC_PORT (Docker DNS: praticien-service:9090).
 * Deadlines are applied per call in PraticienGrpcClient, not here.
 */
@Configuration
public class PraticienGrpcConfig {

    @Bean(destroyMethod = "shutdown")
    public ManagedChannel praticienChannel(
            @Value("${praticien-service.grpc.host}") String host,
            @Value("${praticien-service.grpc.port}") int port) {
        return ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .keepAliveTime(30, TimeUnit.SECONDS)
                .build();
    }

    @Bean
    public PraticienServiceGrpc.PraticienServiceBlockingStub praticienBlockingStub(ManagedChannel praticienChannel) {
        return PraticienServiceGrpc.newBlockingStub(praticienChannel);
    }
}
