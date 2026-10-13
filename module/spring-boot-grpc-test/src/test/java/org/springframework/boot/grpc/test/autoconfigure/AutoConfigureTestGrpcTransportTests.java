/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.test.autoconfigure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutoConfigureTestGrpcTransport}.
 *
 * @author Phillip Webb
 */
@ExtendWith(SpringExtension.class)
@AutoConfigureTestGrpcTransport
class AutoConfigureTestGrpcTransportTests {

	@Test
	void setsEnabledPropertiesToFalse(@Autowired Environment environment) {
		assertThat(environment.getProperty("spring.grpc.server.servlet.enabled", Boolean.class)).isFalse();
		assertThat(environment.getProperty("spring.grpc.server.factory.enabled", Boolean.class)).isFalse();
		assertThat(environment.getProperty("spring.grpc.client.channelfactory.enabled", Boolean.class)).isFalse();
	}

}
