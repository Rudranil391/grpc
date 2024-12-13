package com.example.grpc.Service;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class GrpcServerRunner implements CommandLineRunner {

    private final int port = 50051;
    private Server server;
    private final GrpcTelemetry grpcTelemetry; // Injected by Spring
    private final HelloServiceImpl helloService;

    @Autowired
    public GrpcServerRunner(GrpcTelemetry grpcTelemetry, HelloServiceImpl helloService) {
        this.grpcTelemetry = grpcTelemetry;
        this.helloService = helloService; // Inject HelloServiceImpl
    }

    @Override
    public void run(String... args) throws Exception {
        start();
        System.out.println("gRPC server started on port: " + port);
    }

    private void start() throws Exception {
        // Initialize GrpcTelemetry for tracing

        //System.out.println(grpcTelemetry);
        server = ServerBuilder.forPort(port)
                .addService(helloService)
                .intercept(grpcTelemetry.newServerInterceptor())
                .build()
                .start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("Shutting down gRPC server");
            stop();
            System.err.println("Server shut down");
        }));
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }
}
