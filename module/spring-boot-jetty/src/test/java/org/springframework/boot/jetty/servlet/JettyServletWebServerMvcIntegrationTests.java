/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jetty.servlet;

import org.springframework.boot.jetty.JettyWebServer;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.context.AbstractServletWebServerMvcIntegrationTests;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Integration tests for {@link ServletWebServerApplicationContext} and
 * {@link JettyWebServer} running Spring MVC.
 */
class JettyServletWebServerMvcIntegrationTests extends AbstractServletWebServerMvcIntegrationTests {

	protected JettyServletWebServerMvcIntegrationTests() {
		super(JettyConfig.class);
	}

	@Configuration(proxyBeanMethods = false)
	static class JettyConfig {

		@Bean
		JettyServletWebServerFactory webServerFactory() {
			return new JettyServletWebServerFactory(0);
		}

	}

}
