/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.grpc.testing.localserverport;

interface HelloReply {

	String getMessage();

	static Builder newBuilder() {
		throw new IllegalStateException();
	}

	interface Builder {

		Builder setMessage(String message);

		HelloReply build();

	}

}
