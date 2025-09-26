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

package org.springframework.boot.grpc.server.autoconfigure.exception;

import io.grpc.BindableService;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GrpcExceptionHandlerAutoConfiguration}.
 *
 * @author Chris Bono
 */
class GrpcExceptionHandlerAutoConfigurationTests {

	private ApplicationContextRunner contextRunner() {
		// NOTE: we use noop server lifecycle to avoid startup
		return new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GrpcExceptionHandlerAutoConfiguration.class))
			.withBean("noopServerLifecycle", GrpcServerLifecycle.class, Mockito::mock)
			.withBean("mockGrpcExceptionHandler", GrpcExceptionHandler.class, Mockito::mock);
	}

	@Test
	void whenGrpcNotOnClasspathAutoConfigurationIsSkipped() {
		this.contextRunner()
			.withClassLoader(new FilteredClassLoader(BindableService.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcExceptionHandlerAutoConfiguration.class));
	}

	@Test
	void whenSpringGrpcNotOnClasspathAutoConfigurationIsSkipped() {
		this.contextRunner()
			.withClassLoader(new FilteredClassLoader(GrpcServerFactory.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcExceptionHandlerAutoConfiguration.class));
	}

	@Test
	void whenNoGrpcExceptionHandlerRegisteredAutoConfigurationIsSkipped() {
		new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GrpcExceptionHandlerAutoConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcExceptionHandlerAutoConfiguration.class));
	}

	@Test
	void whenExceptionHandlerPropertyNotSetExceptionHandlerIsAutoConfigured() {
		this.contextRunner()
			.run((context) -> assertThat(context).hasSingleBean(GrpcExceptionHandlerAutoConfiguration.class));
	}

	@Test
	void whenExceptionHandlerPropertyIsTrueExceptionHandlerIsAutoConfigured() {
		this.contextRunner()
			.withPropertyValues("spring.grpc.server.exception-handler.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(GrpcExceptionHandlerAutoConfiguration.class));
	}

	@Test
	void whenExceptionHandlerPropertyIsFalseAutoConfigurationIsSkipped() {
		this.contextRunner()
			.withPropertyValues("spring.grpc.server.exception-handler.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcExceptionHandlerAutoConfiguration.class));
	}

	@Test
	void whenServerEnabledPropertySetFalseThenAutoConfigurationIsSkipped() {
		this.contextRunner()
			.withPropertyValues("spring.grpc.server.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcExceptionHandlerAutoConfiguration.class));
	}

	@Test
	void whenServerEnabledPropertyNotSetThenAutoConfigurationIsNotSkipped() {
		this.contextRunner()
			.run((context) -> assertThat(context).hasSingleBean(GrpcExceptionHandlerAutoConfiguration.class));
	}

	@Test
	void whenServerEnabledPropertySetTrueThenAutoConfigurationIsNotSkipped() {
		this.contextRunner()
			.withPropertyValues("spring.grpc.server.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(GrpcExceptionHandlerAutoConfiguration.class));
	}

	@Test
	void whenHasUserDefinedGrpcExceptionHandlerInterceptorDoesNotAutoConfigureBean() {
		GrpcExceptionHandlerInterceptor customInterceptor = Mockito.mock();
		this.contextRunner()
			.withBean("customInterceptor", GrpcExceptionHandlerInterceptor.class, () -> customInterceptor)
			.run((context) -> assertThat(context).getBean(GrpcExceptionHandlerInterceptor.class)
				.isSameAs(customInterceptor));
	}

	@Test
	void exceptionHandlerInterceptorAutoConfiguredAsExpected() {
		this.contextRunner()
			.run((context) -> assertThat(context).getBean(GrpcExceptionHandlerInterceptor.class)
				.extracting("exceptionHandler.exceptionHandlers",
						InstanceOfAssertFactories.array(GrpcExceptionHandler[].class))
				.containsExactly(context.getBean(GrpcExceptionHandler.class)));
	}

}
