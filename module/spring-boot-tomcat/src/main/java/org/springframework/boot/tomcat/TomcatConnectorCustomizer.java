/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat;

import org.apache.catalina.connector.Connector;

/**
 * Callback interface that can be used to customize a Tomcat {@link Connector}.
 *
 * @author Dave Syer
 * @since 4.0.0
 * @see ConfigurableTomcatWebServerFactory
 */
@FunctionalInterface
public interface TomcatConnectorCustomizer {

	/**
	 * Customize the connector.
	 * @param connector the connector to customize
	 */
	void customize(Connector connector);

}
