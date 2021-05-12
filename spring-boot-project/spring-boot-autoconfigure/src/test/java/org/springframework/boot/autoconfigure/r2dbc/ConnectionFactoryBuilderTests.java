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

import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.r2dbc.ConnectionFactoryBuilder.ConnectionFactoryBeanCreationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for {@link ConnectionFactoryBuilder}.
 *
 * @author Mark Paluch
 * @author Tadaya Tsuyukubo
 * @author Stephane Nicoll
 */
class ConnectionFactoryBuilderTests {

	@Test
	void propertiesWithoutUrlAndNoAvailableEmbeddedConnectionShouldFail() {
		R2dbcProperties properties = new R2dbcProperties();
		assertThatThrownBy(() -> ConnectionFactoryBuilder.of(properties, () -> EmbeddedDatabaseConnection.NONE))
				.isInstanceOf(ConnectionFactoryBeanCreationException.class)
				.hasMessage("Failed to determine a suitable R2DBC Connection URL");
	}

	@Test
	void connectionFactoryBeanCreationProvidesConnectionAndProperties() {
		R2dbcProperties properties = new R2dbcProperties();
		try {
			ConnectionFactoryBuilder.of(properties, () -> EmbeddedDatabaseConnection.NONE);
			fail("Should have thrown a " + ConnectionFactoryBeanCreationException.class.getName());
		}
		catch (ConnectionFactoryBeanCreationException ex) {
			assertThat(ex.getEmbeddedDatabaseConnection()).isEqualTo(EmbeddedDatabaseConnection.NONE);
			assertThat(ex.getProperties()).isSameAs(properties);
		}
	}

