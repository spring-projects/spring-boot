/*
 * Copyright 2012-2014 the original author or authors.
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
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link ConditionalOnProperty}.
 * 
 * @author Maciej Walkowiak
 */
public class ConditionalOnPropertyTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	public void testBeanIsCreatedWhenAllPropertiesAreDefined() {
		EnvironmentTestUtils.addEnvironment(this.context.getEnvironment(),
				"property1=value1", "property2=value2");
		setupContext();
		assertTrue(this.context.containsBean("foo"));
		assertEquals("foo", this.context.getBean("foo"));
	}

	@Test
	public void testBeanIsNotCreatedWhenNotAllPropertiesAreDefined() {
		EnvironmentTestUtils.addEnvironment(this.context.getEnvironment(),
				"property1=value1");
		setupContext();
		assertFalse(this.context.containsBean("foo"));
	}

	private void setupContext() {
		this.context.register(MultiplePropertiesRequiredConfiguration.class);
		this.context.refresh();
	}

	@Configuration
	@ConditionalOnProperty({ "property1", "property2" })
	protected static class MultiplePropertiesRequiredConfiguration {

		@Bean
		public String foo() {
			return "foo";
		}

	}

}
