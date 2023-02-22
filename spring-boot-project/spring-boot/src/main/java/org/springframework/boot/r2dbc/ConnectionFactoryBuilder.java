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

package org.springframework.boot.r2dbc;

import java.time.Duration;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.pool.PoolingConnectionFactoryProvider;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.ConnectionFactoryOptions.Builder;
import io.r2dbc.spi.ValidationDepth;
import org.reactivestreams.Publisher;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Builder for {@link ConnectionFactory}.
 *
 * @author Mark Paluch
 * @author Tadaya Tsuyukubo
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @since 2.5.0
 */
public final class ConnectionFactoryBuilder {

	private static final OptionsCapableWrapper optionsCapableWrapper;

	static {
		if (ClassUtils.isPresent("io.r2dbc.pool.ConnectionPool", ConnectionFactoryBuilder.class.getClassLoader())) {
			optionsCapableWrapper = new PoolingAwareOptionsCapableWrapper();
		}
		else {
			optionsCapableWrapper = new OptionsCapableWrapper();
		}
	}

	private static final String COLON = ":";

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
	 * Initialize a new {@link ConnectionFactoryBuilder} derived from the options of the
	 * specified {@code connectionFactory}.
	 * @param connectionFactory the connection factory whose options are to be used to
	 * initialize the builder
	 * @return a new builder initialized with the options from the connection factory
	 * @since 2.5.1
	 */
	public static ConnectionFactoryBuilder derivedFrom(ConnectionFactory connectionFactory) {
		ConnectionFactoryOptions options = extractOptionsIfPossible(connectionFactory);
		if (options == null) {
			throw new IllegalArgumentException(
					"ConnectionFactoryOptions could not be extracted from " + connectionFactory);
		}
		return withOptions(options.mutate());
	}

	private static ConnectionFactoryOptions extractOptionsIfPossible(ConnectionFactory connectionFactory) {
		OptionsCapableConnectionFactory optionsCapable = OptionsCapableConnectionFactory.unwrapFrom(connectionFactory);
		if (optionsCapable != null) {
			return optionsCapable.getOptions();
		}
		return null;
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
		ConnectionFactoryOptions options = buildOptions();
		return optionsCapableWrapper.buildAndWrap(options);
	}

	/**
	 * Build a {@link ConnectionFactoryOptions} based on the state of this builder.
	 * @return the options
	 */
	public ConnectionFactoryOptions buildOptions() {
		return this.optionsBuilder.build();
	}

	private static class OptionsCapableWrapper {

		ConnectionFactory buildAndWrap(ConnectionFactoryOptions options) {
			ConnectionFactory connectionFactory = ConnectionFactories.get(options);
			return new OptionsCapableConnectionFactory(options, connectionFactory);
		}

	}

	static final class PoolingAwareOptionsCapableWrapper extends OptionsCapableWrapper {

		private final PoolingConnectionFactoryProvider poolingProvider = new PoolingConnectionFactoryProvider();

		@Override
		ConnectionFactory buildAndWrap(ConnectionFactoryOptions options) {
			if (!this.poolingProvider.supports(options)) {
				return super.buildAndWrap(options);
			}
			ConnectionFactoryOptions delegateOptions = delegateFactoryOptions(options);
			ConnectionFactory connectionFactory = super.buildAndWrap(delegateOptions);
			ConnectionPoolConfiguration poolConfiguration = connectionPoolConfiguration(delegateOptions,
					connectionFactory);
			return new ConnectionPool(poolConfiguration);
		}

		private ConnectionFactoryOptions delegateFactoryOptions(ConnectionFactoryOptions options) {
			String protocol = toString(options.getRequiredValue(ConnectionFactoryOptions.PROTOCOL));
			if (protocol.trim().length() == 0) {
				throw new IllegalArgumentException(String.format("Protocol %s is not valid.", protocol));
			}
			String[] protocols = protocol.split(COLON, 2);
			String driverDelegate = protocols[0];
			String protocolDelegate = (protocols.length != 2) ? "" : protocols[1];
			return ConnectionFactoryOptions.builder()
				.from(options)
				.option(ConnectionFactoryOptions.DRIVER, driverDelegate)
				.option(ConnectionFactoryOptions.PROTOCOL, protocolDelegate)
				.build();
		}

