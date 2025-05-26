/*
 * Copyright 2012-2025 the original author or authors.
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

import org.springframework.aop.scope.ScopedProxyFactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.bind.BindMethod;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ConfigurationPropertiesBeanRegistrar}.
 *
 * @author Madhura Bhave
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Yanming Zhou
 */
class ConfigurationPropertiesBeanRegistrarTests {

	private final BeanDefinitionRegistry registry = new DefaultListableBeanFactory();

	private final ConfigurationPropertiesBeanRegistrar registrar = new ConfigurationPropertiesBeanRegistrar(
			this.registry);

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
		this.registry.registerBeanDefinition(beanName, new RootBeanDefinition());
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
		assertThat(definition).satisfies(hasBindMethodAttribute(BindMethod.VALUE_OBJECT));
	}

	@Test
	void registerWhenNotValueObjectRegistersRootBeanDefinitionWithJavaBeanBindMethod() {
		String beanName = MultiConstructorBeanConfigurationProperties.class.getName();
		this.registrar.register(MultiConstructorBeanConfigurationProperties.class);
		BeanDefinition definition = this.registry.getBeanDefinition(beanName);
		assertThat(definition).satisfies(hasBindMethodAttribute(BindMethod.JAVA_BEAN));
	}

	@Test
	void registerWhenNoScopeUsesSingleton() {
		String beanName = "beancp-" + BeanConfigurationProperties.class.getName();
		this.registrar.register(BeanConfigurationProperties.class);
		BeanDefinition definition = this.registry.getBeanDefinition(beanName);
		assertThat(definition).isNotNull();
		assertThat(definition.getScope()).isEqualTo(BeanDefinition.SCOPE_SINGLETON);
	}

	@Test
	void registerScopedBeanDefinition() {
		String beanName = "beancp-" + ScopedBeanConfigurationProperties.class.getName();
		this.registrar.register(ScopedBeanConfigurationProperties.class);
		BeanDefinition beanDefinition = this.registry.getBeanDefinition(beanName);
		assertThat(beanDefinition).isNotNull();
		assertThat(beanDefinition.getBeanClassName()).isEqualTo(ScopedBeanConfigurationProperties.class.getName());
		assertThat(beanDefinition.getScope()).isEqualTo(BeanDefinition.SCOPE_PROTOTYPE);
	}

	@Test
	void registerScopedBeanDefinitionWithProxyMode() {
		String beanName = "beancp-" + ProxyScopedBeanConfigurationProperties.class.getName();
		this.registrar.register(ProxyScopedBeanConfigurationProperties.class);
		BeanDefinition proxiedBeanDefinition = this.registry.getBeanDefinition(beanName);
		assertThat(proxiedBeanDefinition).isNotNull();
		assertThat(proxiedBeanDefinition.getBeanClassName()).isEqualTo(ScopedProxyFactoryBean.class.getName());
		String targetBeanName = (String) proxiedBeanDefinition.getPropertyValues().get("targetBeanName");
		assertThat(targetBeanName).isNotNull();
		BeanDefinition beanDefinition = this.registry.getBeanDefinition(targetBeanName);
		assertThat(beanDefinition).isNotNull();
		assertThat(beanDefinition.getBeanClassName()).isEqualTo(ProxyScopedBeanConfigurationProperties.class.getName());
		assertThat(beanDefinition.getScope()).isEqualTo(BeanDefinition.SCOPE_PROTOTYPE);
	}

	@Test
	void registerBeanDefinitionWithCommonDefinitionAnnotations() {
		String beanName = "beancp-" + PrimaryConfigurationProperties.class.getName();
		this.registrar.register(PrimaryConfigurationProperties.class);
		BeanDefinition beanDefinition = this.registry.getBeanDefinition(beanName);
		assertThat(beanDefinition).isNotNull();
		assertThat(beanDefinition.isPrimary()).isEqualTo(true);
	}

	private Consumer<BeanDefinition> hasBindMethodAttribute(BindMethod bindMethod) {
		return (definition) -> {
			assertThat(definition.hasAttribute(BindMethod.class.getName())).isTrue();
			assertThat(definition.getAttribute(BindMethod.class.getName())).isEqualTo(bindMethod);
		};
	}

	@ConfigurationProperties("beancp")
	static class BeanConfigurationProperties {

	}

	@ConfigurationProperties("beancp")
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	static class ScopedBeanConfigurationProperties {

	}

	@ConfigurationProperties("beancp")
	@Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE, proxyMode = ScopedProxyMode.TARGET_CLASS)
	static class ProxyScopedBeanConfigurationProperties {

	}

	@ConfigurationProperties("beancp")
	@Primary
	static class PrimaryConfigurationProperties {

	}

	static class NoAnnotationConfigurationProperties {

	}

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
