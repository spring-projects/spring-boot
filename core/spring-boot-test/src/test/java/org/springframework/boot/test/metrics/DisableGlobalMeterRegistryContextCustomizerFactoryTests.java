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

package org.springframework.boot.test.metrics;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DisableGlobalMeterRegistryContextCustomizerFactory}.
 *
 * @author Lordwill Kandiro
 */
class DisableGlobalMeterRegistryContextCustomizerFactoryTests {

	private final DisableGlobalMeterRegistryContextCustomizerFactory factory = new DisableGlobalMeterRegistryContextCustomizerFactory();

	@Test
	void createContextCustomizerReturnsNullWhenMetricsIsNotPresent() {
		assertThat(this.factory.createContextCustomizer(getClass(), Collections.emptyList())).isNull();
	}

	@Test
	void disablesGlobalMeterRegistryByDefault() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.setEnvironment(new StandardEnvironment());
			new DisableGlobalMeterRegistryContextCustomizerFactory.DisableGlobalMeterRegistryContextCustomizer()
				.customizeContext(context, mock(MergedContextConfiguration.class));
			assertThat(context.getEnvironment().getProperty("management.metrics.use-global-registry"))
				.isEqualTo("false");
		}
	}

	@Test
	void doesNotOverrideExplicitTrue() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			StandardEnvironment environment = new StandardEnvironment();
			TestPropertyValues.of("management.metrics.use-global-registry=true").applyTo(environment);
			context.setEnvironment(environment);
			new DisableGlobalMeterRegistryContextCustomizerFactory.DisableGlobalMeterRegistryContextCustomizer()
				.customizeContext(context, mock(MergedContextConfiguration.class));
			assertThat(context.getEnvironment().getProperty("management.metrics.use-global-registry"))
				.isEqualTo("true");
		}
	}

	@Test
	void doesNotOverrideExplicitFalse() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			StandardEnvironment environment = new StandardEnvironment();
			TestPropertyValues.of("management.metrics.use-global-registry=false").applyTo(environment);
			context.setEnvironment(environment);
			new DisableGlobalMeterRegistryContextCustomizerFactory.DisableGlobalMeterRegistryContextCustomizer()
				.customizeContext(context, mock(MergedContextConfiguration.class));
			assertThat(context.getEnvironment().getProperty("management.metrics.use-global-registry"))
				.isEqualTo("false");
		}
	}

	@Test
	void equalsAndHashCode() {
		ContextCustomizer customizer1 = new DisableGlobalMeterRegistryContextCustomizerFactory.DisableGlobalMeterRegistryContextCustomizer();
		ContextCustomizer customizer2 = new DisableGlobalMeterRegistryContextCustomizerFactory.DisableGlobalMeterRegistryContextCustomizer();
		assertThat(customizer1).isEqualTo(customizer2).hasSameHashCodeAs(customizer2);
	}

}
