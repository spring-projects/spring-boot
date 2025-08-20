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

package org.springframework.boot.r2dbc.autoconfigure;

import java.util.function.Supplier;

import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.ConnectionFactoryOptions.Builder;
import io.r2dbc.spi.Option;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for R2DBC.
 *
 * @author Mark Paluch
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(before = { R2dbcInitializationAutoConfiguration.class },
		beforeName = "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration")
@ConditionalOnClass(ConnectionFactory.class)
@ConditionalOnResource(resources = "classpath:META-INF/services/io.r2dbc.spi.ConnectionFactoryProvider")
@EnableConfigurationProperties(R2dbcProperties.class)
@Import({ ConnectionFactoryConfigurations.PoolConfiguration.class,
		ConnectionFactoryConfigurations.GenericConfiguration.class, ConnectionFactoryDependentConfiguration.class })
public final class R2dbcAutoConfiguration {

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

		PropertiesR2dbcConnectionDetails(R2dbcProperties properties) {
			this.properties = properties;
		}

		@Override
		public ConnectionFactoryOptions getConnectionFactoryOptions() {
			String url = this.properties.getUrl();
			Assert.state(url != null, "'url' must not be null");
			ConnectionFactoryOptions urlOptions = ConnectionFactoryOptions.parse(url);
			Builder optionsBuilder = urlOptions.mutate();
			configureUser(optionsBuilder, urlOptions);
			configurePassword(optionsBuilder, urlOptions);
			configureDatabase(optionsBuilder, urlOptions);
			this.properties.getProperties().forEach((key, value) -> optionsBuilder.option(Option.valueOf(key), value));
			return optionsBuilder.build();
		}

		// Lambda isn't detected with the correct nullability
		@SuppressWarnings("NullAway")
		private void configureDatabase(Builder optionsBuilder, ConnectionFactoryOptions urlOptions) {
			configureIf(optionsBuilder, urlOptions, ConnectionFactoryOptions.DATABASE,
					() -> determineDatabaseName(this.properties));
		}

		// Lambda isn't detected with the correct nullability
		@SuppressWarnings("NullAway")
		private void configurePassword(Builder optionsBuilder, ConnectionFactoryOptions urlOptions) {
			configureIf(optionsBuilder, urlOptions, ConnectionFactoryOptions.PASSWORD, this.properties::getPassword);
		}

		// Lambda isn't detected with the correct nullability
		@SuppressWarnings("NullAway")
		private void configureUser(Builder optionsBuilder, ConnectionFactoryOptions urlOptions) {
			configureIf(optionsBuilder, urlOptions, ConnectionFactoryOptions.USER, this.properties::getUsername);
		}

		private <T extends CharSequence> void configureIf(Builder optionsBuilder,
				ConnectionFactoryOptions originalOptions, Option<T> option, Supplier<@Nullable T> valueSupplier) {
			if (originalOptions.hasOption(option)) {
				return;
			}
			T value = valueSupplier.get();
			if (StringUtils.hasText(value)) {
				optionsBuilder.option(option, value);
			}
		}

		private @Nullable String determineDatabaseName(R2dbcProperties properties) {
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
