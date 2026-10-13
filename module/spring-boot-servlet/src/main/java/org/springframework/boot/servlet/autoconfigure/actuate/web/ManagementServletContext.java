/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.servlet.autoconfigure.actuate.web;

/**
 * Provides information about the management servlet context for MVC controllers to use.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 4.0.0
 */
@FunctionalInterface
public interface ManagementServletContext {

	/**
	 * Return the servlet path of the management server.
	 * @return the servlet path
	 */
	String getServletPath();

}
