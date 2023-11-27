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

package org.springframework.boot.autoconfigure.flyway;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.FlywayAutoConfigurationRuntimeHints;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.FlywayDataSourceCondition;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.boot.sql.init.dependency.DatabaseInitializationDependencyConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Flyway database migrations.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @author Jacques-Etienne Beaudet
 * @author Eddú Meléndez
 * @author Dominic Gunn
 * @author Dan Zheng
 * @author András Deák
 * @author Semyon Danilov
 * @author Chris Bono
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 1.1.0
 */
@AutoConfiguration(after = { DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class })
@ConditionalOnClass(Flyway.class)
@Conditional(FlywayDataSourceCondition.class)
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", matchIfMissing = true)
@Import(DatabaseInitializationDependencyConfigurer.class)
@ImportRuntimeHints(FlywayAutoConfigurationRuntimeHints.class)
public class FlywayAutoConfiguration {

	@Bean
	@ConfigurationPropertiesBinding
	public StringOrNumberToMigrationVersionConverter stringOrNumberMigrationVersionConverter() {
		return new StringOrNumberToMigrationVersionConverter();
	}

	@Bean
	public FlywaySchemaManagementProvider flywayDefaultDdlModeProvider(ObjectProvider<Flyway> flyways) {
		return new FlywaySchemaManagementProvider(flyways);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(JdbcUtils.class)
	@ConditionalOnMissingBean(Flyway.class)
	public static class FlywayConfiguration {


		@Bean
		ResourceProviderCustomizer resourceProviderCustomizer() {
			return new ResourceProviderCustomizer();
		}

		@Bean
		FlywayFactory flywayFactory(ResourceLoader resourceLoader, ObjectProvider<DataSource> dataSource, ResourceProviderCustomizer resourceProviderCustomizer) {
			return new FlywayFactory(resourceLoader, dataSource, resourceProviderCustomizer);
		}

		@Configuration(proxyBeanMethods = false)
		@Import(FlywayInstancesRegistrar.class)
		static class FlywayInstancesConfiguration {
		}

	}



	/**
	 * Convert a String or Number to a {@link MigrationVersion}.
	 */
	static class StringOrNumberToMigrationVersionConverter implements GenericConverter {

		private static final Set<ConvertiblePair> CONVERTIBLE_TYPES;

		static {
			Set<ConvertiblePair> types = new HashSet<>(2);
			types.add(new ConvertiblePair(String.class, MigrationVersion.class));
			types.add(new ConvertiblePair(Number.class, MigrationVersion.class));
			CONVERTIBLE_TYPES = Collections.unmodifiableSet(types);
		}

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return CONVERTIBLE_TYPES;
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			String value = ObjectUtils.nullSafeToString(source);
			return MigrationVersion.fromVersion(value);
		}

	}

	static final class FlywayDataSourceCondition extends AnyNestedCondition {

		FlywayDataSourceCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnBean(DataSource.class)
		private static final class DataSourceBeanCondition {

		}

		@ConditionalOnBean(JdbcConnectionDetails.class)
		private static final class JdbcConnectionDetailsCondition {

		}

		@ConditionalOnProperty(prefix = "spring.flyway", name = "url")
		private static final class FlywayUrlCondition {

		}

	}

	static class FlywayAutoConfigurationRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			hints.resources().registerPattern("db/migration/*");
		}

	}

	/**
	 * Adapts {@link FlywayProperties} to {@link FlywayConnectionDetails}.
	 */
	static final class PropertiesFlywayConnectionDetails implements FlywayConnectionDetails {

		private final FlywayProperties properties;

		PropertiesFlywayConnectionDetails(FlywayProperties properties) {
			this.properties = properties;
		}

		@Override
		public String getUsername() {
			return this.properties.getUser();
		}

		@Override
		public String getPassword() {
			return this.properties.getPassword();
		}

		@Override
		public String getJdbcUrl() {
			return this.properties.getUrl();
		}

		@Override
		public String getDriverClassName() {
			return this.properties.getDriverClassName();
		}

	}

}
