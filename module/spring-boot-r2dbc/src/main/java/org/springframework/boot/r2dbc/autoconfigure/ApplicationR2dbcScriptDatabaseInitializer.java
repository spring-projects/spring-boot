/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.autoconfigure;

import io.r2dbc.spi.ConnectionFactory;

import org.springframework.boot.r2dbc.init.R2dbcScriptDatabaseInitializer;
import org.springframework.boot.sql.autoconfigure.init.ApplicationScriptDatabaseInitializer;
import org.springframework.boot.sql.autoconfigure.init.SqlInitializationProperties;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;

/**
 * {@link R2dbcScriptDatabaseInitializer} for the primary SQL database. May be registered
 * as a bean to override auto-configuration.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 4.0.0
 */
public class ApplicationR2dbcScriptDatabaseInitializer extends R2dbcScriptDatabaseInitializer
		implements ApplicationScriptDatabaseInitializer {

	/**
	 * Create a new {@code ApplicationR2dbcScriptDatabaseInitializer} instance.
	 * @param connectionFactory the primary SQL connection factory
	 * @param properties the SQL initialization properties
	 */
	public ApplicationR2dbcScriptDatabaseInitializer(ConnectionFactory connectionFactory,
			SqlInitializationProperties properties) {
		super(connectionFactory, ApplicationScriptDatabaseInitializer.getSettings(properties));
	}

	/**
	 * Create a new {@code ApplicationR2dbcScriptDatabaseInitializer} instance.
	 * @param connectionFactory the primary SQL connection factory
	 * @param settings the database initialization settings
	 */
	public ApplicationR2dbcScriptDatabaseInitializer(ConnectionFactory connectionFactory,
			DatabaseInitializationSettings settings) {
		super(connectionFactory, settings);
	}

}
