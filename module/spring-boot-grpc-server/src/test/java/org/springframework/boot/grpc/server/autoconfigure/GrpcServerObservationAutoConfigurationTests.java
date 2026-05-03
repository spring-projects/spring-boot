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

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import io.micrometer.context.ContextRegistry;
import io.micrometer.core.instrument.binder.grpc.GrpcServerObservationConvention;
import io.micrometer.core.instrument.binder.grpc.ObservationGrpcServerInterceptor;
import io.micrometer.core.instrument.kotlin.ObservationCoroutineContextServerInterceptor;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.grpc.server.GrpcServerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for the {@link GrpcServerObservationAutoConfiguration}.
 *
 * @author Chris Bono
 */
class GrpcServerObservationAutoConfigurationTests {

	private static final AutoConfigurations autoConfigurations = AutoConfigurations
		.of(GrpcServerObservationAutoConfiguration.class);

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(autoConfigurations)
		.withBean("observationRegistry", ObservationRegistry.class, Mockito::mock);

	@Test
	void whenGrpcNotOnClasspathAutoConfigurationIsSkipped() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(BindableService.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerObservationAutoConfiguration.class));
	}

	@Test
	void whenSpringGrpcNotOnClasspathAutoConfigurationIsSkipped() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(GrpcServerFactory.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerObservationAutoConfiguration.class));
	}

	@Test
	void whenObservationRegistryNotOnClasspathAutoConfigurationSkipped() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(ObservationRegistry.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerObservationAutoConfiguration.class));
	}

	@Test
	void whenObservationGrpcServerInterceptorNotOnClasspathAutoConfigurationSkipped() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(ObservationGrpcServerInterceptor.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerObservationAutoConfiguration.class));
	}

	@Test
	void whenObservationRegistryNotProvidedThenAutoConfigurationSkipped() {
		new ApplicationContextRunner().withConfiguration(autoConfigurations)
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerObservationAutoConfiguration.class));
	}

	@Test
	void whenObservationPropertyEnabledThenAutoConfigurationNotSkipped() {
		this.contextRunner.withPropertyValues("spring.grpc.server.observation.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(GrpcServerObservationAutoConfiguration.class));
	}

	@Test
	void whenObservationPropertyDisabledThenAutoConfigurationIsSkipped() {
		this.contextRunner.withPropertyValues("spring.grpc.server.observation.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerObservationAutoConfiguration.class));
	}

	@Test
	void whenServerEnabledPropertySetFalseThenAutoConfigurationIsSkipped() {
		this.contextRunner.withPropertyValues("spring.grpc.server.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerObservationAutoConfiguration.class));
	}

	@Test
	void whenServerEnabledPropertyNotSetThenAutoConfigurationIsNotSkipped() {
		this.contextRunner
			.run((context) -> assertThat(context).hasSingleBean(GrpcServerObservationAutoConfiguration.class));
	}

	@Test
	void whenServerEnabledPropertySetTrueThenAutoConfigurationIsNotSkipped() {
		this.contextRunner.withPropertyValues("spring.grpc.server.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(GrpcServerObservationAutoConfiguration.class));
	}

	@Test
	void whenAllConditionsAreMetThenInterceptorConfiguredAsExpected() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(ObservationGrpcServerInterceptor.class);
			Map<String, Object> annotated = context.getBeansWithAnnotation(GlobalServerInterceptor.class);
			List<ServerInterceptor> interceptors = context.getBeanProvider(ServerInterceptor.class)
				.orderedStream()
				.toList();
			assertThat(annotated).hasSize(2);
			assertThat(interceptors.get(0)).isInstanceOf(ObservationGrpcServerInterceptor.class);
		});
	}

	@Test
	void whenCustomConventionBeanIsPresentThenInterceptorUsesIt() {
		GrpcServerObservationConvention customConvention = mock(GrpcServerObservationConvention.class);
		this.contextRunner.withBean(GrpcServerObservationConvention.class, () -> customConvention)
			.run((context) -> assertThat(context.getBean(ObservationGrpcServerInterceptor.class))
				.hasFieldOrPropertyWithValue("customConvention", customConvention));
	}

	@Test
	void whenMicrometerContextPropagationIsNotOnClasspathCoroutineInterceptorIsNotCreated() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(ContextRegistry.class))
			.run((context) -> assertThat(context).hasSingleBean(ObservationGrpcServerInterceptor.class)
				.doesNotHaveBean(ObservationCoroutineContextServerInterceptor.class));
	}

}
