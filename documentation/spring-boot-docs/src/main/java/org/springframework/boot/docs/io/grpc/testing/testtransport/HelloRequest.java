/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.grpc.testing.testtransport;

interface HelloRequest {

	String getName();

	static Builder newBuilder() {
		throw new IllegalStateException();
	}

	interface Builder {

		Builder setName(String name);

		HelloRequest build();

	}

}
