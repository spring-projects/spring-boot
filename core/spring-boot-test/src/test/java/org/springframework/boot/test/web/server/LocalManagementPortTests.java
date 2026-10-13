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
 * Tests for {@link LocalManagementPort @LocalManagementPort}.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = "local.management.port=8181")
class LocalManagementPortTests {

	@Value("${local.management.port}")
	private @Nullable String fromValue;

	@LocalManagementPort
	private @Nullable String fromAnnotation;

	@Test
	void testLocalManagementPortAnnotation() {
		assertThat(this.fromAnnotation).isNotNull().isEqualTo(this.fromValue);
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

	}

}
