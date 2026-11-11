/*
 * Copyright 2012-present the original author or authors.
 */

package com.example;

import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.server.AllowedResourceAliasChecker;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.jetty.JettyServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link JettyServerCustomizer} that:
 * <ul>
 * <li>Approves all aliases to allow access to unusually named static resources
 * <li>Relaxes URI compliance to allow access to static resources with {@code %} in their file name.
 * </ul>
 *
 * @author Madhura Bhave
 * @author Andy Wilkinson
 */
@ConditionalOnClass(name = {"org.eclipse.jetty.server.handler.ContextHandler"})
@Configuration(proxyBeanMethods = false)
public class JettyServerCustomizerConfig {

	@Bean
	public JettyServerCustomizer jettyServerCustomizer() {
		return (server) -> {
			ContextHandler handler = (ContextHandler) server.getHandler();
			handler.addAliasCheck((path, resource) -> true);

			for (Connector connector : server.getConnectors()) {
				connector.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration()
						.setUriCompliance(UriCompliance.LEGACY);
			}
		};
	}

}
