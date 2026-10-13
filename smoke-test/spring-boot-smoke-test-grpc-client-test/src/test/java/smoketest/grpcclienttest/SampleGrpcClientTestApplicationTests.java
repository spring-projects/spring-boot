/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.grpcclienttest;

import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import smoketest.grpcclienttest.SampleGrpcClientTestApplicationTests.HelloWorldService;
import smoketest.grpcclienttest.proto.HelloReply;
import smoketest.grpcclienttest.proto.HelloRequest;
import smoketest.grpcclienttest.proto.HelloWorldGrpc;

import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestGrpcTransport
@Import(HelloWorldService.class)
@ExtendWith(OutputCaptureExtension.class)
class SampleGrpcClientTestApplicationTests {

	@Test
	void callsService(CapturedOutput output) {
		assertThat(output).contains(">>> Test hello 'Spring'");
	}

	static class HelloWorldService extends HelloWorldGrpc.HelloWorldImplBase {

		@Override
		public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
			String message = "Test hello '%s'".formatted(request.getName());
			HelloReply reply = HelloReply.newBuilder().setMessage(message).build();
			responseObserver.onNext(reply);
			responseObserver.onCompleted();
		}

	}

}
