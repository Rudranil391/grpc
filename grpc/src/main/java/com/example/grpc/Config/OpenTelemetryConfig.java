package com.example.grpc.Config;


import com.example.grpc.Utility.MetricsParquetWriter;
import com.example.grpc.Utility.MetricsParser;
import com.example.grpc.database.MetricsData;
import com.example.grpc.database.MetricsDataRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.exporters.inmemory.InMemorySpanExporter;
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Configuration
public class OpenTelemetryConfig {

    @Bean
    @Primary
    public InMemorySpanExporter inMemorySpanExporter() {
        return InMemorySpanExporter.create(); // Define InMemorySpanExporter as a bean
    }


//    @Bean
//    public OtlpGrpcSpanExporter otlpGrpcSpanExporter() {
//        return OtlpGrpcSpanExporter.builder()
//                .setEndpoint("https://api.openobserve.ai") // OpenObserve gRPC endpoint
//                .addHeader("Authorization", "Basic cnVkcmFuaWwxOTk2ZGFzZ3VwdGFAZ21haWwuY29tOjJjNjBUM0U4SGF2SzlRTjcxdDQ1")
//                .addHeader("organization", "rudranil_organization_50503_22FfMYY6kkH42f7")
//                .addHeader("stream-name", "default")
//                .setTimeout(Duration.ofSeconds(5))
//                .build();
//    }

    @Bean
    public JaegerGrpcSpanExporter jaegerGrpcSpanExporter() {
        return JaegerGrpcSpanExporter.builder()
                .setEndpoint("http://localhost:14250") // Jaeger gRPC endpoint
                .setTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Bean
    public PrometheusMeterRegistry prometheusMeterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }

//    @Bean
//    public OtlpGrpcMetricExporter otlpGrpcMetricExporter() {
//        return OtlpGrpcMetricExporter.builder()
//                .setEndpoint("http://localhost:4317") // OpenTelemetry Collector endpoint
//                .setTimeout(Duration.ofSeconds(5))
//                .build();
//    }

//    @Bean
//    public SdkMeterProvider meterProvider(OtlpGrpcMetricExporter metricExporter) {
//        Resource resource = Resource.getDefault()
//                .merge(Resource.builder()
//                        .put(ResourceAttributes.SERVICE_NAME, "grpc-hello-service")
//                        .put(ResourceAttributes.SERVICE_VERSION, "1.0.0")
//                        .build());
//
//        return SdkMeterProvider.builder()
//                .setResource(resource)
//                .registerMetricReader(
//                        PeriodicMetricReader.builder(metricExporter)
//                                .setInterval(Duration.ofSeconds(5))
//                                .build()
//                )
//                .build();
//    }
//    @Bean
//    public PrometheusMeterRegistry prometheusMeterRegistry() {
//        // Set up Prometheus registry for Micrometer
//        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
//    }
//
//    @Bean
//    public MetricExporter prometheusMetricExporter(PrometheusMeterRegistry prometheusMeterRegistry) {
//        return new MetricExporter() {
//            @Override
//            public CompletableResultCode export(Collection<MetricData> metrics) {
//                prometheusMeterRegistry.scrape();
//                return CompletableResultCode.ofSuccess();
//            }
//
//            @Override
//            public CompletableResultCode shutdown() {
//                return CompletableResultCode.ofSuccess();
//            }
//
//            @Override
//            public CompletableResultCode flush() {
//                return CompletableResultCode.ofSuccess();
//            }
//
//            @Override
//            public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
//                return AggregationTemporality.CUMULATIVE;
//            }
//        };
//    }
//    @Bean
//    public SdkMeterProvider meterProvider(MetricExporter prometheusMetricExporter) {
//        Resource resource = Resource.getDefault()
//                .merge(Resource.builder()
//                        .put(ResourceAttributes.SERVICE_NAME, "grpc-hello-service")
//                        .put(ResourceAttributes.SERVICE_VERSION, "1.0.0")
//                        .build());
//
//        return SdkMeterProvider.builder()
//                .setResource(resource)
//                .registerMetricReader(
//                        PeriodicMetricReader.builder(prometheusMetricExporter)
//                                .setInterval(Duration.ofSeconds(5))
//                                .build()
//                )
//                .build();
//    }


//    @Bean
//    @Primary
//    public LoggerProvider loggerProvider() {
//        // Create a LoggerProvider
//        return SdkLoggerProvider.builder().build();
//    }

//    @Bean
//    public LogExporter logExporter() {
//        // Use an in-memory exporter to view logs for debugging (logs will be stored in memory)
//        InMemoryLogExporter logExporter = InMemoryLogExporter.create();
//        return logExporter;
//    }

