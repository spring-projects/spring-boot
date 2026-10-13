/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.client.autoconfigure;

import java.util.Map;

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
 *
 * @author Chris Bono
 */
class GrpcClientObservationAutoConfigurationTests {

	private static final AutoConfigurations autoConfigurations = AutoConfigurations
		.of(GrpcClientObservationAutoConfiguration.class);

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(autoConfigurations)
		.withBean("observationRegistry", ObservationRegistry.class, Mockito::mock);

	@Test
	void whenObservationRegistryNotOnClasspathAutoConfigSkipped() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(ObservationRegistry.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcClientObservationAutoConfiguration.class));
	}

	@Test
	void whenObservationGrpcClientInterceptorNotOnClasspathAutoConfigSkipped() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(ObservationGrpcClientInterceptor.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcClientObservationAutoConfiguration.class));
	}

	@Test
	void whenObservationRegistryNotProvidedThenAutoConfigSkipped() {
		new ApplicationContextRunner().withConfiguration(autoConfigurations)
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcClientObservationAutoConfiguration.class));
	}

	@Test
	void whenObservationPropertyEnabledThenAutoConfigNotSkipped() {
		this.contextRunner.withPropertyValues("spring.grpc.client.observation.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(GrpcClientObservationAutoConfiguration.class));
	}

	@Test
	void whenObservationPropertyDisabledThenAutoConfigIsSkipped() {
		this.contextRunner.withPropertyValues("spring.grpc.client.observation.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcClientObservationAutoConfiguration.class));
	}

	@Test
	void whenClientEnabledPropertyNotSetThenAutoConfigNotSkipped() {
		this.contextRunner
			.run((context) -> assertThat(context).hasSingleBean(GrpcClientObservationAutoConfiguration.class));
	}

	@Test
	void whenClientEnabledPropertySetTrueThenAutoConfigIsNotSkipped() {
		this.contextRunner.withPropertyValues("spring.grpc.client.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(GrpcClientObservationAutoConfiguration.class));
	}

	@Test
	void whenClientEnabledPropertySetFalseThenAutoConfigIsSkipped() {
		this.contextRunner.withPropertyValues("spring.grpc.client.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcClientObservationAutoConfiguration.class));
	}

	@Test
	void whenGrpcStubNotOnClasspathThenAutoConfigIsSkipped() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(AbstractStub.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcClientObservationAutoConfiguration.class));
	}

	@Test
	void whenAllConditionsAreMetThenInterceptorConfiguredAsExpected() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(ObservationGrpcClientInterceptor.class);
			Map<String, Object> annotated = context.getBeansWithAnnotation(GlobalClientInterceptor.class);
			assertThat(annotated).hasEntrySatisfying("grpcClientObservationInterceptor",
					ObservationGrpcClientInterceptor.class::isInstance);
		});
	}

}
