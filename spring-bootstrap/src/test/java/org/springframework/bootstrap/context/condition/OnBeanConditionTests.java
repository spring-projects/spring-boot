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

package org.springframework.bootstrap.context.condition;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Dave Syer
 */
@Ignore
public class OnBeanConditionTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	public void testNameOnBeanCondition() {
		this.context.register(FooConfiguration.class, OnBeanNameConfiguration.class);
		this.context.refresh();
		assertTrue(this.context.containsBean("bar"));
		assertEquals("bar", this.context.getBean("bar"));
	}

	@Test
	public void testNameOnBeanConditionReverseOrder() {
		this.context.register(OnBeanNameConfiguration.class, FooConfiguration.class);
		this.context.refresh();
		assertTrue(this.context.containsBean("bar"));
		assertEquals("bar", this.context.getBean("bar"));
	}

	@Test
	public void testClassOnBeanCondition() {
		this.context.register(OnBeanClassConfiguration.class, FooConfiguration.class);
		this.context.refresh();
		assertTrue(this.context.containsBean("bar"));
		assertEquals("bar", this.context.getBean("bar"));
	}

	@Test
	public void testOnBeanConditionWithXml() {
		this.context.register(OnBeanNameConfiguration.class, XmlConfiguration.class);
		this.context.refresh();
		assertTrue(this.context.containsBean("bar"));
		assertEquals("bar", this.context.getBean("bar"));
	}

	@Test
	public void testOnBeanConditionWithCombinedXml() {
		this.context.register(CombinedXmlConfiguration.class);
		this.context.refresh();
		assertTrue(this.context.containsBean("bar"));
		assertEquals("bar", this.context.getBean("bar"));
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
	@ConditionalOnBean(String.class)
	protected static class OnBeanClassConfiguration {
		@Bean
		public String bar() {
			return "bar";
		}
	}

	@Configuration
	protected static class FooConfiguration {
		@Bean
		public String foo() {
			return "foo";
		}
	}

	@Configuration
	@ImportResource("org/springframework/bootstrap/context/annotation/foo.xml")
	protected static class XmlConfiguration {
	}

	@Configuration
	@Import(OnBeanNameConfiguration.class)
	@ImportResource("org/springframework/bootstrap/context/annotation/foo.xml")
	protected static class CombinedXmlConfiguration {
	}
}
