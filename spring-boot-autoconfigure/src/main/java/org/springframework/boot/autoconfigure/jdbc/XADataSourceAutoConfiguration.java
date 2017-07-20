/*
 * Copyright 2012-2017 the original author or authors.
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

import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertyNameAliases;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.jta.XADataSourceWrapper;
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
 * @since 1.2.0
 */
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@EnableConfigurationProperties(DataSourceProperties.class)
@ConditionalOnClass({ DataSource.class, TransactionManager.class,
		EmbeddedDatabaseType.class })
@ConditionalOnBean(XADataSourceWrapper.class)
@ConditionalOnMissingBean(DataSource.class)
public class XADataSourceAutoConfiguration implements BeanClassLoaderAware {

	@Autowired
	private XADataSourceWrapper wrapper;

	@Autowired
	private DataSourceProperties properties;

	@Autowired(required = false)
	private XADataSource xaDataSource;

	private ClassLoader classLoader;

	@Bean
	public DataSource dataSource() throws Exception {
		XADataSource xaDataSource = this.xaDataSource;
		if (xaDataSource == null) {
			xaDataSource = createXaDataSource();
		}
		return this.wrapper.wrapDataSource(xaDataSource);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	private XADataSource createXaDataSource() {
		String className = this.properties.getXa().getDataSourceClassName();
		if (!StringUtils.hasLength(className)) {
			className = DatabaseDriver.fromJdbcUrl(this.properties.determineUrl())
					.getXaDataSourceClassName();
		}
		Assert.state(StringUtils.hasLength(className),
				"No XA DataSource class name specified");
		XADataSource dataSource = createXaDataSourceInstance(className);
		bindXaProperties(dataSource, this.properties);
		return dataSource;
	}

	private XADataSource createXaDataSourceInstance(String className) {
		try {
			Class<?> dataSourceClass = ClassUtils.forName(className, this.classLoader);
			Object instance = BeanUtils.instantiateClass(dataSourceClass);
			Assert.isInstanceOf(XADataSource.class, instance);
			return (XADataSource) instance;
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Unable to create XADataSource instance from '" + className + "'");
		}
	}

	private void bindXaProperties(XADataSource target,
			DataSourceProperties dataSourceProperties) {
		Binder binder = new Binder(getBinderSource(dataSourceProperties));
		binder.bind(ConfigurationPropertyName.EMPTY, Bindable.ofInstance(target));
	}

	private ConfigurationPropertySource getBinderSource(
			DataSourceProperties dataSourceProperties) {
		MapConfigurationPropertySource source = new MapConfigurationPropertySource();
		source.put("user", this.properties.determineUsername());
		source.put("password", this.properties.determinePassword());
		source.put("url", this.properties.determineUrl());
		source.putAll(dataSourceProperties.getXa().getProperties());
		ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases();
		aliases.addAliases("user", "username");
		return source.withAliases(aliases);
	}

}
