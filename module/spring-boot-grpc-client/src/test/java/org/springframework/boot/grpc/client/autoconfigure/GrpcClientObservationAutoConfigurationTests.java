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

import io.grpc.stub.AbstractStub;
import io.micrometer.core.instrument.binder.grpc.ObservationGrpcClientInterceptor;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.grpc.client.GlobalClientInterceptor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@link GrpcClientObservationAutoConfiguration}.
 */
class GrpcClientObservationAutoConfigurationTests {

	private final ApplicationContextRunner baseContextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(GrpcClientObservationAutoConfiguration.class));

	private ApplicationContextRunner validContextRunner() {
		return new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GrpcClientObservationAutoConfiguration.class))
			.withBean("observationRegistry", ObservationRegistry.class, Mockito::mock);
	}

	@Test
	void whenObservationRegistryNotOnClasspathAutoConfigSkipped() {
		this.validContextRunner()
			.withClassLoader(new FilteredClassLoader(ObservationRegistry.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcClientObservationAutoConfiguration.class));
	}

	@Test
	void whenObservationGrpcClientInterceptorNotOnClasspathAutoConfigSkipped() {
		this.validContextRunner()
			.withClassLoader(new FilteredClassLoader(ObservationGrpcClientInterceptor.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcClientObservationAutoConfiguration.class));
	}

	@Test
	void whenObservationRegistryNotProvidedThenAutoConfigSkipped() {
		this.baseContextRunner
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcClientObservationAutoConfiguration.class));
	}

	@Test
	void whenObservationPropertyEnabledThenAutoConfigNotSkipped() {
		this.validContextRunner()
			.withPropertyValues("spring.grpc.client.observation.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(GrpcClientObservationAutoConfiguration.class));
	}

	@Test
	void whenObservationPropertyDisabledThenAutoConfigIsSkipped() {
		this.validContextRunner()
			.withPropertyValues("spring.grpc.client.observation.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcClientObservationAutoConfiguration.class));
	}

	@Test
	void whenClientEnabledPropertyNotSetThenAutoConfigNotSkipped() {
		this.validContextRunner()
			.run((context) -> assertThat(context).hasSingleBean(GrpcClientObservationAutoConfiguration.class));
	}

	@Test
	void whenClientEnabledPropertySetTrueThenAutoConfigIsNotSkipped() {
		this.validContextRunner()
			.withPropertyValues("spring.grpc.client.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(GrpcClientObservationAutoConfiguration.class));
	}

	@Test
	void whenClientEnabledPropertySetFalseThenAutoConfigIsSkipped() {
		this.validContextRunner()
			.withPropertyValues("spring.grpc.client.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcClientObservationAutoConfiguration.class));
	}

	@Test
	void whenGrpcStubNotOnClasspathThenAutoConfigIsSkipped() {
		this.validContextRunner()
			.withClassLoader(new FilteredClassLoader(AbstractStub.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcClientObservationAutoConfiguration.class));
	}

	@Test
	void whenAllConditionsAreMetThenInterceptorConfiguredAsExpected() {
		this.validContextRunner().run((context) -> {
			assertThat(context).hasSingleBean(ObservationGrpcClientInterceptor.class);
			assertThat(context.getBeansWithAnnotation(GlobalClientInterceptor.class)).hasEntrySatisfying(
					"observationGrpcClientInterceptor",
					(bean) -> assertThat(bean.getClass().isAssignableFrom(ObservationGrpcClientInterceptor.class))
						.isTrue());
		});
	}

}
