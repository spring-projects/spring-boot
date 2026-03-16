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
