/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.servlet;

/**
 * Enumeration of filter dispatcher types, identical to
 * {@link jakarta.servlet.DispatcherType} and used in configuration as the servlet API may
 * not be present.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public enum DispatcherType {

	/**
	 * Apply the filter on "RequestDispatcher.forward()" calls.
	 */
	FORWARD,

	/**
	 * Apply the filter on "RequestDispatcher.include()" calls.
	 */
	INCLUDE,

	/**
	 * Apply the filter on ordinary client calls.
	 */
	REQUEST,

	/**
	 * Apply the filter under calls dispatched from an AsyncContext.
	 */
	ASYNC,

	/**
	 * Apply the filter when an error is handled.
	 */
	ERROR

}
