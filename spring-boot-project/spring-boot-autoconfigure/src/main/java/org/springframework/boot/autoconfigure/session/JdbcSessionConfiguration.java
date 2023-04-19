/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.session;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.sql.init.OnDatabaseInitializationCondition;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.sql.init.dependency.DatabaseInitializationDependencyConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.session.SessionRepository;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.jdbc.config.annotation.SpringSessionDataSource;
import org.springframework.session.jdbc.config.annotation.web.http.JdbcHttpSessionConfiguration;

/**
 * JDBC backed session configuration.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Vedran Pavic
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ JdbcTemplate.class, JdbcIndexedSessionRepository.class })
@ConditionalOnMissingBean(SessionRepository.class)
@ConditionalOnBean(DataSource.class)
@EnableConfigurationProperties(JdbcSessionProperties.class)
@Import({ DatabaseInitializationDependencyConfigurer.class, JdbcHttpSessionConfiguration.class })
class JdbcSessionConfiguration {

	@Bean
	@ConditionalOnMissingBean(JdbcSessionDataSourceScriptDatabaseInitializer.class)
	@Conditional(OnJdbcSessionDatasourceInitializationCondition.class)
	JdbcSessionDataSourceScriptDatabaseInitializer jdbcSessionDataSourceScriptDatabaseInitializer(
			@SpringSessionDataSource ObjectProvider<DataSource> sessionDataSource,
			ObjectProvider<DataSource> dataSource, JdbcSessionProperties properties) {
		DataSource dataSourceToInitialize = sessionDataSource.getIfAvailable(dataSource::getObject);
		return new JdbcSessionDataSourceScriptDatabaseInitializer(dataSourceToInitialize, properties);
	}

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	SessionRepositoryCustomizer<JdbcIndexedSessionRepository> springBootSessionRepositoryCustomizer(
			SessionProperties sessionProperties, JdbcSessionProperties jdbcSessionProperties,
			ServerProperties serverProperties) {
		return (sessionRepository) -> {
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(sessionProperties.determineTimeout(() -> serverProperties.getServlet().getSession().getTimeout()))
				.to(sessionRepository::setDefaultMaxInactiveInterval);
			map.from(jdbcSessionProperties::getTableName).to(sessionRepository::setTableName);
			map.from(jdbcSessionProperties::getFlushMode).to(sessionRepository::setFlushMode);
			map.from(jdbcSessionProperties::getSaveMode).to(sessionRepository::setSaveMode);
			map.from(jdbcSessionProperties::getCleanupCron).to(sessionRepository::setCleanupCron);
		};
	}

	static class OnJdbcSessionDatasourceInitializationCondition extends OnDatabaseInitializationCondition {

		OnJdbcSessionDatasourceInitializationCondition() {
			super("Jdbc Session", "spring.session.jdbc.initialize-schema");
		}

	}

}
