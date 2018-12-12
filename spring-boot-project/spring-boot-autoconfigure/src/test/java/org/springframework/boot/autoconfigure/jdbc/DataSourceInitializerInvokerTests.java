/*
 * Copyright 2012-2018 the original author or authors.
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

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.UUID;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import static org.junit.Assert.fail;

/**
 * Tests for {@link DataSourceInitializerInvoker}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
public class DataSourceInitializerInvokerTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.withPropertyValues("spring.datasource.initialization-mode=never",
					"spring.datasource.url:jdbc:hsqldb:mem:init-" + UUID.randomUUID());

	@Test
	public void dataSourceInitialized() {
		this.contextRunner
				.withPropertyValues("spring.datasource.initialization-mode:always")
				.run((context) -> {
					assertThat(context).hasSingleBean(DataSource.class);
					DataSource dataSource = context.getBean(DataSource.class);
					assertThat(dataSource).isInstanceOf(HikariDataSource.class);
					assertDataSourceIsInitialized(dataSource);
				});
	}

	@Test
	public void initializationAppliesToCustomDataSource() {
		this.contextRunner.withUserConfiguration(OneDataSource.class)
				.withPropertyValues("spring.datasource.initialization-mode:always")
				.run((context) -> {
					assertThat(context).hasSingleBean(DataSource.class);
					assertDataSourceIsInitialized(context.getBean(DataSource.class));
				});
	}

	private void assertDataSourceIsInitialized(DataSource dataSource) {
		JdbcOperations template = new JdbcTemplate(dataSource);
		assertThat(template.queryForObject("SELECT COUNT(*) from BAR", Integer.class))
				.isEqualTo(1);
	}

	@Test
	public void dataSourceInitializedWithExplicitScript() {
		this.contextRunner
				.withPropertyValues("spring.datasource.initialization-mode:always",
						"spring.datasource.schema:"
								+ getRelativeLocationFor("schema.sql"),
						"spring.datasource.data:" + getRelativeLocationFor("data.sql"))
				.run((context) -> {
					DataSource dataSource = context.getBean(DataSource.class);
					assertThat(dataSource).isInstanceOf(HikariDataSource.class);
					assertThat(dataSource).isNotNull();
					JdbcOperations template = new JdbcTemplate(dataSource);
					assertThat(template.queryForObject("SELECT COUNT(*) from FOO",
							Integer.class)).isEqualTo(1);
				});
	}

	@Test
	public void dataSourceInitializedWithMultipleScripts() {
		this.contextRunner
				.withPropertyValues("spring.datasource.initialization-mode:always",
						"spring.datasource.schema:" + getRelativeLocationFor("schema.sql")
								+ "," + getRelativeLocationFor("another.sql"),
						"spring.datasource.data:" + getRelativeLocationFor("data.sql"))
				.run((context) -> {
					DataSource dataSource = context.getBean(DataSource.class);
					assertThat(dataSource).isInstanceOf(HikariDataSource.class);
					assertThat(dataSource).isNotNull();
					JdbcOperations template = new JdbcTemplate(dataSource);
					assertThat(template.queryForObject("SELECT COUNT(*) from FOO",
							Integer.class)).isEqualTo(1);
					assertThat(template.queryForObject("SELECT COUNT(*) from SPAM",
							Integer.class)).isEqualTo(0);
				});
	}

	@Test
	public void dataSourceInitializedWithExplicitSqlScriptEncoding() {
		this.contextRunner
				.withPropertyValues("spring.datasource.initialization-mode:always",
						"spring.datasource.sqlScriptEncoding:UTF-8",
						"spring.datasource.schema:"
								+ getRelativeLocationFor("encoding-schema.sql"),
						"spring.datasource.data:"
								+ getRelativeLocationFor("encoding-data.sql"))
				.run((context) -> {
					DataSource dataSource = context.getBean(DataSource.class);
					assertThat(dataSource).isInstanceOf(HikariDataSource.class);
					assertThat(dataSource).isNotNull();
					JdbcOperations template = new JdbcTemplate(dataSource);
					assertThat(template.queryForObject("SELECT COUNT(*) from BAR",
							Integer.class)).isEqualTo(2);
					assertThat(template.queryForObject("SELECT name from BAR WHERE id=1",
							String.class)).isEqualTo("bar");
					assertThat(template.queryForObject("SELECT name from BAR WHERE id=2",
							String.class)).isEqualTo("ばー");
				});
	}

	@Test
	public void initializationDisabled() {
		this.contextRunner.run(assertInitializationIsDisabled());
	}

	@Test
	public void initializationDoesNotApplyWithSeveralDataSources() {
		this.contextRunner.withUserConfiguration(TwoDataSources.class)
				.withPropertyValues("spring.datasource.initialization-mode:always")
				.run((context) -> {
					assertThat(context.getBeanNamesForType(DataSource.class)).hasSize(2);
					assertDataSourceNotInitialized(
							context.getBean("oneDataSource", DataSource.class));
					assertDataSourceNotInitialized(
							context.getBean("twoDataSource", DataSource.class));
				});
	}

	private ContextConsumer<AssertableApplicationContext> assertInitializationIsDisabled() {
		return (context) -> {
			assertThat(context).hasSingleBean(DataSource.class);
			DataSource dataSource = context.getBean(DataSource.class);
			context.publishEvent(new DataSourceSchemaCreatedEvent(dataSource));
			assertDataSourceNotInitialized(dataSource);
		};
	}

	private void assertDataSourceNotInitialized(DataSource dataSource) {
		JdbcOperations template = new JdbcTemplate(dataSource);
		try {
			template.queryForObject("SELECT COUNT(*) from BAR", Integer.class);
			fail("Query should have failed as BAR table does not exist");
		}
		catch (BadSqlGrammarException ex) {
			SQLException sqlException = ex.getSQLException();
			int expectedCode = -5501; // user lacks privilege or object not
			// found
			assertThat(sqlException.getErrorCode()).isEqualTo(expectedCode);
		}
	}

	@Test
	public void dataSourceInitializedWithSchemaCredentials() {
		this.contextRunner.withPropertyValues(
				"spring.datasource.initialization-mode:always",
				"spring.datasource.sqlScriptEncoding:UTF-8",
				"spring.datasource.schema:"
						+ getRelativeLocationFor("encoding-schema.sql"),
				"spring.datasource.data:" + getRelativeLocationFor("encoding-data.sql"),
				"spring.datasource.schema-username:admin",
				"spring.datasource.schema-password:admin").run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure())
							.isInstanceOf(BeanCreationException.class);
				});
	}

	@Test
	public void dataSourceInitializedWithDataCredentials() {
		this.contextRunner.withPropertyValues(
				"spring.datasource.initialization-mode:always",
				"spring.datasource.sqlScriptEncoding:UTF-8",
				"spring.datasource.schema:"
						+ getRelativeLocationFor("encoding-schema.sql"),
				"spring.datasource.data:" + getRelativeLocationFor("encoding-data.sql"),
				"spring.datasource.data-username:admin",
				"spring.datasource.data-password:admin").run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure())
							.isInstanceOf(BeanCreationException.class);
				});
	}

	@Test
	public void multipleScriptsAppliedInLexicalOrder() {
		new ApplicationContextRunner(() -> {
			AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
			context.setResourceLoader(
					new ReverseOrderResourceLoader(new DefaultResourceLoader()));
			return context;
		}).withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
				.withPropertyValues("spring.datasource.initialization-mode=always",
						"spring.datasource.url:jdbc:hsqldb:mem:testdb-"
								+ new Random().nextInt(),
						"spring.datasource.schema:"
								+ getRelativeLocationFor("lexical-schema-*.sql"),
						"spring.datasource.data:" + getRelativeLocationFor("data.sql"))
				.run((context) -> {
					DataSource dataSource = context.getBean(DataSource.class);
					assertThat(dataSource).isInstanceOf(HikariDataSource.class);
					assertThat(dataSource).isNotNull();
					JdbcOperations template = new JdbcTemplate(dataSource);
					assertThat(template.queryForObject("SELECT COUNT(*) from FOO",
							Integer.class)).isEqualTo(1);
				});
	}

	@Test
	public void testDataSourceInitializedWithInvalidSchemaResource() {
		this.contextRunner
				.withPropertyValues("spring.datasource.initialization-mode:always",
						"spring.datasource.schema:classpath:does/not/exist.sql")
				.run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure())
							.isInstanceOf(BeanCreationException.class);
					assertThat(context.getStartupFailure())
							.hasMessageContaining("does/not/exist.sql");
					assertThat(context.getStartupFailure())
							.hasMessageContaining("spring.datasource.schema");
				});
	}

	@Test
	public void dataSourceInitializedWithInvalidDataResource() {
		this.contextRunner
				.withPropertyValues("spring.datasource.initialization-mode:always",
						"spring.datasource.schema:"
								+ getRelativeLocationFor("schema.sql"),
						"spring.datasource.data:classpath:does/not/exist.sql")
				.run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure())
							.isInstanceOf(BeanCreationException.class);
					assertThat(context.getStartupFailure())
							.hasMessageContaining("does/not/exist.sql");
					assertThat(context.getStartupFailure())
							.hasMessageContaining("spring.datasource.data");
				});
	}

	private String getRelativeLocationFor(String resource) {
		return ClassUtils.addResourcePathToPackagePath(getClass(), resource);
	}

	@Configuration
	protected static class OneDataSource {

		@Bean
		public DataSource oneDataSource() {
			return new TestDataSource();
		}

	}

	@Configuration
	protected static class TwoDataSources extends OneDataSource {

		@Bean
		public DataSource twoDataSource() {
			return new TestDataSource();
		}

	}

	/**
	 * {@link ResourcePatternResolver} used to ensure consistently wrong resource
	 * ordering.
	 */
	private static class ReverseOrderResourceLoader implements ResourcePatternResolver {

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
			Arrays.sort(resources,
					Comparator.comparing(Resource::getFilename).reversed());
			return resources;
		}

	}

}
