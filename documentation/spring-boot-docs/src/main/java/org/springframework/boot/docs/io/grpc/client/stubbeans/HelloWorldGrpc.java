/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.grpc.client.stubbeans;

class HelloWorldGrpc {

	interface HelloWorldBlockingStub {

		HelloReply sayHello(HelloRequest request);

	}

}
