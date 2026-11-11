/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.servlet;

import jakarta.servlet.annotation.WebListener;

/**
 * Interface to be implemented by types that register {@link WebListener @WebListeners}.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public interface WebListenerRegistrar {

	/**
	 * Register web listeners with the given registry.
	 * @param registry the web listener registry
	 */
	void register(WebListenerRegistry registry);

}
