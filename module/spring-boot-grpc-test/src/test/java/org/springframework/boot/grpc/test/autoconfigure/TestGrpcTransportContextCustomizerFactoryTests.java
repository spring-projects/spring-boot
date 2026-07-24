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

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TestGrpcTransportContextCustomizerFactory} and
 * {@link TestGrpcTransportContextCustomizer}.
 *
 * @author xing
 */
class TestGrpcTransportContextCustomizerFactoryTests {

	private final TestGrpcTransportContextCustomizerFactory factory = new TestGrpcTransportContextCustomizerFactory();

	@Test
	void whenNotAnnotatedDoesNotCreateCustomizer() {
		assertThat(this.factory.createContextCustomizer(NoAnnotation.class, Collections.emptyList())).isNull();
	}

	@Test
	void whenAnnotatedCreatesCustomizerAndSetsTestOnlyInProcessName() {
		ContextCustomizer customizer = createContextCustomizer(WithAnnotation.class);
		ConfigurableApplicationContext context = new GenericApplicationContext();
		applyCustomizerToContext(customizer, context);
		String name = context.getEnvironment()
			.getProperty(TestGrpcTransportContextCustomizer.INPROCESS_NAME_PROPERTY);
		assertThat(name).isNotBlank();
		assertThat(context.getEnvironment().getProperty("spring.grpc.server.inprocess.name")).isNull();
	}

	@Test
	void customizeContextGeneratesDifferentNamesForDifferentContexts() {
		ContextCustomizer customizer = createContextCustomizer(WithAnnotation.class);
		ConfigurableApplicationContext first = new GenericApplicationContext();
		ConfigurableApplicationContext second = new GenericApplicationContext();
		applyCustomizerToContext(customizer, first);
		applyCustomizerToContext(customizer, second);
		assertThat(first.getEnvironment().getProperty(TestGrpcTransportContextCustomizer.INPROCESS_NAME_PROPERTY))
			.isNotEqualTo(
					second.getEnvironment().getProperty(TestGrpcTransportContextCustomizer.INPROCESS_NAME_PROPERTY));
	}

	@Test
	void equalsWhenAnnotationAttributesMatch() {
		ContextCustomizer first = createContextCustomizer(WithAnnotation.class);
		ContextCustomizer second = createContextCustomizer(WithAnnotation.class);
		assertThat(first).isEqualTo(second);
		assertThat(first).hasSameHashCodeAs(second);
	}

	@Test
	void notEqualsWhenAnnotationAttributesDiffer() {
		ContextCustomizer defaults = createContextCustomizer(WithAnnotation.class);
		ContextCustomizer overrides = createContextCustomizer(WithOverrides.class);
		assertThat(defaults).isNotEqualTo(overrides);
	}

	private void applyCustomizerToContext(ContextCustomizer customizer, ConfigurableApplicationContext context) {
		customizer.customizeContext(context,
				new MergedContextConfiguration(getClass(), null, null, null, mock(ContextLoader.class)));
	}

	private ContextCustomizer createContextCustomizer(Class<?> testClass) {
		ContextCustomizer customizer = this.factory.createContextCustomizer(testClass, Collections.emptyList());
		assertThat(customizer).as("contextCustomizer").isNotNull();
		return customizer;
	}

	static class NoAnnotation {

	}

	@AutoConfigureTestGrpcTransport
	static class WithAnnotation {

	}

	@AutoConfigureTestGrpcTransport(enableServlet = true, enableServerFactory = true, enableChannelFactory = true)
	static class WithOverrides {

	}

}
