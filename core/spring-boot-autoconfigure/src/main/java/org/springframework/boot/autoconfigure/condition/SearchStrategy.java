/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.condition;

/**
 * Some named search strategies for beans in the bean factory hierarchy.
 *
 * @author Dave Syer
 * @since 1.0.0
 */
public enum SearchStrategy {

	/**
	 * Search only the current context.
	 */
	CURRENT,

	/**
	 * Search all ancestors, but not the current context.
	 */
	ANCESTORS,

	/**
	 * Search the entire hierarchy.
	 */
	ALL

}
