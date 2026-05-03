/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
