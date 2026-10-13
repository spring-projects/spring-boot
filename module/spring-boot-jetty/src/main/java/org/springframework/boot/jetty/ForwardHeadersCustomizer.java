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

	@Override
	public void customize(Server server) {
		ForwardedRequestCustomizer customizer = new ForwardedRequestCustomizer();
		for (Connector connector : server.getConnectors()) {
			for (ConnectionFactory connectionFactory : connector.getConnectionFactories()) {
				if (connectionFactory instanceof HttpConfiguration.ConnectionFactory jettyConnectionFactory) {
					jettyConnectionFactory.getHttpConfiguration().addCustomizer(customizer);
				}
			}
		}
	}

}
