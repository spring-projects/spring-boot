/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.util.Assert;

/**
 * Abstract base class for database schema initializers.
 *
 * @author Vedran Pavic
 * @since 1.5.0
 */
public abstract class AbstractDatabaseInitializer {

	private static final String PLATFORM_PLACEHOLDER = "@@platform@@";

	private DataSource dataSource;

	private ResourceLoader resourceLoader;

	public AbstractDatabaseInitializer(DataSource dataSource, ResourceLoader resourceLoader) {
		Assert.notNull(dataSource, "DataSource must not be null");
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		this.dataSource = dataSource;
		this.resourceLoader = resourceLoader;
	}

	@PostConstruct
	protected void initialize() {
		if (isEnabled()) {
			ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
			String schemaLocation = getSchemaLocation();
			if (schemaLocation.contains(PLATFORM_PLACEHOLDER)) {
				String platform = customizeDatabaseName(getDatabaseName());
				schemaLocation = schemaLocation.replace(PLATFORM_PLACEHOLDER, platform);
			}
			populator.addScript(this.resourceLoader.getResource(schemaLocation));
			populator.setContinueOnError(true);
			DatabasePopulatorUtils.execute(populator, this.dataSource);
		}
	}

	protected abstract boolean isEnabled();

	protected abstract String getSchemaLocation();

	protected String customizeDatabaseName(String databaseName) {
		return databaseName;
	}

	private String getDatabaseName() {
		try {
			String databaseProductName = JdbcUtils.extractDatabaseMetaData(
					this.dataSource, "getDatabaseProductName").toString();
			databaseProductName = JdbcUtils.commonDatabaseName(databaseProductName);
			DatabaseDriver databaseDriver = DatabaseDriver.fromProductName(
					databaseProductName);
			if (databaseDriver == DatabaseDriver.UNKNOWN) {
				throw new IllegalStateException("Unable to detect database type");
			}
			return databaseDriver.toString().toLowerCase();
		}
		catch (MetaDataAccessException ex) {
			throw new IllegalStateException("Unable to detect database type", ex);
		}
	}

}
