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

package org.springframework.boot.grpc.client.autoconfigure;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.grpc.client.BlockingStubFactory;
import org.springframework.grpc.client.CoroutineStubFactory;
import org.springframework.grpc.client.GrpcClientFactory.GrpcClientRegistrationSpec;
import org.springframework.grpc.client.ReactorStubFactory;
import org.springframework.grpc.client.StubFactory;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@link DefaultGrpcClientRegistrations}.
 *
 * @author CheolHwan Ihn
 * @author Chris Bono
 */
class DefaultGrpcClientRegistrationsTests {

	private static void assertThatPropertiesResultInExpectedStubFactory(Map<String, Object> properties,
			Class<? extends StubFactory<?>> expectedStubFactoryClass) {
		MockEnvironment environment = new MockEnvironment();
		environment.getPropertySources()
			.addFirst(new MapPropertySource("defaultGrpcClientRegistrationsTests", properties));
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			AutoConfigurationPackages.register(context, "org.springframework.boot.grpc.client.autoconfigure");
			DefaultGrpcClientRegistrations registrations = new DefaultGrpcClientRegistrations(environment,
					context.getBeanFactory());

			GrpcClientRegistrationSpec[] specs = registrations.collect(null);

			assertThat(specs).hasSize(1);
			assertThat(specs[0].factory()).isEqualTo(expectedStubFactoryClass);
		}
	}

	@Test
	void withReactorStubFactory() {
		Map<String, Object> properties = new HashMap<>();
		properties.put("spring.grpc.client.default-stub-factory", ReactorStubFactory.class.getName());
		properties.put("spring.grpc.client.default-channel.address", "static://localhost:9090");
		assertThatPropertiesResultInExpectedStubFactory(properties, ReactorStubFactory.class);
	}

	@Test
	void withDefaultStubFactory() {
		Map<String, Object> properties = new HashMap<>();
		properties.put("spring.grpc.client.default-channel.address", "static://localhost:9090");
		assertThatPropertiesResultInExpectedStubFactory(properties, BlockingStubFactory.class);
	}

	@Test
	void withCoroutineStubFactory() {
		Map<String, Object> properties = new HashMap<>();
		properties.put("spring.grpc.client.default-stub-factory", CoroutineStubFactory.class.getName());
		properties.put("spring.grpc.client.default-channel.address", "static://localhost:9090");
		assertThatPropertiesResultInExpectedStubFactory(properties, CoroutineStubFactory.class);
	}

}
