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

package org.springframework.boot.grpc.server.autoconfigure;

import io.grpc.BindableService;
import io.grpc.protobuf.services.ProtoReflectionServiceV1;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.grpc.server.autoconfigure.GrpcServerServicesAutoConfiguration.GrpcServerReflectionServiceConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GrpcServerServicesAutoConfiguration}.
 *
 * @author Haris Zujo
 * @author Chris Bono
 * @author Andrey Litvitski
 * @author Phillip Webb
 */
class GrpcServerServicesAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(GrpcServerServicesAutoConfiguration.class))
		.withBean("noopServerLifecycle", GrpcServerLifecycle.class, Mockito::mock)
		.withBean(BindableService.class, Mockito::mock);

	@Nested
	class GrpcServerReflectionServiceConfigurationTests {

		private final ApplicationContextRunner contextRunner = GrpcServerServicesAutoConfigurationTests.this.contextRunner;

		@Test
		void whenAutoConfigurationIsNotSkippedCreatesReflectionServiceBean() {
			this.contextRunner.run((context) -> {
				assertThat(context).hasSingleBean(GrpcServerReflectionServiceConfiguration.class);
				assertThat(context).hasBean("grpcServerReflectionService");
			});
		}

		@Test
		void whenGrpcServicesNotOnClasspathAutoConfigurationIsSkipped() {
			this.contextRunner.withClassLoader(new FilteredClassLoader(ProtoReflectionServiceV1.class))
				.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerReflectionServiceConfiguration.class));
		}

		@Test
		void whenNoBindableServiceDefinedAutoConfigurationIsSkipped() {
			new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(GrpcServerServicesAutoConfiguration.class))
				.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerReflectionServiceConfiguration.class));
		}

		@Test
		void whenReflectionEnabledPropertyIsTrueAutoConfigurationIsNotSkipped() {
			this.contextRunner.withPropertyValues("spring.grpc.server.reflection.enabled=true")
				.run((context) -> assertThat(context).hasBean("grpcServerReflectionService"));
		}

		@Test
		void whenReflectionEnabledPropertyIsFalseAutoConfigurationIsSkipped() {
			this.contextRunner.withPropertyValues("spring.grpc.server.reflection.enabled=false").run((context) -> {
				assertThat(context).doesNotHaveBean("grpcServerReflectionService");
				assertThat(context).doesNotHaveBean(GrpcServerReflectionServiceConfiguration.class);
			});
		}

		@Test
		void whenServerEnabledPropertyIsTrueAutoConfigurationIsNotSkipped() {
			this.contextRunner.withPropertyValues("spring.grpc.server.enabled=true")
				.run((context) -> assertThat(context).hasBean("grpcServerReflectionService"));
		}

		@Test
		void whenServerEnabledPropertyIsFalseAutoConfigurationIsSkipped() {
			this.contextRunner.withPropertyValues("spring.grpc.server.enabled=false").run((context) -> {
				assertThat(context).doesNotHaveBean("grpcServerReflectionService");
				assertThat(context).doesNotHaveBean(GrpcServerReflectionServiceConfiguration.class);
			});
		}

	}

}
