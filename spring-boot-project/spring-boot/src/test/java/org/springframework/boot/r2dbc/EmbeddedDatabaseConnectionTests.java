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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import io.r2dbc.spi.ConnectionFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link EmbeddedDatabaseConnection}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
class EmbeddedDatabaseConnectionTests {

	@ParameterizedTest
	@MethodSource("urlParameters")
	void getUrlWithTestDatabase(EmbeddedDatabaseConnection connection, String expectUrl) {
		assertThat(connection.getUrl("test-database")).isEqualTo(expectUrl);
	}

	@Test
	void getReturnsH2ByDefault() {
		assertThat(EmbeddedDatabaseConnection.get(EmbeddedDatabaseConnectionTests.class.getClassLoader()))
				.isEqualTo(EmbeddedDatabaseConnection.H2);
	}

	@Test
	void getWhenH2IsNotOnTheClasspathReturnsNone() {
		assertThat(EmbeddedDatabaseConnection.get(new HidePackagesClassLoader("io.r2dbc.h2")))
				.isEqualTo(EmbeddedDatabaseConnection.NONE);
	}

	@Test
	void whenH2IsInMemoryThenIsEmbeddedReturnsTrue() {
		assertThat(EmbeddedDatabaseConnection
				.isEmbedded(ConnectionFactoryBuilder.withUrl("r2dbc:h2:mem:///" + UUID.randomUUID()).build())).isTrue();
	}

	@Test
	void whenH2IsUsingFileStorageThenIsEmbeddedReturnsFalse() {
		assertThat(EmbeddedDatabaseConnection
				.isEmbedded(ConnectionFactoryBuilder.withUrl("r2dbc:h2:file:///" + UUID.randomUUID()).build()))
						.isFalse();
	}

	@Test
	void whenPoolIsBasedByH2InMemoryThenIsEmbeddedReturnsTrue() {
		assertThat(EmbeddedDatabaseConnection
				.isEmbedded(ConnectionFactoryBuilder.withUrl("r2dbc:pool:h2:mem:///" + UUID.randomUUID()).build()))
						.isTrue();
	}

	@Test
	void whenPoolIsBasedByH2WithFileStorageThenIsEmbeddedReturnsFalse() {
		assertThat(EmbeddedDatabaseConnection
				.isEmbedded(ConnectionFactoryBuilder.withUrl("r2dbc:pool:h2:file:///" + UUID.randomUUID()).build()))
						.isFalse();
	}

	@Test
	void whenConnectionFactoryIsNotOptionsCapableThenIsEmbeddedThrows() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> EmbeddedDatabaseConnection
						.isEmbedded(ConnectionFactories.get("r2dbc:pool:h2:mem:///" + UUID.randomUUID())))
				.withMessage("Cannot determine database's type as ConnectionFactory is not options-capable");
	}

	static Stream<Arguments> urlParameters() {
		return Stream.of(Arguments.arguments(EmbeddedDatabaseConnection.NONE, null),
				Arguments.arguments(EmbeddedDatabaseConnection.H2,
						"r2dbc:h2:mem:///test-database?options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"));
	}

	private static class HidePackagesClassLoader extends URLClassLoader {

		private final String[] hiddenPackages;

		HidePackagesClassLoader(String... hiddenPackages) {
			super(new URL[0], EmbeddedDatabaseConnectionTests.HidePackagesClassLoader.class.getClassLoader());
			this.hiddenPackages = hiddenPackages;
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			if (Arrays.stream(this.hiddenPackages).anyMatch(name::startsWith)) {
				throw new ClassNotFoundException();
			}
			return super.loadClass(name, resolve);
		}

	}

}