	@Test
	void regularConnectionIsConfiguredAutomaticallyWithUrl() {
		R2dbcProperties properties = new R2dbcProperties();
		properties.setUrl("r2dbc:simple://:pool:");
		ConnectionFactoryOptions options = ConnectionFactoryBuilder
				.of(properties, () -> EmbeddedDatabaseConnection.NONE).buildOptions();
		assertThat(options.hasOption(ConnectionFactoryOptions.USER)).isFalse();
		assertThat(options.hasOption(ConnectionFactoryOptions.PASSWORD)).isFalse();
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.DRIVER)).isEqualTo("simple");
	}

	@Test
	void regularConnectionShouldInitializeUrlOptions() {
		R2dbcProperties properties = new R2dbcProperties();
		properties.setUrl("r2dbc:simple:proto://user:password@myhost:4711/mydatabase");
		ConnectionFactoryOptions options = buildOptions(properties);
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.DRIVER)).isEqualTo("simple");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.PROTOCOL)).isEqualTo("proto");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.USER)).isEqualTo("user");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.PASSWORD)).isEqualTo("password");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.HOST)).isEqualTo("myhost");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.PORT)).isEqualTo(4711);
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.DATABASE)).isEqualTo("mydatabase");
	}

	@Test
	void regularConnectionShouldUseUrlOptionsOverProperties() {
		R2dbcProperties properties = new R2dbcProperties();
		properties.setUrl("r2dbc:simple://user:password@myhost/mydatabase");
		properties.setUsername("another-user");
		properties.setPassword("another-password");
		properties.setName("another-database");
		ConnectionFactoryOptions options = buildOptions(properties);
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.USER)).isEqualTo("user");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.PASSWORD)).isEqualTo("password");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.DATABASE)).isEqualTo("mydatabase");
	}

	@Test
	void regularConnectionShouldUseDatabaseNameOverRandomName() {
		R2dbcProperties properties = new R2dbcProperties();
		properties.setUrl("r2dbc:simple://user:password@myhost/mydatabase");
		properties.setGenerateUniqueName(true);
		ConnectionFactoryOptions options = buildOptions(properties);
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.DATABASE)).isEqualTo("mydatabase");
	}

	@Test
	void regularConnectionWithRandomNameShouldIgnoreNameFromProperties() {
		R2dbcProperties properties = new R2dbcProperties();
		properties.setUrl("r2dbc:h2://host");
		properties.setName("test-database");
		properties.setGenerateUniqueName(true);
		ConnectionFactoryOptions options = buildOptions(properties);
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.DATABASE)).isNotEqualTo("test-database")
				.isNotEmpty();
	}

	@Test
	void regularConnectionShouldSetCustomDriverProperties() {
		R2dbcProperties properties = new R2dbcProperties();
		properties.setUrl("r2dbc:simple://user:password@myhost");
		properties.getProperties().put("simpleOne", "one");
		properties.getProperties().put("simpleTwo", "two");
		ConnectionFactoryOptions options = buildOptions(properties);
		assertThat(options.getRequiredValue(Option.<String>valueOf("simpleOne"))).isEqualTo("one");
		assertThat(options.getRequiredValue(Option.<String>valueOf("simpleTwo"))).isEqualTo("two");
	}

	@Test
	void regularConnectionShouldUseBuilderValuesOverProperties() {
		R2dbcProperties properties = new R2dbcProperties();
		properties.setUrl("r2dbc:simple://user:password@myhost:47111/mydatabase");
		properties.setUsername("user");
		properties.setPassword("password");
		ConnectionFactoryOptions options = ConnectionFactoryBuilder
				.of(properties, () -> EmbeddedDatabaseConnection.NONE).username("another-user")
				.password("another-password").hostname("another-host").port(1234).database("another-database")
				.buildOptions();
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.USER)).isEqualTo("another-user");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.PASSWORD)).isEqualTo("another-password");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.HOST)).isEqualTo("another-host");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.PORT)).isEqualTo(1234);
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.DATABASE)).isEqualTo("another-database");
	}

	@Test
	void embeddedConnectionIsConfiguredAutomaticallyWithoutUrl() {
		ConnectionFactoryOptions options = ConnectionFactoryBuilder
				.of(new R2dbcProperties(), () -> EmbeddedDatabaseConnection.H2).buildOptions();
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.USER)).isEqualTo("sa");
		assertThat(options.hasOption(ConnectionFactoryOptions.PASSWORD)).isFalse();
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.DRIVER)).isEqualTo("h2");
	}

	@Test
	void embeddedConnectionWithUsernameAndPassword() {
		R2dbcProperties properties = new R2dbcProperties();
		properties.setUsername("embedded");
		properties.setPassword("secret");
		ConnectionFactoryOptions options = ConnectionFactoryBuilder.of(properties, () -> EmbeddedDatabaseConnection.H2)
				.buildOptions();
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.USER)).isEqualTo("embedded");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.PASSWORD)).isEqualTo("secret");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.DRIVER)).isEqualTo("h2");
	}

	@Test
	void embeddedConnectionUseDefaultDatabaseName() {
		ConnectionFactoryOptions options = ConnectionFactoryBuilder
				.of(new R2dbcProperties(), () -> EmbeddedDatabaseConnection.H2).buildOptions();
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.DATABASE)).isEqualTo("testdb");
	}

	@Test
	void embeddedConnectionUseNameIfSet() {
		R2dbcProperties properties = new R2dbcProperties();
		properties.setName("test-database");
		ConnectionFactoryOptions options = buildOptions(properties);
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.DATABASE)).isEqualTo("test-database");
	}

	@Test
	void embeddedConnectionCanGenerateUniqueDatabaseName() {
		R2dbcProperties firstProperties = new R2dbcProperties();
		firstProperties.setGenerateUniqueName(true);
		ConnectionFactoryOptions options11 = buildOptions(firstProperties);
		ConnectionFactoryOptions options12 = buildOptions(firstProperties);
		assertThat(options11.getRequiredValue(ConnectionFactoryOptions.DATABASE))
				.isEqualTo(options12.getRequiredValue(ConnectionFactoryOptions.DATABASE));
		R2dbcProperties secondProperties = new R2dbcProperties();
		firstProperties.setGenerateUniqueName(true);
		ConnectionFactoryOptions options21 = buildOptions(secondProperties);
		ConnectionFactoryOptions options22 = buildOptions(secondProperties);
		assertThat(options21.getRequiredValue(ConnectionFactoryOptions.DATABASE))
				.isEqualTo(options22.getRequiredValue(ConnectionFactoryOptions.DATABASE));
		assertThat(options11.getRequiredValue(ConnectionFactoryOptions.DATABASE))
				.isNotEqualTo(options21.getRequiredValue(ConnectionFactoryOptions.DATABASE));
	}

	@Test
	void embeddedConnectionShouldIgnoreNameIfRandomNameIsRequired() {
		R2dbcProperties properties = new R2dbcProperties();
		properties.setGenerateUniqueName(true);
		properties.setName("test-database");
		ConnectionFactoryOptions options = buildOptions(properties);
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.DATABASE)).isNotEqualTo("test-database");
	}

	private ConnectionFactoryOptions buildOptions(R2dbcProperties properties) {
		return ConnectionFactoryBuilder.of(properties, () -> EmbeddedDatabaseConnection.H2).buildOptions();
	}

}
