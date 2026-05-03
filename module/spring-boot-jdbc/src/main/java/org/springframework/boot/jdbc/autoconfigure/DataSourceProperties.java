/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.jdbc.autoconfigure;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Base class for configuration of a data source.
 *
 * @author Dave Syer
 * @author Maciej Walkowiak
 * @author Stephane Nicoll
 * @author Benedikt Ritter
 * @author Eddú Meléndez
 * @author Scott Frederick
 * @since 4.0.0
 */
@ConfigurationProperties("spring.datasource")
public class DataSourceProperties implements BeanClassLoaderAware, InitializingBean {

	@SuppressWarnings("NullAway.Init")
	private ClassLoader classLoader;

	/**
	 * Whether to generate a random datasource name.
	 */
	private boolean generateUniqueName = true;

	/**
	 * Datasource name to use if "generate-unique-name" is false. Defaults to "testdb"
	 * when using an embedded database, otherwise null.
	 */
	private @Nullable String name;

	/**
	 * Fully qualified name of the DataSource implementation to use. By default, a
	 * connection pool implementation is auto-detected from the classpath.
	 */
	private @Nullable Class<? extends DataSource> type;

	/**
	 * Fully qualified name of the JDBC driver. Auto-detected based on the URL by default.
	 */
	private @Nullable String driverClassName;

	/**
	 * JDBC URL of the database.
	 */
	private @Nullable String url;

	/**
	 * Login username of the database.
	 */
	private @Nullable String username;

	/**
	 * Login password of the database.
	 */
	private @Nullable String password;

	/**
	 * JNDI location of the datasource. Class, url, username and password are ignored when
	 * set.
	 */
	private @Nullable String jndiName;

	/**
	 * Connection details for an embedded database. Defaults to the most suitable embedded
	 * database that is available on the classpath.
	 */
	@SuppressWarnings("NullAway.Init")
	private EmbeddedDatabaseConnection embeddedDatabaseConnection;

	private Xa xa = new Xa();

