package com.example.grpc.Controller;

import com.example.grpc.Hello;
import com.example.grpc.HelloServiceGrpc;
import com.example.grpc.Utility.ParquetWriterUtil;
import com.example.grpc.database.TraceData;
import com.example.grpc.database.TraceDataRepository;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.exporters.inmemory.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RestController
public class HttpController {
    Logger logger = LoggerFactory.getLogger(HttpController.class);
    @Autowired
    private InMemorySpanExporter inMemorySpanExporter;
    private final HelloServiceGrpc.HelloServiceBlockingStub grpcClient;

    @Autowired
    private TraceDataRepository traceDataRepository;

    // Inject the gRPC client (assuming it's already configured as a bean)
    public HttpController(HelloServiceGrpc.HelloServiceBlockingStub grpcClient) {
        this.grpcClient = grpcClient;
    }

    @GetMapping("/api/hello")
    public String sayHello(@RequestParam String name) {
        // Create a gRPC request
        Hello.HelloRequest request = Hello.HelloRequest.newBuilder()
                .setName(name)
                .build();

        // Log the trace ID before sending the request
        Span currentSpan = Span.current();
        logger.info("Sending request to Service 2 with trace ID: {}", currentSpan.getSpanContext().getTraceId());
        // Call the gRPC service
        Hello.HelloResponse response = grpcClient.sayHello(request);


        // Log the response received
        logger.info("Received response from Service 2: {}", response.getMessage());

        List<SpanData> spans = inMemorySpanExporter.getFinishedSpanItems();
        logger.info("Finished spans size: {}", spans.size());
        System.out.println(spans);
        for (SpanData span : spans) {

            logger.info("Span ID: {}, Trace ID: {}, Name: {}", span.getSpanId(), span.getTraceId(), span.getName());
            System.out.println(span);
        }

        try {
            byte[] parquetData = ParquetWriterUtil.writeSpansToParquet(spans); // Get Parquet data as byte array
            System.out.println(Arrays.toString(parquetData));
            // Save Parquet data to MySQL as a blob
            TraceData traceData = new TraceData(parquetData, LocalDateTime.now(),"service_1");
            traceDataRepository.save(traceData);

            logger.info("Spans written to Parquet format and saved to MySQL as blob");

        } catch (IOException e) {
            logger.error("Error writing spans to Parquet format", e);
        }

        // Return the response as a plain string
        return response.getMessage();
    }
}

