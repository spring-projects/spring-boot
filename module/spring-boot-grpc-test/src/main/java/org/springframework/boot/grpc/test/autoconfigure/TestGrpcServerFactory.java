/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.test.autoconfigure;

import java.util.Collections;

import io.grpc.inprocess.InProcessServerBuilder;

import org.springframework.grpc.server.DefaultGrpcServerFactory;
import org.springframework.grpc.server.GrpcServerFactory;

/**
 * {@link GrpcServerFactory} for testing with in-process transport.
 *
 * @author Chris Bono
 * @author Phillip Webb
 */
class TestGrpcServerFactory extends DefaultGrpcServerFactory<InProcessServerBuilder> {

	TestGrpcServerFactory(String address) {
		super(address, Collections.emptyList(), null, null, null);
	}

	@Override
	protected InProcessServerBuilder newServerBuilder() {
		return InProcessServerBuilder.forName(address());
	}

}
