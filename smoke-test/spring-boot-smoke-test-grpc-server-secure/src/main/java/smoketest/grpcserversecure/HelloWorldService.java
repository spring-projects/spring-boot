/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.grpcserversecure;

import io.grpc.stub.StreamObserver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import smoketest.grpcserversecure.proto.HelloReply;
import smoketest.grpcserversecure.proto.HelloRequest;
import smoketest.grpcserversecure.proto.HelloWorldGrpc;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class HelloWorldService extends HelloWorldGrpc.HelloWorldImplBase {

	private static Log logger = LogFactory.getLog(HelloWorldService.class);

	@Override
	public void sayHelloAdmin(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
		sayHello("sayHelloAdmin", request, responseObserver);
	}

	@Override
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public void sayHelloAdminAnnotated(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
		sayHello("sayHelloAdminAnnotated", request, responseObserver);
	}

	@Override
	public void sayHelloUser(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
		sayHello("sayHelloUser", request, responseObserver);
	}

	@Override
	@PreAuthorize("hasAuthority('ROLE_USER')")
	public void sayHelloUserAnnotated(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
		sayHello("sayHelloUserAnnotated", request, responseObserver);
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
