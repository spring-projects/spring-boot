/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.actuate.metrics;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.ContextCustomizer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutoConfigureMetrics} and
 * {@link MetricsExportContextCustomizerFactory} working together.
 *
 * @author Chris Bono
 */
class MetricsExportContextCustomizerFactoryTests {

	private final MetricsExportContextCustomizerFactory factory = new MetricsExportContextCustomizerFactory();

	@Test
	void getContextCustomizerWhenHasNoAnnotationShouldReturnCustomizer() {
		ContextCustomizer customizer = this.factory.createContextCustomizer(NoAnnotation.class,
				Collections.emptyList());
		assertThat(customizer).isNotNull();
		ConfigurableApplicationContext context = new GenericApplicationContext();
		customizer.customizeContext(context, null);
		assertThat(context.getEnvironment().getProperty("management.metrics.export.defaults.enabled"))
				.isEqualTo("false");
		assertThat(context.getEnvironment().getProperty("management.metrics.export.simple.enabled")).isEqualTo("true");
	}

	@Test
	void getContextCustomizerWhenHasAnnotationShouldReturnNull() {
		ContextCustomizer customizer = this.factory.createContextCustomizer(WithAnnotation.class, null);
		assertThat(customizer).isNull();
	}

	@Test
	void hashCodeAndEquals() {
		ContextCustomizer customizer1 = this.factory.createContextCustomizer(NoAnnotation.class, null);
		ContextCustomizer customizer2 = this.factory.createContextCustomizer(OtherWithNoAnnotation.class, null);
		assertThat(customizer1.hashCode()).isEqualTo(customizer2.hashCode());
		assertThat(customizer1).isEqualTo(customizer1).isEqualTo(customizer2);
	}

	static class NoAnnotation {

	}

	static class OtherWithNoAnnotation {

	}

	@AutoConfigureMetrics
	static class WithAnnotation {

	}

}
