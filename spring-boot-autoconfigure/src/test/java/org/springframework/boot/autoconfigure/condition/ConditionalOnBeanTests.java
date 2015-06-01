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

import java.util.Date;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
		assertTrue(this.context.containsBean("bar"));
		assertEquals("bar", this.context.getBean("bar"));
	}

	@Test
	public void testNameAndTypeOnBeanCondition() {
		this.context.register(FooConfiguration.class,
				OnBeanNameAndTypeConfiguration.class);
		this.context.refresh();
		/*
		 * Arguably this should be true, but as things are implemented the conditions
		 * specified in the different attributes of @ConditionalOnBean are combined with
		 * logical OR (not AND) so if any of them match the condition is true.
		 */
		assertFalse(this.context.containsBean("bar"));
	}

	@Test
	public void testNameOnBeanConditionReverseOrder() {
		this.context.register(OnBeanNameConfiguration.class, FooConfiguration.class);
		this.context.refresh();
		// Ideally this should be true
		assertFalse(this.context.containsBean("bar"));
	}

	@Test
	public void testClassOnBeanCondition() {
		this.context.register(FooConfiguration.class, OnBeanClassConfiguration.class);
		this.context.refresh();
		assertTrue(this.context.containsBean("bar"));
		assertEquals("bar", this.context.getBean("bar"));
	}

	@Test
	public void testOnBeanConditionWithXml() {
		this.context.register(XmlConfiguration.class, OnBeanNameConfiguration.class);
		this.context.refresh();
		assertTrue(this.context.containsBean("bar"));
		assertEquals("bar", this.context.getBean("bar"));
	}

	@Test
	public void testOnBeanConditionWithCombinedXml() {
		this.context.register(CombinedXmlConfiguration.class);
		this.context.refresh();
		// Ideally this should be true
		assertFalse(this.context.containsBean("bar"));
	}

	@Test
	public void testAnnotationOnBeanCondition() {
		this.context.register(FooConfiguration.class, OnAnnotationConfiguration.class);
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
	@ConditionalOnMissingBean(name = "foo", value = Date.class)
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
}
