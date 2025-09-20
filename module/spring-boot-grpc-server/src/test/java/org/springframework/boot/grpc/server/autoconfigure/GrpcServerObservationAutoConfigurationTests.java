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

import java.util.List;
import java.util.Map;

import io.grpc.ServerInterceptor;
import io.micrometer.core.instrument.binder.grpc.ObservationGrpcServerInterceptor;
import io.micrometer.observation.ObservationRegistry;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.grpc.server.GlobalServerInterceptor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@link GrpcServerObservationAutoConfiguration}.
 */
class GrpcServerObservationAutoConfigurationTests {

	private final ApplicationContextRunner baseContextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(GrpcServerObservationAutoConfiguration.class));

	private ApplicationContextRunner validContextRunner() {
		return new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GrpcServerObservationAutoConfiguration.class))
			.withBean("observationRegistry", ObservationRegistry.class, Mockito::mock);
	}

	@Test
	void whenObservationRegistryNotOnClasspathAutoConfigSkipped() {
		this.validContextRunner()
			.withClassLoader(new FilteredClassLoader(ObservationRegistry.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerObservationAutoConfiguration.class));
	}

	@Test
	void whenObservationGrpcServerInterceptorNotOnClasspathAutoConfigSkipped() {
		this.validContextRunner()
			.withClassLoader(new FilteredClassLoader(ObservationGrpcServerInterceptor.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerObservationAutoConfiguration.class));
	}

	@Test
	void whenObservationRegistryNotProvidedThenAutoConfigSkipped() {
		this.baseContextRunner
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerObservationAutoConfiguration.class));
	}

	@Test
	void whenObservationPropertyEnabledThenAutoConfigNotSkipped() {
		this.validContextRunner()
			.withPropertyValues("spring.grpc.server.observation.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(GrpcServerObservationAutoConfiguration.class));
	}

	@Test
	void whenObservationPropertyDisabledThenAutoConfigIsSkipped() {
		this.validContextRunner()
			.withPropertyValues("spring.grpc.server.observation.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerObservationAutoConfiguration.class));
	}

	@Test
	void whenServerEnabledPropertySetFalseThenAutoConfigurationIsSkipped() {
		this.validContextRunner()
			.withPropertyValues("spring.grpc.server.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerObservationAutoConfiguration.class));
	}

	@Test
	void whenServerEnabledPropertyNotSetThenAutoConfigurationIsNotSkipped() {
		this.validContextRunner()
			.run((context) -> assertThat(context).hasSingleBean(GrpcServerObservationAutoConfiguration.class));
	}

	@Test
	void whenServerEnabledPropertySetTrueThenAutoConfigurationIsNotSkipped() {
		this.validContextRunner()
			.withPropertyValues("spring.grpc.server.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(GrpcServerObservationAutoConfiguration.class));
	}

	@Test
	void whenAllConditionsAreMetThenInterceptorConfiguredAsExpected() {
		this.validContextRunner()
			.run((context) -> assertThat(context).hasSingleBean(ObservationGrpcServerInterceptor.class)
				.has(new Condition<>((beans) -> {
					Map<String, Object> annotated = beans.getBeansWithAnnotation(GlobalServerInterceptor.class);
					List<ServerInterceptor> interceptors = beans.getBeanProvider(ServerInterceptor.class)
						.orderedStream()
						.toList();
					return annotated.size() == 2 && interceptors.get(0) instanceof ObservationGrpcServerInterceptor;
				}, "Two global interceptors expected")));
	}

}
