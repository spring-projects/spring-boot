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
import org.springframework.security.core.userdetails.jdbc.JdbcDaoImpl;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the schema used by Spring
 * Security's JDBC-backed user details support.
 * <p>
 * Only the schema is initialized. The {@link JdbcDaoImpl} or
 * {@link org.springframework.security.provisioning.JdbcUserDetailsManager} bean itself is
 * left to the application to declare.
 *
 * @author Chaitanya
 * @since 4.2.0
 */
@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@ConditionalOnClass({ JdbcDaoImpl.class, DataSourceScriptDatabaseInitializer.class })
@ConditionalOnSingleCandidate(DataSource.class)
@EnableConfigurationProperties(SecurityJdbcProperties.class)
@Import(DatabaseInitializationDependencyConfigurer.class)
public final class SecurityJdbcAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Conditional(OnSecurityDatasourceInitializationCondition.class)
	SecurityDataSourceScriptDatabaseInitializer securityDataSourceScriptDatabaseInitializer(DataSource dataSource,
			SecurityJdbcProperties properties) {
		return new SecurityDataSourceScriptDatabaseInitializer(dataSource, properties);
	}

	static class OnSecurityDatasourceInitializationCondition extends OnDatabaseInitializationCondition {

		OnSecurityDatasourceInitializationCondition() {
			super("Spring Security", "spring.security.jdbc.initialize-schema");
		}

	}

}
