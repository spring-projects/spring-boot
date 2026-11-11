/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.servlet;

import jakarta.servlet.annotation.WebListener;

/**
 * A registry that holds {@link WebListener @WebListeners}.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public interface WebListenerRegistry {

	/**
	 * Adds web listeners that will be registered with the servlet web server.
	 * @param webListenerClassNames the class names of the web listeners
	 */
	void addWebListeners(String... webListenerClassNames);

}
