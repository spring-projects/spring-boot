/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.webserver.enablemultipleconnectors.jetty;

import org.eclipse.jetty.server.ServerConnector;

import org.springframework.boot.jetty.ConfigurableJettyWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MyJettyConfiguration {

	@Bean
	public WebServerFactoryCustomizer<ConfigurableJettyWebServerFactory> connectorCustomizer() {
		return (jetty) -> jetty.addServerCustomizers((server) -> {
			ServerConnector connector = new ServerConnector(server);
			connector.setPort(8081);
			server.addConnector(connector);
		});
	}

}
