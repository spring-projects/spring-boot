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

package org.springframework.boot.integration.autoconfigure;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.jdbc.init.PropertiesBasedDataSourceScriptDatabaseInitializer;

/**
 * {@link DataSourceScriptDatabaseInitializer} for the Spring Integration database. May be
 * registered as a bean to override auto-configuration.
 *
 * @author Vedran Pavic
 * @author Andy Wilkinson
 * @author Yanming Zhou
 * @since 4.0.0
 */
public class IntegrationDataSourceScriptDatabaseInitializer
		extends PropertiesBasedDataSourceScriptDatabaseInitializer<IntegrationJdbcProperties> {

	/**
	 * Create a new {@link IntegrationDataSourceScriptDatabaseInitializer} instance.
	 * @param dataSource the Spring Integration data source
	 * @param properties the Spring Integration JDBC properties
	 * @see #getSettings
	 */
	public IntegrationDataSourceScriptDatabaseInitializer(DataSource dataSource, IntegrationJdbcProperties properties) {
		super(dataSource, properties, Map.of(DatabaseDriver.MARIADB, "mysql"));
	}

}
