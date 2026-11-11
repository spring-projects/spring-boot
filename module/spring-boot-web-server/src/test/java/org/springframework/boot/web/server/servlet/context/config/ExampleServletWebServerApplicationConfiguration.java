/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.servlet.context.config;

import jakarta.servlet.Servlet;

import org.springframework.boot.web.server.servlet.MockServletWebServerFactory;
import org.springframework.boot.web.servlet.mock.MockServlet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Example {@code @Configuration} for use with
 * {@code AnnotationConfigServletWebServerApplicationContextTests}.
 *
 * @author Phillip Webb
 */
@Configuration(proxyBeanMethods = false)
public class ExampleServletWebServerApplicationConfiguration {

	@Bean
	public MockServletWebServerFactory webServerFactory() {
		return new MockServletWebServerFactory();
	}

	@Bean
	public Servlet servlet() {
		return new MockServlet();
	}

}
