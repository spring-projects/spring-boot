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

package org.springframework.boot.security.jdbc.autoconfigure;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.sql.init.DatabaseInitializationMode;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link SecurityJdbcAutoConfiguration}.
 *
 * @author Chaitanya
 */
class SecurityJdbcAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class, SecurityJdbcAutoConfiguration.class))
		.withPropertyValues("spring.datasource.generate-unique-name=true");

	@Test
	void byDefaultTheSchemaIsNotInitialized() {
		this.contextRunner.run((context) -> {
			assertThat(context.getBean(SecurityJdbcProperties.class).getInitializeSchema())
				.isEqualTo(DatabaseInitializationMode.NEVER);
			assertThat(context).doesNotHaveBean(SecurityDataSourceScriptDatabaseInitializer.class);
			assertThatExceptionOfType(BadSqlGrammarException.class)
				.isThrownBy(() -> new JdbcTemplate(context.getBean(DataSource.class)).queryForList("select * from users"));
		});
	}

	@Test
	void whenInitializeSchemaIsAlwaysThenSchemaIsInitialized() {
		this.contextRunner.withPropertyValues("spring.security.jdbc.initialize-schema=always").run((context) -> {
			assertThat(context).hasSingleBean(SecurityDataSourceScriptDatabaseInitializer.class);
			JdbcTemplate jdbc = new JdbcTemplate(context.getBean(DataSource.class));
			assertThat(jdbc.queryForList("select * from users")).isEmpty();
			assertThat(jdbc.queryForList("select * from authorities")).isEmpty();
		});
	}

	@Test
	void whenInitializeSchemaIsEmbeddedThenSchemaIsInitialized() {
		this.contextRunner.withPropertyValues("spring.security.jdbc.initialize-schema=embedded").run((context) -> {
			assertThat(context).hasSingleBean(SecurityDataSourceScriptDatabaseInitializer.class);
			assertThat(new JdbcTemplate(context.getBean(DataSource.class)).queryForList("select * from users"))
				.isEmpty();
		});
	}

	@Test
	void whenCustomSchemaIsConfiguredThenItIsUsed() {
		this.contextRunner
			.withPropertyValues("spring.security.jdbc.initialize-schema=always",
					"spring.security.jdbc.schema=classpath:org/springframework/boot/security/jdbc/autoconfigure/custom-schema.sql")
			.run((context) -> assertThat(
					new JdbcTemplate(context.getBean(DataSource.class)).queryForList("select * from custom_users"))
				.isEmpty());
	}

	@Test
	void whenThereIsNoDataSourceThenAutoConfigurationBacksOff() {
		new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(SecurityJdbcAutoConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean(SecurityDataSourceScriptDatabaseInitializer.class));
	}

	@Test
	void whenTheUserDefinesTheirOwnSecurityDatabaseInitializerThenTheAutoConfiguredInitializerBacksOff() {
		this.contextRunner.withPropertyValues("spring.security.jdbc.initialize-schema=always")
			.withUserConfiguration(CustomSecurityDatabaseInitializerConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(SecurityDataSourceScriptDatabaseInitializer.class)
				.doesNotHaveBean("securityDataSourceScriptDatabaseInitializer")
				.hasBean("customInitializer"));
	}

	@Test
	void whenTheUserDefinesTheirOwnDatabaseInitializerThenTheAutoConfiguredSecurityInitializerRemains() {
		this.contextRunner.withPropertyValues("spring.security.jdbc.initialize-schema=always")
			.withUserConfiguration(CustomDatabaseInitializerConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(SecurityDataSourceScriptDatabaseInitializer.class)
				.hasBean("customInitializer"));
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomSecurityDatabaseInitializerConfiguration {

		@Bean
		SecurityDataSourceScriptDatabaseInitializer customInitializer(DataSource dataSource,
				SecurityJdbcProperties properties) {
			return new SecurityDataSourceScriptDatabaseInitializer(dataSource, properties);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomDatabaseInitializerConfiguration {

		@Bean
		DataSourceScriptDatabaseInitializer customInitializer(DataSource dataSource) {
			return new DataSourceScriptDatabaseInitializer(dataSource, new DatabaseInitializationSettings());
		}

	}

}
