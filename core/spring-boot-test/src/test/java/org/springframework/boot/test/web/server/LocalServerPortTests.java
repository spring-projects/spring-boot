/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.web.server;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LocalServerPort @LocalServerPort}.
 *
 * @author Anand Shah
 * @author Phillip Webb
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = "local.server.port=8181")
class LocalServerPortTests {

	@Value("${local.server.port}")
	private @Nullable String fromValue;

	@LocalServerPort
	private @Nullable String fromAnnotation;

	@Test
	void testLocalServerPortAnnotation() {
		assertThat(this.fromAnnotation).isNotNull().isEqualTo(this.fromValue);
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

	}

}
