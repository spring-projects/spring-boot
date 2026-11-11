/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.servlet;

import jakarta.servlet.ServletContext;

import org.springframework.boot.web.server.AbstractConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.servlet.MockServletWebServer.RegisteredFilter;
import org.springframework.boot.web.server.servlet.MockServletWebServer.RegisteredServlet;
import org.springframework.boot.web.servlet.ServletContextInitializer;

import static org.mockito.Mockito.spy;

/**
 * Mock {@link ServletWebServerFactory}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class MockServletWebServerFactory extends AbstractConfigurableWebServerFactory
		implements ConfigurableServletWebServerFactory {

	private final ServletWebServerSettings settings = new ServletWebServerSettings();

	private MockServletWebServer webServer;

	@Override
	public WebServer getWebServer(ServletContextInitializer... initializers) {
		this.webServer = spy(
				new MockServletWebServer(ServletContextInitializers.from(this.settings, initializers), getPort()));
		return this.webServer;
	}

	public MockServletWebServer getWebServer() {
		return this.webServer;
	}

	public ServletContext getServletContext() {
		return (getWebServer() != null) ? getWebServer().getServletContext() : null;
	}

	public RegisteredServlet getRegisteredServlet(int index) {
		return (getWebServer() != null) ? getWebServer().getRegisteredServlet(index) : null;
	}

	public RegisteredFilter getRegisteredFilter(int index) {
		return (getWebServer() != null) ? getWebServer().getRegisteredFilters(index) : null;
	}

	@Override
	public ServletWebServerSettings getSettings() {
		return this.settings;
	}

}
