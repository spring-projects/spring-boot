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

package org.springframework.boot.autoconfigure.r2dbc;

import java.util.function.Predicate;
import java.util.function.Supplier;

import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.ConnectionFactoryOptions.Builder;
import io.r2dbc.spi.Option;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.r2dbc.EmbeddedDatabaseConnection;
import org.springframework.util.StringUtils;

/**
 * Initialize a {@link ConnectionFactoryOptions.Builder} based on {@link R2dbcProperties}.
 *
 * @author Stephane Nicoll
 */
class ConnectionFactoryOptionsInitializer {

	/**
	 * Initialize a {@link io.r2dbc.spi.ConnectionFactoryOptions.Builder
	 * ConnectionFactoryOptions.Builder} using the specified properties.
	 * @param properties the properties to use to initialize the builder
	 * @param embeddedDatabaseConnection the embedded connection to use as a fallback
	 * @return an initialized builder
	 * @throws ConnectionFactoryBeanCreationException if no suitable connection could be
	 * determined
	 */
	ConnectionFactoryOptions.Builder initialize(R2dbcProperties properties,
			Supplier<EmbeddedDatabaseConnection> embeddedDatabaseConnection) {
		if (StringUtils.hasText(properties.getUrl())) {
			return initializeRegularOptions(properties);
		}
		EmbeddedDatabaseConnection embeddedConnection = embeddedDatabaseConnection.get();
		if (embeddedConnection != EmbeddedDatabaseConnection.NONE) {
			return initializeEmbeddedOptions(properties, embeddedConnection);
		}
		throw connectionFactoryBeanCreationException("Failed to determine a suitable R2DBC Connection URL", properties,
				embeddedConnection);
	}

	private ConnectionFactoryOptions.Builder initializeRegularOptions(R2dbcProperties properties) {
		ConnectionFactoryOptions urlOptions = ConnectionFactoryOptions.parse(properties.getUrl());
		Builder optionsBuilder = urlOptions.mutate();
		configureIf(optionsBuilder, urlOptions, ConnectionFactoryOptions.USER, properties::getUsername,
				StringUtils::hasText);
		configureIf(optionsBuilder, urlOptions, ConnectionFactoryOptions.PASSWORD, properties::getPassword,
				StringUtils::hasText);
		configureIf(optionsBuilder, urlOptions, ConnectionFactoryOptions.DATABASE,
				() -> determineDatabaseName(properties), StringUtils::hasText);
		if (properties.getProperties() != null) {
			properties.getProperties().forEach((key, value) -> optionsBuilder.option(Option.valueOf(key), value));
		}
		return optionsBuilder;
	}

	private Builder initializeEmbeddedOptions(R2dbcProperties properties,
			EmbeddedDatabaseConnection embeddedDatabaseConnection) {
		String url = embeddedDatabaseConnection.getUrl(determineEmbeddedDatabaseName(properties));
		if (url == null) {
			throw connectionFactoryBeanCreationException("Failed to determine a suitable R2DBC Connection URL",
					properties, embeddedDatabaseConnection);
		}
		Builder builder = ConnectionFactoryOptions.parse(url).mutate();
		String username = determineEmbeddedUsername(properties);
		if (StringUtils.hasText(username)) {
			builder.option(ConnectionFactoryOptions.USER, username);
		}
		if (StringUtils.hasText(properties.getPassword())) {
			builder.option(ConnectionFactoryOptions.PASSWORD, properties.getPassword());
		}
		return builder;
	}

	private String determineDatabaseName(R2dbcProperties properties) {
		if (properties.isGenerateUniqueName()) {
			return properties.determineUniqueName();
		}
		if (StringUtils.hasLength(properties.getName())) {
			return properties.getName();
		}
		return null;
	}

	private String determineEmbeddedDatabaseName(R2dbcProperties properties) {
		String databaseName = determineDatabaseName(properties);
		return (databaseName != null) ? databaseName : "testdb";
	}

	private String determineEmbeddedUsername(R2dbcProperties properties) {
		String username = ifHasText(properties.getUsername());
		return (username != null) ? username : "sa";
	}

	private <T extends CharSequence> void configureIf(Builder optionsBuilder, ConnectionFactoryOptions originalOptions,
			Option<T> option, Supplier<T> valueSupplier, Predicate<T> setIf) {
		if (originalOptions.hasOption(option)) {
			return;
		}
		T value = valueSupplier.get();
		if (setIf.test(value)) {
			optionsBuilder.option(option, value);
		}
	}

	private ConnectionFactoryBeanCreationException connectionFactoryBeanCreationException(String message,
			R2dbcProperties properties, EmbeddedDatabaseConnection embeddedDatabaseConnection) {
		return new ConnectionFactoryBeanCreationException(message, properties, embeddedDatabaseConnection);
	}

	private String ifHasText(String candidate) {
		return (StringUtils.hasText(candidate)) ? candidate : null;
	}

	static class ConnectionFactoryBeanCreationException extends BeanCreationException {

		private final R2dbcProperties properties;

		private final EmbeddedDatabaseConnection embeddedDatabaseConnection;

		ConnectionFactoryBeanCreationException(String message, R2dbcProperties properties,
				EmbeddedDatabaseConnection embeddedDatabaseConnection) {
			super(message);
			this.properties = properties;
			this.embeddedDatabaseConnection = embeddedDatabaseConnection;
		}

		EmbeddedDatabaseConnection getEmbeddedDatabaseConnection() {
			return this.embeddedDatabaseConnection;
		}

		R2dbcProperties getProperties() {
			return this.properties;
		}

	}

}
