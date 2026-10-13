/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.sql.init;

/**
 * Supported database initialization modes.
 *
 * @author Andy Wilkinson
 * @since 2.5.1
 * @see AbstractScriptDatabaseInitializer
 */
public enum DatabaseInitializationMode {

	/**
	 * Always initialize the database.
	 */
	ALWAYS,

	/**
	 * Only initialize an embedded database.
	 */
	EMBEDDED,

	/**
	 * Never initialize the database.
	 */
	NEVER

}
