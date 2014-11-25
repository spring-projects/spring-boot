/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.flyway;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Flyway database migrations. These are only the properties
 * that Spring needs to validate and enable the migrations. If you want to control the
 * location or format of the scripts you can use the same prefix ("flyway") to inject
 * properties into the {@link Flyway} instance.
 *
 * @author Dave Syer
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "flyway", ignoreUnknownFields = true)
public class FlywayProperties {

	/**
	 * Locations of migrations scripts.
	 */
	private List<String> locations = Arrays.asList("db/migration");

	/**
	 * Check that migration scripts location exists.
	 */
	private boolean checkLocation = false;

	/**
	 * Enable flyway.
	 */
	private boolean enabled = true;

	/**
	 * Login user of the database to migrate.
	 */
	private String user;

	/**
	 *Login password of the database to migrate.
	 */
	private String password;

	/**
	 * JDBC url of the database to migrate. If not set, the primary configured
	 * data source is used.
	 */
	private String url;

	/**
	 * SQL statements to execute to initialize a connection immediately after obtaining it.
	 */
	private List<String> initSqls = Collections.emptyList();

	public void setLocations(List<String> locations) {
		this.locations = locations;
	}

	public List<String> getLocations() {
		return this.locations;
	}

	public void setCheckLocation(boolean checkLocation) {
		this.checkLocation = checkLocation;
	}

	public boolean isCheckLocation() {
		return this.checkLocation;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getUser() {
		return this.user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return (this.password == null ? "" : this.password);
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public List<String> getInitSqls() {
		return this.initSqls;
	}

	public void setInitSqls(List<String> initSqls) {
		this.initSqls = initSqls;
	}

	public boolean isCreateDataSource() {
		return this.url != null && this.user != null;
	}
}
