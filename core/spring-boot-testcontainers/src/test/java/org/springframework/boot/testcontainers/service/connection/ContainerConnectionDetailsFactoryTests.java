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

package org.springframework.boot.testcontainers.service.connection;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactory;
import org.springframework.boot.origin.Origin;
import org.springframework.core.annotation.MergedAnnotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ContainerConnectionDetailsFactory}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ContainerConnectionDetailsFactoryTests {

	private String beanNameSuffix;

	private Origin origin;

	private PostgreSQLContainer container;

	private MergedAnnotation<ServiceConnection> annotation;

	private ContainerConnectionSource<?> source;

	@BeforeEach
	void setup() {
		this.beanNameSuffix = "MyBean";
		this.origin = mock(Origin.class);
		this.container = mock(PostgreSQLContainer.class);
		this.annotation = MergedAnnotation.of(ServiceConnection.class,
				Map.of("name", "myname", "type", new Class<?>[0]));
		this.source = new ContainerConnectionSource<>(this.beanNameSuffix, this.origin, PostgreSQLContainer.class,
				this.container.getDockerImageName(), this.annotation, () -> this.container, null, null);
	}

	@Test
	void getConnectionDetailsWhenTypesMatchAndNoNameRestrictionReturnsDetails() {
		TestContainerConnectionDetailsFactory factory = new TestContainerConnectionDetailsFactory();
		ConnectionDetails connectionDetails = getConnectionDetails(factory, this.source);
		assertThat(connectionDetails).isNotNull();
	}

	@Test
	void getConnectionDetailsWhenTypesMatchAndNameRestrictionMatchesReturnsDetails() {
		TestContainerConnectionDetailsFactory factory = new TestContainerConnectionDetailsFactory("myname");
		ConnectionDetails connectionDetails = getConnectionDetails(factory, this.source);
		assertThat(connectionDetails).isNotNull();
	}

	@Test
	void getConnectionDetailsWhenTypesMatchAndNameRestrictionsMatchReturnsDetails() {
		TestContainerConnectionDetailsFactory factory = new TestContainerConnectionDetailsFactory(
				List.of("notmyname", "myname"));
		ConnectionDetails connectionDetails = getConnectionDetails(factory, this.source);
		assertThat(connectionDetails).isNotNull();
	}

	@Test
	void getConnectionDetailsWhenTypesMatchAndNameRestrictionDoesNotMatchReturnsNull() {
		TestContainerConnectionDetailsFactory factory = new TestContainerConnectionDetailsFactory("notmyname");
		ConnectionDetails connectionDetails = getConnectionDetails(factory, this.source);
		assertThat(connectionDetails).isNull();
	}

	@Test
	void getConnectionDetailsWhenTypesMatchAndNameRestrictionsDoNotMatchReturnsNull() {
		TestContainerConnectionDetailsFactory factory = new TestContainerConnectionDetailsFactory(
				List.of("notmyname", "alsonotmyname"));
		ConnectionDetails connectionDetails = getConnectionDetails(factory, this.source);
		assertThat(connectionDetails).isNull();
	}

	@Test
	@SuppressWarnings("resource")
	void getConnectionDetailsWhenContainerTypeDoesNotMatchReturnsNull() {
		GenericContainer<?> container = mock(GenericContainer.class);
		ContainerConnectionSource<?> source = new ContainerConnectionSource<>(this.beanNameSuffix, this.origin,
				GenericContainer.class, container.getDockerImageName(), this.annotation, () -> container, null, null);
		TestContainerConnectionDetailsFactory factory = new TestContainerConnectionDetailsFactory();
		ConnectionDetails connectionDetails = getConnectionDetails(factory, source);
		assertThat(connectionDetails).isNull();
	}

	@Test
	void getConnectionDetailsHasOrigin() {
		TestContainerConnectionDetailsFactory factory = new TestContainerConnectionDetailsFactory();
		ConnectionDetails connectionDetails = getConnectionDetails(factory, this.source);
		assertThat(Origin.from(connectionDetails)).isSameAs(this.origin);
	}

	@Test
	void getContainerWhenNotInitializedThrowsException() {
		TestContainerConnectionDetailsFactory factory = new TestContainerConnectionDetailsFactory();
		TestDatabaseConnectionDetails connectionDetails = getConnectionDetails(factory, this.source);
		assertThat(connectionDetails).isNotNull();
		assertThatIllegalStateException().isThrownBy(connectionDetails::callGetContainer)
			.withMessage("Container cannot be obtained before the connection details bean has been initialized");
	}

	@Test
	void getContainerWhenInitializedReturnsSuppliedContainer() throws Exception {
		TestContainerConnectionDetailsFactory factory = new TestContainerConnectionDetailsFactory();
		TestDatabaseConnectionDetails connectionDetails = getConnectionDetails(factory, this.source);
		assertThat(connectionDetails).isNotNull();
		connectionDetails.afterPropertiesSet();
		assertThat(connectionDetails.callGetContainer()).isSameAs(this.container);
	}

	@Test
	void creatingFactoryWithEmptyNamesThrows() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new TestContainerConnectionDetailsFactory(Collections.emptyList()));
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void creatingFactoryWithNullNamesThrows() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new TestContainerConnectionDetailsFactory((List<String>) null));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private @Nullable TestDatabaseConnectionDetails getConnectionDetails(ConnectionDetailsFactory<?, ?> factory,
			ContainerConnectionSource<?> source) {
		return (TestDatabaseConnectionDetails) ((ConnectionDetailsFactory) factory).getConnectionDetails(source);
	}

	/**
	 * Test {@link ContainerConnectionDetailsFactory}.
	 */
	static class TestContainerConnectionDetailsFactory
			extends ContainerConnectionDetailsFactory<JdbcDatabaseContainer<?>, DatabaseConnectionDetails> {

		TestContainerConnectionDetailsFactory() {
			this(ANY_CONNECTION_NAME);
		}

		TestContainerConnectionDetailsFactory(@Nullable String connectionName) {
			super(connectionName);
		}

		TestContainerConnectionDetailsFactory(List<String> connectionNames) {
			super(connectionNames);
		}

		@Override
		protected DatabaseConnectionDetails getContainerConnectionDetails(
				ContainerConnectionSource<JdbcDatabaseContainer<?>> source) {
			return new TestDatabaseConnectionDetails(source);
		}

	}

}
