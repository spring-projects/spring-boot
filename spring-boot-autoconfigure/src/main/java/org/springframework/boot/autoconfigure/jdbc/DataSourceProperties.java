/*
 * Copyright 2012-2015 the original author or authors.
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

import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Base class for configuration of a data source.
 *
 * @author Dave Syer
 * @author Maciej Walkowiak
 * @author Stephane Nicoll
 * @author Benedikt Ritter
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = DataSourceProperties.PREFIX)
public class DataSourceProperties
		implements BeanClassLoaderAware, EnvironmentAware, InitializingBean {

	public static final String PREFIX = "spring.datasource";

	private ClassLoader classLoader;

	private Environment environment;

	/**
	 * Name of the datasource.
	 */
	private String name = "testdb";

	/**
	 * Fully qualified name of the connection pool implementation to use. By default, it
	 * is auto-detected from the classpath.
	 */
	private Class<? extends DataSource> type;

	/**
	 * Fully qualified name of the JDBC driver. Auto-detected based on the URL by default.
	 */
	private String driverClassName;

	/**
	 * JDBC url of the database.
	 */
	private String url;

	/**
	 * Login user of the database.
	 */
	private String username;

	/**
	 * Login password of the database.
	 */
	private String password;

	/**
	 * JNDI location of the datasource. Class, url, username & password are ignored when
	 * set.
	 */
	private String jndiName;

	/**
	 * Populate the database using 'data.sql'.
	 */
	private boolean initialize = true;

	/**
	 * Platform to use in the schema resource (schema-${platform}.sql).
	 */
	private String platform = "all";

	/**
	 * Schema (DDL) script resource reference.
	 */
	private String schema;

	/**
	 * Data (DML) script resource reference.
	 */
	private String data;

	/**
	 * Do not stop if an error occurs while initializing the database.
	 */
	private boolean continueOnError = false;

	/**
	 * Statement separator in SQL initialization scripts.
	 */
	private String separator = ";";

	/**
	 * SQL scripts encoding.
	 */
	private Charset sqlScriptEncoding;

	private EmbeddedDatabaseConnection embeddedDatabaseConnection = EmbeddedDatabaseConnection.NONE;

	private Xa xa = new Xa();

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.embeddedDatabaseConnection = EmbeddedDatabaseConnection
				.get(this.classLoader);
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Class<? extends DataSource> getType() {
		return this.type;
	}

	public void setType(Class<? extends DataSource> type) {
		this.type = type;
	}

	public String getDriverClassName() {
		if (StringUtils.hasText(this.driverClassName)) {
			Assert.state(driverClassIsLoadable(),
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
			throw new DataSourceBeanCreationException(this.embeddedDatabaseConnection,
					this.environment, "driver class");
		}
		return driverClassName;
	}

	private boolean driverClassIsLoadable() {
		try {
			ClassUtils.forName(this.driverClassName, null);
			return true;
		}
		catch (UnsupportedClassVersionError ex) {
			// Driver library has been compiled with a later JDK, propagate error
			throw ex;
		}
		catch (Throwable ex) {
			return false;
		}
	}

	public void setDriverClassName(String driverClassName) {
		this.driverClassName = driverClassName;
	}

	public String getUrl() {
		if (StringUtils.hasText(this.url)) {
			return this.url;
		}
		String url = this.embeddedDatabaseConnection.getUrl();
		if (!StringUtils.hasText(url)) {
			throw new DataSourceBeanCreationException(this.embeddedDatabaseConnection,
					this.environment, "url");
		}
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
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

	public void setUsername(String username) {
		this.username = username;
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

	public Charset getSqlScriptEncoding() {
		return this.sqlScriptEncoding;
	}

	public void setSqlScriptEncoding(Charset sqlScriptEncoding) {
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

		/**
		 * XA datasource fully qualified name.
		 */
		private String dataSourceClassName;

		/**
		 * Properties to pass to the XA data source.
		 */
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

	private static class DataSourceBeanCreationException extends BeanCreationException {

		DataSourceBeanCreationException(EmbeddedDatabaseConnection connection,
				Environment environment, String property) {
			super(getMessage(connection, environment, property));
		}

		private static String getMessage(EmbeddedDatabaseConnection connection,
				Environment environment, String property) {
			StringBuilder message = new StringBuilder();
			message.append("Cannot determine embedded database " + property
					+ " for database type " + connection + ". ");
			message.append("If you want an embedded database please put a supported "
					+ "one on the classpath. ");
			message.append("If you have database settings to be loaded from a "
					+ "particular profile you may need to active it");
			if (environment != null) {
				String[] profiles = environment.getActiveProfiles();
				if (ObjectUtils.isEmpty(profiles)) {
					message.append(" (no profiles are currently active)");
				}
				else {
					message.append(" (the profiles \""
							+ StringUtils.arrayToCommaDelimitedString(
									environment.getActiveProfiles())
							+ "\" are currently active)");

				}
			}
			message.append(".");
			return message.toString();
		}

	}

}
