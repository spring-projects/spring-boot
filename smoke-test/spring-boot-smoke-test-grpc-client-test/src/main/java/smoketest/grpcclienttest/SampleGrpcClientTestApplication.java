/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.grpcclienttest;

import smoketest.grpcclienttest.proto.HelloReply;
import smoketest.grpcclienttest.proto.HelloRequest;
import smoketest.grpcclienttest.proto.HelloWorldGrpc.HelloWorldBlockingStub;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.client.ImportGrpcClients;

@SpringBootApplication
@ImportGrpcClients(types = HelloWorldBlockingStub.class)
public class SampleGrpcClientTestApplication {

	@Bean
	ApplicationRunner applicationRunner(HelloWorldBlockingStub hello) {
		return (args) -> {
			HelloRequest request = HelloRequest.newBuilder().setName("Spring").build();
			HelloReply reply = hello.sayHello(request);
			System.out.println(">>> " + reply.getMessage());
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleGrpcClientTestApplication.class, args);
	}

}
