/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.Test;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.condition.scan.ScannedFactoryBeanConfiguration;
import org.springframework.boot.autoconfigure.condition.scan.ScannedFactoryBeanWithBeanMethodArgumentsConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
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

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	public void testNameOnMissingBeanCondition() {
		this.context.register(FooConfiguration.class, OnBeanNameConfiguration.class);
		this.context.refresh();
		assertThat(this.context.containsBean("bar")).isFalse();
		assertThat(this.context.getBean("foo")).isEqualTo("foo");
	}

	@Test
	public void testNameOnMissingBeanConditionReverseOrder() {
		this.context.register(OnBeanNameConfiguration.class, FooConfiguration.class);
		this.context.refresh();
		// FIXME: ideally this would be false, but the ordering is a problem
		assertThat(this.context.containsBean("bar")).isTrue();
		assertThat(this.context.getBean("foo")).isEqualTo("foo");
	}

	@Test
	public void testNameAndTypeOnMissingBeanCondition() {
		this.context.register(FooConfiguration.class,
				OnBeanNameAndTypeConfiguration.class);
		this.context.refresh();
		/*
		 * Arguably this should be true, but as things are implemented the conditions
		 * specified in the different attributes of @ConditionalOnBean are combined with
		 * logical OR (not AND) so if any of them match the condition is true.
		 */
		assertThat(this.context.containsBean("bar")).isFalse();
	}

	@Test
	public void hierarchyConsidered() throws Exception {
		this.context.register(FooConfiguration.class);
		this.context.refresh();
		AnnotationConfigApplicationContext childContext = new AnnotationConfigApplicationContext();
		childContext.setParent(this.context);
		childContext.register(HierarchyConsidered.class);
		childContext.refresh();
		assertThat(childContext.containsLocalBean("bar")).isFalse();
	}

	@Test
	public void hierarchyNotConsidered() throws Exception {
		this.context.register(FooConfiguration.class);
		this.context.refresh();
		AnnotationConfigApplicationContext childContext = new AnnotationConfigApplicationContext();
		childContext.setParent(this.context);
		childContext.register(HierarchyNotConsidered.class);
		childContext.refresh();
		assertThat(childContext.containsLocalBean("bar")).isTrue();
	}

	@Test
	public void impliedOnBeanMethod() throws Exception {
		this.context.register(ExampleBeanConfiguration.class, ImpliedOnBeanMethod.class);
		this.context.refresh();
		assertThat(this.context.getBeansOfType(ExampleBean.class).size()).isEqualTo(1);
	}

	@Test
	public void testAnnotationOnMissingBeanCondition() {
		this.context.register(FooConfiguration.class, OnAnnotationConfiguration.class);
		this.context.refresh();
		assertThat(this.context.containsBean("bar")).isFalse();
		assertThat(this.context.getBean("foo")).isEqualTo("foo");
	}

	// Rigorous test for SPR-11069
	@Test
	public void testAnnotationOnMissingBeanConditionWithEagerFactoryBean() {
		this.context.register(FooConfiguration.class, OnAnnotationConfiguration.class,
				FactoryBeanXmlConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.containsBean("bar")).isFalse();
		assertThat(this.context.containsBean("example")).isTrue();
		assertThat(this.context.getBean("foo")).isEqualTo("foo");
	}

	@Test
	public void testOnMissingBeanConditionWithFactoryBean() {
		this.context.register(FactoryBeanConfiguration.class,
				ConditionalOnFactoryBean.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(ExampleBean.class).toString())
				.isEqualTo("fromFactory");
	}

	@Test
	public void testOnMissingBeanConditionWithComponentScannedFactoryBean() {
		this.context.register(ComponentScannedFactoryBeanBeanMethodConfiguration.class,
				ConditionalOnFactoryBean.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(ExampleBean.class).toString())
				.isEqualTo("fromFactory");
	}

	@Test
	public void testOnMissingBeanConditionWithComponentScannedFactoryBeanWithBeanMethodArguments() {
		this.context.register(
				ComponentScannedFactoryBeanBeanMethodWithArgumentsConfiguration.class,
				ConditionalOnFactoryBean.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(ExampleBean.class).toString())
				.isEqualTo("fromFactory");
	}

	@Test
	public void testOnMissingBeanConditionWithFactoryBeanWithBeanMethodArguments() {
		this.context.register(FactoryBeanWithBeanMethodArgumentsConfiguration.class,
				ConditionalOnFactoryBean.class,
				PropertyPlaceholderAutoConfiguration.class);
		TestPropertyValues.of("theValue:foo").applyTo(this.context);
		this.context.refresh();
		assertThat(this.context.getBean(ExampleBean.class).toString())
				.isEqualTo("fromFactory");
	}

	@Test
	public void testOnMissingBeanConditionWithConcreteFactoryBean() {
		this.context.register(ConcreteFactoryBeanConfiguration.class,
				ConditionalOnFactoryBean.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(ExampleBean.class).toString())
				.isEqualTo("fromFactory");
	}

	@Test
	public void testOnMissingBeanConditionWithUnhelpfulFactoryBean() {
		this.context.register(UnhelpfulFactoryBeanConfiguration.class,
				ConditionalOnFactoryBean.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		// We could not tell that the FactoryBean would ultimately create an ExampleBean
		assertThat(this.context.getBeansOfType(ExampleBean.class).values()).hasSize(2);
	}

	@Test
	public void testOnMissingBeanConditionWithRegisteredFactoryBean() {
		this.context.register(RegisteredFactoryBeanConfiguration.class,
				ConditionalOnFactoryBean.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(ExampleBean.class).toString())
				.isEqualTo("fromFactory");
	}

	@Test
	public void testOnMissingBeanConditionWithNonspecificFactoryBeanWithClassAttribute() {
		this.context.register(NonspecificFactoryBeanClassAttributeConfiguration.class,
				ConditionalOnFactoryBean.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(ExampleBean.class).toString())
				.isEqualTo("fromFactory");
	}

	@Test
	public void testOnMissingBeanConditionWithNonspecificFactoryBeanWithStringAttribute() {
		this.context.register(NonspecificFactoryBeanStringAttributeConfiguration.class,
				ConditionalOnFactoryBean.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(ExampleBean.class).toString())
				.isEqualTo("fromFactory");
	}

	@Test
	public void testOnMissingBeanConditionWithFactoryBeanInXml() {
		this.context.register(FactoryBeanXmlConfiguration.class,
				ConditionalOnFactoryBean.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(ExampleBean.class).toString())
				.isEqualTo("fromFactory");
	}

	@Test
	public void testOnMissingBeanConditionWithIgnoredSubclass() {
		this.context.register(CustomExampleBeanConfiguration.class,
				ConditionalOnIgnoredSubclass.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeansOfType(ExampleBean.class)).hasSize(2);
		assertThat(this.context.getBeansOfType(CustomExampleBean.class)).hasSize(1);
	}

	@Test
	public void testOnMissingBeanConditionWithIgnoredSubclassByName() {
		this.context.register(CustomExampleBeanConfiguration.class,
				ConditionalOnIgnoredSubclassByName.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeansOfType(ExampleBean.class)).hasSize(2);
		assertThat(this.context.getBeansOfType(CustomExampleBean.class)).hasSize(1);
	}

	@Test
	public void grandparentIsConsideredWhenUsingAncestorsStrategy() {
		this.context.register(ExampleBeanConfiguration.class);
		this.context.refresh();
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		parent.setParent(this.context);
		parent.refresh();
		AnnotationConfigApplicationContext child = new AnnotationConfigApplicationContext();
		child.setParent(parent);
		child.register(ExampleBeanConfiguration.class,
				OnBeanInAncestorsConfiguration.class);
		child.refresh();
		assertThat(child.getBeansOfType(ExampleBean.class)).hasSize(1);
		child.close();
		parent.close();
	}

	@Test
	public void currentContextIsIgnoredWhenUsingAncestorsStrategy() {
		this.context.refresh();
		AnnotationConfigApplicationContext child = new AnnotationConfigApplicationContext();
		child.register(ExampleBeanConfiguration.class,
				OnBeanInAncestorsConfiguration.class);
		child.setParent(this.context);
		child.refresh();
		assertThat(child.getBeansOfType(ExampleBean.class)).hasSize(2);
	}

	@Test
	public void beanProducedByFactoryBeanIsConsideredWhenMatchingOnAnnotation() {
		this.context.register(ConcreteFactoryBeanConfiguration.class,
				OnAnnotationWithFactoryBeanConfiguration.class);
		this.context.refresh();
		assertThat(this.context.containsBean("bar")).isFalse();
		assertThat(this.context.getBeansOfType(ExampleBean.class)).hasSize(1);
	}

	@Configuration
	protected static class OnBeanInAncestorsConfiguration {

		@Bean
		@ConditionalOnMissingBean(search = SearchStrategy.ANCESTORS)
		public ExampleBean exampleBean2() {
			return new ExampleBean("test");
		}

	}

	@Configuration
	@ConditionalOnMissingBean(name = "foo")
	protected static class OnBeanNameConfiguration {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	@Configuration
	@ConditionalOnMissingBean(name = "foo", value = Date.class)
	@ConditionalOnBean(name = "foo", value = Date.class)
	protected static class OnBeanNameAndTypeConfiguration {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	@Configuration
	protected static class FactoryBeanConfiguration {

		@Bean
		public FactoryBean<ExampleBean> exampleBeanFactoryBean() {
			return new ExampleFactoryBean("foo");
		}

	}

	@Configuration
	@ComponentScan(basePackages = "org.springframework.boot.autoconfigure.condition.scan", includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ScannedFactoryBeanConfiguration.class))
	protected static class ComponentScannedFactoryBeanBeanMethodConfiguration {

	}

	@Configuration
	@ComponentScan(basePackages = "org.springframework.boot.autoconfigure.condition.scan", includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ScannedFactoryBeanWithBeanMethodArgumentsConfiguration.class))
	protected static class ComponentScannedFactoryBeanBeanMethodWithArgumentsConfiguration {

	}

	@Configuration
	protected static class FactoryBeanWithBeanMethodArgumentsConfiguration {

		@Bean
		public FactoryBean<ExampleBean> exampleBeanFactoryBean(
				@Value("${theValue}") String value) {
			return new ExampleFactoryBean(value);
		}

	}

	@Configuration
	protected static class ConcreteFactoryBeanConfiguration {

		@Bean
		public ExampleFactoryBean exampleBeanFactoryBean() {
			return new ExampleFactoryBean("foo");
		}

	}

	@Configuration
	protected static class UnhelpfulFactoryBeanConfiguration {

		@Bean
		@SuppressWarnings("rawtypes")
		public FactoryBean exampleBeanFactoryBean() {
			return new ExampleFactoryBean("foo");
		}

	}

	@Configuration
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

	@Configuration
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

	@Configuration
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

	@Configuration
	@ImportResource("org/springframework/boot/autoconfigure/condition/factorybean.xml")
	protected static class FactoryBeanXmlConfiguration {

	}

	@Configuration
	protected static class ConditionalOnFactoryBean {

		@Bean
		@ConditionalOnMissingBean(ExampleBean.class)
		public ExampleBean createExampleBean() {
			return new ExampleBean("direct");
		}

	}

	@Configuration
	protected static class ConditionalOnIgnoredSubclass {

		@Bean
		@ConditionalOnMissingBean(value = ExampleBean.class, ignored = CustomExampleBean.class)
		public ExampleBean exampleBean() {
			return new ExampleBean("test");
		}

	}

	@Configuration
	protected static class ConditionalOnIgnoredSubclassByName {

		@Bean
		@ConditionalOnMissingBean(value = ExampleBean.class, ignoredType = "org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBeanTests.CustomExampleBean")
		public ExampleBean exampleBean() {
			return new ExampleBean("test");
		}

	}

	@Configuration
	protected static class CustomExampleBeanConfiguration {

		@Bean
		public CustomExampleBean customExampleBean() {
			return new CustomExampleBean();
		}

	}

	@Configuration
	@ConditionalOnMissingBean(annotation = EnableScheduling.class)
	protected static class OnAnnotationConfiguration {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	@Configuration
	@ConditionalOnMissingBean(annotation = TestAnnotation.class)
	protected static class OnAnnotationWithFactoryBeanConfiguration {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	@Configuration
	@EnableScheduling
	protected static class FooConfiguration {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	@ConditionalOnMissingBean(name = "foo")
	protected static class HierarchyConsidered {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	@Configuration
	@ConditionalOnMissingBean(name = "foo", search = SearchStrategy.CURRENT)
	protected static class HierarchyNotConsidered {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	@Configuration
	protected static class ExampleBeanConfiguration {

		@Bean
		public ExampleBean exampleBean() {
			return new ExampleBean("test");
		}

	}

	@Configuration
	protected static class ImpliedOnBeanMethod {

		@Bean
		@ConditionalOnMissingBean
		public ExampleBean exampleBean2() {
			return new ExampleBean("test");
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

	public static class ExampleFactoryBean implements FactoryBean<ExampleBean> {

		public ExampleFactoryBean(String value) {
			Assert.state(!value.contains("$"), "value should not contain '$'");
		}

		@Override
		public ExampleBean getObject() throws Exception {
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
		public ExampleBean getObject() throws Exception {
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

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	public @interface TestAnnotation {

	}

}
