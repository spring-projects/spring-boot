/*
 * Copyright 2012-2021 the original author or authors.
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

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for DataSource initialization.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
@Deprecated
class DataSourceInitializationIntegrationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.withPropertyValues("spring.datasource.initialization-mode=never",
					"spring.datasource.url:jdbc:hsqldb:mem:init-" + UUID.randomUUID());

	@Test
	void dataSourceInitialized() {
		this.contextRunner.withPropertyValues("spring.datasource.initialization-mode:always").run((context) -> {
			assertThat(context).hasSingleBean(DataSource.class);
			DataSource dataSource = context.getBean(DataSource.class);
			assertThat(dataSource).isInstanceOf(HikariDataSource.class);
			assertDataSourceIsInitialized(dataSource);
		});
	}

	@Test
	void initializationAppliesToCustomDataSource() {
		this.contextRunner.withUserConfiguration(OneDataSource.class)
				.withPropertyValues("spring.datasource.initialization-mode:always").run((context) -> {
					assertThat(context).hasSingleBean(DataSource.class);
					assertDataSourceIsInitialized(context.getBean(DataSource.class));
				});
	}

	@Test
	void initializationWithUsernameAndPasswordAppliesToCustomDataSource() {
		this.contextRunner.withUserConfiguration(OneDataSource.class)
				.withPropertyValues("spring.datasource.initialization-mode:always",
						"spring.datasource.schema-username=test", "spring.datasource.schema-password=secret")
				.run((context) -> {
					assertThat(context).hasSingleBean(DataSource.class);
					assertDataSourceIsInitialized(context.getBean(DataSource.class));
				});
	}

	private void assertDataSourceIsInitialized(DataSource dataSource) {
		JdbcOperations template = new JdbcTemplate(dataSource);
		assertThat(template.queryForObject("SELECT COUNT(*) from BAR", Integer.class)).isEqualTo(1);
	}

	@Test
	void dataSourceInitializedWithExplicitScript() {
		this.contextRunner.withPropertyValues("spring.datasource.initialization-mode:always",
				"spring.datasource.schema:" + getRelativeLocationFor("schema.sql"),
				"spring.datasource.data:" + getRelativeLocationFor("data.sql")).run((context) -> {
					DataSource dataSource = context.getBean(DataSource.class);
					assertThat(dataSource).isInstanceOf(HikariDataSource.class);
					assertThat(dataSource).isNotNull();
					JdbcOperations template = new JdbcTemplate(dataSource);
					assertThat(template.queryForObject("SELECT COUNT(*) from FOO", Integer.class)).isEqualTo(1);
				});
	}

	@Test
	void dataSourceInitializedWithMultipleScripts() {
		this.contextRunner.withPropertyValues("spring.datasource.initialization-mode:always",
				"spring.datasource.schema:" + getRelativeLocationFor("schema.sql") + ","
						+ getRelativeLocationFor("another.sql"),
				"spring.datasource.data:" + getRelativeLocationFor("data.sql")).run((context) -> {
					DataSource dataSource = context.getBean(DataSource.class);
					assertThat(dataSource).isInstanceOf(HikariDataSource.class);
					assertThat(dataSource).isNotNull();
					JdbcOperations template = new JdbcTemplate(dataSource);
					assertThat(template.queryForObject("SELECT COUNT(*) from FOO", Integer.class)).isEqualTo(1);
					assertThat(template.queryForObject("SELECT COUNT(*) from SPAM", Integer.class)).isEqualTo(0);
				});
	}

	@Test
	void dataSourceInitializedWithExplicitSqlScriptEncoding() {
		this.contextRunner.withPropertyValues("spring.datasource.initialization-mode:always",
				"spring.datasource.sqlScriptEncoding:UTF-8",
				"spring.datasource.schema:" + getRelativeLocationFor("encoding-schema.sql"),
				"spring.datasource.data:" + getRelativeLocationFor("encoding-data.sql")).run((context) -> {
					DataSource dataSource = context.getBean(DataSource.class);
					assertThat(dataSource).isInstanceOf(HikariDataSource.class);
					assertThat(dataSource).isNotNull();
					JdbcOperations template = new JdbcTemplate(dataSource);
					assertThat(template.queryForObject("SELECT COUNT(*) from BAR", Integer.class)).isEqualTo(2);
					assertThat(template.queryForObject("SELECT name from BAR WHERE id=1", String.class))
							.isEqualTo("bar");
					assertThat(template.queryForObject("SELECT name from BAR WHERE id=2", String.class))
							.isEqualTo("ばー");
				});
	}

	@Test
	void initializationDisabled() {
		this.contextRunner.run(assertInitializationIsDisabled());
	}

	@Test
	void initializationDoesNotApplyWithSeveralDataSources() {
		this.contextRunner.withUserConfiguration(TwoDataSources.class)
				.withPropertyValues("spring.datasource.initialization-mode:always").run((context) -> {
					assertThat(context.getBeanNamesForType(DataSource.class)).hasSize(2);
					assertDataSourceNotInitialized(context.getBean("oneDataSource", DataSource.class));
					assertDataSourceNotInitialized(context.getBean("twoDataSource", DataSource.class));
				});
	}

	private ContextConsumer<AssertableApplicationContext> assertInitializationIsDisabled() {
		return (context) -> {
			assertThat(context).hasSingleBean(DataSource.class);
			DataSource dataSource = context.getBean(DataSource.class);
			assertDataSourceNotInitialized(dataSource);
		};
	}

	private void assertDataSourceNotInitialized(DataSource dataSource) {
		JdbcOperations template = new JdbcTemplate(dataSource);
		assertThatExceptionOfType(BadSqlGrammarException.class)
				.isThrownBy(() -> template.queryForObject("SELECT COUNT(*) from BAR", Integer.class))
				.satisfies((ex) -> {
					SQLException sqlException = ex.getSQLException();
					int expectedCode = -5501; // user lacks privilege or object not found
					assertThat(sqlException.getErrorCode()).isEqualTo(expectedCode);
				});
	}

	@Test
	void dataSourceInitializedWithSchemaCredentials() {
		this.contextRunner
				.withPropertyValues("spring.datasource.initialization-mode:always",
						"spring.datasource.sqlScriptEncoding:UTF-8",
						"spring.datasource.schema:" + getRelativeLocationFor("encoding-schema.sql"),
						"spring.datasource.data:" + getRelativeLocationFor("encoding-data.sql"),
						"spring.datasource.schema-username:admin", "spring.datasource.schema-password:admin")
				.run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure()).isInstanceOf(BeanCreationException.class)
							.hasMessageContaining("invalid authorization specification");
					context.getStartupFailure().printStackTrace();
				});
	}

	@Test
	void dataSourceInitializedWithDataCredentials() {
		this.contextRunner
				.withPropertyValues("spring.datasource.initialization-mode:always",
						"spring.datasource.sqlScriptEncoding:UTF-8",
						"spring.datasource.schema:" + getRelativeLocationFor("encoding-schema.sql"),
						"spring.datasource.data:" + getRelativeLocationFor("encoding-data.sql"),
						"spring.datasource.data-username:admin", "spring.datasource.data-password:admin")
				.run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure()).isInstanceOf(BeanCreationException.class)
							.hasMessageContaining("invalid authorization specification");
				});
	}

	@Test
	void multipleScriptsAppliedInLexicalOrder() {
		new ApplicationContextRunner(() -> {
			AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
			context.setResourceLoader(new ReverseOrderResourceLoader(new DefaultResourceLoader()));
			return context;
		}).withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
				.withPropertyValues("spring.datasource.initialization-mode=always",
						"spring.datasource.url:jdbc:hsqldb:mem:testdb-" + new Random().nextInt(),
						"spring.datasource.schema:classpath*:" + getRelativeLocationFor("lexical-schema-*.sql"),
						"spring.datasource.data:classpath*:" + getRelativeLocationFor("data.sql"))
				.run((context) -> {
					DataSource dataSource = context.getBean(DataSource.class);
					assertThat(dataSource).isInstanceOf(HikariDataSource.class);
					assertThat(dataSource).isNotNull();
					JdbcOperations template = new JdbcTemplate(dataSource);
					assertThat(template.queryForObject("SELECT COUNT(*) from FOO", Integer.class)).isEqualTo(1);
				});
	}

	@Test
	void testDataSourceInitializedWithInvalidSchemaResource() {
		this.contextRunner.withPropertyValues("spring.datasource.initialization-mode:always",
				"spring.datasource.schema:classpath:does/not/exist.sql").run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure()).isInstanceOf(BeanCreationException.class);
					assertThat(context.getStartupFailure())
							.hasMessageContaining("No schema scripts found at location 'classpath:does/not/exist.sql'");
				});
	}

	@Test
	void dataSourceInitializedWithInvalidDataResource() {
		this.contextRunner.withPropertyValues("spring.datasource.initialization-mode:always",
				"spring.datasource.schema:" + getRelativeLocationFor("schema.sql"),
				"spring.datasource.data:classpath:does/not/exist.sql").run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure()).isInstanceOf(BeanCreationException.class);
					assertThat(context.getStartupFailure())
							.hasMessageContaining("No data scripts found at location 'classpath:does/not/exist.sql'");
				});
	}

	@Test
	void whenDataSourceIsProxiedByABeanPostProcessorThenDataSourceInitializationUsesTheProxy() {
		this.contextRunner.withPropertyValues("spring.datasource.initialization-mode:always")
				.withUserConfiguration(DataSourceProxyConfiguration.class).run((context) -> {
					assertThat(context).hasSingleBean(DataSource.class);
					DataSource dataSource = context.getBean(DataSource.class);
					assertThat(dataSource).isInstanceOf(DataSourceProxy.class);
					assertThat(((DataSourceProxy) dataSource).connectionsRetrieved).hasPositiveValue();
					assertDataSourceIsInitialized(dataSource);
				});
	}

	@Test
	// gh-13042
	void whenDataSourceIsScopedAndJpaIsInvolvedThenInitializationCompletesSuccessfully() {
		this.contextRunner.withPropertyValues("spring.datasource.initialization-mode:always")
				.withConfiguration(AutoConfigurations.of(HibernateJpaAutoConfiguration.class))
				.withUserConfiguration(ScopedDataSourceConfiguration.class).run((context) -> {
					assertThat(context).hasSingleBean(DataSource.class);
					DataSource dataSource = context.getBean(DataSource.class);
					assertThat(dataSource).isInstanceOf(HikariDataSource.class);
					assertDataSourceIsInitialized(dataSource);
				});
	}

	private String getRelativeLocationFor(String resource) {
		return ClassUtils.addResourcePathToPackagePath(getClass(), resource);
	}

	@Configuration(proxyBeanMethods = false)
	static class OneDataSource {

		@Bean
		DataSource oneDataSource() {
			return new TestDataSource(true);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TwoDataSources extends OneDataSource {

		@Bean
		DataSource twoDataSource() {
			return new TestDataSource(true);
		}

	}

	/**
	 * {@link ResourcePatternResolver} used to ensure consistently wrong resource
	 * ordering.
	 */
	static class ReverseOrderResourceLoader implements ResourcePatternResolver {

		private final ResourcePatternResolver resolver;

		ReverseOrderResourceLoader(ResourceLoader loader) {
			this.resolver = ResourcePatternUtils.getResourcePatternResolver(loader);
		}

		@Override
		public Resource getResource(String location) {
			return this.resolver.getResource(location);
		}

		@Override
		public ClassLoader getClassLoader() {
			return this.resolver.getClassLoader();
		}

		@Override
		public Resource[] getResources(String locationPattern) throws IOException {
			Resource[] resources = this.resolver.getResources(locationPattern);
			Arrays.sort(resources, Comparator.comparing(Resource::getFilename).reversed());
			return resources;
		}

	}

	@Configuration(proxyBeanMethods = true)
	static class DataSourceProxyConfiguration {

		@Bean
		static BeanPostProcessor dataSourceProxy() {
			return new BeanPostProcessor() {

				@Override
				public Object postProcessAfterInitialization(Object bean, String beanName) {
					if (bean instanceof DataSource) {
						return new DataSourceProxy((DataSource) bean);
					}
					return bean;
				}

			};
		}

	}

	static class DataSourceProxy implements DataSource {

		private final AtomicInteger connectionsRetrieved = new AtomicInteger();

		private final DataSource delegate;

		DataSourceProxy(DataSource delegate) {
			this.delegate = delegate;
		}

		@Override
		public PrintWriter getLogWriter() throws SQLException {
			return this.delegate.getLogWriter();
		}

		@Override
		public void setLogWriter(PrintWriter out) throws SQLException {
			this.delegate.setLogWriter(out);
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return this.delegate.isWrapperFor(iface);
		}

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException {
			return this.delegate.unwrap(iface);
		}

		@Override
		public Connection getConnection() throws SQLException {
			this.connectionsRetrieved.incrementAndGet();
			return this.delegate.getConnection();
		}

		@Override
		public Connection getConnection(String username, String password) throws SQLException {
			this.connectionsRetrieved.incrementAndGet();
			return this.delegate.getConnection(username, password);
		}

		@Override
		public int getLoginTimeout() throws SQLException {
			return this.delegate.getLoginTimeout();
		}

		@Override
		public void setLoginTimeout(int seconds) throws SQLException {
			this.delegate.setLoginTimeout(seconds);
		}

		@Override
		public Logger getParentLogger() throws SQLFeatureNotSupportedException {
			return this.delegate.getParentLogger();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ScopedDataSourceConfiguration {

		@Bean
		static BeanFactoryPostProcessor fooScope() {
			return (beanFactory) -> beanFactory.registerScope("test", new SimpleThreadScope());
		}

		@Bean
		@Scope("test")
		HikariDataSource dataSource(DataSourceProperties properties) {
			return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
		}

	}

}
