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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ServiceConnectionContextCustomizerFactory}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ServiceConnectionContextCustomizerFactoryTests {

	private final ServiceConnectionContextCustomizerFactory factory = new ServiceConnectionContextCustomizerFactory();

	@Test
	void createContextCustomizerWhenNoServiceConnectionsReturnsCustomizerToApplyInitializer() {
		ContextCustomizer customizer = this.factory.createContextCustomizer(NoServiceConnections.class, null);
		assertThat(customizer).isNotNull();
		GenericApplicationContext context = new GenericApplicationContext();
		int initialNumberOfPostProcessors = context.getBeanFactoryPostProcessors().size();
		MergedContextConfiguration mergedConfig = mock(MergedContextConfiguration.class);
		customizer.customizeContext(context, mergedConfig);
		assertThat(context.getBeanFactoryPostProcessors()).hasSize(initialNumberOfPostProcessors + 1);
	}

	@Test
	void createContextCustomizerWhenClassHasServiceConnectionsReturnsCustomizer() {
		ServiceConnectionContextCustomizer customizer = (ServiceConnectionContextCustomizer) this.factory
			.createContextCustomizer(ServiceConnections.class, null);
		assertThat(customizer).isNotNull();
		assertThat(customizer.getSources()).hasSize(2);
	}

	@Test
	void createContextCustomizerWhenEnclosingClassHasServiceConnectionsReturnsCustomizer() {
		ServiceConnectionContextCustomizer customizer = (ServiceConnectionContextCustomizer) this.factory
			.createContextCustomizer(ServiceConnections.NestedClass.class, null);
		assertThat(customizer).isNotNull();
		assertThat(customizer.getSources()).hasSize(3);
	}

	@Test
	void createContextCustomizerWhenClassHasNonStaticServiceConnectionFailsWithHepfulException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> this.factory.createContextCustomizer(NonStaticServiceConnection.class, null))
			.withMessage("@ServiceConnection field 'service' must be static");

	}

	@Test
	void createContextCustomizerWhenClassHasAnnotationOnNonConnectionFieldFailsWithHepfulException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> this.factory.createContextCustomizer(ServiceConnectionOnWrongFieldType.class, null))
			.withMessage("Field 'service2' in " + ServiceConnectionOnWrongFieldType.class.getName()
					+ " must be a org.testcontainers.containers.Container");
	}

	@Test
	void createContextCustomizerCreatesCustomizerSourceWithSensibleBeanNameSuffix() {
		ServiceConnectionContextCustomizer customizer = (ServiceConnectionContextCustomizer) this.factory
			.createContextCustomizer(SingleServiceConnection.class, null);
		ContainerConnectionSource<?> source = customizer.getSources().get(0);
		assertThat(source.getBeanNameSuffix()).isEqualTo("test");
	}

	@Test
	void createContextCustomizerCreatesCustomizerSourceWithSensibleOrigin() {
		ServiceConnectionContextCustomizer customizer = (ServiceConnectionContextCustomizer) this.factory
			.createContextCustomizer(SingleServiceConnection.class, null);
		ContainerConnectionSource<?> source = customizer.getSources().get(0);
		assertThat(source.getOrigin())
			.hasToString("ServiceConnectionContextCustomizerFactoryTests.SingleServiceConnection.service1");
	}

	@Test
	void createContextCustomizerCreatesCustomizerSourceWithSensibleToString() {
		ServiceConnectionContextCustomizer customizer = (ServiceConnectionContextCustomizer) this.factory
			.createContextCustomizer(SingleServiceConnection.class, null);
		ContainerConnectionSource<?> source = customizer.getSources().get(0);
		assertThat(source).hasToString(
				"@ServiceConnection source for ServiceConnectionContextCustomizerFactoryTests.SingleServiceConnection.service1");
	}

	static class NoServiceConnections {

	}

	static class SingleServiceConnection {

		@ServiceConnection
		private static GenericContainer<?> service1 = new MockContainer();

	}

	static class ServiceConnections {

		@ServiceConnection
		private static Container<?> service1 = new MockContainer();

		@ServiceConnection
		private static Container<?> service2 = new MockContainer();

		@Nested
		class NestedClass {

			@ServiceConnection
			private static Container<?> service3 = new MockContainer();

		}

	}

	static class NonStaticServiceConnection {

		@ServiceConnection
		private Container<?> service = new MockContainer("example");

	}

	static class ServiceConnectionOnWrongFieldType {

		@ServiceConnection
		private static InputStream service2 = new ByteArrayInputStream(new byte[0]);

	}

	static class MockContainer extends GenericContainer<MockContainer> {

		private final String dockerImageName;

		MockContainer() {
			this("example");
		}

		MockContainer(String dockerImageName) {
			super(dockerImageName);
			this.dockerImageName = dockerImageName;
		}

		@Override
		public String getDockerImageName() {
			return this.dockerImageName;
		}

	}

}
