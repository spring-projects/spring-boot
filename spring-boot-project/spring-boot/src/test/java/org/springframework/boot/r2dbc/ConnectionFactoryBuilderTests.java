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

import java.util.UUID;

import io.r2dbc.h2.H2ConnectionFactoryMetadata;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

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

}
