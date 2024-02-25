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

package org.springframework.boot.autoconfigure.r2dbc;

import java.util.function.Predicate;
import java.util.function.Supplier;

import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.ConnectionFactoryOptions.Builder;
import io.r2dbc.spi.Option;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for R2DBC.
 *
 * @author Mark Paluch
 * @author Stephane Nicoll
 * @since 2.3.0
 */
@AutoConfiguration(before = { DataSourceAutoConfiguration.class, SqlInitializationAutoConfiguration.class })
@ConditionalOnClass(ConnectionFactory.class)
@ConditionalOnResource(resources = "classpath:META-INF/services/io.r2dbc.spi.ConnectionFactoryProvider")
@EnableConfigurationProperties(R2dbcProperties.class)
@Import({ ConnectionFactoryConfigurations.PoolConfiguration.class,
		ConnectionFactoryConfigurations.GenericConfiguration.class, ConnectionFactoryDependentConfiguration.class })
public class R2dbcAutoConfiguration {

	/**
	 * Creates a new instance of {@link PropertiesR2dbcConnectionDetails} if no bean of
	 * type {@link R2dbcConnectionDetails} is present in the application context and if
	 * the property "spring.r2dbc.url" is defined.
	 * @param properties the {@link R2dbcProperties} object containing the R2DBC
	 * configuration properties
	 * @return a new instance of {@link PropertiesR2dbcConnectionDetails} initialized with
	 * the provided {@link R2dbcProperties}
	 */
	@Bean
	@ConditionalOnMissingBean(R2dbcConnectionDetails.class)
	@ConditionalOnProperty("spring.r2dbc.url")
	PropertiesR2dbcConnectionDetails propertiesR2dbcConnectionDetails(R2dbcProperties properties) {
		return new PropertiesR2dbcConnectionDetails(properties);
	}

	/**
	 * Adapts {@link R2dbcProperties} to {@link R2dbcConnectionDetails}.
	 */
	static class PropertiesR2dbcConnectionDetails implements R2dbcConnectionDetails {

		private final R2dbcProperties properties;

		/**
		 * Constructs a new instance of PropertiesR2dbcConnectionDetails with the
		 * specified R2dbcProperties.
		 * @param properties the R2dbcProperties to be used for initializing the
		 * connection details
		 */
		PropertiesR2dbcConnectionDetails(R2dbcProperties properties) {
			this.properties = properties;
		}

		/**
		 * Returns the connection factory options based on the properties provided.
		 * @return the connection factory options
		 */
		@Override
		public ConnectionFactoryOptions getConnectionFactoryOptions() {
			ConnectionFactoryOptions urlOptions = ConnectionFactoryOptions.parse(this.properties.getUrl());
			Builder optionsBuilder = urlOptions.mutate();
			configureIf(optionsBuilder, urlOptions, ConnectionFactoryOptions.USER, this.properties::getUsername,
					StringUtils::hasText);
			configureIf(optionsBuilder, urlOptions, ConnectionFactoryOptions.PASSWORD, this.properties::getPassword,
					StringUtils::hasText);
			configureIf(optionsBuilder, urlOptions, ConnectionFactoryOptions.DATABASE,
					() -> determineDatabaseName(this.properties), StringUtils::hasText);
			if (this.properties.getProperties() != null) {
				this.properties.getProperties()
					.forEach((key, value) -> optionsBuilder.option(Option.valueOf(key), value));
			}
			return optionsBuilder.build();
		}

		/**
		 * Configures the optionsBuilder with the specified option if the originalOptions
		 * does not already have the option.
		 * @param optionsBuilder The builder to configure with the option.
		 * @param originalOptions The original options to check if the option already
		 * exists.
		 * @param option The option to configure.
		 * @param valueSupplier The supplier to provide the value for the option.
		 * @param setIf The predicate to determine if the value should be set for the
		 * option.
		 * @param <T> The type of the option value, which must extend CharSequence.
		 */
		private <T extends CharSequence> void configureIf(Builder optionsBuilder,
				ConnectionFactoryOptions originalOptions, Option<T> option, Supplier<T> valueSupplier,
				Predicate<T> setIf) {
			if (originalOptions.hasOption(option)) {
				return;
			}
			T value = valueSupplier.get();
			if (setIf.test(value)) {
				optionsBuilder.option(option, value);
			}
		}

		/**
		 * Determines the database name based on the provided R2dbcProperties. If the
		 * generateUniqueName flag is set to true, it generates a unique name using the
		 * determineUniqueName method. If a name is provided in the properties, it returns
		 * that name. If neither condition is met, it returns null.
		 * @param properties The R2dbcProperties object containing the necessary
		 * information.
		 * @return The determined database name or null if no name is determined.
		 */
		private String determineDatabaseName(R2dbcProperties properties) {
			if (properties.isGenerateUniqueName()) {
				return properties.determineUniqueName();
			}
			if (StringUtils.hasLength(properties.getName())) {
				return properties.getName();
			}
			return null;
		}

	}

}
