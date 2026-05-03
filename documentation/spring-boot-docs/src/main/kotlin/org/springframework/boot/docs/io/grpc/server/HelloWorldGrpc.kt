package org.springframework.boot.docs.io.grpc.server

import io.grpc.stub.StreamObserver

class HelloWorldGrpc {

	abstract class HelloWorldImplBase {
		abstract fun sayHello(
			request: HelloRequest,
			responseObserver: StreamObserver<HelloReply>
		)
	}

}
