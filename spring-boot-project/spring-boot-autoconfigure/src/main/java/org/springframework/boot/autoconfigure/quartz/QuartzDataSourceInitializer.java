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

import javax.sql.DataSource;

import org.springframework.boot.sql.init.DatabaseInitializationMode;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Initialize the Quartz Scheduler schema.
 *
 * @author Vedran Pavic
 * @since 2.0.0
 * @deprecated since 2.6.0 for removal in 2.8.0 in favor of
 * {@link QuartzDataSourceScriptDatabaseInitializer}
 */
@Deprecated
public class QuartzDataSourceInitializer extends org.springframework.boot.jdbc.AbstractDataSourceInitializer {

	private final QuartzProperties properties;

	public QuartzDataSourceInitializer(DataSource dataSource, ResourceLoader resourceLoader,
			QuartzProperties properties) {
		super(dataSource, resourceLoader);
		Assert.notNull(properties, "QuartzProperties must not be null");
		this.properties = properties;
	}

	@Override
	protected void customize(ResourceDatabasePopulator populator) {
		populator.setCommentPrefixes(this.properties.getJdbc().getCommentPrefix().toArray(new String[0]));
	}

	@Override
	protected org.springframework.boot.jdbc.DataSourceInitializationMode getMode() {
		DatabaseInitializationMode mode = this.properties.getJdbc().getInitializeSchema();
		switch (mode) {
		case ALWAYS:
			return org.springframework.boot.jdbc.DataSourceInitializationMode.ALWAYS;
		case EMBEDDED:
			return org.springframework.boot.jdbc.DataSourceInitializationMode.EMBEDDED;
		case NEVER:
		default:
			return org.springframework.boot.jdbc.DataSourceInitializationMode.NEVER;
		}
	}

	@Override
	protected String getSchemaLocation() {
		return this.properties.getJdbc().getSchema();
	}

	@Override
	protected String getDatabaseName() {
		String platform = this.properties.getJdbc().getPlatform();
		if (StringUtils.hasText(platform)) {
			return platform;
		}
		String databaseName = super.getDatabaseName();
		if ("db2".equals(databaseName)) {
			return "db2_v95";
		}
		if ("mysql".equals(databaseName) || "mariadb".equals(databaseName)) {
			return "mysql_innodb";
		}
		if ("postgresql".equals(databaseName)) {
			return "postgres";
		}
		if ("sqlserver".equals(databaseName)) {
			return "sqlServer";
		}
		return databaseName;
	}

}
