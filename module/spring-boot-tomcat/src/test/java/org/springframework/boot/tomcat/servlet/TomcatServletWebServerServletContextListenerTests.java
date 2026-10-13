/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat.servlet;

import jakarta.servlet.ServletContextListener;

import org.springframework.boot.web.server.servlet.AbstractServletWebServerServletContextListenerTests;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tests for Tomcat driving {@link ServletContextListener}s correctly.
 *
 * @author Andy Wilkinson
 */
class TomcatServletWebServerServletContextListenerTests extends AbstractServletWebServerServletContextListenerTests {

	TomcatServletWebServerServletContextListenerTests() {
		super(TomcatConfiguration.class);
	}

	@Configuration(proxyBeanMethods = false)
	static class TomcatConfiguration {

		@Bean
		TomcatServletWebServerFactory webServerFactory() {
			return new TomcatServletWebServerFactory(0);
		}

	}

}
