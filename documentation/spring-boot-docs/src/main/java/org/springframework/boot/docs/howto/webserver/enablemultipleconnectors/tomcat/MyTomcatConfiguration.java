/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.webserver.enablemultipleconnectors.tomcat;

import org.apache.catalina.connector.Connector;

import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MyTomcatConfiguration {

	@Bean
	public WebServerFactoryCustomizer<TomcatServletWebServerFactory> connectorCustomizer() {
		return (tomcat) -> tomcat.addAdditionalConnectors(createConnector());
	}

	private Connector createConnector() {
		Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
		connector.setPort(8081);
		return connector;
	}

}
