/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat;

import org.apache.catalina.Server;

/**
 * Callback interface that can be used to customize a Tomcat {@link Server}.
 *
 * @author Andy Wilkinson
 * @since 4.2.0
 * @see ConfigurableTomcatWebServerFactory
 */
@FunctionalInterface
public interface TomcatServerCustomizer {

	/**
	 * Customize the server.
	 * @param server the server to customize
	 */
	void customize(Server server);

}
