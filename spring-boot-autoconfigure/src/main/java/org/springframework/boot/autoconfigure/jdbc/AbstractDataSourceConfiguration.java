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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Base class for configuration of a database pool.
 * 
 * @author Dave Syer
 */
@ConfigurationProperties(prefix = DataSourceAutoConfiguration.CONFIGURATION_PREFIX)
@EnableConfigurationProperties
public abstract class AbstractDataSourceConfiguration implements BeanClassLoaderAware,
		InitializingBean {

	private String driverClassName;

	private String url;

	private String username;

	private String password;

	private int maxActive = 100;

	private int maxIdle = 8;

	private int minIdle = 8;

	private int initialSize = 10;

	private String validationQuery;

	private boolean testOnBorrow;

	private boolean testOnReturn;

	private boolean testWhileIdle;

	private Integer timeBetweenEvictionRunsMillis;

	private Integer minEvictableIdleTimeMillis;

	private Integer maxWaitMillis;

	private ClassLoader classLoader;

	private EmbeddedDatabaseConnection embeddedDatabaseConnection = EmbeddedDatabaseConnection.NONE;

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
			return this.driverClassName;
		}
		String driverClassName = this.embeddedDatabaseConnection.getDriverClassName();
		if (!StringUtils.hasText(driverClassName)) {
			throw new BeanCreationException(
					"Cannot determine embedded database driver class for database type "
							+ this.embeddedDatabaseConnection
							+ ". If you want an embedded "
							+ "database please put a supported one on the classpath.");
		}
		return driverClassName;
	}

	protected String getUrl() {
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

	protected String getUsername() {
		if (StringUtils.hasText(this.username)) {
			return this.username;
		}
		if (EmbeddedDatabaseConnection.isEmbedded(this.driverClassName)) {
			return "sa";
		}
		return null;
	}

	protected String getPassword() {
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

	public void setInitialSize(int initialSize) {
		this.initialSize = initialSize;
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

	public void setMaxActive(int maxActive) {
		this.maxActive = maxActive;
	}

	public void setMaxIdle(int maxIdle) {
		this.maxIdle = maxIdle;
	}

	public void setMinIdle(int minIdle) {
		this.minIdle = minIdle;
	}

	public void setValidationQuery(String validationQuery) {
		this.validationQuery = validationQuery;
	}

	public void setTestOnBorrow(boolean testOnBorrow) {
		this.testOnBorrow = testOnBorrow;
	}

	public void setTestOnReturn(boolean testOnReturn) {
		this.testOnReturn = testOnReturn;
	}

	public void setTestWhileIdle(boolean testWhileIdle) {
		this.testWhileIdle = testWhileIdle;
	}

	public void setTimeBetweenEvictionRunsMillis(int timeBetweenEvictionRunsMillis) {
		this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
	}

	public void setMinEvictableIdleTimeMillis(int minEvictableIdleTimeMillis) {
		this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
	}

	public void setMaxWait(int maxWaitMillis) {
		this.maxWaitMillis = maxWaitMillis;
	}

	public int getInitialSize() {
		return this.initialSize;
	}

	protected int getMaxActive() {
		return this.maxActive;
	}

	protected int getMaxIdle() {
		return this.maxIdle;
	}

	protected int getMinIdle() {
		return this.minIdle;
	}

	protected String getValidationQuery() {
		return this.validationQuery;
	}

	protected boolean isTestOnBorrow() {
		return this.testOnBorrow;
	}

	protected boolean isTestOnReturn() {
		return this.testOnReturn;
	}

	protected boolean isTestWhileIdle() {
		return this.testWhileIdle;
	}

	protected Integer getTimeBetweenEvictionRunsMillis() {
		return this.timeBetweenEvictionRunsMillis;
	}

	protected Integer getMinEvictableIdleTimeMillis() {
		return this.minEvictableIdleTimeMillis;
	}

	protected Integer getMaxWaitMillis() {
		return this.maxWaitMillis;
	}

}
