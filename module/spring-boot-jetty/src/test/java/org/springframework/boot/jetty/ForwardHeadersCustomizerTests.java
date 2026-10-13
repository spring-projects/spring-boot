/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jetty;

import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ForwardHeadersCustomizer}.
 *
 * @author Brian Clozel
 */
class ForwardHeadersCustomizerTests {

	@Test
	void defaultConstructorUsesXForwardedHeaders() {
		ForwardedRequestCustomizer customizer = customize(new ForwardHeadersCustomizer());
		assertThat(customizer.getForwardedHeader()).isNull();
		assertThat(customizer.getForwardedForHeader()).isNotNull();
	}

	@Test
	void useXForwardedTrueEnablesXForwardedAndDisablesForwarded() {
		ForwardedRequestCustomizer customizer = customize(new ForwardHeadersCustomizer(true));
		assertThat(customizer.getForwardedHeader()).isNull();
		assertThat(customizer.getForwardedForHeader()).isNotNull();
	}

	@Test
	void useXForwardedFalseEnablesForwardedAndDisablesXForwarded() {
		ForwardedRequestCustomizer customizer = customize(new ForwardHeadersCustomizer(false));
		assertThat(customizer.getForwardedHeader()).isNotNull();
		assertThat(customizer.getForwardedForHeader()).isNull();
	}

	private ForwardedRequestCustomizer customize(ForwardHeadersCustomizer customizer) {
		Server server = new Server(0);
		customizer.customize(server);
		HttpConfiguration httpConfiguration = ((HttpConfiguration.ConnectionFactory) server.getConnectors()[0]
			.getConnectionFactories()
			.stream()
			.filter(HttpConfiguration.ConnectionFactory.class::isInstance)
			.findFirst()
			.orElseThrow()).getHttpConfiguration();
		return httpConfiguration.getCustomizer(ForwardedRequestCustomizer.class);
	}

}
