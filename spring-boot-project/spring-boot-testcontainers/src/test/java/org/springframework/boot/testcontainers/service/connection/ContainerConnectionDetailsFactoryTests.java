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

package org.springframework.boot.testcontainers.service.connection;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactory;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactoryTests.TestContainerConnectionDetailsFactory.TestContainerConnectionDetails;
import org.springframework.core.annotation.MergedAnnotation;

import static org.assertj.core.api.Assertions.assertThat;
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

	private PostgreSQLContainer<?> container;

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
				this.container.getDockerImageName(), this.annotation, () -> this.container);
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
	void getConnectionDetailsWhenTypesMatchAndNameRestrictionDoesNotMatchReturnsNull() {
		TestContainerConnectionDetailsFactory factory = new TestContainerConnectionDetailsFactory("notmyname");
		ConnectionDetails connectionDetails = getConnectionDetails(factory, this.source);
		assertThat(connectionDetails).isNull();
	}

	@Test
	void getConnectionDetailsWhenContainerTypeDoesNotMatchReturnsNull() {
		ElasticsearchContainer container = mock(ElasticsearchContainer.class);
		ContainerConnectionSource<?> source = new ContainerConnectionSource<>(this.beanNameSuffix, this.origin,
				ElasticsearchContainer.class, container.getDockerImageName(), this.annotation, () -> container);
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
		TestContainerConnectionDetails connectionDetails = getConnectionDetails(factory, this.source);
		assertThatIllegalStateException().isThrownBy(() -> connectionDetails.callGetContainer())
			.withMessage("Container cannot be obtained before the connection details bean has been initialized");
	}

	@Test
	void getContainerWhenInitializedReturnsSuppliedContainer() throws Exception {
		TestContainerConnectionDetailsFactory factory = new TestContainerConnectionDetailsFactory();
		TestContainerConnectionDetails connectionDetails = getConnectionDetails(factory, this.source);
		connectionDetails.afterPropertiesSet();
		assertThat(connectionDetails.callGetContainer()).isSameAs(this.container);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private TestContainerConnectionDetails getConnectionDetails(ConnectionDetailsFactory<?, ?> factory,
			ContainerConnectionSource<?> source) {
		return (TestContainerConnectionDetails) ((ConnectionDetailsFactory) factory).getConnectionDetails(source);
	}

	/**
	 * Test {@link ContainerConnectionDetailsFactory}.
	 */
	static class TestContainerConnectionDetailsFactory
			extends ContainerConnectionDetailsFactory<JdbcDatabaseContainer<?>, JdbcConnectionDetails> {

		TestContainerConnectionDetailsFactory() {
			this(ANY_CONNECTION_NAME);
		}

		TestContainerConnectionDetailsFactory(String connectionName) {
			super(connectionName);
		}

		@Override
		protected JdbcConnectionDetails getContainerConnectionDetails(
				ContainerConnectionSource<JdbcDatabaseContainer<?>> source) {
			return new TestContainerConnectionDetails(source);
		}

		static final class TestContainerConnectionDetails extends ContainerConnectionDetails<JdbcDatabaseContainer<?>>
				implements JdbcConnectionDetails {

			private TestContainerConnectionDetails(ContainerConnectionSource<JdbcDatabaseContainer<?>> source) {
				super(source);
			}

			@Override
			public String getUsername() {
				return "user";
			}

			@Override
			public String getPassword() {
				return "secret";
			}

			@Override
			public String getJdbcUrl() {
				return "jdbc:example";
			}

			JdbcDatabaseContainer<?> callGetContainer() {
				return super.getContainer();
			}

		}

	}

}
