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

package org.springframework.boot.autoconfigure.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Date;
import java.util.function.Consumer;

import org.junit.Test;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
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
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnMissingBean}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Jakub Kubrynski
 * @author Andy Wilkinson
 */
@SuppressWarnings("resource")
public class ConditionalOnMissingBeanTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	public void testNameOnMissingBeanCondition() {
		this.contextRunner.withUserConfiguration(FooConfiguration.class,
				OnBeanNameConfiguration.class).run((context) -> {
					assertThat(context).doesNotHaveBean("bar");
					assertThat(context.getBean("foo")).isEqualTo("foo");
				});
	}

	@Test
	public void testNameOnMissingBeanConditionReverseOrder() {
		this.contextRunner.withUserConfiguration(OnBeanNameConfiguration.class,
				FooConfiguration.class).run((context) -> {
					// Ideally this would be doesNotHaveBean, but the ordering is a
					// problem
					assertThat(context).hasBean("bar");
					assertThat(context.getBean("foo")).isEqualTo("foo");
				});
	}

	@Test
	public void testNameAndTypeOnMissingBeanCondition() {
		// Arguably this should be hasBean, but as things are implemented the conditions
		// specified in the different attributes of @ConditionalOnBean are combined with
		// logical OR (not AND) so if any of them match the condition is true.
		this.contextRunner
				.withUserConfiguration(FooConfiguration.class,
						OnBeanNameAndTypeConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean("bar"));
	}

	@Test
	public void hierarchyConsidered() {
		this.contextRunner.withUserConfiguration(FooConfiguration.class)
				.run((parent) -> new ApplicationContextRunner().withParent(parent)
						.withUserConfiguration(HierarchyConsidered.class)
						.run((context) -> assertThat(context.containsLocalBean("bar"))
								.isFalse()));
	}

	@Test
	public void hierarchyNotConsidered() {
		this.contextRunner.withUserConfiguration(FooConfiguration.class)
				.run((parent) -> new ApplicationContextRunner().withParent(parent)
						.withUserConfiguration(HierarchyNotConsidered.class)
						.run((context) -> assertThat(context.containsLocalBean("bar"))
								.isTrue()));
	}

	@Test
	public void impliedOnBeanMethod() {
		this.contextRunner
				.withUserConfiguration(ExampleBeanConfiguration.class,
						ImpliedOnBeanMethod.class)
				.run((context) -> assertThat(context).hasSingleBean(ExampleBean.class));
	}

	@Test
	public void testAnnotationOnMissingBeanCondition() {
		this.contextRunner.withUserConfiguration(FooConfiguration.class,
				OnAnnotationConfiguration.class).run((context) -> {
					assertThat(context).doesNotHaveBean("bar");
					assertThat(context.getBean("foo")).isEqualTo("foo");
				});
	}

	@Test
	public void testAnnotationOnMissingBeanConditionWithEagerFactoryBean() {
		// Rigorous test for SPR-11069
		this.contextRunner.withUserConfiguration(FooConfiguration.class,
				OnAnnotationConfiguration.class, FactoryBeanXmlConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class).run((context) -> {
					assertThat(context).doesNotHaveBean("bar");
					assertThat(context).hasBean("example");
					assertThat(context.getBean("foo")).isEqualTo("foo");
				});
	}

	@Test
	public void testOnMissingBeanConditionWithFactoryBean() {
		this.contextRunner
				.withUserConfiguration(FactoryBeanConfiguration.class,
						ConditionalOnFactoryBean.class,
						PropertyPlaceholderAutoConfiguration.class)
				.run((context) -> assertThat(
						context.getBean(ExampleBean.class).toString())
								.isEqualTo("fromFactory"));
	}

	@Test
	public void testOnMissingBeanConditionWithComponentScannedFactoryBean() {
		this.contextRunner
				.withUserConfiguration(
						ComponentScannedFactoryBeanBeanMethodConfiguration.class,
						ConditionalOnFactoryBean.class,
						PropertyPlaceholderAutoConfiguration.class)
				.run((context) -> assertThat(
						context.getBean(ExampleBean.class).toString())
								.isEqualTo("fromFactory"));
	}

	@Test
	public void testOnMissingBeanConditionWithComponentScannedFactoryBeanWithBeanMethodArguments() {
		this.contextRunner.withUserConfiguration(
				ComponentScannedFactoryBeanBeanMethodWithArgumentsConfiguration.class,
				ConditionalOnFactoryBean.class,
				PropertyPlaceholderAutoConfiguration.class)
				.run((context) -> assertThat(
						context.getBean(ExampleBean.class).toString())
								.isEqualTo("fromFactory"));
	}

	@Test
	public void testOnMissingBeanConditionWithFactoryBeanWithBeanMethodArguments() {
		this.contextRunner
				.withUserConfiguration(
						FactoryBeanWithBeanMethodArgumentsConfiguration.class,
						ConditionalOnFactoryBean.class,
						PropertyPlaceholderAutoConfiguration.class)
				.withPropertyValues("theValue=foo")
				.run((context) -> assertThat(
						context.getBean(ExampleBean.class).toString())
								.isEqualTo("fromFactory"));
	}

	@Test
	public void testOnMissingBeanConditionWithConcreteFactoryBean() {
		this.contextRunner
				.withUserConfiguration(ConcreteFactoryBeanConfiguration.class,
						ConditionalOnFactoryBean.class,
						PropertyPlaceholderAutoConfiguration.class)
				.run((context) -> assertThat(
						context.getBean(ExampleBean.class).toString())
								.isEqualTo("fromFactory"));
	}

	@Test
	public void testOnMissingBeanConditionWithUnhelpfulFactoryBean() {
		// We could not tell that the FactoryBean would ultimately create an ExampleBean
		this.contextRunner
				.withUserConfiguration(UnhelpfulFactoryBeanConfiguration.class,
						ConditionalOnFactoryBean.class,
						PropertyPlaceholderAutoConfiguration.class)
				.run((context) -> assertThat(context).getBeans(ExampleBean.class)
						.hasSize(2));
	}

	@Test
	public void testOnMissingBeanConditionWithRegisteredFactoryBean() {
		this.contextRunner
				.withUserConfiguration(RegisteredFactoryBeanConfiguration.class,
						ConditionalOnFactoryBean.class,
						PropertyPlaceholderAutoConfiguration.class)
				.run((context) -> assertThat(
						context.getBean(ExampleBean.class).toString())
								.isEqualTo("fromFactory"));
	}

	@Test
	public void testOnMissingBeanConditionWithNonspecificFactoryBeanWithClassAttribute() {
		this.contextRunner
				.withUserConfiguration(
						NonspecificFactoryBeanClassAttributeConfiguration.class,
						ConditionalOnFactoryBean.class,
						PropertyPlaceholderAutoConfiguration.class)
				.run((context) -> assertThat(
						context.getBean(ExampleBean.class).toString())
								.isEqualTo("fromFactory"));
	}

	@Test
	public void testOnMissingBeanConditionWithNonspecificFactoryBeanWithStringAttribute() {
		this.contextRunner
				.withUserConfiguration(
						NonspecificFactoryBeanStringAttributeConfiguration.class,
						ConditionalOnFactoryBean.class,
						PropertyPlaceholderAutoConfiguration.class)
				.run((context) -> assertThat(
						context.getBean(ExampleBean.class).toString())
								.isEqualTo("fromFactory"));
	}

	@Test
	public void testOnMissingBeanConditionWithFactoryBeanInXml() {
		this.contextRunner
				.withUserConfiguration(FactoryBeanXmlConfiguration.class,
						ConditionalOnFactoryBean.class,
						PropertyPlaceholderAutoConfiguration.class)
				.run((context) -> assertThat(
						context.getBean(ExampleBean.class).toString())
								.isEqualTo("fromFactory"));
	}

	@Test
	public void testOnMissingBeanConditionWithIgnoredSubclass() {
		this.contextRunner.withUserConfiguration(CustomExampleBeanConfiguration.class,
				ConditionalOnIgnoredSubclass.class,
				PropertyPlaceholderAutoConfiguration.class).run((context) -> {
					assertThat(context).getBeans(ExampleBean.class).hasSize(2);
					assertThat(context).getBeans(CustomExampleBean.class).hasSize(1);
				});
	}

	@Test
	public void testOnMissingBeanConditionWithIgnoredSubclassByName() {
		this.contextRunner.withUserConfiguration(CustomExampleBeanConfiguration.class,
				ConditionalOnIgnoredSubclassByName.class,
				PropertyPlaceholderAutoConfiguration.class).run((context) -> {
					assertThat(context).getBeans(ExampleBean.class).hasSize(2);
					assertThat(context).getBeans(CustomExampleBean.class).hasSize(1);
				});
	}

	@Test
	public void grandparentIsConsideredWhenUsingAncestorsStrategy() {
		this.contextRunner.withUserConfiguration(ExampleBeanConfiguration.class)
				.run((grandparent) -> new ApplicationContextRunner()
						.withParent(grandparent)
						.run((parent) -> new ApplicationContextRunner().withParent(parent)
								.withUserConfiguration(ExampleBeanConfiguration.class,
										OnBeanInAncestorsConfiguration.class)
								.run((context) -> assertThat(context)
										.getBeans(ExampleBean.class).hasSize(1))));
	}

	@Test
	public void currentContextIsIgnoredWhenUsingAncestorsStrategy() {
		this.contextRunner
				.run((parent) -> new ApplicationContextRunner().withParent(parent)
						.withUserConfiguration(ExampleBeanConfiguration.class,
								OnBeanInAncestorsConfiguration.class)
						.run((context) -> assertThat(context).getBeans(ExampleBean.class)
								.hasSize(2)));
	}

	@Test
	public void beanProducedByFactoryBeanIsConsideredWhenMatchingOnAnnotation() {
		this.contextRunner.withUserConfiguration(ConcreteFactoryBeanConfiguration.class,
				OnAnnotationWithFactoryBeanConfiguration.class).run((context) -> {
					assertThat(context).doesNotHaveBean("bar");
					assertThat(context).hasSingleBean(ExampleBean.class);
				});
	}

	@Test
	public void parameterizedContainerWhenValueIsOfMissingBeanMatches() {
		this.contextRunner
				.withUserConfiguration(ParameterizedWithoutCustomConfig.class,
						ParameterizedConditionWithValueConfig.class)
				.run((context) -> assertThat(context).satisfies(exampleBeanRequirement(
						"otherExampleBean", "conditionalCustomExampleBean")));
	}

	@Test
	public void parameterizedContainerWhenValueIsOfExistingBeanDoesNotMatch() {
		this.contextRunner
				.withUserConfiguration(ParameterizedWithCustomConfig.class,
						ParameterizedConditionWithValueConfig.class)
				.run((context) -> assertThat(context)
						.satisfies(exampleBeanRequirement("customExampleBean")));
	}

	@Test
	public void parameterizedContainerWhenValueIsOfMissingBeanRegistrationMatches() {
		this.contextRunner
				.withUserConfiguration(ParameterizedWithoutCustomContainerConfig.class,
						ParameterizedConditionWithValueConfig.class)
				.run((context) -> assertThat(context).satisfies(exampleBeanRequirement(
						"otherExampleBean", "conditionalCustomExampleBean")));
	}

	@Test
	public void parameterizedContainerWhenValueIsOfExistingBeanRegistrationDoesNotMatch() {
		this.contextRunner
				.withUserConfiguration(ParameterizedWithCustomContainerConfig.class,
						ParameterizedConditionWithValueConfig.class)
				.run((context) -> assertThat(context)
						.satisfies(exampleBeanRequirement("customExampleBean")));
	}

	@Test
	public void parameterizedContainerWhenReturnTypeIsOfExistingBeanDoesNotMatch() {
		this.contextRunner
				.withUserConfiguration(ParameterizedWithCustomConfig.class,
						ParameterizedConditionWithReturnTypeConfig.class)
				.run((context) -> assertThat(context)
						.satisfies(exampleBeanRequirement("customExampleBean")));
	}

	@Test
	public void parameterizedContainerWhenReturnTypeIsOfExistingBeanRegistrationDoesNotMatch() {
		this.contextRunner
				.withUserConfiguration(ParameterizedWithCustomContainerConfig.class,
						ParameterizedConditionWithReturnTypeConfig.class)
				.run((context) -> assertThat(context)
						.satisfies(exampleBeanRequirement("customExampleBean")));
	}

	@Test
	public void parameterizedContainerWhenReturnRegistrationTypeIsOfExistingBeanDoesNotMatch() {
		this.contextRunner
				.withUserConfiguration(ParameterizedWithCustomConfig.class,
						ParameterizedConditionWithReturnRegistrationTypeConfig.class)
				.run((context) -> assertThat(context)
						.satisfies(exampleBeanRequirement("customExampleBean")));
	}

	@Test
	public void parameterizedContainerWhenReturnRegistrationTypeIsOfExistingBeanRegistrationDoesNotMatch() {
		this.contextRunner
				.withUserConfiguration(ParameterizedWithCustomContainerConfig.class,
						ParameterizedConditionWithReturnRegistrationTypeConfig.class)
				.run((context) -> assertThat(context)
						.satisfies(exampleBeanRequirement("customExampleBean")));
	}

	private Consumer<ConfigurableApplicationContext> exampleBeanRequirement(
			String... names) {
		return (context) -> {
			String[] beans = context.getBeanNamesForType(ExampleBean.class);
			String[] containers = context
					.getBeanNamesForType(TestParameterizedContainer.class);
			assertThat(StringUtils.concatenateStringArrays(beans, containers))
					.containsOnly(names);
		};
	}

	@Configuration(proxyBeanMethods = false)
	protected static class OnBeanInAncestorsConfiguration {

		@Bean
		@ConditionalOnMissingBean(search = SearchStrategy.ANCESTORS)
		public ExampleBean exampleBean2() {
			return new ExampleBean("test");
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(name = "foo")
	protected static class OnBeanNameConfiguration {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(name = "foo", value = Date.class)
	@ConditionalOnBean(name = "foo", value = Date.class)
	protected static class OnBeanNameAndTypeConfiguration {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	protected static class FactoryBeanConfiguration {

		@Bean
		public FactoryBean<ExampleBean> exampleBeanFactoryBean() {
			return new ExampleFactoryBean("foo");
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ComponentScan(basePackages = "org.springframework.boot.autoconfigure.condition.scan", includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ScannedFactoryBeanConfiguration.class))
	protected static class ComponentScannedFactoryBeanBeanMethodConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@ComponentScan(basePackages = "org.springframework.boot.autoconfigure.condition.scan", includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ScannedFactoryBeanWithBeanMethodArgumentsConfiguration.class))
	protected static class ComponentScannedFactoryBeanBeanMethodWithArgumentsConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	protected static class FactoryBeanWithBeanMethodArgumentsConfiguration {

		@Bean
		public FactoryBean<ExampleBean> exampleBeanFactoryBean(
				@Value("${theValue}") String value) {
			return new ExampleFactoryBean(value);
		}

	}

	@Configuration(proxyBeanMethods = false)
	protected static class ConcreteFactoryBeanConfiguration {

		@Bean
		public ExampleFactoryBean exampleBeanFactoryBean() {
			return new ExampleFactoryBean("foo");
		}

	}

	@Configuration(proxyBeanMethods = false)
	protected static class UnhelpfulFactoryBeanConfiguration {

		@Bean
		@SuppressWarnings("rawtypes")
		public FactoryBean exampleBeanFactoryBean() {
			return new ExampleFactoryBean("foo");
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(NonspecificFactoryBeanClassAttributeRegistrar.class)
	protected static class NonspecificFactoryBeanClassAttributeConfiguration {

	}

	protected static class NonspecificFactoryBeanClassAttributeRegistrar
			implements ImportBeanDefinitionRegistrar {

		@Override
		public void registerBeanDefinitions(AnnotationMetadata meta,
				BeanDefinitionRegistry registry) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder
					.genericBeanDefinition(NonspecificFactoryBean.class);
			builder.addConstructorArgValue("foo");
			builder.getBeanDefinition().setAttribute(
					OnBeanCondition.FACTORY_BEAN_OBJECT_TYPE, ExampleBean.class);
			registry.registerBeanDefinition("exampleBeanFactoryBean",
					builder.getBeanDefinition());
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(NonspecificFactoryBeanClassAttributeRegistrar.class)
	protected static class NonspecificFactoryBeanStringAttributeConfiguration {

	}

	protected static class NonspecificFactoryBeanStringAttributeRegistrar
			implements ImportBeanDefinitionRegistrar {

		@Override
		public void registerBeanDefinitions(AnnotationMetadata meta,
				BeanDefinitionRegistry registry) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder
					.genericBeanDefinition(NonspecificFactoryBean.class);
			builder.addConstructorArgValue("foo");
			builder.getBeanDefinition().setAttribute(
					OnBeanCondition.FACTORY_BEAN_OBJECT_TYPE,
					ExampleBean.class.getName());
			registry.registerBeanDefinition("exampleBeanFactoryBean",
					builder.getBeanDefinition());
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(FactoryBeanRegistrar.class)
	protected static class RegisteredFactoryBeanConfiguration {

	}

	protected static class FactoryBeanRegistrar implements ImportBeanDefinitionRegistrar {

		@Override
		public void registerBeanDefinitions(AnnotationMetadata meta,
				BeanDefinitionRegistry registry) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder
					.genericBeanDefinition(ExampleFactoryBean.class);
			builder.addConstructorArgValue("foo");
			registry.registerBeanDefinition("exampleBeanFactoryBean",
					builder.getBeanDefinition());
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ImportResource("org/springframework/boot/autoconfigure/condition/factorybean.xml")
	protected static class FactoryBeanXmlConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	protected static class ConditionalOnFactoryBean {

		@Bean
		@ConditionalOnMissingBean(ExampleBean.class)
		public ExampleBean createExampleBean() {
			return new ExampleBean("direct");
		}

	}

	@Configuration(proxyBeanMethods = false)
	protected static class ConditionalOnIgnoredSubclass {

		@Bean
		@ConditionalOnMissingBean(value = ExampleBean.class, ignored = CustomExampleBean.class)
		public ExampleBean exampleBean() {
			return new ExampleBean("test");
		}

	}

	@Configuration(proxyBeanMethods = false)
	protected static class ConditionalOnIgnoredSubclassByName {

		@Bean
		@ConditionalOnMissingBean(value = ExampleBean.class, ignoredType = "org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBeanTests.CustomExampleBean")
		public ExampleBean exampleBean() {
			return new ExampleBean("test");
		}

	}

	@Configuration(proxyBeanMethods = false)
	protected static class CustomExampleBeanConfiguration {

		@Bean
		public CustomExampleBean customExampleBean() {
			return new CustomExampleBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(annotation = EnableScheduling.class)
	protected static class OnAnnotationConfiguration {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(annotation = TestAnnotation.class)
	protected static class OnAnnotationWithFactoryBeanConfiguration {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableScheduling
	protected static class FooConfiguration {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(name = "foo")
	protected static class HierarchyConsidered {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(name = "foo", search = SearchStrategy.CURRENT)
	protected static class HierarchyNotConsidered {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	protected static class ExampleBeanConfiguration {

		@Bean
		public ExampleBean exampleBean() {
			return new ExampleBean("test");
		}

	}

	@Configuration(proxyBeanMethods = false)
	protected static class ImpliedOnBeanMethod {

		@Bean
		@ConditionalOnMissingBean
		public ExampleBean exampleBean2() {
			return new ExampleBean("test");
		}

	}

	public static class ExampleFactoryBean implements FactoryBean<ExampleBean> {

		public ExampleFactoryBean(String value) {
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

	public static class NonspecificFactoryBean implements FactoryBean<Object> {

		public NonspecificFactoryBean(String value) {
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

	@Configuration(proxyBeanMethods = false)
	static class ParameterizedWithCustomConfig {

		@Bean
		public CustomExampleBean customExampleBean() {
			return new CustomExampleBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ParameterizedWithoutCustomConfig {

		@Bean
		public OtherExampleBean otherExampleBean() {
			return new OtherExampleBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ParameterizedWithoutCustomContainerConfig {

		@Bean
		public TestParameterizedContainer<OtherExampleBean> otherExampleBean() {
			return new TestParameterizedContainer<>();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ParameterizedWithCustomContainerConfig {

		@Bean
		public TestParameterizedContainer<CustomExampleBean> customExampleBean() {
			return new TestParameterizedContainer<>();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ParameterizedConditionWithValueConfig {

		@Bean
		@ConditionalOnMissingBean(value = CustomExampleBean.class, parameterizedContainer = TestParameterizedContainer.class)
		public CustomExampleBean conditionalCustomExampleBean() {
			return new CustomExampleBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ParameterizedConditionWithReturnTypeConfig {

		@Bean
		@ConditionalOnMissingBean(parameterizedContainer = TestParameterizedContainer.class)
		public CustomExampleBean conditionalCustomExampleBean() {
			return new CustomExampleBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ParameterizedConditionWithReturnRegistrationTypeConfig {

		@Bean
		@ConditionalOnMissingBean(parameterizedContainer = TestParameterizedContainer.class)
		public TestParameterizedContainer<CustomExampleBean> conditionalCustomExampleBean() {
			return new TestParameterizedContainer<>();
		}

	}

	@TestAnnotation
	public static class ExampleBean {

		private String value;

		public ExampleBean(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return this.value;
		}

	}

	public static class CustomExampleBean extends ExampleBean {

		public CustomExampleBean() {
			super("custom subclass");
		}

	}

	public static class OtherExampleBean extends ExampleBean {

		public OtherExampleBean() {
			super("other subclass");
		}

	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	public @interface TestAnnotation {

	}

}
