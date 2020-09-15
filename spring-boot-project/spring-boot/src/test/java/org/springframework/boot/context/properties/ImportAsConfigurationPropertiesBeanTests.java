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

package org.springframework.boot.context.properties;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ImportAsConfigurationPropertiesBean}.
 *
 * @author Phillip Webb
 */
class ImportAsConfigurationPropertiesBeanTests {

	@Test
	void importJavaBean() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context.getEnvironment(), "test.name=spring");
			context.register(JavaBeanConfig.class);
			context.refresh();
			assertThat(context.getBean(JavaBean.class).getName()).isEqualTo("spring");
		}
	}

	@Test
	void importValueObject() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context.getEnvironment(), "test.value=spring");
			context.register(ValueObjectConfig.class);
			context.refresh();
			assertThat(context.getBean(ValueObject.class).getValue()).isEqualTo("spring");
		}
	}

	@Test
	void importMultiConstructorValueObjectFails() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context.getEnvironment(), "test.name=spring");
			context.register(MultiConstructorValueObjectConfig.class);
			assertThatIllegalStateException().isThrownBy(context::refresh).havingCause()
					.withMessageContaining("Unable to deduce");
		}
	}

	@Test
	void importMultipleTypes() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context.getEnvironment(), "test.name=spring",
					"test.value=boot");
			context.register(ImportMultipleTypesConfig.class);
			context.refresh();
			assertThat(context.getBean(JavaBean.class).getName()).isEqualTo("spring");
			assertThat(context.getBean(ValueObject.class).getValue()).isEqualTo("boot");
		}
	}

	@Test
	void importRepeatedAnnotations() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context.getEnvironment(), "jb.name=spring",
					"vo.value=boot");
			context.register(ImportRepeatedAnnotationsConfig.class);
			context.refresh();
			assertThat(context.getBean(JavaBean.class).getName()).isEqualTo("spring");
			assertThat(context.getBean(ValueObject.class).getValue()).isEqualTo("boot");
		}
	}

	@Test
	void importAnnoatedBeanConfig() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context.getEnvironment(), "onbean.name=spring");
			context.register(ImportAnnotatedClassConfig.class);
			context.refresh();
			assertThat(context.getBean(JavaBean.class).getName()).isEqualTo("spring");
		}
	}

	@Configuration
	@ImportAsConfigurationPropertiesBean(prefix = "test", type = JavaBean.class)
	static class JavaBeanConfig {

	}

	static class JavaBean {

		private String name;

		String getName() {
			return this.name;
		}

		void setName(String name) {
			this.name = name;
		}

	}

	@Configuration
	@ImportAsConfigurationPropertiesBean(prefix = "test", type = ValueObject.class)
	static class ValueObjectConfig {

	}

	static class ValueObject {

		private final String value;

		ValueObject(String value) {
			this.value = value;
		}

		String getValue() {
			return this.value;
		}

	}

	@Configuration
	@ImportAsConfigurationPropertiesBean(prefix = "test", type = MultiConstructorValueObject.class)
	static class MultiConstructorValueObjectConfig {

	}

	static class MultiConstructorValueObject {

		MultiConstructorValueObject() {
		}

		MultiConstructorValueObject(String name) {
		}

		MultiConstructorValueObject(String name, int age) {
		}

	}

	@Configuration
	@ImportAsConfigurationPropertiesBean(type = { ValueObject.class, JavaBean.class }, prefix = "test")
	static class ImportMultipleTypesConfig {

	}

	@Configuration
	@ImportAsConfigurationPropertiesBean(type = ValueObject.class, prefix = "vo")
	@ImportAsConfigurationPropertiesBean(type = JavaBean.class, prefix = "jb")
	static class ImportRepeatedAnnotationsConfig {

	}

	@Configuration
	@ImportAsConfigurationPropertiesBean(AnnotatedJavaBean.class)
	static class ImportAnnotatedClassConfig {

	}

	@ConfigurationProperties(prefix = "onbean")
	static class AnnotatedJavaBean extends JavaBean {

	}

}
