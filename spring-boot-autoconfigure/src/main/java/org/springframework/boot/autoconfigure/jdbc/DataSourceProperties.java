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

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Base class for configuration of a database pool.
 * 
 * @author Dave Syer
 * @author Maciej Walkowiak
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = DataSourceAutoConfiguration.CONFIGURATION_PREFIX)
public class DataSourceProperties implements BeanClassLoaderAware, InitializingBean {

	private String driverClassName;

	private String url;

	private String username;

	private String password;

	private ClassLoader classLoader;

	private boolean initialize = true;

	private String platform = "all";

	private String schema;

	private String data;

	private boolean continueOnError = false;

	private String separator = ";";

	private String sqlScriptEncoding;

	private EmbeddedDatabaseConnection embeddedDatabaseConnection = EmbeddedDatabaseConnection.NONE;

	private DriverClassNameProvider driverClassNameProvider = new DriverClassNameProvider();

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
			driverClassName = this.driverClassNameProvider.getDriverClassName(this.url);
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
							+ "database please put a supported on on the classpath.");
		}
		return url;
	}

	public String getUsername() {
		if (StringUtils.hasText(this.username)) {
			return this.username;
		}
		if (EmbeddedDatabaseConnection.isEmbedded(this.driverClassName)) {
			return "sa";
		}
		return null;
	}

	public String getPassword() {
		if (StringUtils.hasText(this.password)) {
			return this.password;
		}
		if (EmbeddedDatabaseConnection.isEmbedded(this.driverClassName)) {
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
		return sqlScriptEncoding;
	}

	public void setSqlScriptEncoding(String sqlScriptEncoding) {
		this.sqlScriptEncoding = sqlScriptEncoding;
	}

	public ClassLoader getClassLoader() {
		return this.classLoader;
	}
}
