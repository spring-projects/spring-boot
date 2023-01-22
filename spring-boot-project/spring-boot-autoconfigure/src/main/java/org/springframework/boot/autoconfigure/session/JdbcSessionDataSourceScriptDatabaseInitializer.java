/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.session;

import java.util.List;
import java.util.Objects;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.jdbc.init.PlatformPlaceholderDatabaseDriverResolver;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.util.StringUtils;

/**
 * {@link DataSourceScriptDatabaseInitializer} for the Spring Session JDBC database. May
 * be registered as a bean to override auto-configuration.
 *
 * @author Dave Syer
 * @author Vedran Pavic
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.6.0
 */
public class JdbcSessionDataSourceScriptDatabaseInitializer extends DataSourceScriptDatabaseInitializer {

	private JdbcSessionProperties properties;

	/**
	 * Create a new {@link JdbcSessionDataSourceScriptDatabaseInitializer} instance.
	 * @param dataSource the Spring Session JDBC data source
	 * @param properties the Spring Session JDBC properties
	 * @see #getSettings
	 */
	public JdbcSessionDataSourceScriptDatabaseInitializer(DataSource dataSource, JdbcSessionProperties properties) {
		this(dataSource, getSettings(dataSource, properties));
		this.properties = properties;
	}

	/**
	 * Create a new {@link JdbcSessionDataSourceScriptDatabaseInitializer} instance.
	 * @param dataSource the Spring Session JDBC data source
	 * @param settings the database initialization settings
	 * @see #getSettings
	 */
	public JdbcSessionDataSourceScriptDatabaseInitializer(DataSource dataSource,
			DatabaseInitializationSettings settings) {
		super(dataSource, settings);
	}

	/**
	 * Adapts {@link JdbcSessionProperties Spring Session JDBC properties} to
	 * {@link DatabaseInitializationSettings} replacing any {@literal @@platform@@}
	 * placeholders.
	 * @param dataSource the Spring Session JDBC data source
	 * @param properties the Spring Session JDBC properties
	 * @return a new {@link DatabaseInitializationSettings} instance
	 * @see #JdbcSessionDataSourceScriptDatabaseInitializer(DataSource,
	 * DatabaseInitializationSettings)
	 */
	static DatabaseInitializationSettings getSettings(DataSource dataSource, JdbcSessionProperties properties) {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setSchemaLocations(resolveSchemaLocations(dataSource, properties));
		settings.setMode(properties.getInitializeSchema());
		settings.setContinueOnError(true);
		return settings;
	}

	@Override
	protected void runScripts(Scripts scripts) {
		validateConfiguration();
		super.runScripts(scripts);
	}

	void validateConfiguration() {
		if (properties == null) return; // cannot validate without this
		JdbcSessionProperties defaults = new JdbcSessionProperties();
		boolean willHappen = switch (properties.getInitializeSchema()) {
			case ALWAYS -> true;
			case NEVER -> false;
			case EMBEDDED -> isEmbeddedDatabase();
		};
		boolean tableNameChanged = !Objects.equals(defaults.getTableName(), properties.getTableName());
		boolean schemaUnchanged = Objects.equals(defaults.getSchema(), properties.getSchema());

		if (willHappen && tableNameChanged && schemaUnchanged) {
			String name = "spring.session.jdbc.schema";
			String value = properties.getSchema();
			String reason = "When JDBC Session database initialization will take place " +
					"(spring.session.jdbc.initialize-schema = " + properties.getInitializeSchema() + "), " +
					"and the table name is not the default value (" + properties.getTableName() + "), " +
					"the schema must be a custom schema to match the specified table name.";
			throw new InvalidConfigurationPropertyValueException(name, value, reason);
		}
	}

	private static List<String> resolveSchemaLocations(DataSource dataSource, JdbcSessionProperties properties) {
		PlatformPlaceholderDatabaseDriverResolver platformResolver = new PlatformPlaceholderDatabaseDriverResolver();
		platformResolver = platformResolver.withDriverPlatform(DatabaseDriver.MARIADB, "mysql");
		if (StringUtils.hasText(properties.getPlatform())) {
			return platformResolver.resolveAll(properties.getPlatform(), properties.getSchema());
		}
		return platformResolver.resolveAll(dataSource, properties.getSchema());
	}

}
