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

package org.springframework.boot.test.autoconfigure;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.ContextCustomizer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OverrideAutoConfigurationContextCustomizerFactory}.
 *
 * @author Phillip Webb
 */
class OverrideAutoConfigurationContextCustomizerFactoryTests {

	private final OverrideAutoConfigurationContextCustomizerFactory factory = new OverrideAutoConfigurationContextCustomizerFactory();

	@Test
	void getContextCustomizerWhenHasNoAnnotationShouldReturnNull() {
		ContextCustomizer customizer = this.factory.createContextCustomizer(NoAnnotation.class,
				Collections.emptyList());
		assertThat(customizer).isNull();
	}

	@Test
	void getContextCustomizerWhenHasAnnotationEnabledTrueShouldReturnNull() {
		ContextCustomizer customizer = this.factory.createContextCustomizer(WithAnnotationEnabledTrue.class,
				Collections.emptyList());
		assertThat(customizer).isNull();
	}

	@Test
	void getContextCustomizerWhenHasAnnotationEnabledFalseShouldReturnCustomizer() {
		ContextCustomizer customizer = this.factory.createContextCustomizer(WithAnnotationEnabledFalse.class,
				Collections.emptyList());
		assertThat(customizer).isNotNull();
	}

	@Test
	void hashCodeAndEquals() {
		ContextCustomizer customizer1 = this.factory.createContextCustomizer(WithAnnotationEnabledFalse.class,
				Collections.emptyList());
		ContextCustomizer customizer2 = this.factory.createContextCustomizer(WithSameAnnotation.class,
				Collections.emptyList());
		assertThat(customizer1).hasSameHashCodeAs(customizer2);
		assertThat(customizer1).isEqualTo(customizer1).isEqualTo(customizer2);
	}

	static class NoAnnotation {

	}

	@OverrideAutoConfiguration(enabled = true)
	static class WithAnnotationEnabledTrue {

	}

	@OverrideAutoConfiguration(enabled = false)
	static class WithAnnotationEnabledFalse {

	}

	@OverrideAutoConfiguration(enabled = false)
	static class WithSameAnnotation {

	}

}
