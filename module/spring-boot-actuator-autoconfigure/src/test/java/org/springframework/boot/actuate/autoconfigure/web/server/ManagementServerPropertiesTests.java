/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.web.server;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ManagementServerProperties}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 */
class ManagementServerPropertiesTests {

	@Test
	void defaultPortIsNull() {
		ManagementServerProperties properties = new ManagementServerProperties();
		assertThat(properties.getPort()).isNull();
	}

	@Test
	void definedPort() {
		ManagementServerProperties properties = new ManagementServerProperties();
		properties.setPort(123);
		assertThat(properties.getPort()).isEqualTo(123);
	}

	@Test
	void defaultBasePathIsEmptyString() {
		ManagementServerProperties properties = new ManagementServerProperties();
		assertThat(properties.getBasePath()).isEmpty();
	}

	@Test
	void definedBasePath() {
		ManagementServerProperties properties = new ManagementServerProperties();
		properties.setBasePath("/foo");
		assertThat(properties.getBasePath()).isEqualTo("/foo");
	}

	@Test
	void trailingSlashOfBasePathIsRemoved() {
		ManagementServerProperties properties = new ManagementServerProperties();
		properties.setBasePath("/foo/");
		assertThat(properties.getBasePath()).isEqualTo("/foo");
	}

	@Test
	void slashOfBasePathIsDefaultValue() {
		ManagementServerProperties properties = new ManagementServerProperties();
		properties.setBasePath("/");
		assertThat(properties.getBasePath()).isEmpty();
	}

}
