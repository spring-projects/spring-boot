/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat.servlet;

import java.util.Set;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.tomcat.TomcatEmbeddedContext;
import org.springframework.boot.web.servlet.ServletContextInitializer;

/**
 * {@link ServletContainerInitializer} used to trigger {@link ServletContextInitializer
 * ServletContextInitializers} and track startup errors.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class DeferredServletContainerInitializers
		implements ServletContainerInitializer, TomcatEmbeddedContext.DeferredStartupExceptions {

	private static final Log logger = LogFactory.getLog(DeferredServletContainerInitializers.class);

	private final Iterable<ServletContextInitializer> initializers;

	private volatile @Nullable Exception startUpException;

	DeferredServletContainerInitializers(Iterable<ServletContextInitializer> initializers) {
		this.initializers = initializers;
	}

	@Override
	public void onStartup(Set<Class<?>> classes, ServletContext servletContext) throws ServletException {
		try {
			for (ServletContextInitializer initializer : this.initializers) {
				initializer.onStartup(servletContext);
			}
		}
		catch (Exception ex) {
			this.startUpException = ex;
			// Prevent Tomcat from logging and re-throwing when we know we can
			// deal with it in the main thread, but log for information here.
			if (logger.isErrorEnabled()) {
				logger.error("Error starting Tomcat context. Exception: " + ex.getClass().getName() + ". Message: "
						+ ex.getMessage());
			}
		}
	}

	@Override
	public void rethrow() throws Exception {
		if (this.startUpException != null) {
			throw this.startUpException;
		}
	}

}
