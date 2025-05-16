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

package org.springframework.boot.autoconfigure.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.Date;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport.ConditionAndOutcomes;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnBean @ConditionalOnBean}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Uladzislau Seuruk
 */
class ConditionalOnBeanTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	void testNameOnBeanCondition() {
		this.contextRunner.withUserConfiguration(FooConfiguration.class, OnBeanNameConfiguration.class)
			.run(this::hasBarBean);
	}

	@Test
	void testNameAndTypeOnBeanCondition() {
		this.contextRunner.withUserConfiguration(FooConfiguration.class, OnBeanNameAndTypeConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean("bar"));
	}

	@Test
	void testNameOnBeanConditionReverseOrder() {
		// Ideally this should be true
		this.contextRunner.withUserConfiguration(OnBeanNameConfiguration.class, FooConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean("bar"));
	}

	@Test
	void testClassOnBeanCondition() {
		this.contextRunner.withUserConfiguration(FooConfiguration.class, OnBeanClassConfiguration.class)
			.run(this::hasBarBean);
	}

	@Test
	void testClassOnBeanClassNameCondition() {
		this.contextRunner.withUserConfiguration(FooConfiguration.class, OnBeanClassNameConfiguration.class)
			.run(this::hasBarBean);
	}

	@Test
	void testOnBeanConditionWithXml() {
		this.contextRunner.withUserConfiguration(XmlConfiguration.class, OnBeanNameConfiguration.class)
			.run(this::hasBarBean);
	}

	@Test
	void testOnBeanConditionWithCombinedXml() {
		// Ideally this should be true
		this.contextRunner.withUserConfiguration(CombinedXmlConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean("bar"));
	}

	@Test
	void testAnnotationOnBeanCondition() {
		this.contextRunner.withUserConfiguration(FooConfiguration.class, OnAnnotationConfiguration.class)
			.run(this::hasBarBean);
	}

	@Test
	void testOnMissingBeanType() {
		this.contextRunner.withUserConfiguration(FooConfiguration.class, OnBeanMissingClassConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean("bar"));
	}

	@Test
	void withPropertyPlaceholderClassName() {
		this.contextRunner
			.withUserConfiguration(PropertySourcesPlaceholderConfigurer.class,
					WithPropertyPlaceholderClassNameConfiguration.class, OnBeanClassConfiguration.class)
			.withPropertyValues("mybeanclass=java.lang.String")
			.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	void beanProducedByFactoryBeanIsConsideredWhenMatchingOnAnnotation() {
		this.contextRunner
			.withUserConfiguration(FactoryBeanConfiguration.class, OnAnnotationWithFactoryBeanConfiguration.class)
			.run((context) -> {
				assertThat(context).hasBean("bar");
				assertThat(context).hasSingleBean(ExampleBean.class);
			});
	}

	@Test
	void beanProducedByFactoryBeanIsConsideredWhenMatchingOnAnnotation2() {
		this.contextRunner
			.withUserConfiguration(EarlyInitializationFactoryBeanConfiguration.class,
					EarlyInitializationOnAnnotationFactoryBeanConfiguration.class)
			.run((context) -> {
				assertThat(EarlyInitializationFactoryBeanConfiguration.calledWhenNoFrozen).as("calledWhenNoFrozen")
					.isFalse();
				assertThat(context).hasBean("bar");
				assertThat(context).hasSingleBean(ExampleBean.class);
			});
	}

	private void hasBarBean(AssertableApplicationContext context) {
		assertThat(context).hasBean("bar");
		assertThat(context.getBean("bar")).isEqualTo("bar");
	}

	@Test
	void onBeanConditionOutputShouldNotContainConditionalOnMissingBeanClassInMessage() {
		this.contextRunner.withUserConfiguration(OnBeanNameConfiguration.class).run((context) -> {
			Collection<ConditionAndOutcomes> conditionAndOutcomes = ConditionEvaluationReport
				.get(context.getSourceApplicationContext().getBeanFactory())
				.getConditionAndOutcomesBySource()
				.values();
			String message = conditionAndOutcomes.iterator().next().iterator().next().getOutcome().getMessage();
			assertThat(message).doesNotContain("@ConditionalOnMissingBean");
		});
	}

	@Test
	void conditionEvaluationConsidersChangeInTypeWhenBeanIsOverridden() {
		this.contextRunner.withAllowBeanDefinitionOverriding(true)
			.withUserConfiguration(OriginalDefinitionConfiguration.class, OverridingDefinitionConfiguration.class,
					ConsumingConfiguration.class)
			.run((context) -> {
				assertThat(context).hasBean("testBean");
				assertThat(context).hasSingleBean(Integer.class);
				assertThat(context).doesNotHaveBean(ConsumingConfiguration.class);
			});
	}

	@Test
	void parameterizedContainerWhenValueIsOfMissingBeanDoesNotMatch() {
		this.contextRunner
			.withUserConfiguration(ParameterizedWithoutCustomConfiguration.class,
					ParameterizedConditionWithValueConfiguration.class)
			.run((context) -> assertThat(context)
				.satisfies(beansAndContainersNamed(ExampleBean.class, "otherExampleBean")));
	}

	@Test
	void parameterizedContainerWhenValueIsOfExistingBeanMatches() {
		this.contextRunner
			.withUserConfiguration(ParameterizedWithCustomConfiguration.class,
					ParameterizedConditionWithValueConfiguration.class)
			.run((context) -> assertThat(context).satisfies(
					beansAndContainersNamed(ExampleBean.class, "customExampleBean", "conditionalCustomExampleBean")));
	}

	@Test
	void parameterizedContainerWhenValueIsOfMissingBeanRegistrationDoesNotMatch() {
		this.contextRunner
			.withUserConfiguration(ParameterizedWithoutCustomContainerConfiguration.class,
					ParameterizedConditionWithValueConfiguration.class)
			.run((context) -> assertThat(context)
				.satisfies(beansAndContainersNamed(ExampleBean.class, "otherExampleBean")));
	}

	@Test
	void parameterizedContainerWhenValueIsOfExistingBeanRegistrationMatches() {
		this.contextRunner
			.withUserConfiguration(ParameterizedWithCustomContainerConfiguration.class,
					ParameterizedConditionWithValueConfiguration.class)
			.run((context) -> assertThat(context).satisfies(
					beansAndContainersNamed(ExampleBean.class, "customExampleBean", "conditionalCustomExampleBean")));
	}

	@Test
	void parameterizedContainerWhenReturnTypeIsOfExistingBeanMatches() {
		this.contextRunner
			.withUserConfiguration(ParameterizedWithCustomConfiguration.class,
					ParameterizedConditionWithReturnTypeConfiguration.class)
			.run((context) -> assertThat(context).satisfies(
					beansAndContainersNamed(ExampleBean.class, "customExampleBean", "conditionalCustomExampleBean")));
	}

	@Test
	void parameterizedContainerWhenReturnTypeIsOfExistingBeanRegistrationMatches() {
		this.contextRunner
			.withUserConfiguration(ParameterizedWithCustomContainerConfiguration.class,
					ParameterizedConditionWithReturnTypeConfiguration.class)
			.run((context) -> assertThat(context).satisfies(
					beansAndContainersNamed(ExampleBean.class, "customExampleBean", "conditionalCustomExampleBean")));
	}

	@Test
	void parameterizedContainerWhenReturnRegistrationTypeIsOfExistingBeanMatches() {
		this.contextRunner
			.withUserConfiguration(ParameterizedWithCustomConfiguration.class,
					ParameterizedConditionWithReturnRegistrationTypeConfiguration.class)
			.run((context) -> assertThat(context).satisfies(
					beansAndContainersNamed(ExampleBean.class, "customExampleBean", "conditionalCustomExampleBean")));
	}

	@Test
	void parameterizedContainerWhenReturnRegistrationTypeIsOfExistingBeanRegistrationMatches() {
		this.contextRunner
			.withUserConfiguration(ParameterizedWithCustomContainerConfiguration.class,
					ParameterizedConditionWithReturnRegistrationTypeConfiguration.class)
			.run((context) -> assertThat(context).satisfies(
					beansAndContainersNamed(ExampleBean.class, "customExampleBean", "conditionalCustomExampleBean")));
	}

	@Test
	void conditionalOnBeanTypeIgnoresNotAutowireCandidateBean() {
		this.contextRunner
			.withUserConfiguration(NotAutowireCandidateConfiguration.class, OnBeanClassConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean("bar"));
	}

	@Test
	void conditionalOnBeanNameMatchesNotAutowireCandidateBean() {
		this.contextRunner.withUserConfiguration(NotAutowireCandidateConfiguration.class, OnBeanNameConfiguration.class)
			.run((context) -> assertThat(context).hasBean("bar"));
	}

	@Test
	void conditionalOnAnnotatedBeanIgnoresNotAutowireCandidateBean() {
		this.contextRunner
			.withUserConfiguration(AnnotatedNotAutowireCandidateConfiguration.class, OnAnnotationConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean("bar"));
	}

	@Test
	void conditionalOnBeanTypeIgnoresNotDefaultCandidateBean() {
		this.contextRunner.withUserConfiguration(NotDefaultCandidateConfiguration.class, OnBeanClassConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean("bar"));
	}

	@Test
	void conditionalOnBeanTypeIgnoresNotDefaultCandidateFactoryBean() {
		this.contextRunner
			.withUserConfiguration(NotDefaultCandidateFactoryBeanConfiguration.class,
					OnBeanClassWithFactoryBeanConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean("bar"));
	}

	@Test
	void conditionalOnBeanNameMatchesNotDefaultCandidateBean() {
		this.contextRunner.withUserConfiguration(NotDefaultCandidateConfiguration.class, OnBeanNameConfiguration.class)
			.run((context) -> assertThat(context).hasBean("bar"));
	}

	@Test
	void conditionalOnAnnotatedBeanIgnoresNotDefaultCandidateBean() {
		this.contextRunner
			.withUserConfiguration(AnnotatedNotDefaultCandidateConfiguration.class, OnAnnotationConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean("bar"));
	}

	@Test
	void genericWhenTypeArgumentMatches() {
		this.contextRunner.withUserConfiguration(ParameterizedWithCustomGenericConfiguration.class,
				GenericWithStringTypeArgumentsConfiguration.class, GenericWithIntegerTypeArgumentsConfiguration.class)
			.run((context) -> assertThat(context).satisfies(beansAndContainersNamed(GenericExampleBean.class,
					"customGenericExampleBean", "genericStringTypeArgumentsExampleBean")));
	}

	@Test
	void genericWhenTypeArgumentWithValueMatches() {
		this.contextRunner
			.withUserConfiguration(GenericWithStringConfiguration.class,
					TypeArgumentsConditionWithValueConfiguration.class)
			.run((context) -> assertThat(context).satisfies(beansAndContainersNamed(GenericExampleBean.class,
					"genericStringExampleBean", "genericStringWithValueExampleBean")));
	}

	@Test
	void genericWithValueWhenSubclassTypeArgumentMatches() {
		this.contextRunner
			.withUserConfiguration(ParameterizedWithCustomGenericConfiguration.class,
					TypeArgumentsConditionWithValueConfiguration.class)
			.run((context) -> assertThat(context).satisfies(beansAndContainersNamed(GenericExampleBean.class,
					"customGenericExampleBean", "genericStringWithValueExampleBean")));
	}

	@Test
	void parameterizedContainerGenericWhenTypeArgumentNotMatches() {
		this.contextRunner
			.withUserConfiguration(GenericWithIntegerConfiguration.class,
					TypeArgumentsConditionWithParameterizedContainerConfiguration.class)
			.run((context) -> assertThat(context)
				.satisfies(beansAndContainersNamed(GenericExampleBean.class, "genericIntegerExampleBean")));
	}

	@Test
	void parameterizedContainerGenericWhenTypeArgumentMatches() {
		this.contextRunner
			.withUserConfiguration(GenericWithStringConfiguration.class,
					TypeArgumentsConditionWithParameterizedContainerConfiguration.class)
			.run((context) -> assertThat(context).satisfies(beansAndContainersNamed(GenericExampleBean.class,
					"genericStringExampleBean", "parameterizedContainerGenericExampleBean")));
	}

	@Test
	void parameterizedContainerGenericWhenSubclassTypeArgumentMatches() {
		this.contextRunner
			.withUserConfiguration(ParameterizedWithCustomGenericConfiguration.class,
					TypeArgumentsConditionWithParameterizedContainerConfiguration.class)
			.run((context) -> assertThat(context).satisfies(beansAndContainersNamed(GenericExampleBean.class,
					"customGenericExampleBean", "parameterizedContainerGenericExampleBean")));
	}

	private Consumer<ConfigurableApplicationContext> beansAndContainersNamed(Class<?> type, String... names) {
		return (context) -> {
			String[] beans = context.getBeanNamesForType(type);
			String[] containers = context.getBeanNamesForType(TestParameterizedContainer.class);
			assertThat(StringUtils.concatenateStringArrays(beans, containers)).containsOnly(names);
		};
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(name = "foo")
	static class OnBeanNameConfiguration {

		@Bean
		String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(name = "foo", value = Date.class)
	static class OnBeanNameAndTypeConfiguration {

		@Bean
		String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(annotation = TestAnnotation.class)
	static class OnAnnotationConfiguration {

		@Bean
		String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(String.class)
	static class OnBeanClassConfiguration {

		@Bean
		String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(ExampleFactoryBean.class)
	static class OnBeanClassWithFactoryBeanConfiguration {

		@Bean
		String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(type = "java.lang.String")
	static class OnBeanClassNameConfiguration {

		@Bean
		String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(type = "some.type.Missing")
	static class OnBeanMissingClassConfiguration {

		@Bean
		String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@TestAnnotation
	static class FooConfiguration {

		@Bean
		String foo() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class NotAutowireCandidateConfiguration {

		@Bean(autowireCandidate = false)
		String foo() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class NotDefaultCandidateConfiguration {

		@Bean(defaultCandidate = false)
		String foo() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class NotDefaultCandidateFactoryBeanConfiguration {

		@Bean(defaultCandidate = false)
		ExampleFactoryBean exampleBeanFactoryBean() {
			return new ExampleFactoryBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ImportResource("org/springframework/boot/autoconfigure/condition/foo.xml")
	static class XmlConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@ImportResource("org/springframework/boot/autoconfigure/condition/foo.xml")
	@Import(OnBeanNameConfiguration.class)
	static class CombinedXmlConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@Import(WithPropertyPlaceholderClassNameRegistrar.class)
	static class WithPropertyPlaceholderClassNameConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class FactoryBeanConfiguration {

		@Bean
		ExampleFactoryBean exampleBeanFactoryBean() {
			return new ExampleFactoryBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(annotation = TestAnnotation.class)
	static class OnAnnotationWithFactoryBeanConfiguration {

		@Bean
		String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class EarlyInitializationFactoryBeanConfiguration {

		static boolean calledWhenNoFrozen;

		@Bean
		@TestAnnotation
		static FactoryBean<?> exampleBeanFactoryBean(ApplicationContext applicationContext) {
			// NOTE: must be static and return raw FactoryBean and not the subclass so
			// Spring can't guess type
			ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext) applicationContext)
				.getBeanFactory();
			calledWhenNoFrozen = calledWhenNoFrozen || !beanFactory.isConfigurationFrozen();
			return new ExampleFactoryBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(annotation = TestAnnotation.class)
	static class EarlyInitializationOnAnnotationFactoryBeanConfiguration {

		@Bean
		String bar() {
			return "bar";
		}

	}

	static class WithPropertyPlaceholderClassNameRegistrar implements ImportBeanDefinitionRegistrar {

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			RootBeanDefinition bd = new RootBeanDefinition();
			bd.setBeanClassName("${mybeanclass}");
			registry.registerBeanDefinition("mybean", bd);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class OriginalDefinitionConfiguration {

		@Bean
		String testBean() {
			return "test";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(String.class)
	static class OverridingDefinitionConfiguration {

		@Bean
		Integer testBean() {
			return 1;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(String.class)
	static class ConsumingConfiguration {

		ConsumingConfiguration(String testBean) {
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ParameterizedWithCustomConfiguration {

		@Bean
		CustomExampleBean customExampleBean() {
			return new CustomExampleBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ParameterizedWithoutCustomConfiguration {

		@Bean
		OtherExampleBean otherExampleBean() {
			return new OtherExampleBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ParameterizedWithoutCustomContainerConfiguration {

		@Bean
		TestParameterizedContainer<OtherExampleBean> otherExampleBean() {
			return new TestParameterizedContainer<>();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ParameterizedWithCustomContainerConfiguration {

		@Bean
		TestParameterizedContainer<CustomExampleBean> customExampleBean() {
			return new TestParameterizedContainer<>();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ParameterizedConditionWithValueConfiguration {

		@Bean
		@ConditionalOnBean(value = CustomExampleBean.class, parameterizedContainer = TestParameterizedContainer.class)
		CustomExampleBean conditionalCustomExampleBean() {
			return new CustomExampleBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ParameterizedConditionWithReturnTypeConfiguration {

		@Bean
		@ConditionalOnBean(parameterizedContainer = TestParameterizedContainer.class)
		CustomExampleBean conditionalCustomExampleBean() {
			return new CustomExampleBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ParameterizedConditionWithReturnRegistrationTypeConfiguration {

		@Bean
		@ConditionalOnBean(parameterizedContainer = TestParameterizedContainer.class)
		TestParameterizedContainer<CustomExampleBean> conditionalCustomExampleBean() {
			return new TestParameterizedContainer<>();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class AnnotatedNotAutowireCandidateConfiguration {

		@Bean(autowireCandidate = false)
		ExampleBean exampleBean() {
			return new ExampleBean("value");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class AnnotatedNotDefaultCandidateConfiguration {

		@Bean(defaultCandidate = false)
		ExampleBean exampleBean() {
			return new ExampleBean("value");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ParameterizedWithCustomGenericConfiguration {

		@Bean
		CustomGenericExampleBean customGenericExampleBean() {
			return new CustomGenericExampleBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class GenericWithStringConfiguration {

		@Bean
		GenericExampleBean<String> genericStringExampleBean() {
			return new GenericExampleBean<>("genericStringExampleBean");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class GenericWithStringTypeArgumentsConfiguration {

		@Bean
		@ConditionalOnBean
		GenericExampleBean<String> genericStringTypeArgumentsExampleBean() {
			return new GenericExampleBean<>("genericStringTypeArgumentsExampleBean");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class GenericWithIntegerConfiguration {

		@Bean
		GenericExampleBean<Integer> genericIntegerExampleBean() {
			return new GenericExampleBean<>(1_000);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class GenericWithIntegerTypeArgumentsConfiguration {

		@Bean
		@ConditionalOnBean
		GenericExampleBean<Integer> genericIntegerTypeArgumentsExampleBean() {
			return new GenericExampleBean<>(1_000);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TypeArgumentsConditionWithValueConfiguration {

		@Bean
		@ConditionalOnBean(GenericExampleBean.class)
		GenericExampleBean<String> genericStringWithValueExampleBean() {
			return new GenericExampleBean<>("genericStringWithValueExampleBean");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TypeArgumentsConditionWithParameterizedContainerConfiguration {

		@Bean
		@ConditionalOnBean(parameterizedContainer = TestParameterizedContainer.class)
		TestParameterizedContainer<GenericExampleBean<String>> parameterizedContainerGenericExampleBean() {
			return new TestParameterizedContainer<>();
		}

	}

	static class ExampleFactoryBean implements FactoryBean<ExampleBean> {

		@Override
		public ExampleBean getObject() {
			return new ExampleBean("fromFactory");
		}

		@Override
		public Class<?> getObjectType() {
			return ExampleBean.class;
		}

		@Override
		public boolean isSingleton() {
			return false;
		}

	}

	@TestAnnotation
	static class ExampleBean {

		private final String value;

		ExampleBean(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return this.value;
		}

	}

	static class CustomExampleBean extends ExampleBean {

		CustomExampleBean() {
			super("custom subclass");
		}

	}

	static class OtherExampleBean extends ExampleBean {

		OtherExampleBean() {
			super("other subclass");
		}

	}

	@TestAnnotation
	static class GenericExampleBean<T> {

		private final T value;

		GenericExampleBean(T value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return String.valueOf(this.value);
		}

	}

	static class CustomGenericExampleBean extends GenericExampleBean<String> {

		CustomGenericExampleBean() {
			super("custom subclass");
		}

	}

	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@interface TestAnnotation {

	}

}
