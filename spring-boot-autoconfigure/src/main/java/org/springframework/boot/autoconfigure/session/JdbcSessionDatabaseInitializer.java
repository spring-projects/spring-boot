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

package org.springframework.boot.autoconfigure.session;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.util.Assert;

/**
 * Initializer for Spring Session schema.
 *
 * @author Vedran Pavic
 * @since 1.4.0
 */
public class JdbcSessionDatabaseInitializer {

	private static Map<String, String> ALIASES;

	static {
		Map<String, String> aliases = new HashMap<String, String>();
		aliases.put("hsql", "hsqldb");
		aliases.put("postgres", "postgresql");
		ALIASES = Collections.unmodifiableMap(aliases);
	}

	private SessionProperties properties;

	private DataSource dataSource;

	private ResourceLoader resourceLoader;

	public JdbcSessionDatabaseInitializer(SessionProperties properties,
			DataSource dataSource, ResourceLoader resourceLoader) {
		Assert.notNull(properties, "SessionProperties must not be null");
		Assert.notNull(dataSource, "DataSource must not be null");
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		this.properties = properties;
		this.dataSource = dataSource;
		this.resourceLoader = resourceLoader;
	}

	@PostConstruct
	protected void initialize() {
		if (this.properties.getJdbc().getInitializer().isEnabled()) {
			ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
			String schemaLocation = this.properties.getJdbc().getSchema();
			schemaLocation = schemaLocation.replace("@@platform@@", getPlatform());
			populator.addScript(this.resourceLoader.getResource(schemaLocation));
			populator.setContinueOnError(true);
			DatabasePopulatorUtils.execute(populator, this.dataSource);
		}
	}

	private String getPlatform() {
		String databaseName = getDatabaseName();
		if (ALIASES.containsKey(databaseName)) {
			return ALIASES.get(databaseName);
		}
		return databaseName;
	}

	private String getDatabaseName() {
		try {
			String databaseProductName = JdbcUtils
					.extractDatabaseMetaData(this.dataSource, "getDatabaseProductName")
					.toString();
			return JdbcUtils.commonDatabaseName(databaseProductName).toLowerCase();
		}
		catch (MetaDataAccessException ex) {
			throw new IllegalStateException("Unable to detect database type", ex);
		}
	}

}
