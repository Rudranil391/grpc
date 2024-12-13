package com.example.grpc.Config;

import com.example.grpc.HelloServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientConfig {

//    @Bean
//    public Tracer tracer() {
//        return GlobalOpenTelemetry.getTracer("com.example.grpc");
//    }

    @Bean
    public ManagedChannel managedChannel(GrpcTelemetry grpcTelemetry) {
        // Initialize GrpcTelemetry for tracing

        return ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .intercept(grpcTelemetry.newClientInterceptor())
                .build();
    }

    @Bean
    public HelloServiceGrpc.HelloServiceBlockingStub helloServiceBlockingStub(ManagedChannel channel) {
        return HelloServiceGrpc.newBlockingStub(channel);
    }


}



