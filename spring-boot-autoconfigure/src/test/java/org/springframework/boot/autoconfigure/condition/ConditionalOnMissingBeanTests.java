/*
 * Copyright 2012-2015 the original author or authors.
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

import org.junit.Test;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.Assert;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

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
		assertFalse(this.context.containsBean("bar"));
		assertEquals("foo", this.context.getBean("foo"));
	}

	@Test
	public void testNameOnMissingBeanConditionReverseOrder() {
		this.context.register(OnBeanNameConfiguration.class, FooConfiguration.class);
		this.context.refresh();
		// FIXME: ideally this would be false, but the ordering is a problem
		assertTrue(this.context.containsBean("bar"));
		assertEquals("foo", this.context.getBean("foo"));
	}

	@Test
	public void hierarchyConsidered() throws Exception {
		this.context.register(FooConfiguration.class);
		this.context.refresh();
		AnnotationConfigApplicationContext childContext = new AnnotationConfigApplicationContext();
		childContext.setParent(this.context);
		childContext.register(HierarchyConsidered.class);
		childContext.refresh();
		assertFalse(childContext.containsLocalBean("bar"));
	}

	@Test
	public void hierarchyNotConsidered() throws Exception {
		this.context.register(FooConfiguration.class);
		this.context.refresh();
		AnnotationConfigApplicationContext childContext = new AnnotationConfigApplicationContext();
		childContext.setParent(this.context);
		childContext.register(HierarchyNotConsidered.class);
		childContext.refresh();
		assertTrue(childContext.containsLocalBean("bar"));
	}

	@Test
	public void impliedOnBeanMethod() throws Exception {
		this.context.register(ExampleBeanConfiguration.class, ImpliedOnBeanMethod.class);
		this.context.refresh();
		assertThat(this.context.getBeansOfType(ExampleBean.class).size(), equalTo(1));
	}

	@Test
	public void testAnnotationOnMissingBeanCondition() {
		this.context.register(FooConfiguration.class, OnAnnotationConfiguration.class);
		this.context.refresh();
		assertFalse(this.context.containsBean("bar"));
		assertEquals("foo", this.context.getBean("foo"));
	}

	// Rigorous test for SPR-11069
	@Test
	public void testAnnotationOnMissingBeanConditionWithEagerFactoryBean() {
		this.context.register(FooConfiguration.class, OnAnnotationConfiguration.class,
				FactoryBeanXmlConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertFalse(this.context.containsBean("bar"));
		assertTrue(this.context.containsBean("example"));
		assertEquals("foo", this.context.getBean("foo"));
	}

	@Test
	public void testOnMissingBeanConditionWithFactoryBean() {
		this.context.register(FactoryBeanConfiguration.class,
				ConditionalOnFactoryBean.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(ExampleBean.class).toString(),
				equalTo("fromFactory"));
	}

	@Test
	public void testOnMissingBeanConditionWithConcreteFactoryBean() {
		this.context.register(ConcreteFactoryBeanConfiguration.class,
				ConditionalOnFactoryBean.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(ExampleBean.class).toString(),
				equalTo("fromFactory"));
	}

	@Test
	public void testOnMissingBeanConditionWithUnhelpfulFactoryBean() {
		this.context.register(UnhelpfulFactoryBeanConfiguration.class,
				ConditionalOnFactoryBean.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		// We could not tell that the FactoryBean would ultimately create an ExampleBean
		assertThat(this.context.getBeansOfType(ExampleBean.class).values().size(),
				equalTo(2));
	}

	@Test
	public void testOnMissingBeanConditionWithRegisteredFactoryBean() {
		this.context.register(RegisteredFactoryBeanConfiguration.class,
				ConditionalOnFactoryBean.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(ExampleBean.class).toString(),
				equalTo("fromFactory"));
	}

	@Test
	public void testOnMissingBeanConditionWithNonspecificFactoryBeanWithClassAttribute() {
		this.context.register(NonspecificFactoryBeanClassAttributeConfiguration.class,
				ConditionalOnFactoryBean.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(ExampleBean.class).toString(),
				equalTo("fromFactory"));
	}

	@Test
	public void testOnMissingBeanConditionWithNonspecificFactoryBeanWithStringAttribute() {
		this.context.register(NonspecificFactoryBeanStringAttributeConfiguration.class,
				ConditionalOnFactoryBean.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(ExampleBean.class).toString(),
				equalTo("fromFactory"));
	}

	@Test
	public void testOnMissingBeanConditionWithFactoryBeanInXml() {
		this.context.register(FactoryBeanXmlConfiguration.class,
				ConditionalOnFactoryBean.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(ExampleBean.class).toString(),
				equalTo("fromFactory"));
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
	protected static class FactoryBeanConfiguration {
		@Bean
		public FactoryBean<ExampleBean> exampleBeanFactoryBean() {
			return new ExampleFactoryBean("foo");
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

	protected static class NonspecificFactoryBeanClassAttributeRegistrar implements
			ImportBeanDefinitionRegistrar {

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

	protected static class NonspecificFactoryBeanStringAttributeRegistrar implements
			ImportBeanDefinitionRegistrar {

		@Override
		public void registerBeanDefinitions(AnnotationMetadata meta,
				BeanDefinitionRegistry registry) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder
					.genericBeanDefinition(NonspecificFactoryBean.class);
			builder.addConstructorArgValue("foo");
			builder.getBeanDefinition()
					.setAttribute(OnBeanCondition.FACTORY_BEAN_OBJECT_TYPE,
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
	@ConditionalOnMissingBean(annotation = EnableScheduling.class)
	protected static class OnAnnotationConfiguration {
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

	public static class ExampleFactoryBean implements FactoryBean<ExampleBean> {

		public ExampleFactoryBean(String value) {
			Assert.state(!value.contains("$"));
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
			Assert.state(!value.contains("$"));
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
}
