/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.grpcserveroauth;

import io.grpc.stub.StreamObserver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import smoketest.grpcserveroauth.proto.HelloReply;
import smoketest.grpcserveroauth.proto.HelloRequest;
import smoketest.grpcserveroauth.proto.HelloWorldGrpc;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class HelloWorldService extends HelloWorldGrpc.HelloWorldImplBase {

	private static Log logger = LogFactory.getLog(HelloWorldService.class);

	@Override
	public void sayHelloProfileScope(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
		sayHello("sayHelloProfileScope", request, responseObserver);
	}

	@Override
	public void sayHelloEmailScope(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
		sayHello("sayHelloEmailScope", request, responseObserver);
	}

	@Override
	public void sayHelloAuthenticated(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
		sayHello("sayHelloAuthenticated", request, responseObserver);
	}

	public void sayHello(String methodName, HelloRequest request, StreamObserver<HelloReply> responseObserver) {
		String name = request.getName();
		logger.info(methodName + " " + name);
		Assert.isTrue(!name.startsWith("error"), () -> "Bad name: " + name);
		Assert.state(!name.startsWith("internal"), "Internal error");
		String message = "%s '%s'".formatted(methodName, name);
		HelloReply reply = HelloReply.newBuilder().setMessage(message).build();
		responseObserver.onNext(reply);
		responseObserver.onCompleted();
	}

}
