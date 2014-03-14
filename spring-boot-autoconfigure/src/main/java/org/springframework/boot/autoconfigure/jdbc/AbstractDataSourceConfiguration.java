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

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.HashSet;
import java.util.Set;

/**
 * Base class for configuration of a database pool.
 *
 * @author Dave Syer
 */
@ConfigurationProperties(name = DataSourceAutoConfiguration.CONFIGURATION_PREFIX)
@EnableConfigurationProperties
public abstract class AbstractDataSourceConfiguration implements BeanClassLoaderAware,
		InitializingBean, ApplicationContextAware {

	public static final String DEFAULT_TRANSACTION_ISOLATION_PROPERTY = "defaultTransactionIsolation";
	public static final String URL_PROPERTY = "url";

	private ApplicationContext applicationContext;

	private String driverClassName;

	private String username;

	private String password;

	private ClassLoader classLoader;

	private EmbeddedDatabaseConnection embeddedDatabaseConnection = EmbeddedDatabaseConnection.NONE;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.embeddedDatabaseConnection = EmbeddedDatabaseConnection.get(this.classLoader);
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

	public void setDriverClassName(String driverClassName) { this.driverClassName = driverClassName; }

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	protected Integer getDefaultTransactionIsolationLevel(String tx) {
		if ("NONE".equals(tx)) {
			return Connection.TRANSACTION_NONE;
		}
		if ("READ_COMMITTED".equals(tx)) {
			return Connection.TRANSACTION_READ_COMMITTED;
		}
		if ("READ_UNCOMMITTED".equals(tx)) {
			return Connection.TRANSACTION_READ_UNCOMMITTED;
		}
		if ("REPEATABLE_READ".equals(tx)) {
			return Connection.TRANSACTION_REPEATABLE_READ;
		}
		if ("SERIALIZABLE".equals(tx)) {
			return Connection.TRANSACTION_SERIALIZABLE;
		}
		throw new IllegalArgumentException("Unsupported transaction isolation level: " + tx);
	}

	protected void configurePoolUsingJavaBeanProperties(Object pool) throws InvocationTargetException, IllegalAccessException {
		PropertyDescriptor[] propertyDescriptors = BeanUtils.getPropertyDescriptors(pool.getClass());
		Set<String> ignoredProperties = new HashSet<String>();
		ignoredProperties.add("username");
		ignoredProperties.add("password");
		ignoredProperties.add("driverClassName");
		for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
			if (ignoredProperties.contains(propertyDescriptor.getName())) {
				continue;
			}
			configureProperty(pool, propertyDescriptor);
		}
	}

	private void configureProperty(Object pool, PropertyDescriptor propertyDescriptor) throws IllegalAccessException, InvocationTargetException {
		String property = propertyDescriptor.getName();
		Method writeMethod = propertyDescriptor.getWriteMethod();
		Object propertyValue = null;
		if (DEFAULT_TRANSACTION_ISOLATION_PROPERTY.equals(propertyDescriptor.getName())) {
			String txIsolationLevel = (String) getDataSourcePropertyValue(DEFAULT_TRANSACTION_ISOLATION_PROPERTY, String.class);
			if (txIsolationLevel != null) {
				propertyValue = getDefaultTransactionIsolationLevel(txIsolationLevel);
			}
		} else if (URL_PROPERTY.equals(property)) {
			propertyValue = getUrlProperty(property);
		} else {
			propertyValue = getDataSourcePropertyValue(property, propertyDescriptor.getPropertyType());
		}
		if (propertyValue != null) {
			writeMethod.invoke(pool, propertyValue);
		}
	}

	private String getUrlProperty(String property) {
		String url = (String) getDataSourcePropertyValue(property, String.class);
		if (!StringUtils.hasText(url)) {
			url = this.embeddedDatabaseConnection.getUrl();
			if (!StringUtils.hasText(url)) {
				throw new BeanCreationException(
					"Cannot determine embedded database url for database type "
							+ this.embeddedDatabaseConnection
							+ ". If you want an embedded "
							+ "database please put a supported on on the classpath.");
			}
		}
		return url;
	}

	private Object getDataSourcePropertyValue(String property, Class<?> propertyEditorClass) {
		String finalName = DataSourceAutoConfiguration.CONFIGURATION_PREFIX + "." + property;
		return applicationContext.getEnvironment().getProperty(finalName, propertyEditorClass);
	}
}
