/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.grpcservertest;

import io.grpc.stub.StreamObserver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import smoketest.grpcservertest.proto.HelloReply;
import smoketest.grpcservertest.proto.HelloRequest;
import smoketest.grpcservertest.proto.HelloWorldGrpc;

import org.springframework.grpc.server.advice.GrpcAdvice;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@GrpcAdvice
public class HelloWorldService extends HelloWorldGrpc.HelloWorldImplBase {

	private static Log logger = LogFactory.getLog(HelloWorldService.class);

	@Override
	public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
		String name = request.getName();
		logger.info("sayHello " + name);
		Assert.isTrue(!name.startsWith("error"), () -> "Bad name: " + name);
		Assert.state(!name.startsWith("internal"), "Internal error");
		String message = "Hello '%s'".formatted(name);
		HelloReply reply = HelloReply.newBuilder().setMessage(message).build();
		responseObserver.onNext(reply);
		responseObserver.onCompleted();
	}

	@Override
	public void streamHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
		String name = request.getName();
		logger.info("streamHello " + name);
		int count = 0;
		while (count < 10) {
			String message = "Hello(" + count + ") '%s'".formatted(name);
			HelloReply reply = HelloReply.newBuilder().setMessage(message).build();
			responseObserver.onNext(reply);
			count++;
			try {
				Thread.sleep(100L);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				responseObserver.onError(ex);
				return;
			}
		}
		responseObserver.onCompleted();
	}

}
