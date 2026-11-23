package com.medirdv.praticien.grpc;

import io.grpc.protobuf.services.ProtoReflectionService;
import net.devh.boot.grpc.server.serverfactory.GrpcServerConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcReflectionConfiguration {

    @Bean
    GrpcServerConfigurer grpcReflectionConfigurer() {
        return serverBuilder -> serverBuilder.addService(ProtoReflectionService.newInstance());
    }
}
