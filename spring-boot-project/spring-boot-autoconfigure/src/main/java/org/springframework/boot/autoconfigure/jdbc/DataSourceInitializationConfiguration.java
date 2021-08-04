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

package org.springframework.boot.autoconfigure.jdbc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.jdbc.DataSourceInitializationConfiguration.InitializationSpecificCredentialsDataSourceInitializationConfiguration.DifferentCredentialsCondition;
import org.springframework.boot.autoconfigure.jdbc.DataSourceInitializationConfiguration.SharedCredentialsDataSourceInitializationConfiguration.DataSourceInitializationCondition;
import org.springframework.boot.autoconfigure.sql.init.SqlDataSourceScriptDatabaseInitializer;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.sql.init.DatabaseInitializationMode;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.boot.sql.init.dependency.DatabaseInitializationDependencyConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.util.StringUtils;

/**
 * Configuration for {@link DataSource} initialization using a
 * {@link DataSourceScriptDatabaseInitializer} with DDL and DML scripts.
 *
 * @author Andy Wilkinson
 */
@Deprecated
class DataSourceInitializationConfiguration {

	private static DataSource determineDataSource(Supplier<DataSource> dataSource, String username, String password) {
		if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
			return DataSourceBuilder.derivedFrom(dataSource.get()).type(SimpleDriverDataSource.class).username(username)
					.password(password).build();
		}
		return dataSource.get();
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

	private static DatabaseInitializationMode mapMode(org.springframework.boot.jdbc.DataSourceInitializationMode mode) {
		switch (mode) {
		case ALWAYS:
			return DatabaseInitializationMode.ALWAYS;
		case EMBEDDED:
			return DatabaseInitializationMode.EMBEDDED;
		case NEVER:
			return DatabaseInitializationMode.NEVER;
		default:
			throw new IllegalStateException("Unexpected initialization mode '" + mode + "'");
		}
	}

	// Fully-qualified to work around javac bug in JDK 1.8
	@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
	@org.springframework.context.annotation.Conditional(DifferentCredentialsCondition.class)
	@org.springframework.context.annotation.Import(DatabaseInitializationDependencyConfigurer.class)
	@ConditionalOnSingleCandidate(DataSource.class)
	@ConditionalOnMissingBean(DataSourceScriptDatabaseInitializer.class)
	static class InitializationSpecificCredentialsDataSourceInitializationConfiguration {

		@Bean
		SqlDataSourceScriptDatabaseInitializer ddlOnlyScriptDataSourceInitializer(ObjectProvider<DataSource> dataSource,
				DataSourceProperties properties) {
			DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
			settings.setSchemaLocations(scriptLocations(properties.getSchema(), "schema", properties.getPlatform()));
			settings.setContinueOnError(properties.isContinueOnError());
			settings.setSeparator(properties.getSeparator());
			settings.setEncoding(properties.getSqlScriptEncoding());
			settings.setMode(mapMode(properties.getInitializationMode()));
			DataSource initializationDataSource = determineDataSource(dataSource::getObject,
					properties.getSchemaUsername(), properties.getSchemaPassword());
			return new SqlDataSourceScriptDatabaseInitializer(initializationDataSource, settings);
		}

		@Bean
		@DependsOn("ddlOnlyScriptDataSourceInitializer")
		SqlDataSourceScriptDatabaseInitializer dmlOnlyScriptDataSourceInitializer(ObjectProvider<DataSource> dataSource,
				DataSourceProperties properties) {
			DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
			settings.setDataLocations(scriptLocations(properties.getData(), "data", properties.getPlatform()));
			settings.setContinueOnError(properties.isContinueOnError());
			settings.setSeparator(properties.getSeparator());
			settings.setEncoding(properties.getSqlScriptEncoding());
			settings.setMode(mapMode(properties.getInitializationMode()));
			DataSource initializationDataSource = determineDataSource(dataSource::getObject,
					properties.getDataUsername(), properties.getDataPassword());
			return new SqlDataSourceScriptDatabaseInitializer(initializationDataSource, settings);
		}

		static class DifferentCredentialsCondition extends AnyNestedCondition {

			DifferentCredentialsCondition() {
				super(ConfigurationPhase.PARSE_CONFIGURATION);
			}

			@ConditionalOnProperty(prefix = "spring.datasource", name = "schema-username")
			static class SchemaCredentials {

			}

			@ConditionalOnProperty(prefix = "spring.datasource", name = "data-username")
			static class DataCredentials {

			}

		}

	}

	// Fully-qualified to work around javac bug in JDK 1.8
	@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
	@org.springframework.context.annotation.Import(DatabaseInitializationDependencyConfigurer.class)
	@org.springframework.context.annotation.Conditional(DataSourceInitializationCondition.class)
	@ConditionalOnSingleCandidate(DataSource.class)
	@ConditionalOnMissingBean(DataSourceScriptDatabaseInitializer.class)
	static class SharedCredentialsDataSourceInitializationConfiguration {

		@Bean
		SqlDataSourceScriptDatabaseInitializer scriptDataSourceInitializer(DataSource dataSource,
				DataSourceProperties properties) {
			DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
			settings.setSchemaLocations(scriptLocations(properties.getSchema(), "schema", properties.getPlatform()));
			settings.setDataLocations(scriptLocations(properties.getData(), "data", properties.getPlatform()));
			settings.setContinueOnError(properties.isContinueOnError());
			settings.setSeparator(properties.getSeparator());
			settings.setEncoding(properties.getSqlScriptEncoding());
			settings.setMode(mapMode(properties.getInitializationMode()));
			return new SqlDataSourceScriptDatabaseInitializer(dataSource, settings);
		}

		static class DataSourceInitializationCondition extends SpringBootCondition {

			private static final Set<String> INITIALIZATION_PROPERTIES = Collections
					.unmodifiableSet(new HashSet<>(Arrays.asList("spring.datasource.initialization-mode",
							"spring.datasource.platform", "spring.datasource.schema", "spring.datasource.schema[0]",
							"spring.datasource.schema-username", "spring.datasource.schema-password",
							"spring.datasource.data", "spring.datasource.data[0]", "spring.datasource.data-username",
							"spring.datasource.data-password", "spring.datasource.continue-on-error",
							"spring.datasource.separator", "spring.datasource.sql-script-encoding")));

			@Override
			public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
				ConditionMessage.Builder message = ConditionMessage.forCondition("DataSource Initialization");
				Environment environment = context.getEnvironment();
				Set<String> configuredProperties = INITIALIZATION_PROPERTIES.stream()
						.filter(environment::containsProperty).collect(Collectors.toSet());
				if (configuredProperties.isEmpty()) {
					return ConditionOutcome
							.noMatch(message.didNotFind("configured properties").items(INITIALIZATION_PROPERTIES));
				}
				return ConditionOutcome.match(
						message.found("configured property", "configured properties").items(configuredProperties));
			}

		}

	}

}
