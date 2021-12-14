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

package org.springframework.boot.autoconfigure.batch;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.batch.BatchProperties.Jdbc;
import org.springframework.boot.sql.init.DatabaseInitializationMode;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Initialize the Spring Batch schema (ignoring errors, so it should be idempotent).
 *
 * @author Dave Syer
 * @author Vedran Pavic
 * @since 1.0.0
 * @deprecated since 2.6.0 for removal in 2.8.0 in favor of
 * {@link BatchDataSourceScriptDatabaseInitializer}
 */
@Deprecated
public class BatchDataSourceInitializer extends org.springframework.boot.jdbc.AbstractDataSourceInitializer {

	private final Jdbc jdbcProperties;

	public BatchDataSourceInitializer(DataSource dataSource, ResourceLoader resourceLoader,
			BatchProperties properties) {
		super(dataSource, resourceLoader);
		Assert.notNull(properties, "BatchProperties must not be null");
		this.jdbcProperties = properties.getJdbc();
	}

	@Override
	protected org.springframework.boot.jdbc.DataSourceInitializationMode getMode() {
		DatabaseInitializationMode mode = this.jdbcProperties.getInitializeSchema();
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
		return this.jdbcProperties.getSchema();
	}

	@Override
	protected String getDatabaseName() {
		String platform = this.jdbcProperties.getPlatform();
		if (StringUtils.hasText(platform)) {
			return platform;
		}
		String databaseName = super.getDatabaseName();
		if ("oracle".equals(databaseName)) {
			return "oracle10g";
		}
		if ("mariadb".equals(databaseName)) {
			return "mysql";
		}
		return databaseName;
	}

}
