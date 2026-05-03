package org.springframework.boot.docs.io.grpc.client.stubbeans

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner


class MyApplicationRunner(val helloStub: HelloWorldGrpc.HelloWorldBlockingStub) : ApplicationRunner {

	override fun run(args: ApplicationArguments) {
		val request = HelloRequest.newBuilder().setName("Spring").build()
		val reply: HelloReply = helloStub.sayHello(request)
		println(reply.getMessage())
	}

}
