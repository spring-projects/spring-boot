/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.grpc.server;

import io.grpc.stub.StreamObserver;

import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class MyHelloWorldService extends HelloWorldGrpc.HelloWorldImplBase {

	@Override
	public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
		String message = "Hello '%s'".formatted(request.getName());
		HelloReply reply = HelloReply.newBuilder().setMessage(message).build();
		responseObserver.onNext(reply);
		responseObserver.onCompleted();
	}

}
