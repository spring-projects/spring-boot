/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jetty.autoconfigure.servlet;

import jakarta.servlet.ServletContextListener;

import org.springframework.boot.jetty.servlet.JettyServletWebServerFactory;
import org.springframework.boot.web.server.servlet.AbstractServletWebServerServletContextListenerTests;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tests for Jetty driving {@link ServletContextListener}s correctly.
 *
 * @author Andy Wilkinson
 */
class JettyServletWebServerServletContextListenerTests extends AbstractServletWebServerServletContextListenerTests {

	JettyServletWebServerServletContextListenerTests() {
		super(JettyConfiguration.class);
	}

	@Configuration(proxyBeanMethods = false)
	static class JettyConfiguration {

		@Bean
		JettyServletWebServerFactory webServerFactory() {
			return new JettyServletWebServerFactory(0);
		}

	}

}
