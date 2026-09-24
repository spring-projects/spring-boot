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

package org.springframework.boot.security.oauth2.server.authorization.jdbc.autoconfigure;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link OAuth2AuthorizationServerJdbcAutoConfiguration}.
 *
 * @author Chaitanya
 */
class OAuth2AuthorizationServerJdbcAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
				OAuth2AuthorizationServerJdbcAutoConfiguration.class))
		.withPropertyValues("spring.datasource.generate-unique-name=true");

	@Test
	void withAnEmbeddedDataSourceAllThreeSchemasAreInitialized() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(RegisteredClientDataSourceScriptDatabaseInitializer.class)
				.hasSingleBean(OAuth2AuthorizationDataSourceScriptDatabaseInitializer.class)
				.hasSingleBean(OAuth2AuthorizationConsentDataSourceScriptDatabaseInitializer.class);
			JdbcTemplate jdbc = new JdbcTemplate(context.getBean(DataSource.class));
			assertThat(jdbc.queryForList("select * from oauth2_registered_client")).isEmpty();
			assertThat(jdbc.queryForList("select * from oauth2_authorization")).isEmpty();
			assertThat(jdbc.queryForList("select * from oauth2_authorization_consent")).isEmpty();
		});
	}

	@Test
	void eachSchemaCanBeDisabledIndependently() {
		this.contextRunner
			.withPropertyValues("spring.security.oauth2.authorizationserver.authorization.jdbc.initialize-schema=never",
					"spring.security.oauth2.authorizationserver.consent.jdbc.initialize-schema=never")
			.run((context) -> {
				assertThat(context).hasSingleBean(RegisteredClientDataSourceScriptDatabaseInitializer.class)
					.doesNotHaveBean(OAuth2AuthorizationDataSourceScriptDatabaseInitializer.class)
					.doesNotHaveBean(OAuth2AuthorizationConsentDataSourceScriptDatabaseInitializer.class);
				JdbcTemplate jdbc = new JdbcTemplate(context.getBean(DataSource.class));
				assertThat(jdbc.queryForList("select * from oauth2_registered_client")).isEmpty();
				assertThatExceptionOfType(BadSqlGrammarException.class)
					.isThrownBy(() -> jdbc.queryForList("select * from oauth2_authorization"));
			});
	}

	@Test
	void whenThereIsNoDataSourceThenAutoConfigurationBacksOff() {
		new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(OAuth2AuthorizationServerJdbcAutoConfiguration.class))
			.run((context) -> assertThat(context)
				.doesNotHaveBean(RegisteredClientDataSourceScriptDatabaseInitializer.class));
	}

	@Test
	void whenTheUserDefinesTheirOwnInitializerThenTheAutoConfiguredOneBacksOff() {
		this.contextRunner.withUserConfiguration(CustomRegisteredClientInitializerConfiguration.class)
			.run((context) -> assertThat(context)
				.hasSingleBean(RegisteredClientDataSourceScriptDatabaseInitializer.class)
				.doesNotHaveBean("registeredClientDataSourceScriptDatabaseInitializer")
				.hasBean("customInitializer"));
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomRegisteredClientInitializerConfiguration {

		@Bean
		RegisteredClientDataSourceScriptDatabaseInitializer customInitializer(DataSource dataSource,
				RegisteredClientJdbcProperties properties) {
			return new RegisteredClientDataSourceScriptDatabaseInitializer(dataSource, properties);
		}

	}

}
