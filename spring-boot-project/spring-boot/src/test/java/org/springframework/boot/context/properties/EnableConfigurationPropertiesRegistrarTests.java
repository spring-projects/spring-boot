/*
 * Copyright 2012-2022 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.ConfigurationPropertiesBean.BindMethod;
import org.springframework.core.type.AnnotationMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link EnableConfigurationPropertiesRegistrar}.
 *
 * @author Madhura Bhave
 * @author Stephane Nicoll
 */
class EnableConfigurationPropertiesRegistrarTests {

	private DefaultListableBeanFactory beanFactory;

	private EnableConfigurationPropertiesRegistrar registrar;

	@BeforeEach
	void setup() {
		this.beanFactory = spy(new DefaultListableBeanFactory());
		this.registrar = new EnableConfigurationPropertiesRegistrar();
	}

	@Test
	void typeWithDefaultConstructorShouldRegisterRootBeanDefinition() {
		register(TestConfiguration.class);
		BeanDefinition definition = this.beanFactory
				.getBeanDefinition("foo-" + getClass().getName() + "$FooProperties");
		assertThat(definition).satisfies(configurationPropertiesBeanDefinition(BindMethod.JAVA_BEAN));
	}

	@Test
	void constructorBoundPropertiesShouldRegisterConfigurationPropertiesBeanDefinition() {
		register(TestConfiguration.class);
		BeanDefinition definition = this.beanFactory
				.getBeanDefinition("bar-" + getClass().getName() + "$BarProperties");
		assertThat(definition).satisfies(configurationPropertiesBeanDefinition(BindMethod.VALUE_OBJECT));
	}

	@Test
	void typeWithMultipleConstructorsShouldRegisterGenericBeanDefinition() {
		register(TestConfiguration.class);
		BeanDefinition definition = this.beanFactory
				.getBeanDefinition("bing-" + getClass().getName() + "$BingProperties");
		assertThat(definition).satisfies(configurationPropertiesBeanDefinition(BindMethod.JAVA_BEAN));
	}

	@Test
	void typeWithNoAnnotationShouldFail() {
		assertThatIllegalStateException().isThrownBy(() -> register(InvalidConfiguration.class))
				.withMessageContaining("No ConfigurationProperties annotation found")
				.withMessageContaining(EnableConfigurationPropertiesRegistrar.class.getName());
	}

	@Test
	void registrationWithDuplicatedTypeShouldRegisterSingleBeanDefinition() {
		register(DuplicateConfiguration.class);
		String name = "foo-" + getClass().getName() + "$FooProperties";
		then(this.beanFactory).should().registerBeanDefinition(eq(name), any());
	}

	@Test
	void registrationWithNoTypeShouldNotRegisterAnything() {
		register(EmptyConfiguration.class);
		String[] names = this.beanFactory.getBeanNamesForType(Object.class);
		for (String name : names) {
			assertThat(name).doesNotContain("-");
		}
	}

	private Consumer<BeanDefinition> configurationPropertiesBeanDefinition(BindMethod bindMethod) {
		return (definition) -> {
			assertThat(definition).isExactlyInstanceOf(RootBeanDefinition.class);
			assertThat(definition.hasAttribute(BindMethod.class.getName())).isTrue();
			assertThat(definition.getAttribute(BindMethod.class.getName())).isEqualTo(bindMethod);
		};
	}

	private void register(Class<?> configuration) {
		AnnotationMetadata metadata = AnnotationMetadata.introspect(configuration);
		this.registrar.registerBeanDefinitions(metadata, this.beanFactory);
	}

	@EnableConfigurationProperties({ FooProperties.class, BarProperties.class, BingProperties.class })
	static class TestConfiguration {

	}

	@EnableConfigurationProperties(EnableConfigurationPropertiesRegistrarTests.class)
	static class InvalidConfiguration {

	}

	@EnableConfigurationProperties({ FooProperties.class, FooProperties.class })
	static class DuplicateConfiguration {

	}

	@EnableConfigurationProperties
	static class EmptyConfiguration {

	}

	@ConfigurationProperties(prefix = "foo")
	static class FooProperties {

	}

	@ConfigurationProperties(prefix = "bar")
	static class BarProperties {

		BarProperties(String foo) {
		}

	}

	@ConfigurationProperties(prefix = "bing")
	static class BingProperties {

		BingProperties() {
		}

		BingProperties(String foo) {
		}

	}

}
