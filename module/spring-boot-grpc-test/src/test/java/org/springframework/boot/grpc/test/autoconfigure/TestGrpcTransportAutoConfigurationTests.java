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

package org.springframework.boot.grpc.test.autoconfigure;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.AbstractStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.grpc.client.autoconfigure.GrpcClientAutoConfiguration;
import org.springframework.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration;
import org.springframework.boot.grpc.server.autoconfigure.GrpcServerServicesAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.grpc.server.GrpcServerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TestGrpcTransportAutoConfiguration}.
 *
 * @author Chris Bono
 * @author Phillip Webb
 */
class TestGrpcTransportAutoConfigurationTests {

	private static final AutoConfigurations autoConfiguration = AutoConfigurations.of(
			TestGrpcTransportAutoConfiguration.class, GrpcServerAutoConfiguration.class,
			GrpcServerServicesAutoConfiguration.class, SslAutoConfiguration.class, GrpcClientAutoConfiguration.class);

	private final BindableService service = mock();

	private final ServerServiceDefinition serviceDefinition = ServerServiceDefinition.builder("my-service").build();

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(autoConfiguration)
		.withBean(BindableService.class, () -> this.service);

	@BeforeEach
	void setup() {
		given(this.service.bindService()).willReturn(this.serviceDefinition);
	}

	@Test
	void createsTestGrpcServerFactoryBean() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(TestGrpcServerFactory.class));
	}

	@Test
	void createsGrpcServerLifecycleBean() {
		this.contextRunner.run((context) -> assertThat(context).hasBean("testGrpcServerLifecycle"));
	}

	@Test
	void whenNoBindableServiceDoesNotCreateServerBeans() {
		new ApplicationContextRunner().withConfiguration(autoConfiguration).run((context) -> {
			assertThat(context).doesNotHaveBean(TestGrpcServerFactory.class);
			assertThat(context).doesNotHaveBean("testGrpcServerLifecycle");
		});
	}

	@Test
	void whenNoGrpcServerFactoryClassDoesNotCreateServerBeans() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(GrpcServerFactory.class)).run((context) -> {
			assertThat(context).doesNotHaveBean(TestGrpcServerFactory.class);
			assertThat(context).doesNotHaveBean("testGrpcServerLifecycle");
		});
	}

	@Test
	void createsTestGrpcChannelFactoryBean() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(TestGrpcChannelFactory.class));
	}

	@Test
	void whenNoAbstractStubClassDoesNotCreateClientBeans() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(AbstractStub.class))
			.run((context) -> assertThat(context).doesNotHaveBean(TestGrpcChannelFactory.class));
	}

	@Test
	void whenNoGrpcChannelFactoryClassDoesNotCreateClientBeans() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(GrpcChannelFactory.class))
			.run((context) -> assertThat(context).doesNotHaveBean(TestGrpcChannelFactory.class));
	}

}
