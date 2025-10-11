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

package org.springframework.boot.devtools.autoconfigure;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;

/**
 * Base class for tests for {@link DevToolsDataSourceAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
abstract class AbstractDevToolsDataSourceAutoConfigurationTests {

	@Test
	void singleManuallyConfiguredDataSourceIsNotClosed() throws Exception {
		try (ConfigurableApplicationContext context = getContext(
				() -> createContext(SingleDataSourceConfiguration.class))) {
			DataSource dataSource = context.getBean(DataSource.class);
			Statement statement = configureDataSourceBehavior(dataSource);
			then(statement).should(never()).execute("SHUTDOWN");
		}
	}

	@Test
	void multipleDataSourcesAreIgnored() throws Exception {
		try (ConfigurableApplicationContext context = getContext(
				() -> createContext(MultipleDataSourcesConfiguration.class))) {
			Collection<DataSource> dataSources = context.getBeansOfType(DataSource.class).values();
			for (DataSource dataSource : dataSources) {
				Statement statement = configureDataSourceBehavior(dataSource);
				then(statement).should(never()).execute("SHUTDOWN");
			}
		}
	}

	@Test
	void emptyFactoryMethodMetadataIgnored() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			DataSource dataSource = mock(DataSource.class);
			AnnotatedGenericBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(dataSource.getClass());
			context.registerBeanDefinition("dataSource", beanDefinition);
			context.register(DevToolsDataSourceAutoConfiguration.class);
			context.refresh();
		}
	}

	protected final Statement configureDataSourceBehavior(DataSource dataSource) throws SQLException {
		Connection connection = mock(Connection.class);
		Statement statement = mock(Statement.class);
		willReturn(connection).given(dataSource).getConnection();
		given(connection.createStatement()).willReturn(statement);
		return statement;
	}

	protected ConfigurableApplicationContext getContext(Supplier<ConfigurableApplicationContext> supplier)
			throws Exception {
		AtomicReference<ConfigurableApplicationContext> atomicReference = new AtomicReference<>();
		Thread thread = new Thread(() -> {
			ConfigurableApplicationContext context = supplier.get();
			atomicReference.getAndSet(context);
		});
		thread.start();
		thread.join();
		ConfigurableApplicationContext context = atomicReference.get();
		assertThat(context).isNotNull();
		return context;
	}

	protected final ConfigurableApplicationContext createContext(Class<?>... classes) {
		return createContext(null, classes);
	}

	protected final ConfigurableApplicationContext createContext(@Nullable String driverClassName,
			Class<?>... classes) {
		return createContext(driverClassName, null, classes);
	}

	protected final ConfigurableApplicationContext createContext(@Nullable String driverClassName, @Nullable String url,
			Class<?>... classes) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(classes);
		context.register(DevToolsDataSourceAutoConfiguration.class);
		if (driverClassName != null) {
			TestPropertyValues.of("spring.datasource.driver-class-name:" + driverClassName).applyTo(context);
		}
		if (url != null) {
			TestPropertyValues.of("spring.datasource.url:" + url).applyTo(context);
		}
		context.refresh();
		return context;
	}

	@Configuration(proxyBeanMethods = false)
	static class SingleDataSourceConfiguration {

		@Bean
		DataSource dataSource() {
			return mock(DataSource.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MultipleDataSourcesConfiguration {

		@Bean
		DataSource dataSourceOne() {
			return mock(DataSource.class);
		}

		@Bean
		DataSource dataSourceTwo() {
			return mock(DataSource.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DataSourceSpyConfiguration {

		@Bean
		static DataSourceSpyBeanPostProcessor dataSourceSpyBeanPostProcessor() {
			return new DataSourceSpyBeanPostProcessor();
		}

	}

	static class DataSourceSpyBeanPostProcessor implements BeanPostProcessor {

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			if (bean instanceof DataSource) {
				bean = spy(bean);
			}
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			return bean;
		}

	}

}
