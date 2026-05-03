package org.springframework.boot.docs.io.grpc.testing.localserverport

import io.grpc.ManagedChannel
import io.grpc.netty.NettyChannelBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.grpc.test.autoconfigure.LocalGrpcServerPort
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(properties = ["spring.grpc.server.port=0"])
class MyGrpcIntegrationTests {

	@LocalGrpcServerPort
	var port = 0

	@Test
	fun sayHello() {
		val target = "localhost:${port}"
		val channel: ManagedChannel = NettyChannelBuilder.forTarget(target).usePlaintext().build()
		try {
			val hello: HelloWorldGrpc.HelloWorldBlockingStub = HelloWorldGrpc.newBlockingStub(channel)
			val request = HelloRequest.newBuilder().setName("Spring").build()
			assertThat(hello.sayHello(request).getMessage()).isEqualTo("Hello 'Spring'")
		} finally {
			channel.shutdown()
		}
	}
}
