/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.data;

/**
 * Type of Spring Data repositories to enable.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public enum RepositoryType {

	/**
	 * Enables all repository types automatically based on their availability.
	 */
	AUTO,

	/**
	 * Enables imperative repositories.
	 */
	IMPERATIVE,

	/**
	 * Enables no repositories.
	 */
	NONE,

	/**
	 * Enables reactive repositories.
	 */
	REACTIVE

}
