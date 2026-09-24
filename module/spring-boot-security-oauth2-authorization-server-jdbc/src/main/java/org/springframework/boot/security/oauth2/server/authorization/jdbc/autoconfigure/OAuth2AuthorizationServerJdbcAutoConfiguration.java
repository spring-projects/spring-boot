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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.sql.autoconfigure.init.OnDatabaseInitializationCondition;
import org.springframework.boot.sql.init.dependency.DatabaseInitializationDependencyConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the schemas used by Spring
 * Authorization Server's JDBC-backed services.
 * <p>
 * Only the schemas are initialized. The {@code JdbcRegisteredClientRepository},
 * {@code JdbcOAuth2AuthorizationService} and {@code JdbcOAuth2AuthorizationConsentService}
 * beans themselves are left to the application to declare.
 *
 * @author Chaitanya
 * @since 4.2.0
 */
@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@ConditionalOnClass({ OAuth2Authorization.class, DataSourceScriptDatabaseInitializer.class })
@ConditionalOnSingleCandidate(DataSource.class)
@EnableConfigurationProperties({ RegisteredClientJdbcProperties.class, OAuth2AuthorizationJdbcProperties.class,
		OAuth2AuthorizationConsentJdbcProperties.class })
@Import(DatabaseInitializationDependencyConfigurer.class)
public final class OAuth2AuthorizationServerJdbcAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Conditional(OnRegisteredClientDatasourceInitializationCondition.class)
	RegisteredClientDataSourceScriptDatabaseInitializer registeredClientDataSourceScriptDatabaseInitializer(
			DataSource dataSource, RegisteredClientJdbcProperties properties) {
		return new RegisteredClientDataSourceScriptDatabaseInitializer(dataSource, properties);
	}

	@Bean
	@ConditionalOnMissingBean
	@Conditional(OnOAuth2AuthorizationDatasourceInitializationCondition.class)
	OAuth2AuthorizationDataSourceScriptDatabaseInitializer oauth2AuthorizationDataSourceScriptDatabaseInitializer(
			DataSource dataSource, OAuth2AuthorizationJdbcProperties properties) {
		return new OAuth2AuthorizationDataSourceScriptDatabaseInitializer(dataSource, properties);
	}

	@Bean
	@ConditionalOnMissingBean
	@Conditional(OnOAuth2AuthorizationConsentDatasourceInitializationCondition.class)
	OAuth2AuthorizationConsentDataSourceScriptDatabaseInitializer oauth2AuthorizationConsentDataSourceScriptDatabaseInitializer(
			DataSource dataSource, OAuth2AuthorizationConsentJdbcProperties properties) {
		return new OAuth2AuthorizationConsentDataSourceScriptDatabaseInitializer(dataSource, properties);
	}

	static class OnRegisteredClientDatasourceInitializationCondition extends OnDatabaseInitializationCondition {

		OnRegisteredClientDatasourceInitializationCondition() {
			super("OAuth2 Registered Client",
					"spring.security.oauth2.authorizationserver.client.jdbc.initialize-schema");
		}

	}

	static class OnOAuth2AuthorizationDatasourceInitializationCondition extends OnDatabaseInitializationCondition {

		OnOAuth2AuthorizationDatasourceInitializationCondition() {
			super("OAuth2 Authorization",
					"spring.security.oauth2.authorizationserver.authorization.jdbc.initialize-schema");
		}

	}

	static class OnOAuth2AuthorizationConsentDatasourceInitializationCondition
			extends OnDatabaseInitializationCondition {

		OnOAuth2AuthorizationConsentDatasourceInitializationCondition() {
			super("OAuth2 Authorization Consent",
					"spring.security.oauth2.authorizationserver.consent.jdbc.initialize-schema");
		}

	}

}
