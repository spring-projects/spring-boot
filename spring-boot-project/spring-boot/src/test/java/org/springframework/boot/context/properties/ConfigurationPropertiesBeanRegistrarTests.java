/*
 * Copyright 2012-2021 the original author or authors.
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

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.ConfigurationPropertiesBean.BindMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ConfigurationPropertiesBeanRegistrar}.
 *
 * @author Madhura Bhave
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class ConfigurationPropertiesBeanRegistrarTests {

	private BeanDefinitionRegistry registry = new DefaultListableBeanFactory();

	private ConfigurationPropertiesBeanRegistrar registrar = new ConfigurationPropertiesBeanRegistrar(this.registry);

	@Test
	void registerWhenNotAlreadyRegisteredAddBeanDefinition() {
		String beanName = "beancp-" + BeanConfigurationProperties.class.getName();
		this.registrar.register(BeanConfigurationProperties.class);
		BeanDefinition definition = this.registry.getBeanDefinition(beanName);
		assertThat(definition).isNotNull();
		assertThat(definition.getBeanClassName()).isEqualTo(BeanConfigurationProperties.class.getName());
	}

	@Test
	void registerWhenAlreadyContainsNameDoesNotReplace() {
		String beanName = "beancp-" + BeanConfigurationProperties.class.getName();
		this.registry.registerBeanDefinition(beanName, new GenericBeanDefinition());
		this.registrar.register(BeanConfigurationProperties.class);
		BeanDefinition definition = this.registry.getBeanDefinition(beanName);
		assertThat(definition).isNotNull();
		assertThat(definition.getBeanClassName()).isNull();
	}

	@Test
	void registerWhenNoAnnotationThrowsException() {
		assertThatIllegalStateException()
				.isThrownBy(() -> this.registrar.register(NoAnnotationConfigurationProperties.class))
				.withMessageContaining("No ConfigurationProperties annotation found");
	}

	@Test
	void registerWhenValueObjectRegistersValueObjectBeanDefinition() {
		String beanName = "valuecp-" + ValueObjectConfigurationProperties.class.getName();
		this.registrar.register(ValueObjectConfigurationProperties.class);
		BeanDefinition definition = this.registry.getBeanDefinition(beanName);
		assertThat(definition).satisfies(configurationPropertiesBeanDefinition(BindMethod.VALUE_OBJECT));
	}

	@Test
	void registerWhenNotValueObjectRegistersRootBeanDefinitionWithJavaBeanBindMethod() {
		String beanName = MultiConstructorBeanConfigurationProperties.class.getName();
		this.registrar.register(MultiConstructorBeanConfigurationProperties.class);
		BeanDefinition definition = this.registry.getBeanDefinition(beanName);
		assertThat(definition).satisfies(configurationPropertiesBeanDefinition(BindMethod.JAVA_BEAN));
	}

	private Consumer<BeanDefinition> configurationPropertiesBeanDefinition(BindMethod bindMethod) {
		return (definition) -> {
			assertThat(definition).isExactlyInstanceOf(RootBeanDefinition.class);
			assertThat(definition.hasAttribute(BindMethod.class.getName())).isTrue();
			assertThat(definition.getAttribute(BindMethod.class.getName())).isEqualTo(bindMethod);
		};
	}

	@ConfigurationProperties(prefix = "beancp")
	static class BeanConfigurationProperties {

	}

	static class NoAnnotationConfigurationProperties {

	}

	@ConstructorBinding
	@ConfigurationProperties("valuecp")
	static class ValueObjectConfigurationProperties {

		ValueObjectConfigurationProperties(String name) {
		}

	}

	@ConfigurationProperties
	static class MultiConstructorBeanConfigurationProperties {

		MultiConstructorBeanConfigurationProperties() {
		}

		MultiConstructorBeanConfigurationProperties(String name) {
		}

	}

}
