/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.test.autoconfigure;

import java.util.Collections;

import io.grpc.ChannelCredentials;
import io.grpc.inprocess.InProcessChannelBuilder;

import org.springframework.grpc.client.ClientInterceptorsConfigurer;
import org.springframework.grpc.client.DefaultGrpcChannelFactory;
import org.springframework.grpc.client.GrpcChannelFactory;

/**
 * {@link GrpcChannelFactory} for testing with in-process transport.
 *
 * @author Chris Bono
 * @author Phillip Webb
 */
class TestGrpcChannelFactory extends DefaultGrpcChannelFactory<InProcessChannelBuilder> {

	TestGrpcChannelFactory(String address, ClientInterceptorsConfigurer interceptorsConfigurer) {
		super(Collections.emptyList(), interceptorsConfigurer);
		setVirtualTargets((path) -> address);
	}

	@Override
	public boolean supports(String target) {
		return true;
	}

	@Override
	protected InProcessChannelBuilder newChannelBuilder(String target, ChannelCredentials creds) {
		return InProcessChannelBuilder.forName(target);
	}

}
