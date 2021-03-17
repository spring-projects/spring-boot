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

package org.springframework.boot.r2dbc;

import java.util.function.Consumer;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.ConnectionFactoryOptions.Builder;

import org.springframework.util.Assert;

/**
 * Builder for {@link ConnectionFactory}.
 *
 * @author Mark Paluch
 * @author Tadaya Tsuyukubo
 * @author Stephane Nicoll
 * @since 2.5.0
 */
public final class ConnectionFactoryBuilder {

	private final Builder optionsBuilder;

	private ConnectionFactoryBuilder(Builder optionsBuilder) {
		this.optionsBuilder = optionsBuilder;
	}

	/**
	 * Initialize a new {@link ConnectionFactoryBuilder} based on the specified R2DBC url.
	 * @param url the url to use
	 * @return a new builder initialized with the options exposed in the specified url
	 * @see EmbeddedDatabaseConnection#getUrl(String)
	 */
	public static ConnectionFactoryBuilder withUrl(String url) {
		Assert.hasText(url, () -> "Url must not be null");
		return withOptions(ConnectionFactoryOptions.parse(url).mutate());
	}

	/**
	 * Initialize a new {@link ConnectionFactoryBuilder} based on the specified
	 * {@link Builder options}.
	 * @param options the options to use to initialize the builder
	 * @return a new builder initialized with the settings defined in the given
	 * {@link Builder options}
	 */
	public static ConnectionFactoryBuilder withOptions(Builder options) {
		return new ConnectionFactoryBuilder(options);
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

}
