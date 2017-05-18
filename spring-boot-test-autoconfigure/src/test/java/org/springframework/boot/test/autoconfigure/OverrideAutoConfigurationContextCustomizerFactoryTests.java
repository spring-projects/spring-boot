/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.test.autoconfigure;

import org.junit.Test;

import org.springframework.test.context.ContextCustomizer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OverrideAutoConfigurationContextCustomizerFactory}.
 *
 * @author pwebb
 */
public class OverrideAutoConfigurationContextCustomizerFactoryTests {

	private OverrideAutoConfigurationContextCustomizerFactory factory = new OverrideAutoConfigurationContextCustomizerFactory();

	@Test
	public void getContextCustomizerWhenHasNoAnnotationShouldReturnNull()
			throws Exception {
		ContextCustomizer customizer = this.factory
				.createContextCustomizer(NoAnnotation.class, null);
		assertThat(customizer).isNull();
	}

	@Test
	public void getContextCustomizerWhenHasAnnotationEnabledTrueShouldReturnNull()
			throws Exception {
		ContextCustomizer customizer = this.factory
				.createContextCustomizer(WithAnnotationEnabledTrue.class, null);
		assertThat(customizer).isNull();
	}

	@Test
	public void getContextCustomizerWhenHasAnnotationEnabledFalseShouldReturnCustomizer()
			throws Exception {
		ContextCustomizer customizer = this.factory
				.createContextCustomizer(WithAnnotationEnabledFalse.class, null);
		assertThat(customizer).isNotNull();
	}

	@Test
	public void hashCodeAndEquals() throws Exception {
		ContextCustomizer customizer1 = this.factory
				.createContextCustomizer(WithAnnotationEnabledFalse.class, null);
		ContextCustomizer customizer2 = this.factory
				.createContextCustomizer(WithSameAnnotation.class, null);
		assertThat(customizer1.hashCode()).isEqualTo(customizer2.hashCode());
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
