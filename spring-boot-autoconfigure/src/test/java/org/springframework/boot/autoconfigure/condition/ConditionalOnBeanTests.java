/*
 * Copyright 2012-2018 the original author or authors.
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

import org.junit.Test;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnBean}.
 *
 * @author Dave Syer
 */
public class ConditionalOnBeanTests {

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	public void testNameOnBeanCondition() {
		this.context.register(FooConfiguration.class, OnBeanNameConfiguration.class);
		this.context.refresh();
		assertThat(this.context.containsBean("bar")).isTrue();
		assertThat(this.context.getBean("bar")).isEqualTo("bar");
	}

	@Test
	public void testNameAndTypeOnBeanCondition() {
		this.context.register(FooConfiguration.class,
				OnBeanNameAndTypeConfiguration.class);
		this.context.refresh();
		assertThat(this.context.containsBean("bar")).isTrue();
	}

	@Test
	public void testNameOnBeanConditionReverseOrder() {
		this.context.register(OnBeanNameConfiguration.class, FooConfiguration.class);
		this.context.refresh();
		// Ideally this should be true
		assertThat(this.context.containsBean("bar")).isFalse();
	}

	@Test
	public void testClassOnBeanCondition() {
		this.context.register(FooConfiguration.class, OnBeanClassConfiguration.class);
		this.context.refresh();
		assertThat(this.context.containsBean("bar")).isTrue();
		assertThat(this.context.getBean("bar")).isEqualTo("bar");
	}

	@Test
	public void testClassOnBeanClassNameCondition() {
		this.context.register(FooConfiguration.class, OnBeanClassNameConfiguration.class);
		this.context.refresh();
		assertThat(this.context.containsBean("bar")).isTrue();
		assertThat(this.context.getBean("bar")).isEqualTo("bar");
	}

	@Test
	public void testOnBeanConditionWithXml() {
		this.context.register(XmlConfiguration.class, OnBeanNameConfiguration.class);
		this.context.refresh();
		assertThat(this.context.containsBean("bar")).isTrue();
		assertThat(this.context.getBean("bar")).isEqualTo("bar");
	}

	@Test
	public void testOnBeanConditionWithCombinedXml() {
		this.context.register(CombinedXmlConfiguration.class);
		this.context.refresh();
		// Ideally this should be true
		assertThat(this.context.containsBean("bar")).isFalse();
	}

	@Test
	public void testAnnotationOnBeanCondition() {
		this.context.register(FooConfiguration.class, OnAnnotationConfiguration.class);
		this.context.refresh();
		assertThat(this.context.containsBean("bar")).isTrue();
		assertThat(this.context.getBean("bar")).isEqualTo("bar");
	}

	@Test
	public void testOnMissingBeanType() throws Exception {
		this.context.register(FooConfiguration.class,
				OnBeanMissingClassConfiguration.class);
		this.context.refresh();
		assertThat(this.context.containsBean("bar")).isFalse();
	}

	@Test
	public void withPropertyPlaceholderClassName() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context, "mybeanclass=java.lang.String");
		this.context.register(PropertySourcesPlaceholderConfigurer.class,
				WithPropertyPlaceholderClassName.class, OnBeanClassConfiguration.class);
		this.context.refresh();
	}

	@Test
	public void beanProducedByFactoryBeanIsConsideredWhenMatchingOnAnnotation() {
		this.context.register(FactoryBeanConfiguration.class,
				OnAnnotationWithFactoryBeanConfiguration.class);
		this.context.refresh();
		assertThat(this.context.containsBean("bar")).isTrue();
		assertThat(this.context.getBeansOfType(ExampleBean.class)).hasSize(1);
	}

	@Test
	public void conditionEvaluationConsidersChangeInTypeWhenBeanIsOverridden() {
		this.context.register(OriginalDefinition.class, OverridingDefinition.class,
				ConsumingConfiguration.class);
		this.context.refresh();
		assertThat(this.context.containsBean("testBean")).isTrue();
		assertThat(this.context.getBean(Integer.class)).isEqualTo(1);
		assertThat(this.context.getBeansOfType(ConsumingConfiguration.class)).isEmpty();
	}

	@Configuration
	@ConditionalOnBean(name = "foo")
	protected static class OnBeanNameConfiguration {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	@Configuration
	@ConditionalOnBean(name = "foo", value = Date.class)
	protected static class OnBeanNameAndTypeConfiguration {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	@Configuration
	@ConditionalOnBean(annotation = EnableScheduling.class)
	protected static class OnAnnotationConfiguration {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	@Configuration
	@ConditionalOnBean(String.class)
	protected static class OnBeanClassConfiguration {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	@Configuration
	@ConditionalOnBean(type = "java.lang.String")
	protected static class OnBeanClassNameConfiguration {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	@Configuration
	@ConditionalOnBean(type = "some.type.Missing")
	protected static class OnBeanMissingClassConfiguration {

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
	@ImportResource("org/springframework/boot/autoconfigure/condition/foo.xml")
	protected static class XmlConfiguration {

	}

	@Configuration
	@ImportResource("org/springframework/boot/autoconfigure/condition/foo.xml")
	@Import(OnBeanNameConfiguration.class)
	protected static class CombinedXmlConfiguration {

	}

	@Configuration
	@Import(WithPropertyPlaceholderClassNameRegistrar.class)
	protected static class WithPropertyPlaceholderClassName {

	}

	@Configuration
	static class FactoryBeanConfiguration {

		@Bean
		public ExampleFactoryBean exampleBeanFactoryBean() {
			return new ExampleFactoryBean();
		}

	}

	@Configuration
	@ConditionalOnBean(annotation = TestAnnotation.class)
	static class OnAnnotationWithFactoryBeanConfiguration {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	protected static class WithPropertyPlaceholderClassNameRegistrar
			implements ImportBeanDefinitionRegistrar {

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			RootBeanDefinition bd = new RootBeanDefinition();
			bd.setBeanClassName("${mybeanclass}");
			registry.registerBeanDefinition("mybean", bd);
		}

	}

	public static class ExampleFactoryBean implements FactoryBean<ExampleBean> {

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

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	public @interface TestAnnotation {

	}

	@Configuration
	public static class OriginalDefinition {

		@Bean
		public String testBean() {
			return "test";
		}

	}

	@Configuration
	@ConditionalOnBean(String.class)
	public static class OverridingDefinition {

		@Bean
		public Integer testBean() {
			return 1;
		}

	}

	@Configuration
	@ConditionalOnBean(String.class)
	public static class ConsumingConfiguration {

		ConsumingConfiguration(String testBean) {
		}

	}

}
