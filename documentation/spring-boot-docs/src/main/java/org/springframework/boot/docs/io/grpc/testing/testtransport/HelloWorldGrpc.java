/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.grpc.testing.testtransport;

class HelloWorldGrpc {

	interface HelloWorldBlockingStub {

		HelloReply sayHello(HelloRequest request);

	}

}
