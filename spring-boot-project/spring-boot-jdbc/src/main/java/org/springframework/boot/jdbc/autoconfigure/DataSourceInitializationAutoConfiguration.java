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

package org.springframework.boot.jdbc.autoconfigure;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.sql.autoconfigure.init.ApplicationScriptDatabaseInitializer;
import org.springframework.boot.sql.autoconfigure.init.ConditionalOnSqlInitialization;
import org.springframework.boot.sql.autoconfigure.init.SqlInitializationProperties;
import org.springframework.boot.sql.init.dependency.DatabaseInitializationDependencyConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.util.StringUtils;

/**
 * Auto-configuration for {@link DataSource} initialization.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnMissingBean(ApplicationScriptDatabaseInitializer.class)
@ConditionalOnSingleCandidate(DataSource.class)
@ConditionalOnClass(DatabasePopulator.class)
@Import(DatabaseInitializationDependencyConfigurer.class)
@EnableConfigurationProperties(SqlInitializationProperties.class)
@ConditionalOnSqlInitialization
public class DataSourceInitializationAutoConfiguration {

	@Bean
	ApplicationDataSourceScriptDatabaseInitializer dataSourceScriptDatabaseInitializer(DataSource dataSource,
			SqlInitializationProperties properties) {
		return new ApplicationDataSourceScriptDatabaseInitializer(
				determineDataSource(dataSource, properties.getUsername(), properties.getPassword()), properties);
	}

	private static DataSource determineDataSource(DataSource dataSource, String username, String password) {
		if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
			return DataSourceBuilder.derivedFrom(dataSource)
				.username(username)
				.password(password)
				.type(SimpleDriverDataSource.class)
				.build();
		}
		return dataSource;
	}

}
