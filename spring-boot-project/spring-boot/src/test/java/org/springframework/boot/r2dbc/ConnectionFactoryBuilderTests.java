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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.r2dbc.h2.H2ConnectionFactoryMetadata;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.pool.PoolingConnectionFactoryProvider;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;
import io.r2dbc.spi.ValidationDepth;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.boot.r2dbc.ConnectionFactoryBuilder.PoolingAwareOptionsCapableWrapper;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConnectionFactoryBuilder}.
 *
 * @author Mark Paluch
 * @author Tadaya Tsuyukubo
 * @author Stephane Nicoll
 */
class ConnectionFactoryBuilderTests {

	@Test
	void createWithNullUrlShouldFail() {
		assertThatIllegalArgumentException().isThrownBy(() -> ConnectionFactoryBuilder.withUrl(null));
	}

	@Test
	void createWithEmptyUrlShouldFail() {
		assertThatIllegalArgumentException().isThrownBy(() -> ConnectionFactoryBuilder.withUrl("  "));
	}

	@Test
	void createWithEmbeddedConnectionNoneShouldFail() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> ConnectionFactoryBuilder.withUrl(EmbeddedDatabaseConnection.NONE.getUrl("test")));
	}

	@Test
	void buildOptionsWithBasicUrlShouldExposeOptions() {
		ConnectionFactoryOptions options = ConnectionFactoryBuilder.withUrl("r2dbc:simple://:pool:").buildOptions();
		assertThat(options.hasOption(ConnectionFactoryOptions.USER)).isFalse();
		assertThat(options.hasOption(ConnectionFactoryOptions.PASSWORD)).isFalse();
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.DRIVER)).isEqualTo("simple");
	}

	@Test
	void buildOptionsWithEmbeddedConnectionH2ShouldExposeOptions() {
		ConnectionFactoryOptions options = ConnectionFactoryBuilder
				.withUrl(EmbeddedDatabaseConnection.H2.getUrl("testdb")).buildOptions();
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.DRIVER)).isEqualTo("h2");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.PROTOCOL)).isEqualTo("mem");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.DATABASE)).isEqualTo("testdb");
		assertThat(options.hasOption(ConnectionFactoryOptions.HOST)).isFalse();
		assertThat(options.hasOption(ConnectionFactoryOptions.PORT)).isFalse();
		assertThat(options.hasOption(ConnectionFactoryOptions.USER)).isFalse();
		assertThat(options.hasOption(ConnectionFactoryOptions.PASSWORD)).isFalse();
		assertThat(options.getValue(Option.<String>valueOf("options")))
				.isEqualTo("DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
	}

	@Test
	void buildOptionsWithCompleteUrlShouldExposeOptions() {
		ConnectionFactoryOptions options = ConnectionFactoryBuilder
				.withUrl("r2dbc:simple:proto://user:password@myhost:4711/mydatabase").buildOptions();
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.DRIVER)).isEqualTo("simple");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.PROTOCOL)).isEqualTo("proto");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.USER)).isEqualTo("user");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.PASSWORD)).isEqualTo("password");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.HOST)).isEqualTo("myhost");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.PORT)).isEqualTo(4711);
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.DATABASE)).isEqualTo("mydatabase");
	}

	@Test
	void buildOptionsWithSpecificSettingsShouldOverrideUrlOptions() {
		ConnectionFactoryOptions options = ConnectionFactoryBuilder
				.withUrl("r2dbc:simple://user:password@myhost/mydatabase").username("another-user")
				.password("another-password").hostname("another-host").port(1234).database("another-database")
				.buildOptions();
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.USER)).isEqualTo("another-user");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.PASSWORD)).isEqualTo("another-password");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.HOST)).isEqualTo("another-host");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.PORT)).isEqualTo(1234);
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.DATABASE)).isEqualTo("another-database");
	}

	@Test
	void buildOptionsWithDriverPropertiesShouldExposeOptions() {
		ConnectionFactoryOptions options = ConnectionFactoryBuilder.withUrl("r2dbc:simple://user:password@myhost")
				.configure(
						(o) -> o.option(Option.valueOf("simpleOne"), "one").option(Option.valueOf("simpleTwo"), "two"))
				.buildOptions();
		assertThat(options.getRequiredValue(Option.<String>valueOf("simpleOne"))).isEqualTo("one");
		assertThat(options.getRequiredValue(Option.<String>valueOf("simpleTwo"))).isEqualTo("two");
	}

	@Test
	void buildShouldExposeConnectionFactory() {
		String databaseName = UUID.randomUUID().toString();
		ConnectionFactory connectionFactory = ConnectionFactoryBuilder
				.withUrl(EmbeddedDatabaseConnection.H2.getUrl(databaseName)).build();
		assertThat(connectionFactory).isNotNull();
		assertThat(connectionFactory.getMetadata().getName()).isEqualTo(H2ConnectionFactoryMetadata.NAME);
	}

	@Test
	void buildWhenDerivedWithNewDatabaseReturnsNewConnectionFactory() {
		String intialDatabaseName = UUID.randomUUID().toString();
		ConnectionFactory connectionFactory = ConnectionFactoryBuilder
				.withUrl(EmbeddedDatabaseConnection.H2.getUrl(intialDatabaseName)).build();
		ConnectionFactoryOptions initialOptions = ((OptionsCapableConnectionFactory) connectionFactory).getOptions();
		String derivedDatabaseName = UUID.randomUUID().toString();
		ConnectionFactory derived = ConnectionFactoryBuilder.derivedFrom(connectionFactory)
				.database(derivedDatabaseName).build();
		ConnectionFactoryOptions derivedOptions = ((OptionsCapableConnectionFactory) derived).getOptions();
		assertThat(derivedOptions.getRequiredValue(ConnectionFactoryOptions.DATABASE)).isEqualTo(derivedDatabaseName);
		assertMatchingOptions(derivedOptions, initialOptions, ConnectionFactoryOptions.CONNECT_TIMEOUT,
				ConnectionFactoryOptions.DRIVER, ConnectionFactoryOptions.HOST, ConnectionFactoryOptions.PASSWORD,
				ConnectionFactoryOptions.PORT, ConnectionFactoryOptions.PROTOCOL, ConnectionFactoryOptions.SSL,
				ConnectionFactoryOptions.USER);
	}

	@Test
	void buildWhenDerivedWithNewCredentialsReturnsNewConnectionFactory() {
		ConnectionFactory connectionFactory = ConnectionFactoryBuilder
				.withUrl(EmbeddedDatabaseConnection.H2.getUrl(UUID.randomUUID().toString())).build();
		ConnectionFactoryOptions initialOptions = ((OptionsCapableConnectionFactory) connectionFactory).getOptions();
		ConnectionFactory derived = ConnectionFactoryBuilder.derivedFrom(connectionFactory).username("admin")
				.password("secret").build();
		ConnectionFactoryOptions derivedOptions = ((OptionsCapableConnectionFactory) derived).getOptions();
		assertThat(derivedOptions.getRequiredValue(ConnectionFactoryOptions.USER)).isEqualTo("admin");
		assertThat(derivedOptions.getRequiredValue(ConnectionFactoryOptions.PASSWORD)).isEqualTo("secret");
		assertMatchingOptions(derivedOptions, initialOptions, ConnectionFactoryOptions.CONNECT_TIMEOUT,
				ConnectionFactoryOptions.DATABASE, ConnectionFactoryOptions.DRIVER, ConnectionFactoryOptions.HOST,
				ConnectionFactoryOptions.PORT, ConnectionFactoryOptions.PROTOCOL, ConnectionFactoryOptions.SSL);
	}

	@Test
	void buildWhenDerivedFromPoolReturnsNewNonPooledConnectionFactory() {
		ConnectionFactory connectionFactory = ConnectionFactoryBuilder
				.withUrl(EmbeddedDatabaseConnection.H2.getUrl(UUID.randomUUID().toString())).build();
		ConnectionFactoryOptions initialOptions = ((OptionsCapableConnectionFactory) connectionFactory).getOptions();
		ConnectionPoolConfiguration poolConfiguration = ConnectionPoolConfiguration.builder(connectionFactory).build();
		ConnectionPool pool = new ConnectionPool(poolConfiguration);
		ConnectionFactory derived = ConnectionFactoryBuilder.derivedFrom(pool).username("admin").password("secret")
				.build();
		assertThat(derived).isNotInstanceOf(ConnectionPool.class).isInstanceOf(OptionsCapableConnectionFactory.class);
		ConnectionFactoryOptions derivedOptions = ((OptionsCapableConnectionFactory) derived).getOptions();
		assertThat(derivedOptions.getRequiredValue(ConnectionFactoryOptions.USER)).isEqualTo("admin");
		assertThat(derivedOptions.getRequiredValue(ConnectionFactoryOptions.PASSWORD)).isEqualTo("secret");
		assertMatchingOptions(derivedOptions, initialOptions, ConnectionFactoryOptions.CONNECT_TIMEOUT,
				ConnectionFactoryOptions.DATABASE, ConnectionFactoryOptions.DRIVER, ConnectionFactoryOptions.HOST,
				ConnectionFactoryOptions.PORT, ConnectionFactoryOptions.PROTOCOL, ConnectionFactoryOptions.SSL);
	}

	@ParameterizedTest
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@MethodSource("poolingConnectionProviderOptions")
	void optionIsMappedWhenCreatingPoolConfiguration(Option option) {
		String url = "r2dbc:pool:h2:mem:///" + UUID.randomUUID();
		ExpectedOption expectedOption = ExpectedOption.get(option);
		ConnectionFactoryOptions options = ConnectionFactoryBuilder.withUrl(url).configure((builder) -> builder
				.option(PoolingConnectionFactoryProvider.POOL_NAME, "defaultName").option(option, expectedOption.value))
				.buildOptions();
		ConnectionPoolConfiguration configuration = new PoolingAwareOptionsCapableWrapper()
				.connectionPoolConfiguration(options, mock(ConnectionFactory.class));
		assertThat(configuration).extracting(expectedOption.property).isEqualTo(expectedOption.value);
	}

	@ParameterizedTest
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@MethodSource("poolingConnectionProviderOptions")
	void stringlyTypedOptionIsMappedWhenCreatingPoolConfiguration(Option option) {
		String url = "r2dbc:pool:h2:mem:///" + UUID.randomUUID();
		ExpectedOption expectedOption = ExpectedOption.get(option);
		ConnectionFactoryOptions options = ConnectionFactoryBuilder.withUrl(url)
				.configure((builder) -> builder.option(PoolingConnectionFactoryProvider.POOL_NAME, "defaultName")
						.option(option, expectedOption.value.toString()))
				.buildOptions();
		ConnectionPoolConfiguration configuration = new PoolingAwareOptionsCapableWrapper()
				.connectionPoolConfiguration(options, mock(ConnectionFactory.class));
		assertThat(configuration).extracting(expectedOption.property).isEqualTo(expectedOption.value);
	}

	private static Iterable<Arguments> poolingConnectionProviderOptions() {
		List<Arguments> arguments = new ArrayList<>();
		ReflectionUtils.doWithFields(PoolingConnectionFactoryProvider.class,
				(field) -> arguments.add(Arguments.of(ReflectionUtils.getField(field, null))),
				(field) -> Option.class.equals(field.getType()));
		return arguments;
	}

	private void assertMatchingOptions(ConnectionFactoryOptions actualOptions, ConnectionFactoryOptions expectedOptions,
			Option<?>... optionsToCheck) {
		for (Option<?> option : optionsToCheck) {
			assertThat(actualOptions.getValue(option)).as(option.name()).isEqualTo(expectedOptions.getValue(option));
		}
	}

	private enum ExpectedOption {

		ACQUIRE_RETRY(PoolingConnectionFactoryProvider.ACQUIRE_RETRY, 4, "acquireRetry"),

		BACKGROUND_EVICTION_INTERVAL(PoolingConnectionFactoryProvider.BACKGROUND_EVICTION_INTERVAL,
				Duration.ofSeconds(120), "backgroundEvictionInterval"),

		INITIAL_SIZE(PoolingConnectionFactoryProvider.INITIAL_SIZE, 2, "initialSize"),

		MAX_SIZE(PoolingConnectionFactoryProvider.MAX_SIZE, 8, "maxSize"),

		MAX_LIFE_TIME(PoolingConnectionFactoryProvider.MAX_LIFE_TIME, Duration.ofMinutes(2), "maxLifeTime"),

		MAX_ACQUIRE_TIME(PoolingConnectionFactoryProvider.MAX_ACQUIRE_TIME, Duration.ofSeconds(30), "maxAcquireTime"),

		MAX_IDLE_TIME(PoolingConnectionFactoryProvider.MAX_IDLE_TIME, Duration.ofMinutes(1), "maxIdleTime"),

		MAX_CREATE_CONNECTION_TIME(PoolingConnectionFactoryProvider.MAX_CREATE_CONNECTION_TIME, Duration.ofSeconds(10),
				"maxCreateConnectionTime"),

		POOL_NAME(PoolingConnectionFactoryProvider.POOL_NAME, "testPool", "name"),

		REGISTER_JMX(PoolingConnectionFactoryProvider.REGISTER_JMX, true, "registerJmx"),

		VALIDATION_QUERY(PoolingConnectionFactoryProvider.VALIDATION_QUERY, "SELECT 1", "validationQuery"),

		VALIDATION_DEPTH(PoolingConnectionFactoryProvider.VALIDATION_DEPTH, ValidationDepth.REMOTE, "validationDepth");

		private final Option<?> option;

		private final Object value;

		private final String property;

		ExpectedOption(Option<?> option, Object value, String property) {
			this.option = option;
			this.value = value;
			this.property = property;
		}

		static ExpectedOption get(Option<?> option) {
			for (ExpectedOption expectedOption : values()) {
				if (expectedOption.option == option) {
					return expectedOption;
				}
			}
			throw new IllegalArgumentException("Unexpected option: '" + option + "'");
		}

	}

}
