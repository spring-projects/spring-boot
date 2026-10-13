/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.test.autoconfigure;

import io.grpc.Server;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TestGrpcServerFactory}.
 *
 * @author Phillip Webb
 */
class TestGrpcServerFactoryTests {

	private TestGrpcServerFactory factory = new TestGrpcServerFactory("test");

	@Test
	void createServerReturnsInProcessServer() {
		Server server = this.factory.createServer();
		assertThat(server).extracting("transportServer")
			.satisfies((transportServer) -> assertThat(transportServer.getClass().getName()).contains("InProcess"));
	}

}
