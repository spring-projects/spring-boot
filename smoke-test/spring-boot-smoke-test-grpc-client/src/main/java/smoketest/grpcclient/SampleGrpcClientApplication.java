/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.grpcclient;

import smoketest.grpcclient.proto.HelloReply;
import smoketest.grpcclient.proto.HelloRequest;
import smoketest.grpcclient.proto.HelloWorldGrpc.HelloWorldBlockingStub;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.client.ImportGrpcClients;

@SpringBootApplication
@ImportGrpcClients(types = HelloWorldBlockingStub.class)
public class SampleGrpcClientApplication {

	@Bean
	ApplicationRunner applicationRunner(HelloWorldBlockingStub hello) {
		return (args) -> {
			HelloRequest request = HelloRequest.newBuilder().setName("Spring").build();
			HelloReply reply = hello.sayHello(request);
			System.out.println(">>> " + reply.getMessage());
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleGrpcClientApplication.class, args);
	}

}
