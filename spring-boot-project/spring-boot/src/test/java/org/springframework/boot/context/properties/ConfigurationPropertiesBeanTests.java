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

package org.springframework.boot.context.properties;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingConsumer;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationPropertiesBean}.
 *
 * @author Phillip Webb
 */
class ConfigurationPropertiesBeanTests {

	@Test
	void getAllReturnsAll() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				NonAnnotatedComponent.class, AnnotatedComponent.class, AnnotatedBeanConfiguration.class)) {
			Map<String, ConfigurationPropertiesBean> all = ConfigurationPropertiesBean.getAll(context);
			assertThat(all).containsOnlyKeys("annotatedComponent", "annotatedBean");
			ConfigurationPropertiesBean component = all.get("annotatedComponent");
			assertThat(component.getName()).isEqualTo("annotatedComponent");
			assertThat(component.getInstance()).isInstanceOf(AnnotatedComponent.class);
			assertThat(component.getAnnotation()).isNotNull();
			ConfigurationPropertiesBean bean = all.get("annotatedBean");
			assertThat(bean.getName()).isEqualTo("annotatedBean");
			assertThat(bean.getInstance()).isInstanceOf(AnnotatedBean.class);
			assertThat(bean.getAnnotation()).isNotNull();
		}
	}

	@Test
	void getWhenNotAnnotatedReturnsNull() throws Throwable {
		get(NonAnnotatedComponent.class, "nonAnnotatedComponent",
				(propertiesBean) -> assertThat(propertiesBean).isNull());
	}

	@Test
	void getWhenBeanIsAnnotatedReturnsBean() throws Throwable {
		get(AnnotatedComponent.class, "annotatedComponent", (propertiesBean) -> {
			assertThat(propertiesBean).isNotNull();
			assertThat(propertiesBean.getName()).isEqualTo("annotatedComponent");
			assertThat(propertiesBean.getInstance()).isInstanceOf(AnnotatedComponent.class);
			assertThat(propertiesBean.getAnnotation().prefix()).isEqualTo("prefix");
		});
	}

	@Test
	void getWhenFactoryMethodIsAnnotatedReturnsBean() throws Throwable {
		get(NonAnnotatedBeanConfiguration.class, "nonAnnotatedBean", (propertiesBean) -> {
			assertThat(propertiesBean).isNotNull();
			assertThat(propertiesBean.getName()).isEqualTo("nonAnnotatedBean");
			assertThat(propertiesBean.getInstance()).isInstanceOf(NonAnnotatedBean.class);
			assertThat(propertiesBean.getAnnotation().prefix()).isEqualTo("prefix");
		});
	}

	@Test
	void getWhenHasFactoryMethodBindsUsingMethodReturnType() throws Throwable {
		get(NonAnnotatedGenericBeanConfiguration.class, "nonAnnotatedGenericBean", (propertiesBean) -> {
			ResolvableType type = propertiesBean.asBindTarget().getType();
			assertThat(type.resolve()).isEqualTo(NonAnnotatedGenericBean.class);
			assertThat(type.resolveGeneric(0)).isEqualTo(String.class);
		});
	}

	@Test
	void getWhenHasFactoryMethodWithoutAnnotationBindsUsingMethodType() throws Throwable {
		get(AnnotatedGenericBeanConfiguration.class, "annotatedGenericBean", (propertiesBean) -> {
			ResolvableType type = propertiesBean.asBindTarget().getType();
			assertThat(type.resolve()).isEqualTo(AnnotatedGenericBean.class);
			assertThat(type.resolveGeneric(0)).isEqualTo(String.class);
		});
	}

	@Test
	void getWhenHasNoFactoryMethodBindsUsingObjectType() throws Throwable {
		get(AnnotatedGenericComponent.class, "annotatedGenericComponent", (propertiesBean) -> {
			ResolvableType type = propertiesBean.asBindTarget().getType();
			assertThat(type.resolve()).isEqualTo(AnnotatedGenericComponent.class);
			assertThat(type.getGeneric(0).resolve()).isNull();
		});
	}

	@Test
	void getWhenHasFactoryMethodAndBeanAnnotationFavorsFactoryMethod() throws Throwable {
		get(AnnotatedBeanConfiguration.class, "annotatedBean",
				(propertiesBean) -> assertThat(propertiesBean.getAnnotation().prefix()).isEqualTo("factory"));
	}

	@Test
	void getWhenHasValidatedBeanBindsWithBeanAnnotation() throws Throwable {
		get(ValidatedBeanConfiguration.class, "validatedBean", (propertiesBean) -> {
			Validated validated = propertiesBean.asBindTarget().getAnnotation(Validated.class);
			assertThat(validated.value()).containsExactly(BeanGroup.class);
		});
	}

	@Test
	void getWhenHasValidatedFactoryMethodBindsWithFactoryMethodAnnotation() throws Throwable {
		get(ValidatedMethodConfiguration.class, "annotatedBean", (propertiesBean) -> {
			Validated validated = propertiesBean.asBindTarget().getAnnotation(Validated.class);
			assertThat(validated.value()).containsExactly(FactoryMethodGroup.class);
		});
	}

	@Test
	void getWhenHasValidatedBeanAndFactoryMethodBindsWithFactoryMethodAnnotation() throws Throwable {
		get(ValidatedMethodAndBeanConfiguration.class, "validatedBean", (propertiesBean) -> {
			Validated validated = propertiesBean.asBindTarget().getAnnotation(Validated.class);
			assertThat(validated.value()).containsExactly(FactoryMethodGroup.class);
		});
	}

	private void get(Class<?> configuration, String beanName, ThrowingConsumer<ConfigurationPropertiesBean> consumer)
			throws Throwable {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(configuration)) {
			Object bean = context.getBean(beanName);
			consumer.accept(ConfigurationPropertiesBean.get(context, bean, beanName));
		}
	}

	@Component("nonAnnotatedComponent")
	static class NonAnnotatedComponent {

	}

	@Component("annotatedComponent")
	@ConfigurationProperties(prefix = "prefix")
	static class AnnotatedComponent {

	}

	@ConfigurationProperties(prefix = "prefix")
	static class AnnotatedBean {

	}

	static class NonAnnotatedBean {

	}

	static class NonAnnotatedGenericBean<T> {

	}

	@ConfigurationProperties
	static class AnnotatedGenericBean<T> {

	}

	@Component("annotatedGenericComponent")
	@ConfigurationProperties
	static class AnnotatedGenericComponent<T> {

	}

	@Validated(BeanGroup.class)
	@ConfigurationProperties
	static class ValidatedBean {

	}

	@Configuration(proxyBeanMethods = false)
	static class NonAnnotatedBeanConfiguration {

		@Bean
		@ConfigurationProperties(prefix = "prefix")
		NonAnnotatedBean nonAnnotatedBean() {
			return new NonAnnotatedBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class NonAnnotatedGenericBeanConfiguration {

		@Bean
		@ConfigurationProperties
		NonAnnotatedGenericBean<String> nonAnnotatedGenericBean() {
			return new NonAnnotatedGenericBean<>();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class AnnotatedGenericBeanConfiguration {

		@Bean
		AnnotatedGenericBean<String> annotatedGenericBean() {
			return new AnnotatedGenericBean<>();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class AnnotatedBeanConfiguration {

		@Bean
		@ConfigurationProperties(prefix = "factory")
		AnnotatedBean annotatedBean() {
			return new AnnotatedBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ValidatedBeanConfiguration {

		@Bean
		ValidatedBean validatedBean() {
			return new ValidatedBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ValidatedMethodConfiguration {

		@Bean
		@Validated(FactoryMethodGroup.class)
		AnnotatedBean annotatedBean() {
			return new AnnotatedBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ValidatedMethodAndBeanConfiguration {

		@Bean
		@Validated(FactoryMethodGroup.class)
		ValidatedBean validatedBean() {
			return new ValidatedBean();
		}

	}

	static class BeanGroup {

	}

	static class FactoryMethodGroup {

	}

}
