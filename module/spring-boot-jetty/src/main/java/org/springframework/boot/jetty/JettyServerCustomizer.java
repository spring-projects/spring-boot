/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jetty;

import org.eclipse.jetty.server.Server;

import org.springframework.boot.jetty.servlet.JettyServletWebServerFactory;

/**
 * Callback interface that can be used to customize a Jetty {@link Server}.
 *
 * @author Dave Syer
 * @since 4.0.0
 * @see JettyServletWebServerFactory
 */
@FunctionalInterface
public interface JettyServerCustomizer {

	/**
	 * Customize the server.
	 * @param server the server to customize
	 */
	void customize(Server server);

}
