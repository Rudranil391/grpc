package com.example.grpc.Service;

import com.example.grpc.Hello;
import com.example.grpc.HelloServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class HelloServiceImpl extends HelloServiceGrpc.HelloServiceImplBase {

    //private final Tracer tracer = GlobalOpenTelemetry.get().getTracer("com.example.grpc.Hello");
    private static final  org.slf4j.Logger logger = LoggerFactory.getLogger(HelloServiceImpl.class);
    //private final Logger otelLogger;

    private final Tracer tracer; // Declare tracer as a final field
    private final GrpcTelemetry grpcTelemetry;
    private final MeterRegistry meterRegistry; // Add MeterRegistry as a field
    private final LongCounter requestCounter;
    private AtomicInteger activeRequestsCount = new AtomicInteger(0); // Field to hold active request count

    @Autowired // Use constructor injection to inject the Tracer
    public HelloServiceImpl(Tracer tracer, GrpcTelemetry grpcTelemetry, MeterRegistry meterRegistry, Meter meter) {
        this.tracer = tracer;
        this.grpcTelemetry=grpcTelemetry;
        this.meterRegistry = meterRegistry;  // Use .gauge() or .counter() as appropriate
        this.requestCounter = meter.counterBuilder("hello_service_requests")
                .setDescription("Number of requests to Hello Service")
                .setUnit("1")
                .build();

        // Register a Gauge to track the number of active requests
        Gauge.builder("hello_service_active_requests", () -> activeRequestsCount)
                .description("Number of active requests to Hello Service")
                .register(meterRegistry);

       // this.otelLogger = (Logger) loggerProvider.get("otelLogger");
    }
    @Override
    public void sayHello(Hello.HelloRequest request, StreamObserver<Hello.HelloResponse> responseObserver) {
        activeRequestsCount.incrementAndGet();

        // Start a new span for the current request
        Span span = tracer.spanBuilder("sayHello").startSpan();

        try (Scope scope = span.makeCurrent()) {

            // Increment the request counter
            requestCounter.add(1); // Record the request
            // Log details for the request

            logger.info("Processing sayHello request in Service 1 for name: {}", request.getName());

            // Create a gRPC channel with an OpenTelemetry context propagation interceptor
            ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50052)
                    .usePlaintext() // Use plaintext for simplicity; switch to SSL in production
                    .intercept(grpcTelemetry.newClientInterceptor()) // Add the OpenTelemetry client interceptor
                    .build();

            // Create a stub for Service 2
            HelloServiceGrpc.HelloServiceBlockingStub stub = HelloServiceGrpc.newBlockingStub(channel);

            System.out.println("ghghgh");
            System.out.println(span);
            // Call Service 2
            Hello.HelloResponse responseFromService2 = stub.sayHello(request);

            System.out.println("ghghghi");
            // Build the response back to the client
            Hello.HelloResponse response = Hello.HelloResponse.newBuilder()
                    .setMessage("Hello from Service 1. Service 2 says: " + responseFromService2.getMessage())
                    .build();

            // Add custom attribute to span
            span.setAttribute(AttributeKey.stringKey("responseMessageKey1"),response.getMessage());
            span.addEvent("Recieved message");
            //System.out.println(span);
            // Send the response back to the original client
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error while calling Service 2", e);
            responseObserver.onError(e);
        } finally {
            // End the span
            activeRequestsCount.decrementAndGet();
            span.end();
        }
    }
}

