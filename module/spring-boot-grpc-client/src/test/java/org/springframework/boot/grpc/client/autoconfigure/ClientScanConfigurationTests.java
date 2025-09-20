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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.grpc.client.BlockingStubFactory;
import org.springframework.grpc.client.CoroutineStubFactory;
import org.springframework.grpc.client.GrpcClientFactory.GrpcClientRegistrationSpec;
import org.springframework.grpc.client.ReactorStubFactory;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for the {@link ClientScanConfiguration}.
 *
 * @author CheolHwan Ihn
 */
class ClientScanConfigurationTests {

	@Test
	void testReactorStubFactory() {
		Map<String, Object> properties = new HashMap<>();
		properties.put("spring.grpc.client.default-stub-factory", ReactorStubFactory.class.getName());
		properties.put("spring.grpc.client.default-channel.address", "static://localhost:9090");

		MockEnvironment environment = new MockEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("test", properties));

		ClientScanConfiguration.DefaultGrpcClientRegistrations registrations = new ClientScanConfiguration.DefaultGrpcClientRegistrations();
		registrations.setEnvironment(environment);

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.setEnvironment(environment);
		AutoConfigurationPackages.register(context, "org.springframework.grpc.autoconfigure.client");
		registrations.setBeanFactory(context.getBeanFactory());

		GrpcClientRegistrationSpec[] specs = registrations.collect(null);

		assertThat(specs).hasSize(1);
		assertThat(specs[0].factory()).isEqualTo(ReactorStubFactory.class);
	}

	@Test
	void testDefaultStubFactory() {
		Map<String, Object> properties = new HashMap<>();
		properties.put("spring.grpc.client.default-channel.address", "static://localhost:9090");

		MockEnvironment environment = new MockEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("test", properties));

		ClientScanConfiguration.DefaultGrpcClientRegistrations registrations = new ClientScanConfiguration.DefaultGrpcClientRegistrations();
		registrations.setEnvironment(environment);

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.setEnvironment(environment);
		AutoConfigurationPackages.register(context, "org.springframework.grpc.autoconfigure.client");
		registrations.setBeanFactory(context.getBeanFactory());

		GrpcClientRegistrationSpec[] specs = registrations.collect(null);

		assertThat(specs).hasSize(1);
		assertThat(specs[0].factory()).isEqualTo(BlockingStubFactory.class);
	}

	@Test
	void testCoroutineStubFactory() {
		Map<String, Object> properties = new HashMap<>();
		properties.put("spring.grpc.client.default-stub-factory", CoroutineStubFactory.class.getName());
		properties.put("spring.grpc.client.default-channel.address", "static://localhost:9090");

		MockEnvironment env = new MockEnvironment();
		env.getPropertySources().addFirst(new MapPropertySource("test", properties));

		var regs = new ClientScanConfiguration.DefaultGrpcClientRegistrations();
		regs.setEnvironment(env);

		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.setEnvironment(env);
			AutoConfigurationPackages.register(context, "org.springframework.grpc.autoconfigure.client");
			regs.setBeanFactory(context.getBeanFactory());

			GrpcClientRegistrationSpec[] specs = regs.collect(null);
			assertThat(specs).hasSize(1);
			assertThat(specs[0].factory()).isEqualTo(CoroutineStubFactory.class);
		}
	}

	@Test
	void testInvalidStubFactoryValueThrowsBindException() {
		Map<String, Object> properties = new HashMap<>();
		properties.put("spring.grpc.client.default-stub-factory", "com.example.InvalidStubFactory");
		properties.put("spring.grpc.client.default-channel.address", "static://localhost:9090");

		MockEnvironment env = new MockEnvironment();
		env.getPropertySources().addFirst(new MapPropertySource("test", properties));

		var regs = new ClientScanConfiguration.DefaultGrpcClientRegistrations();
		regs.setEnvironment(env);

		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.setEnvironment(env);
			AutoConfigurationPackages.register(context, "org.springframework.grpc.autoconfigure.client");
			regs.setBeanFactory(context.getBeanFactory());

			assertThatExceptionOfType(BindException.class).isThrownBy(() -> regs.collect(null))
				.isInstanceOf(BindException.class)
				.withMessageContaining("spring.grpc.client.default-stub-factory");
		}
	}

	@Nested
	@TestConfiguration(proxyBeanMethods = false)
	@SpringBootTest(classes = { ClientScanConfiguration.class, ClientScanConfigurationSpringBootTest.TestConfig.class },
			properties = { "spring.grpc.client.default-channel.address=static://localhost:9090",
					"spring.grpc.client.default-stub-factory=org.springframework.grpc.client.ReactorStubFactory" })
	class ClientScanConfigurationSpringBootTest {

		@Autowired
		private ApplicationContext context;

		@Autowired
		private GrpcClientProperties props;

		@Autowired
		private Environment env;

		@Test
		void propertyIsBoundAsBeanAndUsable() {
			assertThat(this.props.getDefaultStubFactory()).isEqualTo(ReactorStubFactory.class);

			GrpcClientProperties rebound = Binder.get(this.env)
				.bind("spring.grpc.client", GrpcClientProperties.class)
				.get();

			assertThat(rebound.getDefaultStubFactory()).isEqualTo(ReactorStubFactory.class);
		}

		@Configuration
		@EnableConfigurationProperties(GrpcClientProperties.class)
		static class TestConfig {

		}

	}

}
