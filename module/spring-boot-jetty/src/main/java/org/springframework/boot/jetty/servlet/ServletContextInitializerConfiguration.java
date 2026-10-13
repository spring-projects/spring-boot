/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jetty.servlet;

import jakarta.servlet.ServletException;
import org.eclipse.jetty.ee11.webapp.AbstractConfiguration;
import org.eclipse.jetty.ee11.webapp.Configuration;
import org.eclipse.jetty.ee11.webapp.WebAppContext;

import org.springframework.boot.web.server.servlet.ServletContextInitializers;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.util.Assert;

/**
 * Jetty {@link Configuration} that calls {@link ServletContextInitializer}s.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ServletContextInitializerConfiguration extends AbstractConfiguration {

	private final ServletContextInitializers initializers;

	/**
	 * Create a new {@link ServletContextInitializerConfiguration}.
	 * @param initializers the initializers that should be invoked
	 */
	ServletContextInitializerConfiguration(ServletContextInitializers initializers) {
		super(new AbstractConfiguration.Builder());
		Assert.notNull(initializers, "'initializers' must not be null");
		this.initializers = initializers;
	}

	@Override
	public void configure(WebAppContext context) throws Exception {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(context.getClassLoader());
		try {
			callInitializers(context);
		}
		finally {
			Thread.currentThread().setContextClassLoader(classLoader);
		}
	}

	private void callInitializers(WebAppContext context) throws ServletException {
		try {
			context.getContext().setExtendedListenerTypes(true);
			for (ServletContextInitializer initializer : this.initializers) {
				initializer.onStartup(context.getServletContext());
			}
		}
		finally {
			context.getContext().setExtendedListenerTypes(false);
		}
	}

}
