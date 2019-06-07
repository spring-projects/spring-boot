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

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.isA;

/**
 * Tests for {@link ConditionalOnSingleCandidate}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
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
		assertThat(this.context.containsBean("baz")).isFalse();
	}

	@Test
	public void singleCandidateOneCandidate() {
		load(FooConfiguration.class, OnBeanSingleCandidateConfiguration.class);
		assertThat(this.context.containsBean("baz")).isTrue();
		assertThat(this.context.getBean("baz")).isEqualTo("foo");
	}

	@Test
	public void singleCandidateInAncestorsOneCandidateInCurrent() {
		load();
		AnnotationConfigApplicationContext child = new AnnotationConfigApplicationContext();
		child.register(FooConfiguration.class, OnBeanSingleCandidateInAncestorsConfiguration.class);
		child.setParent(this.context);
		child.refresh();
		assertThat(child.containsBean("baz")).isFalse();
		child.close();
	}

	@Test
	public void singleCandidateInAncestorsOneCandidateInParent() {
		load(FooConfiguration.class);
		AnnotationConfigApplicationContext child = new AnnotationConfigApplicationContext();
		child.register(OnBeanSingleCandidateInAncestorsConfiguration.class);
		child.setParent(this.context);
		child.refresh();
		assertThat(child.containsBean("baz")).isTrue();
		assertThat(child.getBean("baz")).isEqualTo("foo");
		child.close();
	}

	@Test
	public void singleCandidateInAncestorsOneCandidateInGrandparent() {
		load(FooConfiguration.class);
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		parent.setParent(this.context);
		parent.refresh();
		AnnotationConfigApplicationContext child = new AnnotationConfigApplicationContext();
		child.register(OnBeanSingleCandidateInAncestorsConfiguration.class);
		child.setParent(parent);
		child.refresh();
		assertThat(child.containsBean("baz")).isTrue();
		assertThat(child.getBean("baz")).isEqualTo("foo");
		child.close();
		parent.close();
	}

	@Test
	public void singleCandidateMultipleCandidates() {
		load(FooConfiguration.class, BarConfiguration.class, OnBeanSingleCandidateConfiguration.class);
		assertThat(this.context.containsBean("baz")).isFalse();
	}

	@Test
	public void singleCandidateMultipleCandidatesOnePrimary() {
		load(FooPrimaryConfiguration.class, BarConfiguration.class, OnBeanSingleCandidateConfiguration.class);
		assertThat(this.context.containsBean("baz")).isTrue();
		assertThat(this.context.getBean("baz")).isEqualTo("foo");
	}

	@Test
	public void singleCandidateMultipleCandidatesMultiplePrimary() {
		load(FooPrimaryConfiguration.class, BarPrimaryConfiguration.class, OnBeanSingleCandidateConfiguration.class);
		assertThat(this.context.containsBean("baz")).isFalse();
	}

	@Test
	public void invalidAnnotationTwoTypes() {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectCause(isA(IllegalArgumentException.class));
		this.thrown.expectMessage(OnBeanSingleCandidateTwoTypesConfiguration.class.getName());
		load(OnBeanSingleCandidateTwoTypesConfiguration.class);
	}

	@Test
	public void invalidAnnotationNoType() {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectCause(isA(IllegalArgumentException.class));
		this.thrown.expectMessage(OnBeanSingleCandidateNoTypeConfiguration.class.getName());
		load(OnBeanSingleCandidateNoTypeConfiguration.class);
	}

	@Test
	public void singleCandidateMultipleCandidatesInContextHierarchy() {
		load(FooPrimaryConfiguration.class, BarConfiguration.class);
		try (AnnotationConfigApplicationContext child = new AnnotationConfigApplicationContext()) {
			child.setParent(this.context);
			child.register(OnBeanSingleCandidateConfiguration.class);
			child.refresh();
			assertThat(child.containsBean("baz")).isTrue();
			assertThat(child.getBean("baz")).isEqualTo("foo");
		}
	}

	private void load(Class<?>... classes) {
		if (classes.length > 0) {
			this.context.register(classes);
		}
		this.context.refresh();
	}

	@Configuration
	@ConditionalOnSingleCandidate(String.class)
	protected static class OnBeanSingleCandidateConfiguration {

		@Bean
		public String baz(String s) {
			return s;
		}

	}

	@Configuration
	@ConditionalOnSingleCandidate(value = String.class, search = SearchStrategy.ANCESTORS)
	protected static class OnBeanSingleCandidateInAncestorsConfiguration {

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
