/*
 * Copyright 2012-2019 the original author or authors.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.session.SessionRepository;
import org.springframework.session.jdbc.JdbcOperationsSessionRepository;
import org.springframework.session.jdbc.config.annotation.web.http.JdbcHttpSessionConfiguration;

/**
 * JDBC backed session configuration.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Vedran Pavic
 */
@Configuration
@ConditionalOnClass({ JdbcTemplate.class, JdbcOperationsSessionRepository.class })
@ConditionalOnMissingBean(SessionRepository.class)
@ConditionalOnBean(DataSource.class)
@Conditional(ServletSessionCondition.class)
@EnableConfigurationProperties(JdbcSessionProperties.class)
class JdbcSessionConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public JdbcSessionDataSourceInitializer jdbcSessionDataSourceInitializer(DataSource dataSource,
			ResourceLoader resourceLoader, JdbcSessionProperties properties) {
		return new JdbcSessionDataSourceInitializer(dataSource, resourceLoader, properties);
	}

	@Configuration
	public static class SpringBootJdbcHttpSessionConfiguration extends JdbcHttpSessionConfiguration {

		@Autowired
		public void customize(SessionProperties sessionProperties, JdbcSessionProperties jdbcSessionProperties) {
			Duration timeout = sessionProperties.getTimeout();
			if (timeout != null) {
				setMaxInactiveIntervalInSeconds((int) timeout.getSeconds());
			}
			setTableName(jdbcSessionProperties.getTableName());
			setCleanupCron(jdbcSessionProperties.getCleanupCron());
		}

	}

}
