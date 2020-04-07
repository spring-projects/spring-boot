/*
 * Copyright 2012-2020 the original author or authors.
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

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.ConnectionFactoryOptions.Builder;
import io.r2dbc.spi.Option;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.util.StringUtils;

/**
 * Builder for {@link ConnectionFactory}.
 *
 * @author Mark Paluch
 * @author Tadaya Tsuyukubo
 * @author Stephane Nicoll
 * @since 2.3.0
 */
public final class ConnectionFactoryBuilder {

	private final ConnectionFactoryOptions.Builder optionsBuilder;

	private ConnectionFactoryBuilder(ConnectionFactoryOptions.Builder optionsBuilder) {
		this.optionsBuilder = optionsBuilder;
	}

	/**
	 * Initialize a new {@link ConnectionFactoryBuilder} based on the specified
	 * {@link R2dbcProperties}. If no url is specified, the
	 * {@link EmbeddedDatabaseConnection} supplier is invoked to determine if an embedded
	 * database can be configured instead.
	 * @param properties the properties to use to initialize the builder
	 * @param embeddedDatabaseConnection a supplier for an
	 * {@link EmbeddedDatabaseConnection}
	 * @return a new builder initialized with the settings defined in
	 * {@link R2dbcProperties}
	 */
	public static ConnectionFactoryBuilder of(R2dbcProperties properties,
			Supplier<EmbeddedDatabaseConnection> embeddedDatabaseConnection) {
		return new ConnectionFactoryBuilder(
				new ConnectionFactoryOptionsInitializer().initializeOptions(properties, embeddedDatabaseConnection));
	}

	/**
	 * Configure additional options.
	 * @param options a {@link Consumer} to customize the options
	 * @return this for method chaining
	 */
	public ConnectionFactoryBuilder configure(Consumer<Builder> options) {
		options.accept(this.optionsBuilder);
		return this;
	}

	/**
	 * Configure the {@linkplain ConnectionFactoryOptions#USER username}.
	 * @param username the connection factory username
	 * @return this for method chaining
	 */
	public ConnectionFactoryBuilder username(String username) {
		return configure((options) -> options.option(ConnectionFactoryOptions.USER, username));
	}

	/**
	 * Configure the {@linkplain ConnectionFactoryOptions#PASSWORD password}.
	 * @param password the connection factory password
	 * @return this for method chaining
	 */
	public ConnectionFactoryBuilder password(CharSequence password) {
		return configure((options) -> options.option(ConnectionFactoryOptions.PASSWORD, password));
	}

	/**
	 * Configure the {@linkplain ConnectionFactoryOptions#HOST host name}.
	 * @param host the connection factory hostname
	 * @return this for method chaining
	 */
	public ConnectionFactoryBuilder hostname(String host) {
		return configure((options) -> options.option(ConnectionFactoryOptions.HOST, host));
	}

	/**
	 * Configure the {@linkplain ConnectionFactoryOptions#PORT port}.
	 * @param port the connection factory port
	 * @return this for method chaining
	 */
	public ConnectionFactoryBuilder port(int port) {
		return configure((options) -> options.option(ConnectionFactoryOptions.PORT, port));
	}

	/**
	 * Configure the {@linkplain ConnectionFactoryOptions#DATABASE database}.
	 * @param database the connection factory database
	 * @return this for method chaining
	 */
	public ConnectionFactoryBuilder database(String database) {
		return configure((options) -> options.option(ConnectionFactoryOptions.DATABASE, database));
	}

	/**
	 * Build a {@link ConnectionFactory} based on the state of this builder.
	 * @return a connection factory
	 */
	public ConnectionFactory build() {
		return ConnectionFactories.get(buildOptions());
	}

	/**
	 * Build a {@link ConnectionFactoryOptions} based on the state of this builder.
	 * @return the options
	 */
	public ConnectionFactoryOptions buildOptions() {
		return this.optionsBuilder.build();
	}

	static class ConnectionFactoryOptionsInitializer {

		/**
		 * Initialize a {@link io.r2dbc.spi.ConnectionFactoryOptions.Builder
		 * ConnectionFactoryOptions.Builder} using the specified properties.
		 * @param properties the properties to use to initialize the builder
		 * @param embeddedDatabaseConnection the embedded connection to use as a fallback
		 * @return an initialized builder
		 * @throws ConnectionFactoryBeanCreationException if no suitable connection could
		 * be determined
		 */
		ConnectionFactoryOptions.Builder initializeOptions(R2dbcProperties properties,
				Supplier<EmbeddedDatabaseConnection> embeddedDatabaseConnection) {
			if (StringUtils.hasText(properties.getUrl())) {
				return initializeRegularOptions(properties);
			}
			EmbeddedDatabaseConnection embeddedConnection = embeddedDatabaseConnection.get();
			if (embeddedConnection != EmbeddedDatabaseConnection.NONE) {
				return initializeEmbeddedOptions(properties, embeddedConnection);
			}
			throw connectionFactoryBeanCreationException("Failed to determine a suitable R2DBC Connection URL",
					properties, embeddedConnection);
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

		private ConnectionFactoryOptions.Builder initializeEmbeddedOptions(R2dbcProperties properties,
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

		private ConnectionFactoryBeanCreationException connectionFactoryBeanCreationException(String message,
				R2dbcProperties properties, EmbeddedDatabaseConnection embeddedDatabaseConnection) {
			return new ConnectionFactoryBeanCreationException(message, properties, embeddedDatabaseConnection);
		}

		private String ifHasText(String candidate) {
			return (StringUtils.hasText(candidate)) ? candidate : null;
		}

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
