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

package org.springframework.boot.autoconfigure.jdbc;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Base class for configuration of a data source.
 *
 * @author Dave Syer
 * @author Maciej Walkowiak
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = DataSourceProperties.PREFIX)
public class DataSourceProperties implements BeanClassLoaderAware, InitializingBean {

	public static final String PREFIX = "spring.datasource";

	private String driverClassName;

	private String url;

	private String username;

	private String password;

	private ClassLoader classLoader;

	private String jndiName;

	private boolean initialize = true;

	private String platform = "all";

	private String schema;

	private String data;

	private boolean continueOnError = false;

	private String separator = ";";

	private String sqlScriptEncoding;

	private EmbeddedDatabaseConnection embeddedDatabaseConnection = EmbeddedDatabaseConnection.NONE;

	private Xa xa = new Xa();

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.embeddedDatabaseConnection = EmbeddedDatabaseConnection
				.get(this.classLoader);
	}

	protected String getDriverClassName() {
		if (StringUtils.hasText(this.driverClassName)) {
			Assert.state(ClassUtils.isPresent(this.driverClassName, null),
					"Cannot load driver class: " + this.driverClassName);
			return this.driverClassName;
		}
		String driverClassName = null;

		if (StringUtils.hasText(this.url)) {
			driverClassName = DatabaseDriver.fromJdbcUrl(this.url).getDriverClassName();
		}

		if (!StringUtils.hasText(driverClassName)) {
			driverClassName = this.embeddedDatabaseConnection.getDriverClassName();
		}

		if (!StringUtils.hasText(driverClassName)) {
			throw new BeanCreationException(
					"Cannot determine embedded database driver class for database type "
							+ this.embeddedDatabaseConnection
							+ ". If you want an embedded "
							+ "database please put a supported one on the classpath.");
		}
		return driverClassName;
	}

	public String getUrl() {
		if (StringUtils.hasText(this.url)) {
			return this.url;
		}
		String url = this.embeddedDatabaseConnection.getUrl();
		if (!StringUtils.hasText(url)) {
			throw new BeanCreationException(
					"Cannot determine embedded database url for database type "
							+ this.embeddedDatabaseConnection
							+ ". If you want an embedded "
							+ "database please put a supported one on the classpath.");
		}
		return url;
	}

	public String getUsername() {
		if (StringUtils.hasText(this.username)) {
			return this.username;
		}
		if (EmbeddedDatabaseConnection.isEmbedded(getDriverClassName())) {
			return "sa";
		}
		return null;
	}

	public String getPassword() {
		if (StringUtils.hasText(this.password)) {
			return this.password;
		}
		if (EmbeddedDatabaseConnection.isEmbedded(getDriverClassName())) {
			return "";
		}
		return null;
	}

	public void setDriverClassName(String driverClassName) {
		this.driverClassName = driverClassName;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getJndiName() {
		return this.jndiName;
	}

	/**
	 * Allows the DataSource to be managed by the container and obtained via JNDI. The
	 * {@code URL}, {@code driverClassName}, {@code username} and {@code password} fields
	 * will be ignored when using JNDI lookups.
	 * @param jndiName the JNDI name
	 */
	public void setJndiName(String jndiName) {
		this.jndiName = jndiName;
	}

	public boolean isInitialize() {
		return this.initialize;
	}

	public void setInitialize(boolean initialize) {
		this.initialize = initialize;
	}

	public String getPlatform() {
		return this.platform;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}

	public String getSchema() {
		return this.schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public String getData() {
		return this.data;
	}

	public void setData(String script) {
		this.data = script;
	}

	public boolean isContinueOnError() {
		return this.continueOnError;
	}

	public void setContinueOnError(boolean continueOnError) {
		this.continueOnError = continueOnError;
	}

	public String getSeparator() {
		return this.separator;
	}

	public void setSeparator(String separator) {
		this.separator = separator;
	}

	public String getSqlScriptEncoding() {
		return this.sqlScriptEncoding;
	}

	public void setSqlScriptEncoding(String sqlScriptEncoding) {
		this.sqlScriptEncoding = sqlScriptEncoding;
	}

	public ClassLoader getClassLoader() {
		return this.classLoader;
	}

	public Xa getXa() {
		return this.xa;
	}

	public void setXa(Xa xa) {
		this.xa = xa;
	}

	/**
	 * XA Specific datasource settings.
	 */
	public static class Xa {

		private String dataSourceClassName;

		private Map<String, String> properties = new LinkedHashMap<String, String>();

		public String getDataSourceClassName() {
			return this.dataSourceClassName;
		}

		public void setDataSourceClassName(String dataSourceClassName) {
			this.dataSourceClassName = dataSourceClassName;
		}

		public Map<String, String> getProperties() {
			return this.properties;
		}

		public void setProperties(Map<String, String> properties) {
			this.properties = properties;
		}

	}

}
