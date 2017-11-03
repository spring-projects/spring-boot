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

package org.springframework.boot.autoconfigure.liquibase;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.liquibase.CommonsLoggingLiquibaseLogger;
import org.springframework.boot.liquibase.LiquibaseServiceLocatorApplicationListener;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.boot.testsupport.Assume;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LiquibaseAutoConfiguration}.
 *
 * @author Marcel Overdijk
 * @author Eddú Meléndez
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
public class LiquibaseAutoConfigurationTests {

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	@Before
	public void init() {
		new LiquibaseServiceLocatorApplicationListener().onApplicationEvent(null);
	}

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(LiquibaseAutoConfiguration.class))
			.withPropertyValues("spring.datasource.generate-unique-name=true");

	@Test
	public void noDataSource() {
		this.contextRunner.run(
				(context) -> assertThat(context).doesNotHaveBean(SpringLiquibase.class));
	}

	@Test
	public void defaultSpringLiquibase() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.run(assertLiquibase((liquibase) -> {
					assertThat(liquibase.getChangeLog()).isEqualTo(
							"classpath:/db/changelog/db.changelog-master.yaml");
					assertThat(liquibase.getContexts()).isNull();
					assertThat(liquibase.getDefaultSchema()).isNull();
					assertThat(liquibase.isDropFirst()).isFalse();
				}));
	}

	@Test
	public void changelogXml() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues(
						"spring.liquibase.change-log:classpath:/db/changelog/db.changelog-override.xml")
				.run(assertLiquibase((liquibase) -> assertThat(liquibase.getChangeLog())
						.isEqualTo("classpath:/db/changelog/db.changelog-override.xml")));
	}

	@Test
	public void changelogJson() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues(
						"spring.liquibase.change-log:classpath:/db/changelog/db.changelog-override.json")
				.run(assertLiquibase(
						(liquibase) -> assertThat(liquibase.getChangeLog()).isEqualTo(
								"classpath:/db/changelog/db.changelog-override.json")));
	}

	@Test
	public void changelogSql() {
		Assume.javaEight();
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues(
						"spring.liquibase.change-log:classpath:/db/changelog/db.changelog-override.sql")
				.run(assertLiquibase((liquibase) -> assertThat(liquibase.getChangeLog())
						.isEqualTo("classpath:/db/changelog/db.changelog-override.sql")));
	}

	@Test
	public void overrideContexts() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.liquibase.contexts:test, production")
				.run(assertLiquibase((liquibase) -> assertThat(liquibase.getContexts())
						.isEqualTo("test, production")));
	}

	@Test
	public void overrideDefaultSchema() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.liquibase.default-schema:public")
				.run(assertLiquibase(
						(liquibase) -> assertThat(liquibase.getDefaultSchema())
								.isEqualTo("public")));
	}

	@Test
	public void overrideDropFirst() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.liquibase.drop-first:true")
				.run(assertLiquibase(
						(liquibase) -> assertThat(liquibase.isDropFirst()).isTrue()));
	}

	@Test
	public void overrideDataSource() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.liquibase.url:jdbc:hsqldb:mem:liquibase",
						"spring.liquibase.user:sa")
				.run(assertLiquibase((liquibase) -> {
					DataSource dataSource = liquibase.getDataSource();
					assertThat(((HikariDataSource) dataSource).isClosed()).isTrue();
					assertThat(((HikariDataSource) dataSource).getJdbcUrl())
							.isEqualTo("jdbc:hsqldb:mem:liquibase");
				}));
	}

	@Test
	public void changeLogDoesNotExist() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues(
						"spring.liquibase.change-log:classpath:/no-such-changelog.yaml")
				.run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context).getFailure()
							.isInstanceOf(BeanCreationException.class);
				});
	}

	@Test
	public void logging() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.run(assertLiquibase((liquibase) -> {
					Object log = ReflectionTestUtils.getField(liquibase, "log");
					assertThat(log).isInstanceOf(CommonsLoggingLiquibaseLogger.class);
					assertThat(this.outputCapture.toString())
							.doesNotContain(": liquibase:");
				}));
	}

	@Test
	public void overrideLabels() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.liquibase.labels:test, production")
				.run(assertLiquibase((liquibase) -> assertThat(liquibase.getLabels())
						.isEqualTo("test, production")));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testOverrideParameters() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.liquibase.parameters.foo:bar")
				.run(assertLiquibase((liquibase) -> {
					Map<String, String> parameters = (Map<String, String>) ReflectionTestUtils
							.getField(liquibase, "parameters");
					assertThat(parameters.containsKey("foo")).isTrue();
					assertThat(parameters.get("foo")).isEqualTo("bar");
				}));
	}

	@Test
	public void rollbackFile() throws IOException {
		File file = this.temp.newFile("rollback-file.sql");
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues(
						"spring.liquibase.rollbackFile:" + file.getAbsolutePath())
				.run((context) -> {
					SpringLiquibase liquibase = context.getBean(SpringLiquibase.class);
					File actualFile = (File) ReflectionTestUtils.getField(liquibase,
							"rollbackFile");
					assertThat(actualFile).isEqualTo(file).exists();
					String content = new String(FileCopyUtils.copyToByteArray(file));
					assertThat(content).contains("DROP TABLE PUBLIC.customer;");
				});
	}

	@Test
	public void liquibaseDataSource() {
		this.contextRunner.withUserConfiguration(LiquibaseDataSourceConfiguration.class,
				EmbeddedDataSourceConfiguration.class).run((context) -> {
					SpringLiquibase liquibase = context.getBean(SpringLiquibase.class);
					assertThat(liquibase.getDataSource())
							.isEqualTo(context.getBean("liquibaseDataSource"));
				});
	}

	private ContextConsumer<AssertableApplicationContext> assertLiquibase(
			Consumer<SpringLiquibase> consumer) {
		return (context) -> {
			assertThat(context).hasSingleBean(SpringLiquibase.class);
			SpringLiquibase liquibase = context.getBean(SpringLiquibase.class);
			consumer.accept(liquibase);
		};
	}

	@Configuration
	static class LiquibaseDataSourceConfiguration {

		@Bean
		@Primary
		public DataSource normalDataSource() {
			return DataSourceBuilder.create().url("jdbc:hsqldb:mem:normal").username("sa")
					.build();
		}

		@LiquibaseDataSource
		@Bean
		public DataSource liquibaseDataSource() {
			return DataSourceBuilder.create().url("jdbc:hsqldb:mem:liquibasetest")
					.username("sa").build();
		}

	}

}
