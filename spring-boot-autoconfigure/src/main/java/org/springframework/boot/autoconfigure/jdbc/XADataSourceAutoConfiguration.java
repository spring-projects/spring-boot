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

import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jta.JtaAutoConfiguration;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jta.XADataSourceWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link DataSource} with XA.
 *
 * @author Phillip Webb
 * @author Josh Long
 * @since 1.2.0
 */
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@AutoConfigureAfter(JtaAutoConfiguration.class)
@EnableConfigurationProperties(DataSourceProperties.class)
@ConditionalOnClass({ DataSource.class, TransactionManager.class })
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
	@ConfigurationProperties(prefix = DataSourceProperties.PREFIX)
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
			className = DatabaseDriver.fromJdbcUrl(this.properties.getUrl())
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
			Object instance = BeanUtils.instantiate(dataSourceClass);
			Assert.isInstanceOf(XADataSource.class, instance);
			return (XADataSource) instance;
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Unable to create XADataSource instance from '" + className + "'");
		}
	}

	private void bindXaProperties(XADataSource target, DataSourceProperties properties) {
		MutablePropertyValues values = new MutablePropertyValues();
		values.add("user", this.properties.getUsername());
		values.add("password", this.properties.getPassword());
		values.add("url", this.properties.getUrl());
		values.addPropertyValues(properties.getXa().getProperties());
		new RelaxedDataBinder(target).withAlias("user", "username").bind(values);
	}

}
