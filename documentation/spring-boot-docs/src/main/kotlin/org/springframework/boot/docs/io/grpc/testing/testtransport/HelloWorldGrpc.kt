package org.springframework.boot.docs.io.grpc.testing.testtransport

import io.grpc.ManagedChannel

class HelloWorldGrpc {

	interface HelloWorldBlockingStub {

		fun sayHello(request: HelloRequest): HelloReply

	}

	companion object {
		fun newBlockingStub(channel: ManagedChannel): HelloWorldBlockingStub {
			throw IllegalStateException()
		}
	}

}
