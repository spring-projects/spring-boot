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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AssertProvider;
import org.assertj.core.error.BasicErrorMessageFactory;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.TypeHint;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.aot.AotServices;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.context.properties.ConfigurationPropertiesBeanFactoryInitializationAotProcessor.ConfigurationPropertiesReflectionHintsContribution;
import org.springframework.boot.context.properties.bind.BindMethod;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigurationPropertiesBeanFactoryInitializationAotProcessor}.
 *
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 * @author Sebastien Deleuze
 * @author Andy Wilkinson
 * @author Ashish Vaghela
 */
class ConfigurationPropertiesBeanFactoryInitializationAotProcessorTests {

	private final ConfigurationPropertiesBeanFactoryInitializationAotProcessor processor = new ConfigurationPropertiesBeanFactoryInitializationAotProcessor();

	@Test
	void configurationPropertiesBeanFactoryInitializationAotProcessorIsRegistered() {
		assertThat(AotServices.factories().load(BeanFactoryInitializationAotProcessor.class))
			.anyMatch(ConfigurationPropertiesBeanFactoryInitializationAotProcessor.class::isInstance);
	}

	@Test
	void processNoMatchesReturnsNullContribution() {
		assertThat(process(String.class)).isNull();
	}

	@Test
	void manuallyRegisteredSingletonBindsAsJavaBean() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("test", new SampleProperties());
		ConfigurationPropertiesReflectionHintsContribution contribution = process(beanFactory);
		assertThat(contribution).isNotNull();
		assertThat(singleBindable(contribution)).hasBindMethod(BindMethod.JAVA_BEAN).hasType(SampleProperties.class);
		assertThat(typeHints(contribution).map(TypeHint::getType))
			.containsExactly(TypeReference.of(SampleProperties.class));
	}

	@Test
	void javaBeanConfigurationPropertiesBindAsJavaBean() {
		ConfigurationPropertiesReflectionHintsContribution contribution = process(EnableJavaBeanProperties.class);
		assertThat(contribution).isNotNull();
		assertThat(singleBindable(contribution)).hasBindMethod(BindMethod.JAVA_BEAN).hasType(JavaBeanProperties.class);
		assertThat(typeHints(contribution).map(TypeHint::getType))
			.containsExactly(TypeReference.of(JavaBeanProperties.class));
	}

	@Test
	void constructorBindingConfigurationPropertiesBindAsValueObject() {
		ConfigurationPropertiesReflectionHintsContribution contribution = process(
				EnableConstructorBindingProperties.class);
		assertThat(contribution).isNotNull();
		assertThat(singleBindable(contribution)).hasBindMethod(BindMethod.VALUE_OBJECT)
			.hasType(ConstructorBindingProperties.class);
		assertThat(typeHints(contribution).map(TypeHint::getType))
			.containsExactly(TypeReference.of(ConstructorBindingProperties.class));
	}

	@Test
	void possibleConstructorBindingPropertiesDefinedThroughBeanMethodBindAsJavaBean() {
		ConfigurationPropertiesReflectionHintsContribution contribution = process(
				PossibleConstructorBindingPropertiesBeanMethodConfiguration.class);
		assertThat(contribution).isNotNull();
		assertThat(singleBindable(contribution)).hasBindMethod(BindMethod.JAVA_BEAN)
			.hasType(PossibleConstructorBindingProperties.class);
		assertThat(typeHints(contribution).map(TypeHint::getType))
			.containsExactly(TypeReference.of(PossibleConstructorBindingProperties.class));
	}

	@Test
	void possibleConstructorBindingPropertiesDefinedThroughEnabledAnnotationBindAsValueObject() {
		ConfigurationPropertiesReflectionHintsContribution contribution = process(
				EnablePossibleConstructorBindingProperties.class);
		assertThat(contribution).isNotNull();
		assertThat(singleBindable(contribution)).hasBindMethod(BindMethod.VALUE_OBJECT)
			.hasType(PossibleConstructorBindingProperties.class);
		assertThat(typeHints(contribution).map(TypeHint::getType))
			.containsExactly(TypeReference.of(PossibleConstructorBindingProperties.class));
	}

	@Test
	void classPropertyRegistersReflectionHintsForConfiguredValue() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("class-value.type", StringBuilder.class.getName());
		ConfigurationPropertiesReflectionHintsContribution contribution = process(EnableClassValueProperties.class,
				environment);
		assertThat(contribution).isNotNull();
		assertThat(typeHints(contribution).map(TypeHint::getType)).contains(TypeReference.of(StringBuilder.class));
	}

	@Test
	void classPropertyInMapRegistersReflectionHintsForConfiguredValues() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("class-value.types.first", StringBuilder.class.getName());
		environment.setProperty("class-value.types.second", StringBuffer.class.getName());
		ConfigurationPropertiesReflectionHintsContribution contribution = process(EnableClassValueProperties.class,
				environment);
		assertThat(contribution).isNotNull();
		assertThat(typeHints(contribution).map(TypeHint::getType)).contains(TypeReference.of(StringBuilder.class),
				TypeReference.of(StringBuffer.class));
	}

	@Test
	void classPropertyWithNoConfiguredValueRegistersNoAdditionalHints() {
		ConfigurationPropertiesReflectionHintsContribution contribution = process(EnableClassValueProperties.class,
				new MockEnvironment());
		assertThat(contribution).isNotNull();
		assertThat(typeHints(contribution).map(TypeHint::getType))
			.containsExactly(TypeReference.of(ClassValueProperties.class));
	}

	@Test
	void classPropertyWithDefaultValueRegistersReflectionHintsForDefaultValue() {
		ConfigurationPropertiesReflectionHintsContribution contribution = process(
				EnableClassValueWithDefaultProperties.class, new MockEnvironment());
		assertThat(contribution).isNotNull();
		assertThat(typeHints(contribution).map(TypeHint::getType)).contains(TypeReference.of(StringBuilder.class));
	}

	private Stream<TypeHint> typeHints(ConfigurationPropertiesReflectionHintsContribution contribution) {
		TestGenerationContext generationContext = new TestGenerationContext();
		contribution.applyTo(generationContext, mock(BeanFactoryInitializationCode.class));
		return generationContext.getRuntimeHints().reflection().typeHints();
	}

	private @Nullable ConfigurationPropertiesReflectionHintsContribution process(Class<?> config) {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(config)) {
			return process(context.getBeanFactory());
		}
	}

	private @Nullable ConfigurationPropertiesReflectionHintsContribution process(Class<?> config,
			ConfigurableEnvironment environment) {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.setEnvironment(environment);
			context.register(config);
			context.refresh();
			return process(context.getBeanFactory());
		}
	}

	private @Nullable ConfigurationPropertiesReflectionHintsContribution process(
			ConfigurableListableBeanFactory beanFactory) {
		return this.processor.processAheadOfTime(beanFactory);
	}

	private BindableAssertProvider singleBindable(ConfigurationPropertiesReflectionHintsContribution contribution) {
		assertThat(contribution.getBindables()).hasSize(1);
		return new BindableAssertProvider(contribution.getBindables().iterator().next());
	}

	@ConfigurationProperties("test")
	static class SampleProperties {

	}

	@EnableConfigurationProperties(JavaBeanProperties.class)
	static class EnableJavaBeanProperties {

	}

	@ConfigurationProperties("java-bean")
	static class JavaBeanProperties {

		private @Nullable String value;

		@Nullable String getValue() {
			return this.value;
		}

		void setValue(@Nullable String value) {
			this.value = value;
		}

	}

	@EnableConfigurationProperties(ConstructorBindingProperties.class)
	static class EnableConstructorBindingProperties {

	}

	@ConfigurationProperties("constructor-binding")
	static class ConstructorBindingProperties {

		private final String value;

		ConstructorBindingProperties(String value) {
			this.value = value;
		}

		String getValue() {
			return this.value;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class PossibleConstructorBindingPropertiesBeanMethodConfiguration {

		@Bean
		@ConfigurationProperties("bean-method")
		PossibleConstructorBindingProperties possibleConstructorBindingProperties() {
			return new PossibleConstructorBindingProperties("alpha");
		}

	}

	@EnableConfigurationProperties(PossibleConstructorBindingProperties.class)
	static class EnablePossibleConstructorBindingProperties {

	}

	@ConfigurationProperties("possible-constructor-binding")
	static class PossibleConstructorBindingProperties {

		private @Nullable String value;

		PossibleConstructorBindingProperties(String arg) {

		}

		@Nullable String getValue() {
			return this.value;
		}

		void setValue(@Nullable String value) {
			this.value = value;
		}

	}

	@EnableConfigurationProperties(ClassValueProperties.class)
	static class EnableClassValueProperties {

	}

	@ConfigurationProperties("class-value")
	static class ClassValueProperties {

		private @Nullable Class<?> type;

		private final Map<String, Class<?>> types = new LinkedHashMap<>();

		@Nullable Class<?> getType() {
			return this.type;
		}

		void setType(@Nullable Class<?> type) {
			this.type = type;
		}

		Map<String, Class<?>> getTypes() {
			return this.types;
		}

	}

	@EnableConfigurationProperties(ClassValueWithDefaultProperties.class)
	static class EnableClassValueWithDefaultProperties {

	}

	@ConfigurationProperties("class-default")
	static class ClassValueWithDefaultProperties {

		private Class<?> type = StringBuilder.class;

		Class<?> getType() {
			return this.type;
		}

		void setType(Class<?> type) {
			this.type = type;
		}

	}

	static class BindableAssertProvider implements AssertProvider<BindableAssert> {

		private final Bindable<?> bindable;

		BindableAssertProvider(Bindable<?> bindable) {
			this.bindable = bindable;
		}

		@Override
		public BindableAssert assertThat() {
			return new BindableAssert(this.bindable);
		}

	}

	static class BindableAssert extends AbstractAssert<BindableAssert, Bindable<?>> {

		BindableAssert(Bindable<?> bindable) {
			super(bindable, BindableAssert.class);
		}

		BindableAssert hasBindMethod(BindMethod bindMethod) {
			if (this.actual.getBindMethod() != bindMethod) {
				throwAssertionError(
						new BasicErrorMessageFactory("Expected %s to have bind method %s but bind method was %s",
								this.actual, bindMethod, this.actual.getBindMethod()));
			}
			return this;
		}

		BindableAssert hasType(Class<?> type) {
			if (!type.equals(this.actual.getType().resolve())) {
				throwAssertionError(new BasicErrorMessageFactory("Expected %s to have type %s but type was %s",
						this.actual, type, this.actual.getType().resolve()));
			}
			return this;
		}

	}

}
