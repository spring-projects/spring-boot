/*
 * Copyright 2012-2017 the original author or authors.
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
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.util.Assert;

/**
 * Base class used for database initialization.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @since 1.5.0
 */
public abstract class AbstractDatabaseInitializer {

	private static final String PLATFORM_PLACEHOLDER = "@@platform@@";

	private final DataSource dataSource;

	private final ResourceLoader resourceLoader;

	protected AbstractDatabaseInitializer(DataSource dataSource,
			ResourceLoader resourceLoader) {
		Assert.notNull(dataSource, "DataSource must not be null");
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		this.dataSource = dataSource;
		this.resourceLoader = resourceLoader;
	}

	@PostConstruct
	protected void initialize() {
		if (!isEnabled()) {
			return;
		}
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
		String schemaLocation = getSchemaLocation();
		if (schemaLocation.contains(PLATFORM_PLACEHOLDER)) {
			String platform = getDatabaseName();
			schemaLocation = schemaLocation.replace(PLATFORM_PLACEHOLDER, platform);
		}
		populator.addScript(this.resourceLoader.getResource(schemaLocation));
		populator.setContinueOnError(true);
		DatabasePopulatorUtils.execute(populator, this.dataSource);
	}

	private boolean isEnabled() {
		if (getMode() == DatabaseInitializationMode.NEVER) {
			return false;
		}
		if (getMode() == DatabaseInitializationMode.EMBEDDED
				&& !EmbeddedDatabaseConnection.isEmbedded(this.dataSource)) {
			return false;
		}
		return true;
	}

	protected abstract DatabaseInitializationMode getMode();

	protected abstract String getSchemaLocation();

	protected String getDatabaseName() {
		try {
			String productName = JdbcUtils.commonDatabaseName(JdbcUtils
					.extractDatabaseMetaData(this.dataSource, "getDatabaseProductName")
					.toString());
			DatabaseDriver databaseDriver = DatabaseDriver.fromProductName(productName);
			if (databaseDriver == DatabaseDriver.UNKNOWN) {
				throw new IllegalStateException("Unable to detect database type");
			}
			return databaseDriver.getId();
		}
		catch (MetaDataAccessException ex) {
			throw new IllegalStateException("Unable to detect database type", ex);
		}
	}

}
