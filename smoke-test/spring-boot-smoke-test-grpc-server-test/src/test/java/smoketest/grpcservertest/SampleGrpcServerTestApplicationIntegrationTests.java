/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.grpcservertest;

import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import org.junit.jupiter.api.Test;
import smoketest.grpcservertest.proto.HelloReply;
import smoketest.grpcservertest.proto.HelloRequest;
import smoketest.grpcservertest.proto.HelloWorldGrpc;
import smoketest.grpcservertest.proto.HelloWorldGrpc.HelloWorldBlockingStub;

import org.springframework.boot.grpc.test.autoconfigure.LocalGrpcServerPort;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.grpc.server.port=0")
class SampleGrpcServerTestApplicationIntegrationTests {

	@LocalGrpcServerPort
	private int port;

	@Test
	void callsService() {
		ManagedChannel channel = NettyChannelBuilder.forTarget("localhost:%s".formatted(this.port))
			.usePlaintext()
			.build();
		try {
			HelloWorldBlockingStub hello = HelloWorldGrpc.newBlockingStub(channel);
			HelloRequest request = HelloRequest.newBuilder().setName("Spring").build();
			HelloReply reply = hello.sayHello(request);
			assertThat(reply.getMessage()).isEqualTo("Hello 'Spring'");
		}
		finally {
			channel.shutdown();
		}
	}

}
