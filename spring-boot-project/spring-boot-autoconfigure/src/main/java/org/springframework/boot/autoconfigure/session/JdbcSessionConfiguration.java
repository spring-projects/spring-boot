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

package org.springframework.boot.autoconfigure.session;

import java.time.Duration;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import org.flywaydb.core.Flyway;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AbstractDependsOnBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.session.SessionRepository;
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
@Conditional(ServletSessionCondition.class)
@EnableConfigurationProperties(JdbcSessionProperties.class)
class JdbcSessionConfiguration {

	@Bean
	@ConditionalOnMissingBean
	JdbcSessionDataSourceInitializer jdbcSessionDataSourceInitializer(
			@SpringSessionDataSource ObjectProvider<DataSource> sessionDataSource,
			ObjectProvider<DataSource> dataSource, ResourceLoader resourceLoader, JdbcSessionProperties properties) {
		return new JdbcSessionDataSourceInitializer(sessionDataSource.getIfAvailable(dataSource::getObject),
				resourceLoader, properties);
	}

	@Configuration(proxyBeanMethods = false)
	static class SpringBootJdbcHttpSessionConfiguration extends JdbcHttpSessionConfiguration {

		@Autowired
		void customize(SessionProperties sessionProperties, JdbcSessionProperties jdbcSessionProperties,
				ServerProperties serverProperties) {
			Duration timeout = sessionProperties
					.determineTimeout(() -> serverProperties.getServlet().getSession().getTimeout());
			if (timeout != null) {
				setMaxInactiveIntervalInSeconds((int) timeout.getSeconds());
			}
			setTableName(jdbcSessionProperties.getTableName());
			setCleanupCron(jdbcSessionProperties.getCleanupCron());
			setFlushMode(jdbcSessionProperties.getFlushMode());
			setSaveMode(jdbcSessionProperties.getSaveMode());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class JdbcIndexedSessionRepositoryDependencyConfiguration {

		@Bean
		JdbcIndexedSessionRepositoryDependsOnBeanFactoryPostProcessor dataSourceInitializerJdbcIndexedSessionRepositoryDependsOnBeanFactoryPostProcessor() {
			return new JdbcIndexedSessionRepositoryDependsOnBeanFactoryPostProcessor(
					JdbcSessionDataSourceInitializer.class);
		}

		@Bean
		@ConditionalOnClass(name = "org.flywaydb.core.Flyway")
		JdbcIndexedSessionRepositoryDependsOnBeanFactoryPostProcessor flywayJdbcIndexedSessionRepositoryDependsOnBeanFactoryPostProcessor() {
			return new JdbcIndexedSessionRepositoryDependsOnBeanFactoryPostProcessor(FlywayMigrationInitializer.class,
					Flyway.class);
		}

		@Bean
		@ConditionalOnClass(name = "liquibase.integration.spring.SpringLiquibase")
		JdbcIndexedSessionRepositoryDependsOnBeanFactoryPostProcessor liquibaseJdbcIndexedSessionRepositoryDependsOnBeanFactoryPostProcessor() {
			return new JdbcIndexedSessionRepositoryDependsOnBeanFactoryPostProcessor(SpringLiquibase.class);
		}

	}

	/**
	 * {@link AbstractDependsOnBeanFactoryPostProcessor} for Spring Session JDBC's
	 * {@link JdbcIndexedSessionRepository}.
	 */
	static class JdbcIndexedSessionRepositoryDependsOnBeanFactoryPostProcessor
			extends AbstractDependsOnBeanFactoryPostProcessor {

		JdbcIndexedSessionRepositoryDependsOnBeanFactoryPostProcessor(Class<?>... dependencyTypes) {
			super(JdbcIndexedSessionRepository.class, dependencyTypes);
		}

	}

}
