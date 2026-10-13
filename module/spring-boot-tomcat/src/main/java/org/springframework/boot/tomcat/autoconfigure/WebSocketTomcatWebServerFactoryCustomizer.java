/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat.autoconfigure;

import org.apache.tomcat.websocket.server.WsSci;

import org.springframework.boot.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;

/**
 * {@link WebServerFactoryCustomizer} that configures Tomcat's WebSocket support.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public class WebSocketTomcatWebServerFactoryCustomizer
		implements WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory> {

	@Override
	public void customize(ConfigurableTomcatWebServerFactory factory) {
		factory.addContextCustomizers((context) -> context.addServletContainerInitializer(new WsSci(), null));
	}

}
