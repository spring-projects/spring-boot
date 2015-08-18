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

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link ConditionalOnSingleCandidate}.
 *
 * @author Stephane Nicoll
 */
public class ConditionalOnSingleCandidateTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void singleCandidateNoCandidate() {
		load(OnBeanSingleCandidateConfiguration.class);
		assertFalse(this.context.containsBean("baz"));
	}

	@Test
	public void singleCandidateOneCandidate() {
		load(FooConfiguration.class, OnBeanSingleCandidateConfiguration.class);
		assertTrue(this.context.containsBean("baz"));
		assertEquals("foo", this.context.getBean("baz"));
	}

	@Test
	public void singleCandidateMultipleCandidates() {
		load(FooConfiguration.class, BarConfiguration.class,
				OnBeanSingleCandidateConfiguration.class);
		assertFalse(this.context.containsBean("baz"));
	}

	@Test
	public void singleCandidateMultipleCandidatesOnePrimary() {
		load(FooPrimaryConfiguration.class, BarConfiguration.class,
				OnBeanSingleCandidateConfiguration.class);
		assertTrue(this.context.containsBean("baz"));
		assertEquals("foo", this.context.getBean("baz"));
	}

	@Test
	public void singleCandidateMultipleCandidatesMultiplePrimary() {
		load(FooPrimaryConfiguration.class, BarPrimaryConfiguration.class,
				OnBeanSingleCandidateConfiguration.class);
		assertFalse(this.context.containsBean("baz"));
	}

	@Test
	public void invalidAnnotationTwoTypes() {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectCause(isA(IllegalArgumentException.class));
		this.thrown.expectMessage(OnBeanSingleCandidateTwoTypesConfiguration.class
				.getName());
		load(OnBeanSingleCandidateTwoTypesConfiguration.class);
	}

	@Test
	public void invalidAnnotationNoType() {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectCause(isA(IllegalArgumentException.class));
		this.thrown.expectMessage(OnBeanSingleCandidateNoTypeConfiguration.class
				.getName());
		load(OnBeanSingleCandidateNoTypeConfiguration.class);
	}

	private void load(Class<?>... classes) {
		this.context.register(classes);
		this.context.refresh();
	}

	@Configuration
	@ConditionalOnSingleCandidate(value = String.class)
	protected static class OnBeanSingleCandidateConfiguration {

		@Bean
		public String baz(String s) {
			return s;
		}

	}

	@Configuration
	@ConditionalOnSingleCandidate(value = String.class, type = "java.lang.String")
	protected static class OnBeanSingleCandidateTwoTypesConfiguration {

	}

	@Configuration
	@ConditionalOnSingleCandidate
	protected static class OnBeanSingleCandidateNoTypeConfiguration {

	}

	@Configuration
	protected static class FooConfiguration {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	protected static class FooPrimaryConfiguration {

		@Bean
		@Primary
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	protected static class BarConfiguration {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	@Configuration
	protected static class BarPrimaryConfiguration {

		@Bean
		@Primary
		public String bar() {
			return "bar";
		}

	}
}
