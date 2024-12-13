package com.example.grpc.Config;

import com.example.grpc.Hello;
import com.example.grpc.HelloServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class GrpcClient {
    public static void main(String[] args) {
        // Create a channel to connect to the gRPC server
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext() // Use plaintext communication
                .build();

        HelloServiceGrpc.HelloServiceBlockingStub stub = HelloServiceGrpc.newBlockingStub(channel);

        // Create a request
        Hello.HelloRequest request = Hello.HelloRequest.newBuilder()
                .setName("World")
                .build();

        // Call the service
        Hello.HelloResponse response = stub.sayHello(request);
        System.out.println(response.getMessage());

        // Shutdown the channel
        channel.shutdown();
    }
}
