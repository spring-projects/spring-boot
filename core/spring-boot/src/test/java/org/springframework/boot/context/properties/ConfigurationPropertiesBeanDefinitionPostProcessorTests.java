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

package org.springframework.boot.context.properties;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.context.properties.bind.BindMethod;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationPropertiesBeanDefinitionPostProcessor}.
 *
 * @author Ujjawal Tyagi
 */
class ConfigurationPropertiesBeanDefinitionPostProcessorTests {

	@Test
	void postProcessorStampsBindMethodAttributeOnJavaBeanProperties() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		BeanDefinition definition = BeanDefinitionBuilder
				.genericBeanDefinition(JavaBeanProperties.class)
				.getBeanDefinition();
		beanFactory.registerBeanDefinition("javaBean", definition);

		new ConfigurationPropertiesBeanDefinitionPostProcessor().postProcessBeanDefinitionRegistry(beanFactory);

		BeanDefinition processed = beanFactory.getBeanDefinition("javaBean");
		assertThat(processed.getAttribute(BindMethod.class.getName())).isEqualTo(BindMethod.JAVA_BEAN);
	}

	@Test
	void postProcessorStampsBindMethodAttributeAndInstanceSupplierOnValueObjectProperties() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		BeanDefinition definition = BeanDefinitionBuilder
				.genericBeanDefinition(ValueObjectProperties.class)
				.getBeanDefinition();
		beanFactory.registerBeanDefinition("valueObject", definition);

		new ConfigurationPropertiesBeanDefinitionPostProcessor().postProcessBeanDefinitionRegistry(beanFactory);

		BeanDefinition processed = beanFactory.getBeanDefinition("valueObject");
		assertThat(processed.getAttribute(BindMethod.class.getName())).isEqualTo(BindMethod.VALUE_OBJECT);
		assertThat(((AbstractBeanDefinition) processed).getInstanceSupplier()).isNotNull();
	}

	@Test
	void postProcessorSkipsBeansAlreadyHavingBindMethodAttribute() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		BeanDefinition definition = BeanDefinitionBuilder
				.genericBeanDefinition(JavaBeanProperties.class)
				.getBeanDefinition();
		// Pre-stamp with VALUE_OBJECT to simulate a bean already processed by the registrar
		BindMethodAttribute.set(definition, BindMethod.VALUE_OBJECT);
		beanFactory.registerBeanDefinition("alreadyProcessed", definition);

		new ConfigurationPropertiesBeanDefinitionPostProcessor().postProcessBeanDefinitionRegistry(beanFactory);

		// Must not override the pre-existing attribute
		assertThat(definition.getAttribute(BindMethod.class.getName())).isEqualTo(BindMethod.VALUE_OBJECT);
	}

	@Test
	void endToEndJavaBeanPropertiesBoundWhenRegisteredProgrammatically() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, "test.java-prop=hello");
			ConfigurationPropertiesBindingPostProcessor.register(context);
			context.registerBeanDefinition("javaBean",
					BeanDefinitionBuilder.genericBeanDefinition(JavaBeanProperties.class).getBeanDefinition());
			context.refresh();

			JavaBeanProperties bean = context.getBean(JavaBeanProperties.class);
			assertThat(bean.getJavaProp()).isEqualTo("hello");
		}
	}

	@Test
	void endToEndValueObjectPropertiesBoundWhenRegisteredProgrammatically() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, "test.value-prop=world");
			ConfigurationPropertiesBindingPostProcessor.register(context);
			context.registerBeanDefinition("valueObject",
					BeanDefinitionBuilder.genericBeanDefinition(ValueObjectProperties.class).getBeanDefinition());
			context.refresh();

			ValueObjectProperties bean = context.getBean(ValueObjectProperties.class);
			assertThat(bean.getValueProp()).isEqualTo("world");
		}
	}

	@ConfigurationProperties("test")
	static class JavaBeanProperties {

		private String javaProp = "";

		public String getJavaProp() {
			return this.javaProp;
		}

		public void setJavaProp(String javaProp) {
			this.javaProp = javaProp;
		}

	}

	@ConfigurationProperties("test")
	static class ValueObjectProperties {

		private final String valueProp;

		ValueObjectProperties(String valueProp) {
			this.valueProp = valueProp;
		}

		public String getValueProp() {
			return this.valueProp;
		}

	}

}
