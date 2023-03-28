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

package org.springframework.boot.test.autoconfigure.service.connection;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.test.autoconfigure.service.connection.ServiceConnectionContextCustomizerFactoryTests.ServiceConnections.NestedClass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ServiceConnectionContextCustomizerFactory}.
 *
 * @author Andy Wilkinson
 */
public class ServiceConnectionContextCustomizerFactoryTests {

	private final ServiceConnectionContextCustomizerFactory factory = new ServiceConnectionContextCustomizerFactory();

	@Test
	void whenClassHasNoServiceConnectionsThenCreateReturnsNull() {
		assertThat(this.factory.createContextCustomizer(NoServiceConnections.class, null)).isNull();
	}

	@Test
	void whenClassHasServiceConnectionsThenCreateReturnsCustomizer() {
		ServiceConnectionContextCustomizer customizer = (ServiceConnectionContextCustomizer) this.factory
			.createContextCustomizer(ServiceConnections.class, null);
		assertThat(customizer).isNotNull();
		assertThat(customizer.getSources()).hasSize(2);
	}

	@Test
	void whenEnclosingClassHasServiceConnectionsThenCreateReturnsCustomizer() {
		ServiceConnectionContextCustomizer customizer = (ServiceConnectionContextCustomizer) this.factory
			.createContextCustomizer(NestedClass.class, null);
		assertThat(customizer).isNotNull();
		assertThat(customizer.getSources()).hasSize(3);
	}

	@Test
	void whenClassHasNonStaticServiceConnectionThenCreateShouldFailWithHelpfulIllegalStateException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> this.factory.createContextCustomizer(NonStaticServiceConnection.class, null))
			.withMessage("@ServiceConnection field 'service' must be static");
	}

	static class NoServiceConnections {

	}

	static class ServiceConnections {

		@ServiceConnection(TestConnectionDetails.class)
		private static GenericContainer<?> service1 = new GenericContainer<>("example");

		@ServiceConnection(TestConnectionDetails.class)
		private static GenericContainer<?> service2 = new GenericContainer<>("example");

		@Nested
		class NestedClass {

			@ServiceConnection(TestConnectionDetails.class)
			private static GenericContainer<?> service3 = new GenericContainer<>("example");

		}

	}

	static class NonStaticServiceConnection {

		@ServiceConnection(TestConnectionDetails.class)
		private GenericContainer<?> service = new GenericContainer<>("example");

	}

	static class TestConnectionDetails implements ConnectionDetails {

	}

}
