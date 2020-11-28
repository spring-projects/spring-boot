/*
 * Copyright 2012-2020 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnSingleCandidate @ConditionalOnSingleCandidate}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
class ConditionalOnSingleCandidateTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	void singleCandidateNoCandidate() {
		this.contextRunner.withUserConfiguration(OnBeanSingleCandidateConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean("consumer"));
	}

	@Test
	void singleCandidateOneCandidate() {
		this.contextRunner.withUserConfiguration(AlphaConfiguration.class, OnBeanSingleCandidateConfiguration.class)
				.run((context) -> {
					assertThat(context).hasBean("consumer");
					assertThat(context.getBean("consumer")).isEqualTo("alpha");
				});
	}

	@Test
	void singleCandidateOneScopedProxyCandidate() {
		this.contextRunner
				.withUserConfiguration(AlphaScopedProxyConfiguration.class, OnBeanSingleCandidateConfiguration.class)
				.run((context) -> {
					assertThat(context).hasBean("consumer");
					assertThat(context.getBean("consumer").toString()).isEqualTo("alpha");
				});
	}

	@Test
	void singleCandidateInAncestorsOneCandidateInCurrent() {
		this.contextRunner.run((parent) -> this.contextRunner
				.withUserConfiguration(AlphaConfiguration.class, OnBeanSingleCandidateInAncestorsConfiguration.class)
				.withParent(parent).run((child) -> assertThat(child).doesNotHaveBean("consumer")));
	}

	@Test
	void singleCandidateInAncestorsOneCandidateInParent() {
		this.contextRunner.withUserConfiguration(AlphaConfiguration.class)
				.run((parent) -> this.contextRunner
						.withUserConfiguration(OnBeanSingleCandidateInAncestorsConfiguration.class).withParent(parent)
						.run((child) -> {
							assertThat(child).hasBean("consumer");
							assertThat(child.getBean("consumer")).isEqualTo("alpha");
						}));
	}

	@Test
	void singleCandidateInAncestorsOneCandidateInGrandparent() {
		this.contextRunner.withUserConfiguration(AlphaConfiguration.class)
				.run((grandparent) -> this.contextRunner.withParent(grandparent)
						.run((parent) -> this.contextRunner
								.withUserConfiguration(OnBeanSingleCandidateInAncestorsConfiguration.class)
								.withParent(parent).run((child) -> {
									assertThat(child).hasBean("consumer");
									assertThat(child.getBean("consumer")).isEqualTo("alpha");
								})));
	}

	@Test
	void singleCandidateMultipleCandidates() {
		this.contextRunner
				.withUserConfiguration(AlphaConfiguration.class, BravoConfiguration.class,
						OnBeanSingleCandidateConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean("consumer"));
	}

	@Test
	void singleCandidateMultipleCandidatesOnePrimary() {
		this.contextRunner.withUserConfiguration(AlphaPrimaryConfiguration.class, BravoConfiguration.class,
				OnBeanSingleCandidateConfiguration.class).run((context) -> {
					assertThat(context).hasBean("consumer");
					assertThat(context.getBean("consumer")).isEqualTo("alpha");
				});
	}

	@Test
	void singleCandidateMultipleCandidatesMultiplePrimary() {
		this.contextRunner
				.withUserConfiguration(AlphaPrimaryConfiguration.class, BravoPrimaryConfiguration.class,
						OnBeanSingleCandidateConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean("consumer"));
	}

	@Test
	void invalidAnnotationTwoTypes() {
		this.contextRunner.withUserConfiguration(OnBeanSingleCandidateTwoTypesConfiguration.class).run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context).getFailure().hasCauseInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining(OnBeanSingleCandidateTwoTypesConfiguration.class.getName());
		});
	}

	@Test
	void invalidAnnotationNoType() {
		this.contextRunner.withUserConfiguration(OnBeanSingleCandidateNoTypeConfiguration.class).run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context).getFailure().hasCauseInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining(OnBeanSingleCandidateNoTypeConfiguration.class.getName());
		});
	}

	@Test
	void singleCandidateMultipleCandidatesInContextHierarchy() {
		this.contextRunner.withUserConfiguration(AlphaPrimaryConfiguration.class, BravoConfiguration.class)
				.run((parent) -> this.contextRunner.withUserConfiguration(OnBeanSingleCandidateConfiguration.class)
						.withParent(parent).run((child) -> {
							assertThat(child).hasBean("consumer");
							assertThat(child.getBean("consumer")).isEqualTo("alpha");
						}));
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnSingleCandidate(String.class)
	static class OnBeanSingleCandidateConfiguration {

		@Bean
		CharSequence consumer(CharSequence s) {
			return s;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnSingleCandidate(value = String.class, search = SearchStrategy.ANCESTORS)
	static class OnBeanSingleCandidateInAncestorsConfiguration {

		@Bean
		CharSequence consumer(CharSequence s) {
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
	static class AlphaConfiguration {

		@Bean
		String alpha() {
			return "alpha";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class AlphaPrimaryConfiguration {

		@Bean
		@Primary
		String alpha() {
			return "alpha";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class AlphaScopedProxyConfiguration {

		@Bean
		@Scope(proxyMode = ScopedProxyMode.INTERFACES)
		String alpha() {
			return "alpha";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class BravoConfiguration {

		@Bean
		String bravo() {
			return "bravo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class BravoPrimaryConfiguration {

		@Bean
		@Primary
		String bravo() {
			return "bravo";
		}

	}

}
