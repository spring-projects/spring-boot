package org.springframework.boot.docs.io.grpc.server

import io.grpc.stub.StreamObserver
import org.springframework.grpc.server.service.GrpcService

@GrpcService
class MyHelloWorldService : HelloWorldGrpc.HelloWorldImplBase() {

	override fun sayHello(request: HelloRequest,  responseObserver: StreamObserver<HelloReply>) {
		val message = "Hello '${request.getName()}'"
		val reply = HelloReply.newBuilder().setMessage(message).build()
		responseObserver.onNext(reply)
		responseObserver.onCompleted()
	}

}
