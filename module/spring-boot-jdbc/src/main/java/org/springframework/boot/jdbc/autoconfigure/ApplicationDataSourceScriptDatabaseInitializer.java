/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.autoconfigure;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.sql.autoconfigure.init.ApplicationScriptDatabaseInitializer;
import org.springframework.boot.sql.autoconfigure.init.SqlInitializationProperties;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;

/**
 * {@link DataSourceScriptDatabaseInitializer} for the primary SQL database. May be
 * registered as a bean to override auto-configuration.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 4.0.0
 */
public class ApplicationDataSourceScriptDatabaseInitializer extends DataSourceScriptDatabaseInitializer
		implements ApplicationScriptDatabaseInitializer {

	/**
	 * Create a new {@link ApplicationDataSourceScriptDatabaseInitializer} instance.
	 * @param dataSource the primary SQL data source
	 * @param properties the SQL initialization properties
	 */
	public ApplicationDataSourceScriptDatabaseInitializer(DataSource dataSource,
			SqlInitializationProperties properties) {
		this(dataSource, ApplicationScriptDatabaseInitializer.getSettings(properties));
	}

	/**
	 * Create a new {@link ApplicationDataSourceScriptDatabaseInitializer} instance.
	 * @param dataSource the primary SQL data source
	 * @param settings the database initialization settings
	 */
	public ApplicationDataSourceScriptDatabaseInitializer(DataSource dataSource,
			DatabaseInitializationSettings settings) {
		super(dataSource, settings);
	}

}