	private @Nullable String uniqueName;

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.embeddedDatabaseConnection == null) {
			this.embeddedDatabaseConnection = EmbeddedDatabaseConnection.get(this.classLoader);
		}
	}

	/**
	 * Initialize a {@link DataSourceBuilder} with the state of this instance.
	 * @return a {@link DataSourceBuilder} initialized with the customizations defined on
	 * this instance
	 */
	public DataSourceBuilder<?> initializeDataSourceBuilder() {
		return DataSourceBuilder.create(getClassLoader())
			.type(getType())
			.driverClassName(determineDriverClassName())
			.url(determineUrl())
			.username(determineUsername())
			.password(determinePassword());
	}

	public boolean isGenerateUniqueName() {
		return this.generateUniqueName;
	}

	public void setGenerateUniqueName(boolean generateUniqueName) {
		this.generateUniqueName = generateUniqueName;
	}

	public @Nullable String getName() {
		return this.name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	public @Nullable Class<? extends DataSource> getType() {
		return this.type;
	}

	public void setType(@Nullable Class<? extends DataSource> type) {
		this.type = type;
	}

	/**
	 * Return the configured driver or {@code null} if none was configured.
	 * @return the configured driver
	 * @see #determineDriverClassName()
	 */
	public @Nullable String getDriverClassName() {
		return this.driverClassName;
	}

	public void setDriverClassName(@Nullable String driverClassName) {
		this.driverClassName = driverClassName;
	}

	/**
	 * Determine the driver to use based on this configuration and the environment.
	 * @return the driver to use
	 */
	public String determineDriverClassName() {
		String driverClassName = findDriverClassName();
		if (!StringUtils.hasText(driverClassName)) {
			throw new DataSourceBeanCreationException("Failed to determine a suitable driver class", this,
					this.embeddedDatabaseConnection);
		}
		return driverClassName;
	}

	@Nullable String findDriverClassName() {
		if (StringUtils.hasText(this.driverClassName)) {
			Assert.state(driverClassIsLoadable(this.driverClassName),
					() -> "Cannot load driver class: " + this.driverClassName);
			return this.driverClassName;
		}
		String driverClassName = null;
		if (StringUtils.hasText(this.url)) {
			driverClassName = DatabaseDriver.fromJdbcUrl(this.url).getDriverClassName();
		}
		if (!StringUtils.hasText(driverClassName)) {
			driverClassName = this.embeddedDatabaseConnection.getDriverClassName();
		}
		return driverClassName;
	}

	private boolean driverClassIsLoadable(String driverClassName) {
		try {
			ClassUtils.forName(driverClassName, null);
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

	/**
	 * Return the configured url or {@code null} if none was configured.
	 * @return the configured url
	 * @see #determineUrl()
	 */
	public @Nullable String getUrl() {
		return this.url;
	}

	public void setUrl(@Nullable String url) {
		this.url = url;
	}

	/**
	 * Determine the url to use based on this configuration and the environment.
	 * @return the url to use
	 */
	public String determineUrl() {
		if (StringUtils.hasText(this.url)) {
			return this.url;
		}
		String databaseName = determineDatabaseName();
		String url = (databaseName != null) ? this.embeddedDatabaseConnection.getUrl(databaseName) : null;
		if (!StringUtils.hasText(url)) {
			throw new DataSourceBeanCreationException("Failed to determine suitable jdbc url", this,
					this.embeddedDatabaseConnection);
		}
		return url;
	}

	/**
	 * Determine the name to used based on this configuration.
	 * @return the database name to use or {@code null}
	 */
	public @Nullable String determineDatabaseName() {
		if (this.generateUniqueName) {
			if (this.uniqueName == null) {
				this.uniqueName = UUID.randomUUID().toString();
			}
			return this.uniqueName;
		}
		if (StringUtils.hasLength(this.name)) {
			return this.name;
		}
		if (this.embeddedDatabaseConnection != EmbeddedDatabaseConnection.NONE) {
			return "testdb";
		}
		return null;
	}

	/**
	 * Return the configured username or {@code null} if none was configured.
	 * @return the configured username
	 * @see #determineUsername()
	 */
	public @Nullable String getUsername() {
		return this.username;
	}

	public void setUsername(@Nullable String username) {
		this.username = username;
	}

	/**
	 * Determine the username to use based on this configuration and the environment.
	 * @return the username to use
	 */
	public @Nullable String determineUsername() {
		if (StringUtils.hasText(this.username)) {
			return this.username;
		}
		if (EmbeddedDatabaseConnection.isEmbedded(findDriverClassName(), determineUrl())) {
			return "sa";
		}
		return null;
	}

	/**
	 * Return the configured password or {@code null} if none was configured.
	 * @return the configured password
	 * @see #determinePassword()
	 */
	public @Nullable String getPassword() {
		return this.password;
	}

	public void setPassword(@Nullable String password) {
		this.password = password;
	}

	/**
	 * Determine the password to use based on this configuration and the environment.
	 * @return the password to use
	 */
	public @Nullable String determinePassword() {
		if (StringUtils.hasText(this.password)) {
			return this.password;
		}
		if (EmbeddedDatabaseConnection.isEmbedded(findDriverClassName(), determineUrl())) {
			return "";
		}
		return null;
	}

	public @Nullable String getJndiName() {
		return this.jndiName;
	}

	/**
	 * Allows the DataSource to be managed by the container and obtained through JNDI. The
	 * {@code URL}, {@code driverClassName}, {@code username} and {@code password} fields
	 * will be ignored when using JNDI lookups.
	 * @param jndiName the JNDI name
	 */
	public void setJndiName(@Nullable String jndiName) {
		this.jndiName = jndiName;
	}

	public EmbeddedDatabaseConnection getEmbeddedDatabaseConnection() {
		return this.embeddedDatabaseConnection;
	}

	public void setEmbeddedDatabaseConnection(EmbeddedDatabaseConnection embeddedDatabaseConnection) {
		this.embeddedDatabaseConnection = embeddedDatabaseConnection;
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
		private @Nullable String dataSourceClassName;

		/**
		 * Properties to pass to the XA data source.
		 */
		private Map<String, String> properties = new LinkedHashMap<>();

		public @Nullable String getDataSourceClassName() {
			return this.dataSourceClassName;
		}

		public void setDataSourceClassName(@Nullable String dataSourceClassName) {
			this.dataSourceClassName = dataSourceClassName;
		}

		public Map<String, String> getProperties() {
			return this.properties;
		}

		public void setProperties(Map<String, String> properties) {
			this.properties = properties;
		}

	}

	static class DataSourceBeanCreationException extends BeanCreationException {

		private final DataSourceProperties properties;

		private final EmbeddedDatabaseConnection connection;

		DataSourceBeanCreationException(String message, DataSourceProperties properties,
				EmbeddedDatabaseConnection connection) {
			super(message);
			this.properties = properties;
			this.connection = connection;
		}

		DataSourceProperties getProperties() {
			return this.properties;
		}

		EmbeddedDatabaseConnection getConnection() {
			return this.connection;
		}

	}

}
