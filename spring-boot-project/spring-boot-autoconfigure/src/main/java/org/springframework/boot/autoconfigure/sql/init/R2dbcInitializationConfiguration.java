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

package org.springframework.boot.autoconfigure.sql.init;

import io.r2dbc.spi.ConnectionFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.r2dbc.ConnectionFactoryBuilder;
import org.springframework.boot.r2dbc.init.R2dbcScriptDatabaseInitializer;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.connection.init.DatabasePopulator;
import org.springframework.util.StringUtils;

/**
 * Configuration for initializing an SQL database accessed via an R2DBC
 * {@link ConnectionFactory}.
 *
 * @author Andy Wilkinson
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ ConnectionFactory.class, DatabasePopulator.class })
@ConditionalOnSingleCandidate(ConnectionFactory.class)
class R2dbcInitializationConfiguration {

	@Bean
	R2dbcScriptDatabaseInitializer r2dbcScriptDatabaseInitializer(ConnectionFactory connectionFactory,
			SqlInitializationProperties properties) {
		DatabaseInitializationSettings settings = SettingsCreator.createFrom(properties);
		return new R2dbcScriptDatabaseInitializer(
				determineConnectionFactory(connectionFactory, properties.getUsername(), properties.getPassword()),
				settings);
	}

	private static ConnectionFactory determineConnectionFactory(ConnectionFactory connectionFactory, String username,
			String password) {
		if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
			return ConnectionFactoryBuilder.derivedFrom(connectionFactory).username(username).password(password)
					.build();
		}
		return connectionFactory;
	}

}
