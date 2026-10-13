/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jetty;

import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Server;

/**
 * {@link JettyServerCustomizer} to add {@link ForwardedRequestCustomizer}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public class ForwardHeadersCustomizer implements JettyServerCustomizer {

	private final boolean useXForwarded;

	/**
	 * Create a new customizer instance.
	 * @param useXForwarded "X-Forwarded-*" headers are used if {@code true}, otherwise
	 * standard "Forwarded" are used instead.
	 * @since 4.2.0
	 */
	public ForwardHeadersCustomizer(boolean useXForwarded) {
		this.useXForwarded = useXForwarded;
	}

	/**
	 * Create a new customizer instance with "X-Forwarded-*" support.
	 */
	public ForwardHeadersCustomizer() {
		this(true);
	}

	@Override
	public void customize(Server server) {
		ForwardedRequestCustomizer customizer = new ForwardedRequestCustomizer();
		if (this.useXForwarded) {
			// disable "Forwarded" support
			customizer.setForwardedHeader(null);
		}
		else {
			// disable "X-Forwarded-*" support
			customizer.setForwardedOnly(true);
		}
		for (Connector connector : server.getConnectors()) {
			for (ConnectionFactory connectionFactory : connector.getConnectionFactories()) {
				if (connectionFactory instanceof HttpConfiguration.ConnectionFactory jettyConnectionFactory) {
					jettyConnectionFactory.getHttpConfiguration().addCustomizer(customizer);
				}
			}
		}
	}

}
