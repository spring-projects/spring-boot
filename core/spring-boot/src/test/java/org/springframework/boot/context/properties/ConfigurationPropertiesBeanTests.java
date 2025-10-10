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

import java.lang.reflect.Constructor;
import java.util.Map;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.annotation.AnnotationDescription;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingConsumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.BindConstructorProvider;
import org.springframework.boot.context.properties.bind.BindMethod;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ResolvableType;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ConfigurationPropertiesBean}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigurationPropertiesBeanTests {

	@Test
	void getAllReturnsAll() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				NonAnnotatedComponent.class, AnnotatedComponent.class, AnnotatedBeanConfiguration.class,
				ValueObjectConfiguration.class)) {
			Map<String, ConfigurationPropertiesBean> all = ConfigurationPropertiesBean.getAll(context);
			assertThat(all).containsOnlyKeys("annotatedComponent", "annotatedBean", ValueObject.class.getName());
			ConfigurationPropertiesBean component = all.get("annotatedComponent");
			assertThat(component).isNotNull();
			assertThat(component.getName()).isEqualTo("annotatedComponent");
			assertThat(component.getInstance()).isInstanceOf(AnnotatedComponent.class);
			assertThat(component.getAnnotation()).isNotNull();
			assertThat(component.getType()).isEqualTo(AnnotatedComponent.class);
			assertThat(component.asBindTarget().getBindMethod()).isEqualTo(BindMethod.JAVA_BEAN);
			ConfigurationPropertiesBean bean = all.get("annotatedBean");
			assertThat(bean).isNotNull();
			assertThat(bean.getName()).isEqualTo("annotatedBean");
			assertThat(bean.getInstance()).isInstanceOf(AnnotatedBean.class);
			assertThat(bean.getType()).isEqualTo(AnnotatedBean.class);
			assertThat(bean.getAnnotation()).isNotNull();
			assertThat(bean.asBindTarget().getBindMethod()).isEqualTo(BindMethod.JAVA_BEAN);
			ConfigurationPropertiesBean valueObject = all.get(ValueObject.class.getName());
			assertThat(valueObject).isNotNull();
			assertThat(valueObject.getName()).isEqualTo(ValueObject.class.getName());
			assertThat(valueObject.getInstance()).isInstanceOf(ValueObject.class);
			assertThat(valueObject.getType()).isEqualTo(ValueObject.class);
			assertThat(valueObject.getAnnotation()).isNotNull();
			assertThat(valueObject.asBindTarget().getBindMethod()).isEqualTo(BindMethod.VALUE_OBJECT);
		}
	}

	@Test
	void getAllDoesNotFindABeanDeclaredInAStaticBeanMethodOnAConfigurationAndConfigurationPropertiesAnnotatedClass() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				StaticBeanMethodConfiguration.class)) {
			Map<String, ConfigurationPropertiesBean> all = ConfigurationPropertiesBean.getAll(context);
			assertThat(all).containsOnlyKeys("configurationPropertiesBeanTests.StaticBeanMethodConfiguration");
		}
	}

	@Test
	void getAllWhenHasBadBeanDoesNotFail() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				NonAnnotatedComponent.class, AnnotatedComponent.class, AnnotatedBeanConfiguration.class,
				ValueObjectConfiguration.class, BadBeanConfiguration.class)) {
			Map<String, ConfigurationPropertiesBean> all = ConfigurationPropertiesBean.getAll(context);
			assertThat(all).isNotEmpty();
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
			assertThat(propertiesBean.getType()).isEqualTo(AnnotatedComponent.class);
			assertThat(propertiesBean.getAnnotation().prefix()).isEqualTo("prefix");
			assertThat(propertiesBean.asBindTarget().getBindMethod()).isEqualTo(BindMethod.JAVA_BEAN);
		});
	}

	@Test
	void getWhenFactoryMethodIsAnnotatedReturnsBean() throws Throwable {
		get(NonAnnotatedBeanConfiguration.class, "nonAnnotatedBean", (propertiesBean) -> {
			assertThat(propertiesBean).isNotNull();
			assertThat(propertiesBean.getName()).isEqualTo("nonAnnotatedBean");
			assertThat(propertiesBean.getInstance()).isInstanceOf(NonAnnotatedBean.class);
			assertThat(propertiesBean.getType()).isEqualTo(NonAnnotatedBean.class);
			assertThat(propertiesBean.getAnnotation().prefix()).isEqualTo("prefix");
			assertThat(propertiesBean.asBindTarget().getBindMethod()).isEqualTo(BindMethod.JAVA_BEAN);
		});
	}

	@Test
	void getWhenImportedFactoryMethodIsAnnotatedAndMetadataCachingIsDisabledReturnsBean() throws Throwable {
		getWithoutBeanMetadataCaching(NonAnnotatedBeanImportConfiguration.class, "nonAnnotatedBean",
				(propertiesBean) -> {
					assertThat(propertiesBean).isNotNull();
					assertThat(propertiesBean.getName()).isEqualTo("nonAnnotatedBean");
					assertThat(propertiesBean.getInstance()).isInstanceOf(NonAnnotatedBean.class);
					assertThat(propertiesBean.getType()).isEqualTo(NonAnnotatedBean.class);
					assertThat(propertiesBean.getAnnotation().prefix()).isEqualTo("prefix");
					assertThat(propertiesBean.asBindTarget().getBindMethod()).isEqualTo(BindMethod.JAVA_BEAN);
				});
	}

	@Test
	void getWhenImportedFactoryMethodIsAnnotatedReturnsBean() throws Throwable {
		get(NonAnnotatedBeanImportConfiguration.class, "nonAnnotatedBean", (propertiesBean) -> {
			assertThat(propertiesBean).isNotNull();
			assertThat(propertiesBean.getName()).isEqualTo("nonAnnotatedBean");
			assertThat(propertiesBean.getInstance()).isInstanceOf(NonAnnotatedBean.class);
			assertThat(propertiesBean.getType()).isEqualTo(NonAnnotatedBean.class);
			assertThat(propertiesBean.getAnnotation().prefix()).isEqualTo("prefix");
			assertThat(propertiesBean.asBindTarget().getBindMethod()).isEqualTo(BindMethod.JAVA_BEAN);
		});
	}

	@Test
	void getWhenHasFactoryMethodBindsUsingMethodReturnType() throws Throwable {
		get(NonAnnotatedGenericBeanConfiguration.class, "nonAnnotatedGenericBean", (propertiesBean) -> {
			assertThat(propertiesBean).isNotNull();
			assertThat(propertiesBean.getType()).isEqualTo(NonAnnotatedGenericBean.class);
			assertThat(propertiesBean.asBindTarget().getBindMethod()).isEqualTo(BindMethod.JAVA_BEAN);
			ResolvableType type = propertiesBean.asBindTarget().getType();
			assertThat(type.resolve()).isEqualTo(NonAnnotatedGenericBean.class);
			assertThat(type.resolveGeneric(0)).isEqualTo(String.class);
		});
	}

	@Test
	void getWhenHasFactoryMethodWithoutAnnotationBindsUsingMethodType() throws Throwable {
		get(AnnotatedGenericBeanConfiguration.class, "annotatedGenericBean", (propertiesBean) -> {
			assertThat(propertiesBean).isNotNull();
			assertThat(propertiesBean.getType()).isEqualTo(AnnotatedGenericBean.class);
			assertThat(propertiesBean.asBindTarget().getBindMethod()).isEqualTo(BindMethod.JAVA_BEAN);
			ResolvableType type = propertiesBean.asBindTarget().getType();
			assertThat(type.resolve()).isEqualTo(AnnotatedGenericBean.class);
			assertThat(type.resolveGeneric(0)).isEqualTo(String.class);
		});
	}

	@Test
	void getWhenHasNoFactoryMethodBindsUsingObjectType() throws Throwable {
		get(AnnotatedGenericComponent.class, "annotatedGenericComponent", (propertiesBean) -> {
			assertThat(propertiesBean).isNotNull();
			assertThat(propertiesBean.getType()).isEqualTo(AnnotatedGenericComponent.class);
			assertThat(propertiesBean.asBindTarget().getBindMethod()).isEqualTo(BindMethod.JAVA_BEAN);
			ResolvableType type = propertiesBean.asBindTarget().getType();
			assertThat(type.resolve()).isEqualTo(AnnotatedGenericComponent.class);
			assertThat(type.getGeneric(0).resolve()).isNull();
		});
	}

	@Test
	void getWhenHasFactoryMethodAndBeanAnnotationFavorsFactoryMethod() throws Throwable {
		get(AnnotatedBeanConfiguration.class, "annotatedBean", (propertiesBean) -> {
			assertThat(propertiesBean).isNotNull();
			assertThat(propertiesBean.getAnnotation().prefix()).isEqualTo("factory");
		});
	}

	@Test
	void getWhenHasValidatedBeanBindsWithBeanAnnotation() throws Throwable {
		get(ValidatedBeanConfiguration.class, "validatedBean", (propertiesBean) -> {
			assertThat(propertiesBean).isNotNull();
			Validated validated = propertiesBean.asBindTarget().getAnnotation(Validated.class);
			assertThat(validated).isNotNull();
			assertThat(validated.value()).containsExactly(BeanGroup.class);
		});
	}

	@Test
	void getWhenHasValidatedFactoryMethodBindsWithFactoryMethodAnnotation() throws Throwable {
		get(ValidatedMethodConfiguration.class, "annotatedBean", (propertiesBean) -> {
			assertThat(propertiesBean).isNotNull();
			Validated validated = propertiesBean.asBindTarget().getAnnotation(Validated.class);
			assertThat(validated).isNotNull();
			assertThat(validated.value()).containsExactly(FactoryMethodGroup.class);
		});
	}

	@Test
	void getWhenHasValidatedBeanAndFactoryMethodBindsWithFactoryMethodAnnotation() throws Throwable {
		get(ValidatedMethodAndBeanConfiguration.class, "validatedBean", (propertiesBean) -> {
			assertThat(propertiesBean).isNotNull();
			Validated validated = propertiesBean.asBindTarget().getAnnotation(Validated.class);
			assertThat(validated).isNotNull();
			assertThat(validated.value()).containsExactly(FactoryMethodGroup.class);
		});
	}

	@Test
	void forValueObjectWithConstructorBindingAnnotatedClassReturnsBean() {
		ConfigurationPropertiesBean propertiesBean = ConfigurationPropertiesBean
			.forValueObject(ConstructorBindingOnConstructor.class, "valueObjectBean");
		assertThat(propertiesBean.getName()).isEqualTo("valueObjectBean");
		assertThat(propertiesBean.getInstance()).isNull();
		assertThat(propertiesBean.getType()).isEqualTo(ConstructorBindingOnConstructor.class);
		assertThat(propertiesBean.asBindTarget().getBindMethod()).isEqualTo(BindMethod.VALUE_OBJECT);
		assertThat(propertiesBean.getAnnotation()).isNotNull();
		Bindable<?> target = propertiesBean.asBindTarget();
		assertThat(target.getType()).isEqualTo(ResolvableType.forClass(ConstructorBindingOnConstructor.class));
		assertThat(target.getValue()).isNull();
		assertThat(BindConstructorProvider.DEFAULT.getBindConstructor(ConstructorBindingOnConstructor.class, false))
			.isNotNull();
	}

	@Test
	void forValueObjectWithRecordReturnsBean() {
		Class<?> implicitConstructorBinding = new ByteBuddy(ClassFileVersion.JAVA_V16).makeRecord()
			.name("org.springframework.boot.context.properties.ImplicitConstructorBinding")
			.annotateType(AnnotationDescription.Builder.ofType(ConfigurationProperties.class)
				.define("prefix", "implicit")
				.build())
			.defineRecordComponent("someString", String.class)
			.defineRecordComponent("someInteger", Integer.class)
			.make()
			.load(getClass().getClassLoader())
			.getLoaded();
		ConfigurationPropertiesBean propertiesBean = ConfigurationPropertiesBean
			.forValueObject(implicitConstructorBinding, "implicitBindingRecord");
		assertThat(propertiesBean.getName()).isEqualTo("implicitBindingRecord");
		assertThat(propertiesBean.getInstance()).isNull();
		assertThat(propertiesBean.getType()).isEqualTo(implicitConstructorBinding);
		assertThat(propertiesBean.asBindTarget().getBindMethod()).isEqualTo(BindMethod.VALUE_OBJECT);
		assertThat(propertiesBean.getAnnotation()).isNotNull();
		Bindable<?> target = propertiesBean.asBindTarget();
		assertThat(target.getType()).isEqualTo(ResolvableType.forClass(implicitConstructorBinding));
		assertThat(target.getValue()).isNull();
		Constructor<?> bindConstructor = BindConstructorProvider.DEFAULT.getBindConstructor(implicitConstructorBinding,
				false);
		assertThat(bindConstructor).isNotNull();
		assertThat(bindConstructor.getParameterTypes()).containsExactly(String.class, Integer.class);
	}

	@Test
	void forValueObjectWhenJavaBeanBindTypeThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> ConfigurationPropertiesBean.forValueObject(AnnotatedBean.class, "annotatedBean"))
			.withMessage("Bean 'annotatedBean' is not a @ConfigurationProperties value object");
		assertThatIllegalStateException()
			.isThrownBy(() -> ConfigurationPropertiesBean.forValueObject(NonAnnotatedBean.class, "nonAnnotatedBean"))
			.withMessage("Bean 'nonAnnotatedBean' is not a @ConfigurationProperties value object");

	}

	@Test
	void deduceBindMethodWhenNoConstructorBindingReturnsJavaBean() {
		BindMethod bindType = ConfigurationPropertiesBean.deduceBindMethod(NoConstructorBinding.class);
		assertThat(bindType).isEqualTo(BindMethod.JAVA_BEAN);
	}

	@Test
	void deduceBindMethodWhenConstructorBindingOnConstructorReturnsValueObject() {
		BindMethod bindType = ConfigurationPropertiesBean.deduceBindMethod(ConstructorBindingOnConstructor.class);
		assertThat(bindType).isEqualTo(BindMethod.VALUE_OBJECT);
	}

	@Test
	void deduceBindMethodWhenNoConstructorBindingAnnotationOnSingleParameterizedConstructorReturnsValueObject() {
		BindMethod bindType = ConfigurationPropertiesBean.deduceBindMethod(ConstructorBindingNoAnnotation.class);
		assertThat(bindType).isEqualTo(BindMethod.VALUE_OBJECT);
	}

	@Test
	void deduceBindMethodWhenConstructorBindingOnMultipleConstructorsThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(
					() -> ConfigurationPropertiesBean.deduceBindMethod(ConstructorBindingOnMultipleConstructors.class))
			.withMessage(ConstructorBindingOnMultipleConstructors.class.getName()
					+ " has more than one @ConstructorBinding constructor");
	}

	@Test
	void deduceBindMethodWithMultipleConstructorsReturnJavaBean() {
		BindMethod bindType = ConfigurationPropertiesBean
			.deduceBindMethod(NoConstructorBindingOnMultipleConstructors.class);
		assertThat(bindType).isEqualTo(BindMethod.JAVA_BEAN);
	}

	@Test
	void deduceBindMethodWithNoArgConstructorReturnsJavaBean() {
		BindMethod bindType = ConfigurationPropertiesBean.deduceBindMethod(JavaBeanWithNoArgConstructor.class);
		assertThat(bindType).isEqualTo(BindMethod.JAVA_BEAN);
	}

	@Test
	void deduceBindMethodWithSingleArgAutowiredConstructorReturnsJavaBean() {
		BindMethod bindType = ConfigurationPropertiesBean.deduceBindMethod(JavaBeanWithAutowiredConstructor.class);
		assertThat(bindType).isEqualTo(BindMethod.JAVA_BEAN);
	}

	@Test
	void constructorBindingAndAutowiredConstructorsShouldThrowException() {
		assertThatIllegalStateException().isThrownBy(
				() -> ConfigurationPropertiesBean.deduceBindMethod(ConstructorBindingAndAutowiredConstructors.class));
	}

	@Test
	void innerClassWithSyntheticFieldShouldReturnJavaBean() {
		BindMethod bindType = ConfigurationPropertiesBean.deduceBindMethod(Inner.class);
		assertThat(bindType).isEqualTo(BindMethod.JAVA_BEAN);
	}

	@Test
	void innerClassWithParameterizedConstructorShouldReturnJavaBean() {
		BindMethod bindType = ConfigurationPropertiesBean.deduceBindMethod(ParameterizedConstructorInner.class);
		assertThat(bindType).isEqualTo(BindMethod.JAVA_BEAN);
	}

	private void get(Class<?> configuration, String beanName,
			ThrowingConsumer<@Nullable ConfigurationPropertiesBean> consumer) throws Throwable {
		get(configuration, beanName, true, consumer);
	}

	private void getWithoutBeanMetadataCaching(Class<?> configuration, String beanName,
			ThrowingConsumer<@Nullable ConfigurationPropertiesBean> consumer) throws Throwable {
		get(configuration, beanName, false, consumer);
	}

	private void get(Class<?> configuration, String beanName, boolean cacheBeanMetadata,
			ThrowingConsumer<@Nullable ConfigurationPropertiesBean> consumer) throws Throwable {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.getBeanFactory().setCacheBeanMetadata(cacheBeanMetadata);
			context.register(configuration);
			context.refresh();
			Object bean = context.getBean(beanName);
			consumer.accept(ConfigurationPropertiesBean.get(context, bean, beanName));
		}
	}

	@Component("nonAnnotatedComponent")
	static class NonAnnotatedComponent {

	}

	@Component("annotatedComponent")
	@ConfigurationProperties("prefix")
	static class AnnotatedComponent {

	}

	@ConfigurationProperties("prefix")
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
		@ConfigurationProperties("prefix")
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
		@ConfigurationProperties("factory")
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

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(ValueObject.class)
	static class ValueObjectConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class BadBeanConfiguration {

		@Bean
		@Lazy
		BadBean badBean() {
			return new BadBean();
		}

	}

	static class BadBean {

		BadBean() {
			throw new IllegalStateException();
		}

	}

	@ConfigurationProperties
	static class ValueObject {

		ValueObject(String name) {
		}

	}

	static class BeanGroup {

	}

	static class FactoryMethodGroup {

	}

	@ConfigurationProperties
	static class NoConstructorBinding {

	}

	@ConfigurationProperties
	static class ConstructorBindingNoAnnotation {

		ConstructorBindingNoAnnotation(String name) {
		}

	}

	@ConfigurationProperties
	static class ConstructorBindingOnConstructor {

		ConstructorBindingOnConstructor(String name) {
			this(name, -1);
		}

		@ConstructorBinding
		ConstructorBindingOnConstructor(String name, int age) {
		}

	}

	@ConfigurationProperties
	static class ConstructorBindingOnMultipleConstructors {

		@ConstructorBinding
		ConstructorBindingOnMultipleConstructors(String name) {
			this(name, -1);
		}

		@ConstructorBinding
		ConstructorBindingOnMultipleConstructors(String name, int age) {
		}

	}

	@ConfigurationProperties
	static class NoConstructorBindingOnMultipleConstructors {

		NoConstructorBindingOnMultipleConstructors(String name) {
			this(name, -1);
		}

		NoConstructorBindingOnMultipleConstructors(String name, int age) {
		}

	}

	@ConfigurationProperties
	static class JavaBeanWithAutowiredConstructor {

		@Autowired
		JavaBeanWithAutowiredConstructor(String name) {
		}

	}

	@ConfigurationProperties
	static class JavaBeanWithNoArgConstructor {

		JavaBeanWithNoArgConstructor() {
		}

	}

	@ConfigurationProperties
	static class ConstructorBindingAndAutowiredConstructors {

		@Autowired
		ConstructorBindingAndAutowiredConstructors(String name) {
		}

		@ConstructorBinding
		ConstructorBindingAndAutowiredConstructors(Integer age) {
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(NonAnnotatedBeanConfigurationImportSelector.class)
	static class NonAnnotatedBeanImportConfiguration {

	}

	static class NonAnnotatedBeanConfigurationImportSelector implements ImportSelector {

		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return new String[] { NonAnnotatedBeanConfiguration.class.getName() };
		}

	}

	@ConfigurationProperties
	class Inner {

	}

	@ConfigurationProperties
	class ParameterizedConstructorInner {

		ParameterizedConstructorInner(Integer age) {

		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConfigurationProperties
	static class StaticBeanMethodConfiguration {

		@Bean
		static String stringBean() {
			return "example";
		}

	}

}