    @Bean
    @Primary
    public OpenTelemetry openTelemetry(JaegerGrpcSpanExporter jaegerExporter, InMemorySpanExporter spanExporter) {

        TextMapPropagator textMapPropagator = W3CTraceContextPropagator.getInstance();

        // Create a Resource with service details
        Resource resource = Resource.getDefault()
                .merge(Resource.builder()
                        .put(ResourceAttributes.SERVICE_NAME, "grpc-hello-service")
                        .put(ResourceAttributes.SERVICE_VERSION, "1.0.0")
                        .build());

        // Create a SdkTracerProvider
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(BatchSpanProcessor.builder(jaegerExporter).build())
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
                .build();

//        // Create the SdkMeterProvider with the metric reader
//        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
//                .setResource(resource)
//                .registerMetricReader(PeriodicMetricReader.builder(metricExporter) // This line should correctly create the PeriodicMetricReader
//                        .setInterval(Duration.ofSeconds(5))
//                        .build()) // Register the metric reader
//                .build();

        // Configure the OpenTelemetry SDK with Context Propagators
//        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
//                .setResource(resource)
//                .registerMetricReader(PrometheusHttpServer.builder()
//                        .setPort(9464) // Port for Prometheus scraping endpoint
//                        .build())
//                .build();

        OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(textMapPropagator))
                .buildAndRegisterGlobal();

        //System.out.println("Current Context Propagators: " + GlobalOpenTelemetry.get().getPropagators());
        return openTelemetrySdk;

    }

    @Bean
    @Primary
    public Tracer tracer(OpenTelemetry openTelemetry) {
        // Retrieve and return the tracer with the specified service name
        return openTelemetry.getTracer("com.example.grpc.Hello");
    }

    @Bean
    @Primary
    public GrpcTelemetry grpcTelemetry(OpenTelemetry openTelemetry) {
        // Initialize GrpcTelemetry with the configured OpenTelemetry instance
        return GrpcTelemetry.create(openTelemetry);
    }



    @Bean
    public Meter meter(OpenTelemetry openTelemetry) {
        // Retrieve and return the Meter instance for creating metrics
        return openTelemetry.getMeter("com.example.grpc.HelloMetrics");
    }

    @Bean
    public  Gauge registerGauges(PrometheusMeterRegistry meterRegistry) {
        // Example of creating a Gauge
        return Gauge.builder("hello_service_active_requests", () -> getActiveRequestsCount())
                .description("Number of active requests to Hello Service")
                .register(meterRegistry);
    }

    // This is a placeholder method. Replace with your actual logic to get the active request count.
    private int getActiveRequestsCount() {
        // Your logic here (e.g., a static counter that increments/decrements with requests)
        return 2; // Replace with actual active request count
    }


    // Metrics Endpoint to expose Prometheus metrics
    @RestController
    static class MetricsEndpoint {

        private final PrometheusMeterRegistry prometheusMeterRegistry;

        @Autowired
        public MetricsEndpoint(PrometheusMeterRegistry prometheusMeterRegistry) {
            this.prometheusMeterRegistry = prometheusMeterRegistry;
        }
        @Autowired
        private MetricsDataRepository metricsDataRepository;

        @GetMapping("/metrics")
        public String scrapeMetrics() throws IOException {
            String metricsText = prometheusMeterRegistry.scrape();
            //String metricsText1 = prometheusMeterRegistry.scrape();
            //System.out.println(metrics);

            List<MetricsParquetWriter.Metric> metrics = MetricsParser.parseMetrics(metricsText);
            MetricsParquetWriter writer = new MetricsParquetWriter();
            String path=writer.writeMetricsToParquet(metrics, "output/metrics.parquet");

            byte[] metricsData = Files.readAllBytes(Path.of(path));

            // Create a new MetricsData entity
            MetricsData metricsDataEntity = new MetricsData(metricsData, LocalDateTime.now(),"Service_1");

            // Save the entity to the database
            metricsDataRepository.save(metricsDataEntity);
            Files.delete(Path.of(path));

            return metricsText + "# EOF";
            }


        private List<MetricData> collectMetrics() {
            // Your logic here to convert prometheus metrics to OpenTelemetry MetricData or another format
            // Example placeholder
            return new ArrayList<>();
        }




    }

}

