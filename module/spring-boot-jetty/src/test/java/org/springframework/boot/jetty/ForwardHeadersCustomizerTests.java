/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jetty;

import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.Test;

import org.springframework.boot.web.server.ForwardedRequestHeaders;
import org.springframework.boot.web.server.ForwardedRequestHeaders.Naming;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ForwardHeadersCustomizer}.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 */
class ForwardHeadersCustomizerTests {

	@Test
	@SuppressWarnings("removal")
	void deprecatedConstructorUsesXForwardedHeaders() {
		ForwardedRequestCustomizer customizer = customize(new ForwardHeadersCustomizer());
		assertThat(customizer.getForwardedHeader()).isNull();
		assertThat(customizer.getForwardedForHeader()).isNotNull();
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenForwardedRequestHeadersIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ForwardHeadersCustomizer(null))
			.withMessage("'forwardedRequestHeaders' must not be null");
	}

	@Test
	void createWhenForwardRequestHeadersHasUsePrefixThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> new ForwardHeadersCustomizer(new ForwardedRequestHeaders(Naming.X_FORWARDED, true)))
			.withMessage("Jetty does not support the 'prefix' forwarded header");
	}

	@Test
	void whenXForwardedNamingEnablesXForwardedAndDisablesForwarded() {
		ForwardedRequestCustomizer customizer = customize(
				new ForwardHeadersCustomizer(new ForwardedRequestHeaders(Naming.X_FORWARDED, false)));
		assertThat(customizer.getForwardedHeader()).isNull();
		assertThat(customizer.getForwardedForHeader()).isNotNull();
	}

	@Test
	void whenRfcNamingEnablesForwardedAndDisablesXForwarded() {
		ForwardedRequestCustomizer customizer = customize(
				new ForwardHeadersCustomizer(new ForwardedRequestHeaders(Naming.RFC_7239, false)));
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
