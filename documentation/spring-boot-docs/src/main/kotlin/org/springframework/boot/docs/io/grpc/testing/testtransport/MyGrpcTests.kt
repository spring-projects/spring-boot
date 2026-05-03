package org.springframework.boot.docs.io.grpc.testing.testtransport

import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.grpc.client.ImportGrpcClients

@SpringBootTest
@AutoConfigureTestGrpcTransport
@ImportGrpcClients(types = [HelloWorldGrpc.HelloWorldBlockingStub::class])
class MyGrpcTests(@Autowired val helloStub: HelloWorldGrpc.HelloWorldBlockingStub) {

	@Test
	fun sayHello() {
		val request = HelloRequest.newBuilder().setName("Spring").build()
		val reply = helloStub.sayHello(request)
		assertThat(reply.getMessage()).isEqualTo("Hello 'Spring'")
	}

}
