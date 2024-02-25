/*
 * Copyright 2012-2020 the original author or authors.
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

	/**
     * Returns the properties of the JpaProperties object.
     * 
     * @return a Map containing the properties as key-value pairs
     */
    public Map<String, String> getProperties() {
		return this.properties;
	}

	/**
     * Sets the properties for the JpaProperties class.
     * 
     * @param properties a Map containing the properties to be set
     */
    public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	/**
     * Returns the list of mapping resources.
     * 
     * @return the list of mapping resources
     */
    public List<String> getMappingResources() {
		return this.mappingResources;
	}

	/**
     * Returns the database platform used by this JpaProperties instance.
     * 
     * @return the database platform used by this JpaProperties instance
     */
    public String getDatabasePlatform() {
		return this.databasePlatform;
	}

	/**
     * Sets the database platform for the JPA properties.
     * 
     * @param databasePlatform the database platform to be set
     */
    public void setDatabasePlatform(String databasePlatform) {
		this.databasePlatform = databasePlatform;
	}

	/**
     * Returns the database object associated with this JpaProperties instance.
     *
     * @return the database object
     */
    public Database getDatabase() {
		return this.database;
	}

	/**
     * Sets the database for the JpaProperties.
     * 
     * @param database the database to be set
     */
    public void setDatabase(Database database) {
		this.database = database;
	}

	/**
     * Returns a boolean value indicating whether DDL generation is enabled.
     * 
     * @return true if DDL generation is enabled, false otherwise
     */
    public boolean isGenerateDdl() {
		return this.generateDdl;
	}

	/**
     * Sets the flag indicating whether to generate DDL (Data Definition Language) scripts.
     * 
     * @param generateDdl the flag indicating whether to generate DDL scripts
     */
    public void setGenerateDdl(boolean generateDdl) {
		this.generateDdl = generateDdl;
	}

	/**
     * Returns a boolean value indicating whether the SQL statements should be shown.
     *
     * @return true if the SQL statements should be shown, false otherwise.
     */
    public boolean isShowSql() {
		return this.showSql;
	}

	/**
     * Sets the flag to determine whether to show SQL statements.
     * 
     * @param showSql the flag indicating whether to show SQL statements
     */
    public void setShowSql(boolean showSql) {
		this.showSql = showSql;
	}

	/**
     * Returns the value of the openInView property.
     * 
     * @return true if the openInView property is set to true, false otherwise
     */
    public Boolean getOpenInView() {
		return this.openInView;
	}

	/**
     * Sets the flag indicating whether the view should be opened.
     * 
     * @param openInView a boolean value indicating whether the view should be opened
     */
    public void setOpenInView(Boolean openInView) {
		this.openInView = openInView;
	}

}
