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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.condition.scan.ScanBean;
import org.springframework.boot.autoconfigure.condition.scan.ScannedFactoryBeanConfiguration;
import org.springframework.boot.autoconfigure.condition.scan.ScannedFactoryBeanWithBeanMethodArgumentsConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnMissingBean @ConditionalOnMissingBean}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Jakub Kubrynski
 * @author Andy Wilkinson
 * @author Uladzislau Seuruk
 */
@SuppressWarnings("resource")
class ConditionalOnMissingBeanTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	void testNameOnMissingBeanCondition() {
		this.contextRunner.withUserConfiguration(FooConfiguration.class, OnBeanNameConfiguration.class)
			.run((context) -> {
				assertThat(context).doesNotHaveBean("bar");
				assertThat(context.getBean("foo")).isEqualTo("foo");
			});
	}

	@Test
	void testNameOnMissingBeanConditionReverseOrder() {
		this.contextRunner.withUserConfiguration(OnBeanNameConfiguration.class, FooConfiguration.class)
			.run((context) -> {
				// Ideally this would be doesNotHaveBean, but the ordering is a
				// problem
				assertThat(context).hasBean("bar");
				assertThat(context.getBean("foo")).isEqualTo("foo");
			});
	}

	@Test
	void testNameAndTypeOnMissingBeanCondition() {
		// Arguably this should be hasBean, but as things are implemented the conditions
		// specified in the different attributes of @ConditionalOnBean are combined with
		// logical OR (not AND) so if any of them match the condition is true.
		this.contextRunner.withUserConfiguration(FooConfiguration.class, OnBeanNameAndTypeConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean("bar"));
	}

	@Test
	void hierarchyConsidered() {
		this.contextRunner.withUserConfiguration(FooConfiguration.class)
			.run((parent) -> new ApplicationContextRunner().withParent(parent)
				.withUserConfiguration(HierarchyConsideredConfiguration.class)
				.run((context) -> assertThat(context.containsLocalBean("bar")).isFalse()));
	}

	@Test
	void hierarchyNotConsidered() {
		this.contextRunner.withUserConfiguration(FooConfiguration.class)
			.run((parent) -> new ApplicationContextRunner().withParent(parent)
				.withUserConfiguration(HierarchyNotConsideredConfiguration.class)
				.run((context) -> assertThat(context.containsLocalBean("bar")).isTrue()));
	}

	@Test
	void impliedOnBeanMethod() {
		this.contextRunner.withUserConfiguration(ExampleBeanConfiguration.class, ImpliedOnBeanMethodConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(ExampleBean.class));
	}

	@Test
	void testAnnotationOnMissingBeanCondition() {
		this.contextRunner.withUserConfiguration(FooConfiguration.class, OnAnnotationConfiguration.class)
			.run((context) -> {
				assertThat(context).doesNotHaveBean("bar");
				assertThat(context.getBean("foo")).isEqualTo("foo");
			});
	}

	@Test
	void testAnnotationOnMissingBeanConditionWithScopedProxy() {
		this.contextRunner.withInitializer(this::registerScope)
			.withUserConfiguration(ScopedExampleBeanConfiguration.class, OnAnnotationConfiguration.class)
			.run((context) -> {
				assertThat(context).doesNotHaveBean("bar");
				assertThat(context.getBean(ScopedExampleBean.class)).hasToString("test");
			});
	}

	@Test
	void testAnnotationOnMissingBeanConditionWithEagerFactoryBean() {
		// Rigorous test for SPR-11069
		this.contextRunner
			.withUserConfiguration(FooConfiguration.class, OnAnnotationConfiguration.class,
					FactoryBeanXmlConfiguration.class, PropertyPlaceholderAutoConfiguration.class)
			.run((context) -> {
				assertThat(context).doesNotHaveBean("bar");
				assertThat(context).hasBean("example");
				assertThat(context.getBean("foo")).isEqualTo("foo");
			});
	}

	@Test // gh-42484
	void testAnnotationOnMissingBeanConditionOnMethodWhenNoAnnotatedBeans() {
		// There are no beans with @TestAnnotation but there is an UnrelatedExampleBean
		this.contextRunner
			.withUserConfiguration(UnrelatedExampleBeanConfiguration.class, OnAnnotationMethodConfiguration.class)
			.run((context) -> assertThat(context).hasBean("conditional"));
	}

	@Test
	void testOnMissingBeanConditionOutputShouldNotContainConditionalOnBeanClassInMessage() {
		this.contextRunner.withUserConfiguration(OnBeanNameConfiguration.class).run((context) -> {
			Collection<ConditionEvaluationReport.ConditionAndOutcomes> conditionAndOutcomes = ConditionEvaluationReport
				.get(context.getSourceApplicationContext().getBeanFactory())
				.getConditionAndOutcomesBySource()
				.values();
			String message = conditionAndOutcomes.iterator().next().iterator().next().getOutcome().getMessage();
			assertThat(message).doesNotContain("@ConditionalOnBean");
		});
	}

	@Test
	void testOnMissingBeanConditionWithFactoryBean() {
		this.contextRunner
			.withUserConfiguration(FactoryBeanConfiguration.class,
					ConditionalOnMissingBeanProducedByFactoryBeanConfiguration.class,
					PropertyPlaceholderAutoConfiguration.class)
			.run((context) -> assertThat(context.getBean(ExampleBean.class)).hasToString("fromFactory"));
	}

	@Test
	void testOnMissingBeanConditionWithComponentScannedFactoryBean() {
		this.contextRunner
			.withUserConfiguration(ComponentScannedFactoryBeanBeanMethodConfiguration.class,
					ConditionalOnMissingBeanProducedByFactoryBeanConfiguration.class,
					PropertyPlaceholderAutoConfiguration.class)
			.run((context) -> assertThat(context.getBean(ScanBean.class)).hasToString("fromFactory"));
	}

	@Test
	void testOnMissingBeanConditionWithComponentScannedFactoryBeanWithBeanMethodArguments() {
		this.contextRunner
			.withUserConfiguration(ComponentScannedFactoryBeanBeanMethodWithArgumentsConfiguration.class,
					ConditionalOnMissingBeanProducedByFactoryBeanConfiguration.class,
					PropertyPlaceholderAutoConfiguration.class)
			.run((context) -> assertThat(context.getBean(ScanBean.class)).hasToString("fromFactory"));
	}

	@Test
	void testOnMissingBeanConditionWithFactoryBeanWithBeanMethodArguments() {
		this.contextRunner
			.withUserConfiguration(FactoryBeanWithBeanMethodArgumentsConfiguration.class,
					ConditionalOnMissingBeanProducedByFactoryBeanConfiguration.class,
					PropertyPlaceholderAutoConfiguration.class)
			.withPropertyValues("theValue=foo")
			.run((context) -> assertThat(context.getBean(ExampleBean.class)).hasToString("fromFactory"));
	}

	@Test
	void testOnMissingBeanConditionWithConcreteFactoryBean() {
		this.contextRunner
			.withUserConfiguration(ConcreteFactoryBeanConfiguration.class,
					ConditionalOnMissingBeanProducedByFactoryBeanConfiguration.class,
					PropertyPlaceholderAutoConfiguration.class)
			.run((context) -> assertThat(context.getBean(ExampleBean.class)).hasToString("fromFactory"));
	}

	@Test
	void testOnMissingBeanConditionWithUnhelpfulFactoryBean() {
		// We could not tell that the FactoryBean would ultimately create an ExampleBean
		this.contextRunner
			.withUserConfiguration(UnhelpfulFactoryBeanConfiguration.class,
					ConditionalOnMissingBeanProducedByFactoryBeanConfiguration.class,
					PropertyPlaceholderAutoConfiguration.class)
			.run((context) -> assertThat(context).getBeans(ExampleBean.class).hasSize(2));
	}

	@Test
	void testOnMissingBeanConditionWithRegisteredFactoryBean() {
		this.contextRunner
			.withUserConfiguration(RegisteredFactoryBeanConfiguration.class,
					ConditionalOnMissingBeanProducedByFactoryBeanConfiguration.class,
					PropertyPlaceholderAutoConfiguration.class)
			.run((context) -> assertThat(context.getBean(ExampleBean.class)).hasToString("fromFactory"));
	}

	@Test
	void testOnMissingBeanConditionWithNonspecificFactoryBeanWithClassAttribute() {
		this.contextRunner
			.withUserConfiguration(NonspecificFactoryBeanClassAttributeConfiguration.class,
					ConditionalOnMissingBeanProducedByFactoryBeanConfiguration.class,
					PropertyPlaceholderAutoConfiguration.class)
			.run((context) -> assertThat(context.getBean(ExampleBean.class)).hasToString("fromFactory"));
	}

	@Test
	void testOnMissingBeanConditionWithNonspecificFactoryBeanWithStringAttribute() {
		this.contextRunner
			.withUserConfiguration(NonspecificFactoryBeanStringAttributeConfiguration.class,
					ConditionalOnMissingBeanProducedByFactoryBeanConfiguration.class,
					PropertyPlaceholderAutoConfiguration.class)
			.run((context) -> assertThat(context.getBean(ExampleBean.class)).hasToString("fromFactory"));
	}

	@Test
	void testOnMissingBeanConditionWithFactoryBeanInXml() {
		this.contextRunner
			.withUserConfiguration(FactoryBeanXmlConfiguration.class,
					ConditionalOnMissingBeanProducedByFactoryBeanConfiguration.class,
					PropertyPlaceholderAutoConfiguration.class)
			.run((context) -> assertThat(context.getBean(ExampleBean.class)).hasToString("fromFactory"));
	}

	@Test
	void testOnMissingBeanConditionWithIgnoredSubclass() {
		this.contextRunner
			.withUserConfiguration(CustomExampleBeanConfiguration.class,
					ConditionalOnIgnoredSubclassConfiguration.class, PropertyPlaceholderAutoConfiguration.class)
			.run((context) -> {
				assertThat(context).getBeans(ExampleBean.class).hasSize(2);
				assertThat(context).getBeans(CustomExampleBean.class).hasSize(1);
			});
	}

	@Test
	void testOnMissingBeanConditionWithIgnoredSubclassByName() {
		this.contextRunner
			.withUserConfiguration(CustomExampleBeanConfiguration.class,
					ConditionalOnIgnoredSubclassByNameConfiguration.class, PropertyPlaceholderAutoConfiguration.class)
			.run((context) -> {
				assertThat(context).getBeans(ExampleBean.class).hasSize(2);
				assertThat(context).getBeans(CustomExampleBean.class).hasSize(1);
			});
	}

	@Test
	void grandparentIsConsideredWhenUsingAncestorsStrategy() {
		this.contextRunner.withUserConfiguration(ExampleBeanConfiguration.class)
			.run((grandparent) -> new ApplicationContextRunner().withParent(grandparent)
				.run((parent) -> new ApplicationContextRunner().withParent(parent)
					.withUserConfiguration(ExampleBeanConfiguration.class, OnBeanInAncestorsConfiguration.class)
					.run((context) -> assertThat(context).getBeans(ExampleBean.class).hasSize(1))));
	}

	@Test
	void currentContextIsIgnoredWhenUsingAncestorsStrategy() {
		this.contextRunner.run((parent) -> new ApplicationContextRunner().withParent(parent)
			.withUserConfiguration(ExampleBeanConfiguration.class, OnBeanInAncestorsConfiguration.class)
			.run((context) -> assertThat(context).getBeans(ExampleBean.class).hasSize(2)));
	}

	@Test
	void beanProducedByFactoryBeanIsConsideredWhenMatchingOnAnnotation() {
		this.contextRunner
			.withUserConfiguration(ConcreteFactoryBeanConfiguration.class,
					OnAnnotationWithFactoryBeanConfiguration.class)
			.run((context) -> {
				assertThat(context).doesNotHaveBean("bar");
				assertThat(context).hasSingleBean(ExampleBean.class);
			});
	}

	@Test
	void parameterizedContainerWhenValueIsOfMissingBeanMatches() {
		this.contextRunner
			.withUserConfiguration(ParameterizedWithoutCustomConfiguration.class,
					ParameterizedConditionWithValueConfiguration.class)
			.run((context) -> assertThat(context).satisfies(
					beansAndContainersNamed(ExampleBean.class, "otherExampleBean", "conditionalCustomExampleBean")));
	}

	@Test
	void parameterizedContainerWhenValueIsOfExistingBeanDoesNotMatch() {
		this.contextRunner
			.withUserConfiguration(ParameterizedWithCustomConfiguration.class,
					ParameterizedConditionWithValueConfiguration.class)
			.run((context) -> assertThat(context)
				.satisfies(beansAndContainersNamed(ExampleBean.class, "customExampleBean")));
	}

	@Test
	void parameterizedContainerWhenValueIsOfMissingBeanRegistrationMatches() {
		this.contextRunner
			.withUserConfiguration(ParameterizedWithoutCustomContainerConfiguration.class,
					ParameterizedConditionWithValueConfiguration.class)
			.run((context) -> assertThat(context).satisfies(
					beansAndContainersNamed(ExampleBean.class, "otherExampleBean", "conditionalCustomExampleBean")));
	}

	@Test
	void parameterizedContainerWhenValueIsOfExistingBeanRegistrationDoesNotMatch() {
		this.contextRunner
			.withUserConfiguration(ParameterizedWithCustomContainerConfiguration.class,
					ParameterizedConditionWithValueConfiguration.class)
			.run((context) -> assertThat(context)
				.satisfies(beansAndContainersNamed(ExampleBean.class, "customExampleBean")));
	}

	@Test
	void parameterizedContainerWhenReturnTypeIsOfExistingBeanDoesNotMatch() {
		this.contextRunner
			.withUserConfiguration(ParameterizedWithCustomConfiguration.class,
					ParameterizedConditionWithReturnTypeConfiguration.class)
			.run((context) -> assertThat(context)
				.satisfies(beansAndContainersNamed(ExampleBean.class, "customExampleBean")));
	}

	@Test
	void parameterizedContainerWhenReturnTypeIsOfExistingBeanRegistrationDoesNotMatch() {
		this.contextRunner
			.withUserConfiguration(ParameterizedWithCustomContainerConfiguration.class,
					ParameterizedConditionWithReturnTypeConfiguration.class)
			.run((context) -> assertThat(context)
				.satisfies(beansAndContainersNamed(ExampleBean.class, "customExampleBean")));
	}

	@Test
	void parameterizedContainerWhenReturnRegistrationTypeIsOfExistingBeanDoesNotMatch() {
		this.contextRunner
			.withUserConfiguration(ParameterizedWithCustomConfiguration.class,
					ParameterizedConditionWithReturnRegistrationTypeConfiguration.class)
			.run((context) -> assertThat(context)
				.satisfies(beansAndContainersNamed(ExampleBean.class, "customExampleBean")));
	}

	@Test
	void parameterizedContainerWhenReturnRegistrationTypeIsOfExistingBeanRegistrationDoesNotMatch() {
		this.contextRunner
			.withUserConfiguration(ParameterizedWithCustomContainerConfiguration.class,
					ParameterizedConditionWithReturnRegistrationTypeConfiguration.class)
			.run((context) -> assertThat(context)
				.satisfies(beansAndContainersNamed(ExampleBean.class, "customExampleBean")));
	}

	@Test
	void typeBasedMatchingIgnoresBeanThatIsNotAutowireCandidate() {
		this.contextRunner.withUserConfiguration(NotAutowireCandidateConfiguration.class, OnBeanTypeConfiguration.class)
			.run((context) -> assertThat(context).hasBean("bar"));
	}

	@Test
	void nameBasedMatchingConsidersBeanThatIsNotAutowireCandidate() {
		this.contextRunner.withUserConfiguration(NotAutowireCandidateConfiguration.class, OnBeanNameConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean("bar"));
	}

	@Test
	void annotationBasedMatchingIgnoresBeanThatIsNotAutowireCandidateBean() {
		this.contextRunner
			.withUserConfiguration(AnnotatedNotAutowireCandidateConfiguration.class, OnAnnotationConfiguration.class)
			.run((context) -> assertThat(context).hasBean("bar"));
	}

	@Test
	void typeBasedMatchingIgnoresBeanThatIsNotDefaultCandidate() {
		this.contextRunner.withUserConfiguration(NotDefaultCandidateConfiguration.class, OnBeanTypeConfiguration.class)
			.run((context) -> assertThat(context).hasBean("bar"));
	}

	@Test
	void typeBasedMatchingIgnoresFactoryBeanThatIsNotDefaultCandidate() {
		this.contextRunner
			.withUserConfiguration(NotDefaultCandidateFactoryBeanConfiguration.class,
					ConditionalOnMissingFactoryBeanConfiguration.class)
			.run((context) -> assertThat(context).hasBean("&exampleFactoryBean")
				.hasBean("&additionalExampleFactoryBean"));
	}

	@Test
	void nameBasedMatchingConsidersBeanThatIsNotDefaultCandidate() {
		this.contextRunner.withUserConfiguration(NotDefaultCandidateConfiguration.class, OnBeanNameConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean("bar"));
	}

	@Test
	void annotationBasedMatchingIgnoresBeanThatIsNotDefaultCandidateBean() {
		this.contextRunner
			.withUserConfiguration(AnnotatedNotDefaultCandidateConfiguration.class, OnAnnotationConfiguration.class)
			.run((context) -> assertThat(context).hasBean("bar"));
	}

	@Test
	void genericWhenTypeArgumentNotMatches() {
		this.contextRunner
			.withUserConfiguration(GenericWithStringTypeArgumentsConfiguration.class,
					GenericWithIntegerTypeArgumentsConfiguration.class)
			.run((context) -> assertThat(context).satisfies(beansAndContainersNamed(GenericExampleBean.class,
					"genericStringExampleBean", "genericIntegerExampleBean")));
	}

	@Test
	void genericWhenSubclassTypeArgumentMatches() {
		this.contextRunner
			.withUserConfiguration(ParameterizedWithCustomGenericConfiguration.class,
					GenericWithStringTypeArgumentsConfiguration.class)
			.run((context) -> assertThat(context)
				.satisfies(beansAndContainersNamed(GenericExampleBean.class, "customGenericExampleBean")));
	}

	@Test
	void genericWhenSubclassTypeArgumentNotMatches() {
		this.contextRunner
			.withUserConfiguration(ParameterizedWithCustomGenericConfiguration.class,
					GenericWithIntegerTypeArgumentsConfiguration.class)
			.run((context) -> assertThat(context).satisfies(beansAndContainersNamed(GenericExampleBean.class,
					"customGenericExampleBean", "genericIntegerExampleBean")));
	}

	@Test
	void genericWhenTypeArgumentWithValueMatches() {
		this.contextRunner
			.withUserConfiguration(GenericWithStringTypeArgumentsConfiguration.class,
					TypeArgumentsConditionWithValueConfiguration.class)
			.run((context) -> assertThat(context)
				.satisfies(beansAndContainersNamed(GenericExampleBean.class, "genericStringExampleBean")));
	}

	@Test
	void genericWithValueWhenSubclassTypeArgumentMatches() {
		this.contextRunner
			.withUserConfiguration(ParameterizedWithCustomGenericConfiguration.class,
					TypeArgumentsConditionWithValueConfiguration.class)
			.run((context) -> assertThat(context)
				.satisfies(beansAndContainersNamed(GenericExampleBean.class, "customGenericExampleBean")));
	}

	@Test
	void parameterizedContainerGenericWhenTypeArgumentNotMatches() {
		this.contextRunner
			.withUserConfiguration(GenericWithIntegerTypeArgumentsConfiguration.class,
					TypeArgumentsConditionWithParameterizedContainerConfiguration.class)
			.run((context) -> assertThat(context).satisfies(beansAndContainersNamed(GenericExampleBean.class,
					"genericIntegerExampleBean", "parameterizedContainerGenericExampleBean")));
	}

	@Test
	void parameterizedContainerGenericWhenTypeArgumentMatches() {
		this.contextRunner
			.withUserConfiguration(GenericWithStringTypeArgumentsConfiguration.class,
					TypeArgumentsConditionWithParameterizedContainerConfiguration.class)
			.run((context) -> assertThat(context)
				.satisfies(beansAndContainersNamed(GenericExampleBean.class, "genericStringExampleBean")));
	}

	@Test
	void parameterizedContainerGenericWhenSubclassTypeArgumentMatches() {
		this.contextRunner
			.withUserConfiguration(ParameterizedWithCustomGenericConfiguration.class,
					TypeArgumentsConditionWithParameterizedContainerConfiguration.class)
			.run((context) -> assertThat(context)
				.satisfies(beansAndContainersNamed(GenericExampleBean.class, "customGenericExampleBean")));
	}

	private Consumer<ConfigurableApplicationContext> beansAndContainersNamed(Class<?> type, String... names) {
		return (context) -> {
			String[] beans = context.getBeanNamesForType(type);
			String[] containers = context.getBeanNamesForType(TestParameterizedContainer.class);
			assertThat(StringUtils.concatenateStringArrays(beans, containers)).containsOnly(names);
		};
	}

	private void registerScope(ConfigurableApplicationContext applicationContext) {
		applicationContext.getBeanFactory().registerScope("test", new TestScope());
	}

	@Configuration(proxyBeanMethods = false)
	static class OnBeanInAncestorsConfiguration {

		@Bean
		@ConditionalOnMissingBean(search = SearchStrategy.ANCESTORS)
		ExampleBean exampleBean2() {
			return new ExampleBean("test");
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(name = "foo")
	static class OnBeanNameConfiguration {

		@Bean
		String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(type = "java.lang.String")
	static class OnBeanTypeConfiguration {

		@Bean
		String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(name = "foo", value = Date.class)
	@ConditionalOnBean(name = "foo", value = Date.class)
	static class OnBeanNameAndTypeConfiguration {

		@Bean
		String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FactoryBeanConfiguration {

		@Bean
		ExampleFactoryBean exampleBeanFactoryBean() {
			return new ExampleFactoryBean("foo");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class NotDefaultCandidateFactoryBeanConfiguration {

		@Bean(defaultCandidate = false)
		ExampleFactoryBean exampleFactoryBean() {
			return new ExampleFactoryBean("foo");
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ComponentScan(basePackages = "org.springframework.boot.autoconfigure.condition.scan", useDefaultFilters = false,
			includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE,
					classes = ScannedFactoryBeanConfiguration.class))
	static class ComponentScannedFactoryBeanBeanMethodConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@ComponentScan(basePackages = "org.springframework.boot.autoconfigure.condition.scan", useDefaultFilters = false,
			includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE,
					classes = ScannedFactoryBeanWithBeanMethodArgumentsConfiguration.class))
	static class ComponentScannedFactoryBeanBeanMethodWithArgumentsConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class FactoryBeanWithBeanMethodArgumentsConfiguration {

		@Bean
		FactoryBean<ExampleBean> exampleBeanFactoryBean(@Value("${theValue}") String value) {
			return new ExampleFactoryBean(value);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConcreteFactoryBeanConfiguration {

		@Bean
		ExampleFactoryBean exampleBeanFactoryBean() {
			return new ExampleFactoryBean("foo");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class UnhelpfulFactoryBeanConfiguration {

		@Bean
		@SuppressWarnings("rawtypes")
		FactoryBean exampleBeanFactoryBean() {
			return new ExampleFactoryBean("foo");
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(NonspecificFactoryBeanClassAttributeRegistrar.class)
	static class NonspecificFactoryBeanClassAttributeConfiguration {

	}

	static class NonspecificFactoryBeanClassAttributeRegistrar implements ImportBeanDefinitionRegistrar {

		@Override
		public void registerBeanDefinitions(AnnotationMetadata meta, BeanDefinitionRegistry registry) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(NonspecificFactoryBean.class);
			builder.addConstructorArgValue("foo");
			builder.getBeanDefinition().setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, ExampleBean.class);
			registry.registerBeanDefinition("exampleBeanFactoryBean", builder.getBeanDefinition());
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(NonspecificFactoryBeanClassAttributeRegistrar.class)
	static class NonspecificFactoryBeanStringAttributeConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@Import(FactoryBeanRegistrar.class)
	static class RegisteredFactoryBeanConfiguration {

	}

	static class FactoryBeanRegistrar implements ImportBeanDefinitionRegistrar {

		@Override
		public void registerBeanDefinitions(AnnotationMetadata meta, BeanDefinitionRegistry registry) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ExampleFactoryBean.class);
			builder.addConstructorArgValue("foo");
			registry.registerBeanDefinition("exampleBeanFactoryBean", builder.getBeanDefinition());
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ImportResource("org/springframework/boot/autoconfigure/condition/factorybean.xml")
	static class FactoryBeanXmlConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class ConditionalOnMissingBeanProducedByFactoryBeanConfiguration {

		@Bean
		@ConditionalOnMissingBean
		ExampleBean createExampleBean() {
			return new ExampleBean("direct");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConditionalOnMissingFactoryBeanConfiguration {

		@Bean
		@ConditionalOnMissingBean
		ExampleFactoryBean additionalExampleFactoryBean() {
			return new ExampleFactoryBean("factory");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConditionalOnIgnoredSubclassConfiguration {

		@Bean
		@ConditionalOnMissingBean(ignored = CustomExampleBean.class)
		ExampleBean exampleBean() {
			return new ExampleBean("test");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConditionalOnIgnoredSubclassByNameConfiguration {

		@Bean
		@ConditionalOnMissingBean(
				ignoredType = "org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBeanTests$CustomExampleBean")
		ExampleBean exampleBean() {
			return new ExampleBean("test");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomExampleBeanConfiguration {

		@Bean
		CustomExampleBean customExampleBean() {
			return new CustomExampleBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(annotation = TestAnnotation.class)
	static class OnAnnotationConfiguration {

		@Bean
		String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class OnAnnotationMethodConfiguration {

		@Bean
		@ConditionalOnMissingBean(annotation = TestAnnotation.class)
		UnrelatedExampleBean conditional() {
			return new UnrelatedExampleBean("conditional");
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(annotation = TestAnnotation.class)
	static class OnAnnotationWithFactoryBeanConfiguration {

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
	@ConditionalOnMissingBean(name = "foo")
	static class HierarchyConsideredConfiguration {

		@Bean
		String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(name = "foo", search = SearchStrategy.CURRENT)
	static class HierarchyNotConsideredConfiguration {

		@Bean
		String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ExampleBeanConfiguration {

		@Bean
		ExampleBean exampleBean() {
			return new ExampleBean("test");
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(ScopedExampleBean.class)
	static class ScopedExampleBeanConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class UnrelatedExampleBeanConfiguration {

		@Bean
		UnrelatedExampleBean unrelatedExampleBean() {
			return new UnrelatedExampleBean("test");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ImpliedOnBeanMethodConfiguration {

		@Bean
		@ConditionalOnMissingBean
		ExampleBean exampleBean2() {
			return new ExampleBean("test");
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
		@ConditionalOnMissingBean(parameterizedContainer = TestParameterizedContainer.class)
		CustomExampleBean conditionalCustomExampleBean() {
			return new CustomExampleBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ParameterizedConditionWithReturnTypeConfiguration {

		@Bean
		@ConditionalOnMissingBean(parameterizedContainer = TestParameterizedContainer.class)
		CustomExampleBean conditionalCustomExampleBean() {
			return new CustomExampleBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ParameterizedConditionWithReturnRegistrationTypeConfiguration {

		@Bean
		@ConditionalOnMissingBean(parameterizedContainer = TestParameterizedContainer.class)
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

		@Bean(autowireCandidate = false)
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
	static class GenericWithStringTypeArgumentsConfiguration {

		@Bean
		@ConditionalOnMissingBean
		GenericExampleBean<String> genericStringExampleBean() {
			return new GenericExampleBean<>("genericStringExampleBean");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class GenericWithIntegerTypeArgumentsConfiguration {

		@Bean
		@ConditionalOnMissingBean
		GenericExampleBean<Integer> genericIntegerExampleBean() {
			return new GenericExampleBean<>(1_000);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TypeArgumentsConditionWithValueConfiguration {

		@Bean
		@ConditionalOnMissingBean(GenericExampleBean.class)
		GenericExampleBean<String> genericStringWithValueExampleBean() {
			return new GenericExampleBean<>("genericStringWithValueExampleBean");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TypeArgumentsConditionWithParameterizedContainerConfiguration {

		@Bean
		@ConditionalOnMissingBean(parameterizedContainer = TestParameterizedContainer.class)
		TestParameterizedContainer<GenericExampleBean<String>> parameterizedContainerGenericExampleBean() {
			return new TestParameterizedContainer<>();
		}

	}

	static class ExampleFactoryBean implements FactoryBean<ExampleBean> {

		ExampleFactoryBean(String value) {
			Assert.state(!value.contains("$"), "value should not contain '$'");
		}

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

	static class NonspecificFactoryBean implements FactoryBean<Object> {

		NonspecificFactoryBean(String value) {
			Assert.state(!value.contains("$"), "value should not contain '$'");
		}

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

	@Scope(scopeName = "test", proxyMode = ScopedProxyMode.TARGET_CLASS)
	static class ScopedExampleBean extends ExampleBean {

		ScopedExampleBean() {
			super("test");
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

	static class UnrelatedExampleBean {

		private final String value;

		UnrelatedExampleBean(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return this.value;
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

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@interface TestAnnotation {

	}

	static class TestScope extends SimpleThreadScope {

	}

}
