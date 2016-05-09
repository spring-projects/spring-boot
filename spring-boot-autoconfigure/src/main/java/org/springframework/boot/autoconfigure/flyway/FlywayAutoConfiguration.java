/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.io.ResourceLoader;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Flyway database migrations.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @author Jacques-Etienne Beaudet
 * @since 1.1.0
 */
@Configuration
@ConditionalOnClass(Flyway.class)
@ConditionalOnBean(DataSource.class)
@ConditionalOnProperty(prefix = "flyway", name = "enabled", matchIfMissing = true)
@AutoConfigureAfter({ DataSourceAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class })
public class FlywayAutoConfiguration {

	@Bean
	@ConfigurationPropertiesBinding
	public StringOrNumberToMigrationVersionConverter stringOrNumberMigrationVersionConverter() {
		return new StringOrNumberToMigrationVersionConverter();
	}

	@Configuration
	@ConditionalOnMissingBean(Flyway.class)
	@EnableConfigurationProperties(FlywayProperties.class)
	public static class FlywayConfiguration {

		private final FlywayProperties properties;

		private final ResourceLoader resourceLoader;

		private final DataSource dataSource;

		private final DataSource flywayDataSource;

		private final FlywayMigrationStrategy migrationStrategy;

		public FlywayConfiguration(FlywayProperties properties,
				ResourceLoader resourceLoader,
				ObjectProvider<DataSource> dataSourceProvider,
				@FlywayDataSource ObjectProvider<DataSource> flywayDataSourceProvider,
				ObjectProvider<FlywayMigrationStrategy> migrationStrategyProvider) {
			this.properties = properties;
			this.resourceLoader = resourceLoader;
			this.dataSource = dataSourceProvider.getIfUnique();
			this.flywayDataSource = flywayDataSourceProvider.getIfAvailable();
			this.migrationStrategy = migrationStrategyProvider.getIfAvailable();
		}

		@PostConstruct
		public void checkLocationExists() {
			if (this.properties.isCheckLocation()) {
				Assert.state(!this.properties.getLocations().isEmpty(),
						"Migration script locations not configured");
				boolean exists = hasAtLeastOneLocation();
				Assert.state(exists,
						"Cannot find migrations location in: " + this.properties
								.getLocations()
						+ " (please add migrations or check your Flyway configuration)");
			}
		}

		private boolean hasAtLeastOneLocation() {
			for (String location : this.properties.getLocations()) {
				if (this.resourceLoader.getResource(location).exists()) {
					return true;
				}
			}
			return false;
		}

		@Bean
		@ConfigurationProperties(prefix = "flyway")
		public Flyway flyway() {
			Flyway flyway = new Flyway();
			if (this.properties.isCreateDataSource()) {
				flyway.setDataSource(this.properties.getUrl(), this.properties.getUser(),
						this.properties.getPassword(),
						this.properties.getInitSqls().toArray(new String[0]));
			}
			else if (this.flywayDataSource != null) {
				flyway.setDataSource(this.flywayDataSource);
			}
			else {
				flyway.setDataSource(this.dataSource);
			}
			flyway.setLocations(this.properties.getLocations().toArray(new String[0]));
			return flyway;
		}

		@Bean
		@ConditionalOnMissingBean
		public FlywayMigrationInitializer flywayInitializer(Flyway flyway) {
			return new FlywayMigrationInitializer(flyway, this.migrationStrategy);
		}

		/**
		 * Additional configuration to ensure that {@link EntityManagerFactory} beans
		 * depend-on the {@code flywayInitializer} bean.
		 */
		@Configuration
		@ConditionalOnClass(LocalContainerEntityManagerFactoryBean.class)
		@ConditionalOnBean(AbstractEntityManagerFactoryBean.class)
		protected static class FlywayInitializerJpaDependencyConfiguration
				extends EntityManagerFactoryDependsOnPostProcessor {

			public FlywayInitializerJpaDependencyConfiguration() {
				super("flywayInitializer");
			}

		}

	}

	/**
	 * Additional configuration to ensure that {@link EntityManagerFactory} beans
	 * depend-on the {@code flyway} bean.
	 */
	@Configuration
	@ConditionalOnClass(LocalContainerEntityManagerFactoryBean.class)
	@ConditionalOnBean(AbstractEntityManagerFactoryBean.class)
	protected static class FlywayJpaDependencyConfiguration
			extends EntityManagerFactoryDependsOnPostProcessor {

		public FlywayJpaDependencyConfiguration() {
			super("flyway");
		}

	}

	/**
	 * Convert a String or Number to a {@link MigrationVersion}.
	 */
	private static class StringOrNumberToMigrationVersionConverter
			implements GenericConverter {

		private static final Set<ConvertiblePair> CONVERTIBLE_TYPES;

		static {
			Set<ConvertiblePair> types = new HashSet<ConvertiblePair>(2);
			types.add(new ConvertiblePair(String.class, MigrationVersion.class));
			types.add(new ConvertiblePair(Number.class, MigrationVersion.class));
			CONVERTIBLE_TYPES = Collections.unmodifiableSet(types);
		}

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return CONVERTIBLE_TYPES;
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType,
				TypeDescriptor targetType) {
			String value = ObjectUtils.nullSafeToString(source);
			return MigrationVersion.fromVersion(value);
		}

	}

}
