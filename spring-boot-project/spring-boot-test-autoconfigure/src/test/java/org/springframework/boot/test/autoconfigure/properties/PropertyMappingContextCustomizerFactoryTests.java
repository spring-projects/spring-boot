/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.properties;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.ContextCustomizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Tests for {@link PropertyMappingContextCustomizerFactory}.
 *
 * @author Phillip Webb
 */
public class PropertyMappingContextCustomizerFactoryTests {

	private PropertyMappingContextCustomizerFactory factory = new PropertyMappingContextCustomizerFactory();

	@Test
	public void getContextCustomizerWhenHasNoMappingShouldNotAddPropertySource() {
		ContextCustomizer customizer = this.factory
				.createContextCustomizer(NoMapping.class, null);
		ConfigurableApplicationContext context = mock(
				ConfigurableApplicationContext.class);
		ConfigurableEnvironment environment = mock(ConfigurableEnvironment.class);
		ConfigurableListableBeanFactory beanFactory = mock(
				ConfigurableListableBeanFactory.class);
		given(context.getEnvironment()).willReturn(environment);
		given(context.getBeanFactory()).willReturn(beanFactory);
		customizer.customizeContext(context, null);
		verifyZeroInteractions(environment);
	}

	@Test
	public void getContextCustomizerWhenHasTypeMappingShouldReturnCustomizer() {
		ContextCustomizer customizer = this.factory
				.createContextCustomizer(TypeMapping.class, null);
		assertThat(customizer).isNotNull();
	}

	@Test
	public void getContextCustomizerWhenHasAttributeMappingShouldReturnCustomizer() {
		ContextCustomizer customizer = this.factory
				.createContextCustomizer(AttributeMapping.class, null);
		assertThat(customizer).isNotNull();
	}

	@Test
	public void hashCodeAndEqualsShouldBeBasedOnPropertyValues() {
		ContextCustomizer customizer1 = this.factory
				.createContextCustomizer(TypeMapping.class, null);
		ContextCustomizer customizer2 = this.factory
				.createContextCustomizer(AttributeMapping.class, null);
		ContextCustomizer customizer3 = this.factory
				.createContextCustomizer(OtherMapping.class, null);
		assertThat(customizer1.hashCode()).isEqualTo(customizer2.hashCode());
		assertThat(customizer1).isEqualTo(customizer1).isEqualTo(customizer2)
				.isNotEqualTo(customizer3);
	}

	@Test
	public void prepareContextShouldAddPropertySource() {
		ContextCustomizer customizer = this.factory
				.createContextCustomizer(AttributeMapping.class, null);
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		customizer.customizeContext(context, null);
		assertThat(context.getEnvironment().getProperty("mapped")).isEqualTo("Mapped");
	}

	@Test
	public void propertyMappingShouldNotBeUsedWithComponent() {
		ContextCustomizer customizer = this.factory
				.createContextCustomizer(AttributeMapping.class, null);
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(ConfigMapping.class);
		customizer.customizeContext(context, null);
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(context::refresh)
				.withMessageContaining("The @PropertyMapping annotation "
						+ "@PropertyMappingContextCustomizerFactoryTests.TypeMappingAnnotation "
						+ "cannot be used in combination with the @Component annotation @Configuration");
	}

	@NoMappingAnnotation
	static class NoMapping {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface NoMappingAnnotation {

	}

	@TypeMappingAnnotation
	static class TypeMapping {

	}

	@Configuration
	@TypeMappingAnnotation
	static class ConfigMapping {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@PropertyMapping
	@interface TypeMappingAnnotation {

		String mapped() default "Mapped";

	}

	@AttributeMappingAnnotation
	static class AttributeMapping {

	}

	@AttributeMappingAnnotation("Other")
	static class OtherMapping {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface AttributeMappingAnnotation {

		@PropertyMapping("mapped")
		String value() default "Mapped";

	}

}
