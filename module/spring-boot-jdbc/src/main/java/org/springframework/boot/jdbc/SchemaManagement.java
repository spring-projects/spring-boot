/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc;

/**
 * An enumeration of the available schema management options.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public enum SchemaManagement {

	/**
	 * The schema is managed and will be created at the appropriate time.
	 */
	MANAGED,

	/**
	 * The schema is not managed.
	 */
	UNMANAGED

}
