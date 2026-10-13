/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.web;

/**
 * Enumeration of management context types.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public enum ManagementContextType {

	/**
	 * The management context is the same as the main application context.
	 */
	SAME,

	/**
	 * The management context is a separate context that is a child of the main
	 * application context.
	 */
	CHILD,

	/**
	 * The management context can be either the same as the main application context or a
	 * child of the main application context.
	 */
	ANY

}
