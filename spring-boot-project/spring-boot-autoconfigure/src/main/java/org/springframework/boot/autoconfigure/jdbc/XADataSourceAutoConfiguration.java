/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.jdbc;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import jakarta.transaction.TransactionManager;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties.DataSourceBeanCreationException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertyNameAliases;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.jdbc.XADataSourceWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link DataSource} with XA.
 *
 * @author Phillip Webb
 * @author Josh Long
 * @author Madhura Bhave
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 1.2.0
 */
@AutoConfiguration(before = DataSourceAutoConfiguration.class)
@EnableConfigurationProperties(DataSourceProperties.class)
@ConditionalOnClass({ DataSource.class, TransactionManager.class, EmbeddedDatabaseType.class })
@ConditionalOnBean(XADataSourceWrapper.class)
@ConditionalOnMissingBean(DataSource.class)
public class XADataSourceAutoConfiguration implements BeanClassLoaderAware {

	private ClassLoader classLoader;

	/**
	 * Creates a new instance of {@link PropertiesJdbcConnectionDetails} if no bean of
	 * type {@link JdbcConnectionDetails} is present.
	 * @param properties the {@link DataSourceProperties} used to configure the JDBC
	 * connection details
	 * @return a new instance of {@link PropertiesJdbcConnectionDetails}
	 */
	@Bean
	@ConditionalOnMissingBean(JdbcConnectionDetails.class)
	PropertiesJdbcConnectionDetails jdbcConnectionDetails(DataSourceProperties properties) {
		return new PropertiesJdbcConnectionDetails(properties);
	}

	/**
	 * Creates and configures a DataSource bean.
	 * @param wrapper the XADataSourceWrapper used to wrap the DataSource
	 * @param properties the DataSourceProperties used to configure the DataSource
	 * @param connectionDetails the JdbcConnectionDetails used to configure the DataSource
	 * @param xaDataSource the XADataSource used to create the DataSource (optional)
	 * @return the configured DataSource bean
	 * @throws Exception if an error occurs during DataSource creation or configuration
	 */
	@Bean
	public DataSource dataSource(XADataSourceWrapper wrapper, DataSourceProperties properties,
			JdbcConnectionDetails connectionDetails, ObjectProvider<XADataSource> xaDataSource) throws Exception {
		return wrapper
			.wrapDataSource(xaDataSource.getIfAvailable(() -> createXaDataSource(properties, connectionDetails)));
	}

	/**
	 * Set the class loader to be used for loading beans.
	 * @param classLoader the class loader to be used for loading beans
	 */
	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * Creates an XADataSource based on the provided DataSourceProperties and
	 * JdbcConnectionDetails.
	 * @param properties the DataSourceProperties containing the necessary configuration
	 * properties
	 * @param connectionDetails the JdbcConnectionDetails containing the connection
	 * details
	 * @return the created XADataSource
	 * @throws IllegalStateException if no XA DataSource class name is specified
	 */
	private XADataSource createXaDataSource(DataSourceProperties properties, JdbcConnectionDetails connectionDetails) {
		String className = connectionDetails.getXaDataSourceClassName();
		Assert.state(StringUtils.hasLength(className), "No XA DataSource class name specified");
		XADataSource dataSource = createXaDataSourceInstance(className);
		bindXaProperties(dataSource, properties, connectionDetails);
		return dataSource;
	}

	/**
	 * Creates an instance of XADataSource based on the provided class name.
	 * @param className the fully qualified class name of the XADataSource implementation
	 * @return the created XADataSource instance
	 * @throws IllegalStateException if unable to create the XADataSource instance
	 */
	private XADataSource createXaDataSourceInstance(String className) {
		try {
			Class<?> dataSourceClass = ClassUtils.forName(className, this.classLoader);
			Object instance = BeanUtils.instantiateClass(dataSourceClass);
			Assert.isInstanceOf(XADataSource.class, instance);
			return (XADataSource) instance;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to create XADataSource instance from '" + className + "'");
		}
	}

	/**
	 * Binds the XA properties to the target XADataSource using the provided
	 * DataSourceProperties and JdbcConnectionDetails.
	 * @param target the XADataSource to bind the properties to
	 * @param dataSourceProperties the DataSourceProperties containing the properties to
	 * bind
	 * @param connectionDetails the JdbcConnectionDetails containing the connection
	 * details to bind
	 */
	private void bindXaProperties(XADataSource target, DataSourceProperties dataSourceProperties,
			JdbcConnectionDetails connectionDetails) {
		Binder binder = new Binder(getBinderSource(dataSourceProperties, connectionDetails));
		binder.bind(ConfigurationPropertyName.EMPTY, Bindable.ofInstance(target));
	}

	/**
	 * Returns a ConfigurationPropertySource for the binder, using the provided
	 * DataSourceProperties and JdbcConnectionDetails. The properties map is populated
	 * with the XA properties from the DataSourceProperties, and the username and password
	 * from the JdbcConnectionDetails. If a URL is present in the JdbcConnectionDetails,
	 * it is also added to the properties map.
	 * @param dataSourceProperties the DataSourceProperties containing the XA properties
	 * @param connectionDetails the JdbcConnectionDetails containing the username,
	 * password, and optional URL
	 * @return the ConfigurationPropertySource for the binder
	 */
	private ConfigurationPropertySource getBinderSource(DataSourceProperties dataSourceProperties,
			JdbcConnectionDetails connectionDetails) {
		Map<Object, Object> properties = new HashMap<>(dataSourceProperties.getXa().getProperties());
		properties.computeIfAbsent("user", (key) -> connectionDetails.getUsername());
		properties.computeIfAbsent("password", (key) -> connectionDetails.getPassword());
		try {
			properties.computeIfAbsent("url", (key) -> connectionDetails.getJdbcUrl());
		}
		catch (DataSourceBeanCreationException ex) {
			// Continue as not all XA DataSource's require a URL
		}
		MapConfigurationPropertySource source = new MapConfigurationPropertySource(properties);
		ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases();
		aliases.addAliases("user", "username");
		return source.withAliases(aliases);
	}

}
