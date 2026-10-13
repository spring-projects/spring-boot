/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.test.autoconfigure;

import io.grpc.ManagedChannel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TestGrpcChannelFactory}.
 *
 * @author Phillip Webb
 */
class TestGrpcChannelFactoryTests {

	private TestGrpcChannelFactory factory = new TestGrpcChannelFactory("test", mock());

	@Test
	void supportsReturnsTrue() {
		assertThat(this.factory.supports("inprocess:test")).isTrue();
		assertThat(this.factory.supports("static:*:0")).isTrue();
	}

	@Test
	void createChannelReturnsInProcessChannel() {
		ManagedChannel channel = this.factory.createChannel("static:*:0");
		assertThat(channel).extracting("delegate.originalTransportFactory.delegate")
			.satisfies((transportFactory) -> assertThat(transportFactory.getClass().getName()).contains("InProcess"));
	}

}
