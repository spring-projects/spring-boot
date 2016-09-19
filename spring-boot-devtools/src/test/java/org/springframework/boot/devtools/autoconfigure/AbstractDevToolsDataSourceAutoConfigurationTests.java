/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.devtools.autoconfigure;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;

import javax.sql.DataSource;

import org.junit.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Base class for tests for {@link DevToolsDataSourceAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
public abstract class AbstractDevToolsDataSourceAutoConfigurationTests {

	@Test
	public void singleManuallyConfiguredDataSourceIsNotClosed() throws SQLException {
		ConfigurableApplicationContext context = createContext(
				DataSourcePropertiesConfiguration.class,
				SingleDataSourceConfiguration.class);
		DataSource dataSource = context.getBean(DataSource.class);
		Statement statement = configureDataSourceBehaviour(dataSource);
		verify(statement, times(0)).execute("SHUTDOWN");
	}

	@Test
	public void multipleDataSourcesAreIgnored() throws SQLException {
		ConfigurableApplicationContext context = createContext(
				DataSourcePropertiesConfiguration.class,
				MultipleDataSourcesConfiguration.class);
		Collection<DataSource> dataSources = context.getBeansOfType(DataSource.class)
				.values();
		for (DataSource dataSource : dataSources) {
			Statement statement = configureDataSourceBehaviour(dataSource);
			verify(statement, times(0)).execute("SHUTDOWN");
		}
	}

	@Test
	public void emptyFactoryMethodMetadataIgnored() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		DataSource dataSource = mock(DataSource.class);
		AnnotatedGenericBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(
				dataSource.getClass());
		context.registerBeanDefinition("dataSource", beanDefinition);
		context.register(DataSourcePropertiesConfiguration.class);
		context.register(DevToolsDataSourceAutoConfiguration.class);
		context.refresh();
		context.close();
	}

	protected final Statement configureDataSourceBehaviour(DataSource dataSource)
			throws SQLException {
		Connection connection = mock(Connection.class);
		Statement statement = mock(Statement.class);
		doReturn(connection).when(dataSource).getConnection();
		given(connection.createStatement()).willReturn(statement);
		return statement;
	}

	protected final ConfigurableApplicationContext createContext(String driverClassName,
			Class<?>... classes) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(classes);
		context.register(DevToolsDataSourceAutoConfiguration.class);
		if (driverClassName != null) {
			EnvironmentTestUtils.addEnvironment(context,
					"spring.datasource.driver-class-name:" + driverClassName);
		}
		context.refresh();
		return context;
	}

	protected final ConfigurableApplicationContext createContext(Class<?>... classes) {
		return this.createContext(null, classes);
	}

	@Configuration
	static class SingleDataSourceConfiguration {

		@Bean
		public DataSource dataSource() {
			return mock(DataSource.class);
		}

	}

	@Configuration
	static class MultipleDataSourcesConfiguration {

		@Bean
		public DataSource dataSourceOne() {
			return mock(DataSource.class);
		}

		@Bean
		public DataSource dataSourceTwo() {
			return mock(DataSource.class);
		}

	}

	@Configuration
	@EnableConfigurationProperties(DataSourceProperties.class)
	static class DataSourcePropertiesConfiguration {

	}

	@Configuration
	static class DataSourceSpyConfiguration {

		@Bean
		public DataSourceSpyBeanPostProcessor dataSourceSpyBeanPostProcessor() {
			return new DataSourceSpyBeanPostProcessor();
		}

	}

	private static class DataSourceSpyBeanPostProcessor implements BeanPostProcessor {

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName)
				throws BeansException {
			if (bean instanceof DataSource) {
				bean = spy(bean);
			}
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName)
				throws BeansException {
			return bean;
		}

	}

}