		@SuppressWarnings("unchecked")
		ConnectionPoolConfiguration connectionPoolConfiguration(ConnectionFactoryOptions options,
				ConnectionFactory connectionFactory) {
			ConnectionPoolConfiguration.Builder builder = ConnectionPoolConfiguration.builder(connectionFactory);
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(options.getValue(PoolingConnectionFactoryProvider.BACKGROUND_EVICTION_INTERVAL))
				.as(this::toDuration)
				.to(builder::backgroundEvictionInterval);
			map.from(options.getValue(PoolingConnectionFactoryProvider.INITIAL_SIZE))
				.as(this::toInteger)
				.to(builder::initialSize);
			map.from(options.getValue(PoolingConnectionFactoryProvider.MAX_SIZE))
				.as(this::toInteger)
				.to(builder::maxSize);
			map.from(options.getValue(PoolingConnectionFactoryProvider.ACQUIRE_RETRY))
				.as(this::toInteger)
				.to(builder::acquireRetry);
			map.from(options.getValue(PoolingConnectionFactoryProvider.MAX_LIFE_TIME))
				.as(this::toDuration)
				.to(builder::maxLifeTime);
			map.from(options.getValue(PoolingConnectionFactoryProvider.MAX_ACQUIRE_TIME))
				.as(this::toDuration)
				.to(builder::maxAcquireTime);
			map.from(options.getValue(PoolingConnectionFactoryProvider.MAX_IDLE_TIME))
				.as(this::toDuration)
				.to(builder::maxIdleTime);
			map.from(options.getValue(PoolingConnectionFactoryProvider.MAX_CREATE_CONNECTION_TIME))
				.as(this::toDuration)
				.to(builder::maxCreateConnectionTime);
			map.from(options.getValue(PoolingConnectionFactoryProvider.MAX_VALIDATION_TIME))
				.as(this::toDuration)
				.to(builder::maxValidationTime);
			map.from(options.getValue(PoolingConnectionFactoryProvider.MIN_IDLE))
				.as(this::toInteger)
				.to(builder::minIdle);
			map.from(options.getValue(PoolingConnectionFactoryProvider.POOL_NAME)).as(this::toString).to(builder::name);
			map.from(options.getValue(PoolingConnectionFactoryProvider.PRE_RELEASE))
				.to((function) -> builder
					.preRelease((Function<? super Connection, ? extends Publisher<Void>>) function));
			map.from(options.getValue(PoolingConnectionFactoryProvider.POST_ALLOCATE))
				.to((function) -> builder
					.postAllocate((Function<? super Connection, ? extends Publisher<Void>>) function));
			map.from(options.getValue(PoolingConnectionFactoryProvider.REGISTER_JMX))
				.as(this::toBoolean)
				.to(builder::registerJmx);
			map.from(options.getValue(PoolingConnectionFactoryProvider.VALIDATION_QUERY))
				.as(this::toString)
				.to(builder::validationQuery);
			map.from(options.getValue(PoolingConnectionFactoryProvider.VALIDATION_DEPTH))
				.as(this::toValidationDepth)
				.to(builder::validationDepth);
			return builder.build();
		}

		private String toString(Object object) {
			return toType(String.class, object, String::valueOf);
		}

		private Integer toInteger(Object object) {
			return toType(Integer.class, object, Integer::valueOf);
		}

		private Duration toDuration(Object object) {
			return toType(Duration.class, object, Duration::parse);
		}

		private Boolean toBoolean(Object object) {
			return toType(Boolean.class, object, Boolean::valueOf);
		}

		private ValidationDepth toValidationDepth(Object object) {
			return toType(ValidationDepth.class, object,
					(string) -> ValidationDepth.valueOf(string.toUpperCase(Locale.ENGLISH)));
		}

		private <T> T toType(Class<T> type, Object object, Function<String, T> converter) {
			if (type.isInstance(object)) {
				return type.cast(object);
			}
			if (object instanceof String) {
				return converter.apply((String) object);
			}
			throw new IllegalArgumentException("Cannot convert '" + object + "' to " + type.getName());
		}

	}

}
