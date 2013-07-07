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

package org.springframework.zero.context.condition;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Dave Syer
 */
public class OnClassConditionTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	public void testVanillaOnClassCondition() {
		this.context.register(BasicConfiguration.class, FooConfiguration.class);
		this.context.refresh();
		assertTrue(this.context.containsBean("bar"));
		assertEquals("bar", this.context.getBean("bar"));
	}

	@Test
	public void testMissingOnClassCondition() {
		this.context.register(MissingConfiguration.class, FooConfiguration.class);
		this.context.refresh();
		assertFalse(this.context.containsBean("bar"));
		assertEquals("foo", this.context.getBean("foo"));
	}

	@Test
	public void testOnClassConditionWithXml() {
		this.context.register(BasicConfiguration.class, XmlConfiguration.class);
		this.context.refresh();
		assertTrue(this.context.containsBean("bar"));
		assertEquals("bar", this.context.getBean("bar"));
	}

	@Test
	public void testOnClassConditionWithCombinedXml() {
		this.context.register(CombinedXmlConfiguration.class);
		this.context.refresh();
		assertTrue(this.context.containsBean("bar"));
		assertEquals("bar", this.context.getBean("bar"));
	}

	@Configuration
	@ConditionalOnClass(OnClassConditionTests.class)
	protected static class BasicConfiguration {
		@Bean
		public String bar() {
			return "bar";
		}
	}

	@Configuration
	@ConditionalOnClass(name = "FOO")
	protected static class MissingConfiguration {
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
	@ImportResource("org/springframework/zero/context/foo.xml")
	protected static class XmlConfiguration {
	}

	@Configuration
	@Import(BasicConfiguration.class)
	@ImportResource("org/springframework/zero/context/foo.xml")
	protected static class CombinedXmlConfiguration {
	}
}
