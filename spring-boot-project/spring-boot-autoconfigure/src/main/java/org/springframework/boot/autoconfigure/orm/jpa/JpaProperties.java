/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.orm.jpa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.orm.jpa.vendor.Database;

/**
 * External configuration properties for a JPA EntityManagerFactory created by Spring.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Madhura Bhave
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "spring.jpa")
public class JpaProperties {

	/**
	 * Additional native properties to set on the JPA provider.
	 */
	private Map<String, String> properties = new HashMap<>();

	/**
	 * Mapping resources (equivalent to "mapping-file" entries in persistence.xml).
	 */
	private final List<String> mappingResources = new ArrayList<>();

	/**
	 * Name of the target database to operate on, auto-detected by default. Can be
	 * alternatively set using the "Database" enum.
	 */
	private String databasePlatform;

	/**
	 * Target database to operate on, auto-detected by default. Can be alternatively set
	 * using the "databasePlatform" property.
	 */
	private Database database;

	/**
	 * Whether to initialize the schema on startup.
	 */
	private boolean generateDdl = false;

	/**
	 * Whether to enable logging of SQL statements.
	 */
	private boolean showSql = false;

	/**
	 * Register OpenEntityManagerInViewInterceptor. Binds a JPA EntityManager to the
	 * thread for the entire processing of the request.
	 */
	private Boolean openInView;

	public Map<String, String> getProperties() {
		return this.properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public List<String> getMappingResources() {
		return this.mappingResources;
	}

	public String getDatabasePlatform() {
		return this.databasePlatform;
	}

	public void setDatabasePlatform(String databasePlatform) {
		this.databasePlatform = databasePlatform;
	}

	public Database getDatabase() {
		return this.database;
	}

	public void setDatabase(Database database) {
		this.database = database;
	}

	public boolean isGenerateDdl() {
		return this.generateDdl;
	}

	public void setGenerateDdl(boolean generateDdl) {
		this.generateDdl = generateDdl;
	}

	public boolean isShowSql() {
		return this.showSql;
	}

	public void setShowSql(boolean showSql) {
		this.showSql = showSql;
	}

	public Boolean getOpenInView() {
		return this.openInView;
	}

	public void setOpenInView(Boolean openInView) {
		this.openInView = openInView;
	}

	/**
	 * Determine the {@link Database} to use based on this configuration and the primary
	 * {@link DataSource}.
	 * @param dataSource the auto-configured data source
	 * @return {@code Database}
	 */
	public Database determineDatabase(DataSource dataSource) {
		if (this.database != null) {
			return this.database;
		}
		return DatabaseLookup.getDatabase(dataSource);
	}

}
