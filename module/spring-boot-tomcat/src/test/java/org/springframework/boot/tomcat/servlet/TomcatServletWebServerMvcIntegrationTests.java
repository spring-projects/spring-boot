/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat.servlet;

import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.context.AbstractServletWebServerMvcIntegrationTests;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Integration tests for {@link ServletWebServerApplicationContext} and
 * {@link TomcatWebServer} running Spring MVC.
 */
class TomcatServletWebServerMvcIntegrationTests extends AbstractServletWebServerMvcIntegrationTests {

	protected TomcatServletWebServerMvcIntegrationTests() {
		super(TomcatConfig.class);
	}

	@Configuration(proxyBeanMethods = false)
	static class TomcatConfig {

		@Bean
		TomcatServletWebServerFactory webServerFactory() {
			return new TomcatServletWebServerFactory(0);
		}

	}

}
