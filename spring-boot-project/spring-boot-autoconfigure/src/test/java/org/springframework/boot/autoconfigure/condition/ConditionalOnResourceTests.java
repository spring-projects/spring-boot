/*
 * Copyright 2012-2019 the original author or authors.
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

import org.junit.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnResource}.
 *
 * @author Dave Syer
 */
public class ConditionalOnResourceTests {

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	public void testResourceExists() {
		this.context.register(BasicConfiguration.class);
		this.context.refresh();
		assertThat(this.context.containsBean("foo")).isTrue();
		assertThat(this.context.getBean("foo")).isEqualTo("foo");
	}

	@Test
	public void testResourceExistsWithPlaceholder() {
		TestPropertyValues.of("schema=schema.sql").applyTo(this.context);
		this.context.register(PlaceholderConfiguration.class);
		this.context.refresh();
		assertThat(this.context.containsBean("foo")).isTrue();
		assertThat(this.context.getBean("foo")).isEqualTo("foo");
	}

	@Test
	public void testResourceNotExists() {
		this.context.register(MissingConfiguration.class);
		this.context.refresh();
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Configuration
	@ConditionalOnResource(resources = "foo")
	protected static class MissingConfiguration {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	@Configuration
	@ConditionalOnResource(resources = "schema.sql")
	protected static class BasicConfiguration {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	@ConditionalOnResource(resources = "${schema}")
	protected static class PlaceholderConfiguration {

		@Bean
		public String foo() {
			return "foo";
		}

	}

}
