/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.rsocket.server;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LocalRSocketServerPort @LocalRSocketServerPort}.
 *
 * @author Verónica Vásquez
 * @author Eddú Meléndez
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = "local.rsocket.server.port=8181")
class LocalRSocketServerPortTests {

	@Value("${local.rsocket.server.port}")
	private @Nullable String fromValue;

	@LocalRSocketServerPort
	private @Nullable String fromAnnotation;

	@Test
	void testLocalRSocketServerPortAnnotation() {
		assertThat(this.fromAnnotation).isNotNull().isEqualTo(this.fromValue);
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

	}

}
