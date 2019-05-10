/*
 * Copyright 2012-2019 the original author or authors.
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

import org.junit.Test;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.context.ContextCustomizer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Madhura Bhave
 */
public class OverrideConfigurationPropertiesScanContextCustomizerFactoryTests {

	private OverrideConfigurationPropertiesScanContextCustomizerFactory factory = new OverrideConfigurationPropertiesScanContextCustomizerFactory();

	@Test
	public void getContextCustomizerWhenHasNoAnnotationShouldReturnNull() {
		ContextCustomizer customizer = this.factory
				.createContextCustomizer(NoAnnotation.class, null);
		assertThat(customizer).isNull();
	}

	@Test
	public void getContextCustomizerWhenHasAnnotationEnabledTrueShouldReturnNull() {
		ContextCustomizer customizer = this.factory
				.createContextCustomizer(WithAnnotationEnabledTrue.class, null);
		assertThat(customizer).isNull();
	}

	@Test
	public void getContextCustomizerWhenHasAnnotationEnabledFalseShouldReturnCustomizer() {
		ContextCustomizer customizer = this.factory
				.createContextCustomizer(WithAnnotationEnabledFalse.class, null);
		assertThat(customizer).isNotNull();
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		customizer.customizeContext(context, null);
		String property = context.getEnvironment().getProperty(
				ConfigurationPropertiesScan.CONFIGURATION_PROPERTIES_SCAN_ENABLED_PROPERTY);
		assertThat(property).isEqualTo("false");
	}

	@Test
	public void hashCodeAndEquals() {
		ContextCustomizer customizer1 = this.factory
				.createContextCustomizer(WithAnnotationEnabledFalse.class, null);
		ContextCustomizer customizer2 = this.factory
				.createContextCustomizer(WithSameAnnotation.class, null);
		assertThat(customizer1.hashCode()).isEqualTo(customizer2.hashCode());
		assertThat(customizer1).isEqualTo(customizer1).isEqualTo(customizer2);
	}

	static class NoAnnotation {

	}

	@OverrideConfigurationPropertiesScan(enabled = true)
	static class WithAnnotationEnabledTrue {

	}

	@OverrideConfigurationPropertiesScan(enabled = false)
	static class WithAnnotationEnabledFalse {

	}

	@OverrideConfigurationPropertiesScan(enabled = false)
	static class WithSameAnnotation {

	}

}
