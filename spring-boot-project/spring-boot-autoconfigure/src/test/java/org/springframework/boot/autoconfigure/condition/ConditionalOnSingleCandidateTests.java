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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ConditionalOnSingleCandidate @ConditionalOnSingleCandidate}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
class ConditionalOnSingleCandidateTests {

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@AfterEach
	void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void singleCandidateNoCandidate() {
		load(OnBeanSingleCandidateConfiguration.class);
		assertThat(this.context.containsBean("baz")).isFalse();
	}

	@Test
	void singleCandidateOneCandidate() {
		load(FooConfiguration.class, OnBeanSingleCandidateConfiguration.class);
		assertThat(this.context.containsBean("baz")).isTrue();
		assertThat(this.context.getBean("baz")).isEqualTo("foo");
	}

	@Test
	void singleCandidateInAncestorsOneCandidateInCurrent() {
		load();
		AnnotationConfigApplicationContext child = new AnnotationConfigApplicationContext();
		child.register(FooConfiguration.class, OnBeanSingleCandidateInAncestorsConfiguration.class);
		child.setParent(this.context);
		child.refresh();
		assertThat(child.containsBean("baz")).isFalse();
		child.close();
	}

	@Test
	void singleCandidateInAncestorsOneCandidateInParent() {
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
	void singleCandidateInAncestorsOneCandidateInGrandparent() {
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
	void singleCandidateMultipleCandidates() {
		load(FooConfiguration.class, BarConfiguration.class, OnBeanSingleCandidateConfiguration.class);
		assertThat(this.context.containsBean("baz")).isFalse();
	}

	@Test
	void singleCandidateMultipleCandidatesOnePrimary() {
		load(FooPrimaryConfiguration.class, BarConfiguration.class, OnBeanSingleCandidateConfiguration.class);
		assertThat(this.context.containsBean("baz")).isTrue();
		assertThat(this.context.getBean("baz")).isEqualTo("foo");
	}

	@Test
	void singleCandidateMultipleCandidatesMultiplePrimary() {
		load(FooPrimaryConfiguration.class, BarPrimaryConfiguration.class, OnBeanSingleCandidateConfiguration.class);
		assertThat(this.context.containsBean("baz")).isFalse();
	}

	@Test
	void invalidAnnotationTwoTypes() {
		assertThatIllegalStateException().isThrownBy(() -> load(OnBeanSingleCandidateTwoTypesConfiguration.class))
				.withCauseInstanceOf(IllegalArgumentException.class)
				.withMessageContaining(OnBeanSingleCandidateTwoTypesConfiguration.class.getName());
	}

	@Test
	void invalidAnnotationNoType() {
		assertThatIllegalStateException().isThrownBy(() -> load(OnBeanSingleCandidateNoTypeConfiguration.class))
				.withCauseInstanceOf(IllegalArgumentException.class)
				.withMessageContaining(OnBeanSingleCandidateNoTypeConfiguration.class.getName());
	}

	@Test
	void singleCandidateMultipleCandidatesInContextHierarchy() {
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

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnSingleCandidate(String.class)
	static class OnBeanSingleCandidateConfiguration {

		@Bean
		String baz(String s) {
			return s;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnSingleCandidate(value = String.class, search = SearchStrategy.ANCESTORS)
	static class OnBeanSingleCandidateInAncestorsConfiguration {

		@Bean
		String baz(String s) {
			return s;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnSingleCandidate(value = String.class, type = "java.lang.Integer")
	static class OnBeanSingleCandidateTwoTypesConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnSingleCandidate
	static class OnBeanSingleCandidateNoTypeConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class FooConfiguration {

		@Bean
		String foo() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FooPrimaryConfiguration {

		@Bean
		@Primary
		String foo() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class BarConfiguration {

		@Bean
		String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class BarPrimaryConfiguration {

		@Bean
		@Primary
		String bar() {
			return "bar";
		}

	}

}
