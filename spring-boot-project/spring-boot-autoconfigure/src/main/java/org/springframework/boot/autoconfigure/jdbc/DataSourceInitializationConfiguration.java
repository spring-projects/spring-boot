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

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.jdbc.DataSourceInitializationConfiguration.InitializationSpecificCredentialsDataSourceInitializationConfiguration.DifferentCredentialsCondition;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.DataSourceInitializationMode;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.jdbc.init.DataSourceInitializationSettings;
import org.springframework.boot.jdbc.init.ScriptDataSourceInitializer;
import org.springframework.boot.jdbc.init.dependency.DataSourceInitializationDependencyConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.util.StringUtils;

/**
 * Configuration for {@link DataSource} initialization using a
 * {@link ScriptDataSourceInitializer} with DDL and DML scripts.
 *
 * @author Andy Wilkinson
 */
class DataSourceInitializationConfiguration {

	private static DataSource determineDataSource(Supplier<DataSource> dataSource, String username, String password,
			DataSourceProperties properties) {
		if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
			DataSourceBuilder.derivedFrom(dataSource.get()).type(SimpleDriverDataSource.class).username(username)
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

	// Fully-qualified to work around javac bug in JDK 1.8
	@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
	@org.springframework.context.annotation.Conditional(DifferentCredentialsCondition.class)
	@org.springframework.context.annotation.Import(DataSourceInitializationDependencyConfigurer.class)
	@ConditionalOnSingleCandidate(DataSource.class)
	@ConditionalOnMissingBean(ScriptDataSourceInitializer.class)
	static class InitializationSpecificCredentialsDataSourceInitializationConfiguration {

		@Bean
		ScriptDataSourceInitializer ddlOnlyScriptDataSourceInitializer(ObjectProvider<DataSource> dataSource,
				DataSourceProperties properties, ResourceLoader resourceLoader) {
			DataSourceInitializationSettings settings = new DataSourceInitializationSettings();
			settings.setDdlScriptLocations(scriptLocations(properties.getSchema(), "schema", properties.getPlatform()));
			settings.setContinueOnError(properties.isContinueOnError());
			settings.setSeparator(properties.getSeparator());
			settings.setEncoding(properties.getSqlScriptEncoding());
			DataSource initializationDataSource = determineDataSource(dataSource::getObject,
					properties.getSchemaUsername(), properties.getSchemaPassword(), properties);
			return new InitializationModeDataSourceScriptDatabaseInitializer(initializationDataSource, settings,
					properties.getInitializationMode());
		}

		@Bean
		@DependsOn("ddlOnlyScriptDataSourceInitializer")
		ScriptDataSourceInitializer dmlOnlyScriptDataSourceInitializer(ObjectProvider<DataSource> dataSource,
				DataSourceProperties properties, ResourceLoader resourceLoader) {
			DataSourceInitializationSettings settings = new DataSourceInitializationSettings();
			settings.setDmlScriptLocations(scriptLocations(properties.getData(), "data", properties.getPlatform()));
			settings.setContinueOnError(properties.isContinueOnError());
			settings.setSeparator(properties.getSeparator());
			settings.setEncoding(properties.getSqlScriptEncoding());
			DataSource initializationDataSource = determineDataSource(dataSource::getObject,
					properties.getDataUsername(), properties.getDataPassword(), properties);
			return new InitializationModeDataSourceScriptDatabaseInitializer(initializationDataSource, settings,
					properties.getInitializationMode());
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

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnSingleCandidate(DataSource.class)
	@ConditionalOnMissingBean(ScriptDataSourceInitializer.class)
	@Import(DataSourceInitializationDependencyConfigurer.class)
	static class SharedCredentialsDataSourceInitializationConfiguration {

		@Bean
		ScriptDataSourceInitializer scriptDataSourceInitializer(DataSource dataSource, DataSourceProperties properties,
				ResourceLoader resourceLoader) {
			DataSourceInitializationSettings settings = new DataSourceInitializationSettings();
			settings.setDdlScriptLocations(scriptLocations(properties.getSchema(), "schema", properties.getPlatform()));
			settings.setDmlScriptLocations(scriptLocations(properties.getData(), "data", properties.getPlatform()));
			settings.setContinueOnError(properties.isContinueOnError());
			settings.setSeparator(properties.getSeparator());
			settings.setEncoding(properties.getSqlScriptEncoding());
			return new InitializationModeDataSourceScriptDatabaseInitializer(dataSource, settings,
					properties.getInitializationMode());
		}

	}

	static class InitializationModeDataSourceScriptDatabaseInitializer extends ScriptDataSourceInitializer {

		private final DataSourceInitializationMode mode;

		InitializationModeDataSourceScriptDatabaseInitializer(DataSource dataSource,
				DataSourceInitializationSettings settings, DataSourceInitializationMode mode) {
			super(dataSource, settings);
			this.mode = mode;
		}

		@Override
		protected void runScripts(List<Resource> resources, boolean continueOnError, String separator,
				Charset encoding) {
			if (this.mode == DataSourceInitializationMode.ALWAYS || (this.mode == DataSourceInitializationMode.EMBEDDED
					&& EmbeddedDatabaseConnection.isEmbedded(getDataSource()))) {
				super.runScripts(resources, continueOnError, separator, encoding);
			}
		}

	}

}
