/*
 * Copyright 2012-2013 the original author or authors.
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

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
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
				ConfigurationWithFactoryBean.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertFalse(this.context.containsBean("bar"));
		assertTrue(this.context.containsBean("example"));
		assertEquals("foo", this.context.getBean("foo"));
	}

	@Test
	@Ignore("This will never work - you need to use XML for FactoryBeans, or else call getObject() inside the @Bean method")
	public void testOnMissingBeanConditionWithFactoryBean() {
		this.context.register(ExampleBeanAndFactoryBeanConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		// There should be only one
		this.context.getBean(ExampleBean.class);
	}

	@Test
	public void testOnMissingBeanConditionWithFactoryBeanInXml() {
		this.context.register(ConfigurationWithFactoryBean.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		// There should be only one
		this.context.getBean(ExampleBean.class);
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
	protected static class ExampleBeanAndFactoryBeanConfiguration {

		@Bean
		public FactoryBean<ExampleBean> exampleBeanFactoryBean() {
			return new ExampleFactoryBean("foo");
		}

		@Bean
		@ConditionalOnMissingBean(ExampleBean.class)
		public ExampleBean createExampleBean() {
			return new ExampleBean();
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
	@ImportResource("org/springframework/boot/autoconfigure/condition/factorybean.xml")
	protected static class ConfigurationWithFactoryBean {
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
			return new ExampleBean();
		}
	}

	@Configuration
	protected static class ImpliedOnBeanMethod {

		@Bean
		@ConditionalOnMissingBean
		public ExampleBean exampleBean2() {
			return new ExampleBean();
		}

	}

	public static class ExampleBean {
	}

	public static class ExampleFactoryBean implements FactoryBean<ExampleBean> {

		public ExampleFactoryBean(String value) {
			Assert.state(!value.contains("$"));
		}

		@Override
		public ExampleBean getObject() throws Exception {
			return new ExampleBean();
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
