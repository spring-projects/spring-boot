/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jetty;

import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Server;

import org.springframework.boot.web.server.ForwardedRequestHeaders;
import org.springframework.boot.web.server.ForwardedRequestHeaders.Naming;
import org.springframework.util.Assert;

/**
 * {@link JettyServerCustomizer} to add {@link ForwardedRequestCustomizer}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public class ForwardHeadersCustomizer implements JettyServerCustomizer {

	private final ForwardedRequestHeaders forwardedHeaders;

	/**
	 * Create a new customizer instance with "X-Forwarded-*" support.
	 * @deprecated since 4.2.0 for removal in 4.4.0 in favor of
	 * {@link #ForwardHeadersCustomizer(ForwardedRequestHeaders)}
	 */
	@Deprecated(since = "4.2.0", forRemoval = true)
	public ForwardHeadersCustomizer() {
		this(ForwardedRequestHeaders.X_FORWARDED_WITHOUT_PREFIX);
	}

	/**
	 * Create a new customizer instance.
	 * @param forwardedRequestHeaders the forward request headers configuration
	 * @since 4.2.0
	 */
	public ForwardHeadersCustomizer(ForwardedRequestHeaders forwardedRequestHeaders) {
		Assert.notNull(forwardedRequestHeaders, "'forwardedRequestHeaders' must not be null");
		Assert.state(!forwardedRequestHeaders.usePrefix(), "Jetty does not support the 'prefix' forwarded header");
		this.forwardedHeaders = forwardedRequestHeaders;
	}

	@Override
	public void customize(Server server) {
		ForwardedRequestCustomizer requestCustomizer = createRequestCustomizer();
		for (Connector connector : server.getConnectors()) {
			for (ConnectionFactory connectionFactory : connector.getConnectionFactories()) {
				if (connectionFactory instanceof HttpConfiguration.ConnectionFactory jettyConnectionFactory) {
					jettyConnectionFactory.getHttpConfiguration().addCustomizer(requestCustomizer);
				}
			}
		}
	}

	private ForwardedRequestCustomizer createRequestCustomizer() {
		ForwardedRequestCustomizer customizer = new ForwardedRequestCustomizer();
		customizer.setForwardedOnly(this.forwardedHeaders.naming() == Naming.RFC_7239);
		return customizer;
	}

}
