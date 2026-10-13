/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jetty.autoconfigure.actuate.web.server;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JettyManagementServerProperties}.
 *
 * @author Andy Wilkinson
 */
class JettyManagementServerPropertiesTests {

	@Test
	void accessLogsArePrefixedByDefault() {
		JettyManagementServerProperties properties = new JettyManagementServerProperties();
		assertThat(properties.getAccesslog().getPrefix()).isEqualTo("management_");
	}

}
