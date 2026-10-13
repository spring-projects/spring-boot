/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.context;

import org.jspecify.annotations.Nullable;

import org.springframework.context.ConfigurableApplicationContext;

/**
 * SPI interface to be implemented by most if not all {@link WebServerApplicationContext
 * web server application contexts}. Provides facilities to configure the context, in
 * addition to the methods in the {WebServerApplicationContext} interface.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public interface ConfigurableWebServerApplicationContext
		extends ConfigurableApplicationContext, WebServerApplicationContext {

	/**
	 * Set the server namespace of the context.
	 * @param serverNamespace the server namespace
	 * @see #getServerNamespace()
	 */
	void setServerNamespace(@Nullable String serverNamespace);

}
