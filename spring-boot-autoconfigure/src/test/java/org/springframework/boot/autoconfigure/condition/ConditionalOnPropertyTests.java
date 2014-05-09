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
	public void allPropertiesAreDefined() {
		EnvironmentTestUtils.addEnvironment(this.context.getEnvironment(),
				"property1=value1", "property2=value2");
		this.context.register(MultiplePropertiesRequiredConfiguration.class);
		this.context.refresh();
		assertTrue(this.context.containsBean("foo"));
	}

	@Test
	public void notAllPropertiesAreDefined() {
		EnvironmentTestUtils.addEnvironment(this.context.getEnvironment(),
				"property1=value1");
		this.context.register(MultiplePropertiesRequiredConfiguration.class);
		this.context.refresh();
		assertFalse(this.context.containsBean("foo"));
	}

	@Test
	public void propertyValueEqualsFalse() {
		EnvironmentTestUtils.addEnvironment(this.context.getEnvironment(),
				"property1=false", "property2=value2");
		this.context.register(MultiplePropertiesRequiredConfiguration.class);
		this.context.refresh();
		assertFalse(this.context.containsBean("foo"));
	}

	@Test
	public void propertyValueEqualsFALSE() {
		EnvironmentTestUtils.addEnvironment(this.context.getEnvironment(),
				"property1=FALSE", "property2=value2");
		this.context.register(MultiplePropertiesRequiredConfiguration.class);
		this.context.refresh();
		assertFalse(this.context.containsBean("foo"));
	}

	@Test
	public void relaxedName() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context.getEnvironment(),
				"spring.theRelaxedProperty=value1");
		this.context.register(RelaxedPropertiesRequiredConfiguration.class);
		this.context.refresh();
		assertTrue(this.context.containsBean("foo"));
	}

	@Test
	public void nonRelaxedName() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context.getEnvironment(),
				"theRelaxedProperty=value1");
		this.context.register(NonRelaxedPropertiesRequiredConfiguration.class);
		this.context.refresh();
		assertFalse(this.context.containsBean("foo"));
	}

	@Configuration
	@ConditionalOnProperty({ "property1", "property2" })
	protected static class MultiplePropertiesRequiredConfiguration {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	@ConditionalOnProperty(prefix = "spring.", value = "the-relaxed-property")
	protected static class RelaxedPropertiesRequiredConfiguration {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	@ConditionalOnProperty(value = "the-relaxed-property", relaxedNames = false)
	protected static class NonRelaxedPropertiesRequiredConfiguration {

		@Bean
		public String foo() {
			return "foo";
		}

	}

}
