/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.boot.web.server.autoconfigure.ServerProperties.ForwardHeadersStrategy;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ForwardedHeaderStrategy}.
 *
 * @author Phillip Webb
 */
class ForwardedHeaderStrategyTests {

	@Test
	void getFromEnvironmentWhenHasProperty() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("server.request.header.forwarded.strategy", "web-server");
		assertThat(ForwardedHeaderStrategy.get(environment)).isEqualTo(ForwardedHeaderStrategy.WEB_SERVER);
	}

	@Test
	void getFromEnvironmentWhenHasDeprecatedProperty() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("server.forward-headers-strategy", "native");
		assertThat(ForwardedHeaderStrategy.get(environment)).isEqualTo(ForwardedHeaderStrategy.WEB_SERVER);
	}

	@Test
	void getFromEnvironmentWhenHasNoProperty() {
		MockEnvironment environment = new MockEnvironment();
		assertThat(ForwardedHeaderStrategy.get(environment)).isNull();
	}

	@Test
	void getFromEnvironmentWhenHasBothPropertiesThrowsException() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("server.request.header.forwarded.strategy", "web-server");
		environment.setProperty("server.forward-headers-strategy", "native");
	}

	@Test
	void getFromServerPropertiesWhenHasProperty() {
		ServerProperties serverProperties = new ServerProperties();
		serverProperties.getRequest().getHeader().getForwarded().setStrategy(ForwardedHeaderStrategy.WEB_SERVER);
		assertThat(ForwardedHeaderStrategy.get(serverProperties)).isEqualTo(ForwardedHeaderStrategy.WEB_SERVER);
	}

	@Test
	@SuppressWarnings("removal")
	void getFromServerPropertiesWhenHasDeprecatedProperty() {
		ServerProperties serverProperties = new ServerProperties();
		serverProperties.setForwardHeadersStrategy(ForwardHeadersStrategy.NATIVE);
		assertThat(ForwardedHeaderStrategy.get(serverProperties)).isEqualTo(ForwardedHeaderStrategy.WEB_SERVER);
	}

	@Test
	void getFromServerPropertiesWhenHasNoProperty() {
		ServerProperties serverProperties = new ServerProperties();
		assertThat(ForwardedHeaderStrategy.get(serverProperties)).isNull();
	}

	@Test
	@SuppressWarnings("removal")
	void getFromServerPropertiesWhenHasBothPropertiesThrowsException() {
		ServerProperties serverProperties = new ServerProperties();
		serverProperties.getRequest().getHeader().getForwarded().setStrategy(ForwardedHeaderStrategy.WEB_SERVER);
		serverProperties.setForwardHeadersStrategy(ForwardHeadersStrategy.NATIVE);
	}

	@Test
	@SuppressWarnings("removal")
	void adaptDeprecated() {
		assertThat(ForwardedHeaderStrategy.adaptDeprecated(ForwardHeadersStrategy.NATIVE))
			.isEqualTo(ForwardedHeaderStrategy.WEB_SERVER);
		assertThat(ForwardedHeaderStrategy.adaptDeprecated(ForwardHeadersStrategy.FRAMEWORK))
			.isEqualTo(ForwardedHeaderStrategy.FRAMEWORK);
		assertThat(ForwardedHeaderStrategy.adaptDeprecated(ForwardHeadersStrategy.NONE))
			.isEqualTo(ForwardedHeaderStrategy.NONE);
		assertThat(ForwardedHeaderStrategy.adaptDeprecated(null)).isNull();
	}

}
