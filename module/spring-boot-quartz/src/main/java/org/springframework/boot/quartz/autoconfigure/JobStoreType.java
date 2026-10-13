/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.quartz.autoconfigure;

/**
 * Define the supported Quartz {@code JobStore}.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public enum JobStoreType {

	/**
	 * Store jobs in memory.
	 */
	MEMORY,

	/**
	 * Store jobs in the database.
	 */
	JDBC

}
