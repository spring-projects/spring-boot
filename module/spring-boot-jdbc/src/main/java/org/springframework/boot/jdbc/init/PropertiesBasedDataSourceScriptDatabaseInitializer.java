/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.jdbc.init;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.util.StringUtils;

/**
 * Convenience class for construct {@link DataSourceScriptDatabaseInitializer} base on
 * {@link DatabaseInitializationProperties}.
 *
 * @param <T> the {@link DatabaseInitializationProperties} type being used
 * @author Yanming Zhou
 * @since 4.0.0
 */
public class PropertiesBasedDataSourceScriptDatabaseInitializer<T extends DatabaseInitializationProperties>
		extends DataSourceScriptDatabaseInitializer {

	/**
	 * Create a new {@link PropertiesBasedDataSourceScriptDatabaseInitializer} instance.
	 * @param dataSource the data source
	 * @param properties the configuration properties
	 * @see #getSettings
	 */
	public PropertiesBasedDataSourceScriptDatabaseInitializer(DataSource dataSource, T properties) {
		this(dataSource, properties, Collections.emptyMap());
	}

	/**
	 * Create a new {@link PropertiesBasedDataSourceScriptDatabaseInitializer} instance.
	 * @param dataSource the data source
	 * @param properties the configuration properties
	 * @param driverMappings the driver mappings
	 * @see #getSettings
	 */
	public PropertiesBasedDataSourceScriptDatabaseInitializer(DataSource dataSource, T properties,
			Map<DatabaseDriver, String> driverMappings) {
		super(dataSource, getSettings(dataSource, properties, driverMappings));
	}

	/**
	 * Adapts {@link DatabaseInitializationProperties configuration properties} to
	 * {@link DatabaseInitializationSettings} replacing any {@literal @@platform@@}
	 * placeholders.
	 * @param dataSource the data source
	 * @param properties the configuration properties
	 * @param driverMappings the driver mappings
	 * @param <T> the {@link DatabaseInitializationProperties} type being used
	 * @return a new {@link DatabaseInitializationSettings} instance
	 */
	private static <T extends DatabaseInitializationProperties> DatabaseInitializationSettings getSettings(
			DataSource dataSource, T properties, Map<DatabaseDriver, String> driverMappings) {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setSchemaLocations(resolveSchemaLocations(dataSource, properties, driverMappings));
		settings.setMode(properties.getInitializeSchema());
		settings.setContinueOnError(properties.isContinueOnError());
		return settings;
	}

	private static <T extends DatabaseInitializationProperties> List<String> resolveSchemaLocations(
			DataSource dataSource, T properties, Map<DatabaseDriver, String> driverMappings) {
		PlatformPlaceholderDatabaseDriverResolver platformResolver = new PlatformPlaceholderDatabaseDriverResolver();
		for (Map.Entry<DatabaseDriver, String> entry : driverMappings.entrySet()) {
			platformResolver = platformResolver.withDriverPlatform(entry.getKey(), entry.getValue());
		}
		if (StringUtils.hasText(properties.getPlatform())) {
			return platformResolver.resolveAll(properties.getPlatform(), properties.getSchema());
		}
		return platformResolver.resolveAll(dataSource, properties.getSchema());
	}

}
