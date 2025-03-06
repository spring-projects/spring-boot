/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.quartz;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.jdbc.init.PlatformPlaceholderDatabaseDriverResolver;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link DataSourceScriptDatabaseInitializer} for the Quartz Scheduler database. May be
 * registered as a bean to override auto-configuration.
 *
 * @author Vedran Pavic
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.6.0
 */
public class QuartzDataSourceScriptDatabaseInitializer extends DataSourceScriptDatabaseInitializer {

	private final List<String> commentPrefixes;

	/**
	 * Create a new {@link QuartzDataSourceScriptDatabaseInitializer} instance.
	 * @param dataSource the Quartz Scheduler data source
	 * @param properties the Quartz properties
	 * @see #getSettings
	 */
	public QuartzDataSourceScriptDatabaseInitializer(DataSource dataSource, QuartzProperties properties) {
		this(dataSource, getSettings(dataSource, properties), properties.getJdbc().getCommentPrefix());
	}

	/**
	 * Create a new {@link QuartzDataSourceScriptDatabaseInitializer} instance.
	 * @param dataSource the Quartz Scheduler data source
	 * @param settings the database initialization settings
	 * @see #getSettings
	 */
	public QuartzDataSourceScriptDatabaseInitializer(DataSource dataSource, DatabaseInitializationSettings settings) {
		this(dataSource, settings, null);
	}

	private QuartzDataSourceScriptDatabaseInitializer(DataSource dataSource, DatabaseInitializationSettings settings,
			List<String> commentPrefixes) {
		super(dataSource, settings);
		this.commentPrefixes = commentPrefixes;
	}

	@Override
	protected void customize(ResourceDatabasePopulator populator) {
		if (!ObjectUtils.isEmpty(this.commentPrefixes)) {
			populator.setCommentPrefixes(this.commentPrefixes.toArray(new String[0]));
		}
	}

	/**
	 * Adapts {@link QuartzProperties Quartz properties} to
	 * {@link DatabaseInitializationSettings} replacing any {@literal @@platform@@}
	 * placeholders.
	 * @param dataSource the Quartz Scheduler data source
	 * @param properties the Quartz properties
	 * @return a new {@link DatabaseInitializationSettings} instance
	 * @see #QuartzDataSourceScriptDatabaseInitializer(DataSource,
	 * DatabaseInitializationSettings)
	 */
	public static DatabaseInitializationSettings getSettings(DataSource dataSource, QuartzProperties properties) {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setSchemaLocations(resolveSchemaLocations(dataSource, properties.getJdbc()));
		settings.setMode(properties.getJdbc().getInitializeSchema());
		settings.setContinueOnError(true);
		return settings;
	}

	private static List<String> resolveSchemaLocations(DataSource dataSource, QuartzProperties.Jdbc properties) {
		PlatformPlaceholderDatabaseDriverResolver platformResolver = new PlatformPlaceholderDatabaseDriverResolver();
		platformResolver = platformResolver.withDriverPlatform(DatabaseDriver.DB2, "db2_v95");
		platformResolver = platformResolver.withDriverPlatform(DatabaseDriver.MYSQL, "mysql_innodb");
		platformResolver = platformResolver.withDriverPlatform(DatabaseDriver.MARIADB, "mysql_innodb");
		platformResolver = platformResolver.withDriverPlatform(DatabaseDriver.POSTGRESQL, "postgres");
		platformResolver = platformResolver.withDriverPlatform(DatabaseDriver.SQLSERVER, "sqlServer");
		if (StringUtils.hasText(properties.getPlatform())) {
			return platformResolver.resolveAll(properties.getPlatform(), properties.getSchema());
		}
		return platformResolver.resolveAll(dataSource, properties.getSchema());
	}

}
