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

package org.springframework.boot.quartz.autoconfigure;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.jdbc.init.PropertiesBasedDataSourceScriptDatabaseInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.util.ObjectUtils;

/**
 * {@link DataSourceScriptDatabaseInitializer} for the Quartz Scheduler database. May be
 * registered as a bean to override auto-configuration.
 *
 * @author Vedran Pavic
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Yanming Zhou
 * @since 4.0.0
 */
public class QuartzDataSourceScriptDatabaseInitializer
		extends PropertiesBasedDataSourceScriptDatabaseInitializer<QuartzJdbcProperties> {

	private final @Nullable List<String> commentPrefixes;

	/**
	 * Create a new {@link QuartzDataSourceScriptDatabaseInitializer} instance.
	 * @param dataSource the Quartz Scheduler data source
	 * @param properties the Quartz JDBC properties
	 * @see #getSettings
	 */
	public QuartzDataSourceScriptDatabaseInitializer(DataSource dataSource, QuartzJdbcProperties properties) {
		super(dataSource, properties,
				Map.of(DatabaseDriver.DB2, "db2_v95", DatabaseDriver.MYSQL, "mysql_innodb", DatabaseDriver.MARIADB,
						"mysql_innodb", DatabaseDriver.POSTGRESQL, "postgres", DatabaseDriver.SQLSERVER, "sqlServer"));
		this.commentPrefixes = properties.getCommentPrefix();
	}

	@Override
	protected void customize(ResourceDatabasePopulator populator) {
		if (!ObjectUtils.isEmpty(this.commentPrefixes)) {
			populator.setCommentPrefixes(this.commentPrefixes.toArray(new String[0]));
		}
	}

}
