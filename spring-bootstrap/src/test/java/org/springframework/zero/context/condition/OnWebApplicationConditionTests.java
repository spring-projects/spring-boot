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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.zero.context.condition.ConditionalOnNotWebApplication;
import org.springframework.zero.context.condition.ConditionalOnWebApplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link OnWebApplicationCondition}.
 * 
 * @author Dave Syer
 */
public class OnWebApplicationConditionTests {

	private AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();

	@Test
	public void testWebApplication() {
		this.context.register(BasicConfiguration.class);
		this.context.setServletContext(new MockServletContext());
		this.context.refresh();
		assertTrue(this.context.containsBean("foo"));
		assertEquals("foo", this.context.getBean("foo"));
	}

	@Test
	public void testNotWebApplication() {
		this.context.register(MissingConfiguration.class);
		this.context.setServletContext(new MockServletContext());
		this.context.refresh();
		assertFalse(this.context.containsBean("foo"));
	}

	@Configuration
	@ConditionalOnNotWebApplication
	protected static class MissingConfiguration {
		@Bean
		public String bar() {
			return "bar";
		}
	}

	@Configuration
	@ConditionalOnWebApplication
	protected static class BasicConfiguration {
		@Bean
		public String foo() {
			return "foo";
		}
	}
}
