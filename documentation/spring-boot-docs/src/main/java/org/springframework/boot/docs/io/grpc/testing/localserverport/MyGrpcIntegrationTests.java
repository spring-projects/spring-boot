/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.grpc.testing.localserverport;

import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import org.junit.jupiter.api.Test;

import org.springframework.boot.docs.io.grpc.testing.localserverport.HelloWorldGrpc.HelloWorldBlockingStub;
import org.springframework.boot.grpc.test.autoconfigure.LocalGrpcServerPort;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.grpc.server.port=0")
class MyGrpcIntegrationTests {

	@LocalGrpcServerPort
	private int port;

	@Test
	void sayHello() {
		String target = "localhost:%s".formatted(this.port);
		ManagedChannel channel = NettyChannelBuilder.forTarget(target).usePlaintext().build();
		try {
			HelloWorldBlockingStub hello = HelloWorldGrpc.newBlockingStub(channel);
			HelloRequest request = HelloRequest.newBuilder().setName("Spring").build();
			assertThat(hello.sayHello(request).getMessage()).isEqualTo("Hello 'Spring'");
		}
		finally {
			channel.shutdown();
		}
	}

}
