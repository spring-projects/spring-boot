/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.grpc.testing.localserverport;

import io.grpc.ManagedChannel;

interface HelloWorldGrpc {

	static HelloWorldBlockingStub newBlockingStub(ManagedChannel channel) {
		throw new IllegalStateException();
	}

	interface HelloWorldBlockingStub {

		HelloReply sayHello(HelloRequest request);

	}

}
