/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat.autoconfigure.actuate.web.server;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TomcatManagementServerProperties}.
 *
 * @author Andy Wilkinson
 */
class TomcatManagementServerPropertiesTests {

	@Test
	void accessLogsArePrefixedByDefault() {
		TomcatManagementServerProperties properties = new TomcatManagementServerProperties();
		assertThat(properties.getAccesslog().getPrefix()).isEqualTo("management_");
	}

}
