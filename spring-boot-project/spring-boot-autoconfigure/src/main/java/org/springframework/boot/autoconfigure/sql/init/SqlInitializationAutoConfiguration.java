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

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.init.DataSourceInitializationSettings;
import org.springframework.boot.jdbc.init.ScriptDataSourceInitializer;
import org.springframework.boot.jdbc.init.dependency.DataSourceInitializationDependencyConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for initializing an SQL database.
 *
 * @author Andy Wilkinson
 * @since 2.5.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(ScriptDataSourceInitializer.class)
@ConditionalOnSingleCandidate(DataSource.class)
@ConditionalOnProperty(prefix = "spring.sql.init", name = "enabled", matchIfMissing = true)
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
@EnableConfigurationProperties(SqlInitializationProperties.class)
@Import(DataSourceInitializationDependencyConfigurer.class)
public class SqlInitializationAutoConfiguration {

	@Bean
	ScriptDataSourceInitializer dataSourceScriptDatabaseInitializer(DataSource dataSource,
			SqlInitializationProperties initializationProperties) {
		DataSourceInitializationSettings settings = createSettings(initializationProperties);
		return new ScriptDataSourceInitializer(determineDataSource(dataSource, initializationProperties.getUsername(),
				initializationProperties.getPassword()), settings);
	}

	private static DataSourceInitializationSettings createSettings(SqlInitializationProperties properties) {
		DataSourceInitializationSettings settings = new DataSourceInitializationSettings();
		settings.setSchemaLocations(
				scriptLocations(properties.getSchemaLocations(), "schema", properties.getPlatform()));
		settings.setDataLocations(scriptLocations(properties.getDataLocations(), "data", properties.getPlatform()));
		settings.setContinueOnError(properties.isContinueOnError());
		settings.setSeparator(properties.getSeparator());
		settings.setEncoding(properties.getEncoding());
		return settings;
	}

	private static List<String> scriptLocations(List<String> locations, String fallback, String platform) {
		if (locations != null) {
			return locations;
		}
		List<String> fallbackLocations = new ArrayList<>();
		fallbackLocations.add("optional:classpath*:" + fallback + "-" + platform + ".sql");
		fallbackLocations.add("optional:classpath*:" + fallback + ".sql");
		return fallbackLocations;
	}

	private static DataSource determineDataSource(DataSource dataSource, String username, String password) {
		if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
			DataSourceBuilder.derivedFrom(dataSource).username(username).password(password)
					.type(SimpleDriverDataSource.class).build();
		}
		return dataSource;
	}

}
