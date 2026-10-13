/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.grpc.testing.testtransport;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.grpc.client.ImportGrpcClients;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestGrpcTransport
@ImportGrpcClients(types = HelloWorldGrpc.HelloWorldBlockingStub.class)
class MyGrpcTests {

	@Autowired
	private HelloWorldGrpc.HelloWorldBlockingStub helloStub;

	@Test
	void sayHello() {
		HelloRequest request = HelloRequest.newBuilder().setName("Spring").build();
		HelloReply reply = this.helloStub.sayHello(request);
		assertThat(reply.getMessage()).isEqualTo("Hello 'Spring'");
	}

}
