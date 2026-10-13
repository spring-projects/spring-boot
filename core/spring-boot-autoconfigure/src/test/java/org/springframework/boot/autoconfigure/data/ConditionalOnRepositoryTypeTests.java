/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.data;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnRepositoryType @ConditionalOnRepositoryType}.
 *
 * @author Andy Wilkinson
 */
class ConditionalOnRepositoryTypeTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	void imperativeRepositoryMatchesWithNoConfiguredType() {
		this.contextRunner.withUserConfiguration(ImperativeRepository.class)
			.run((context) -> assertThat(context).hasSingleBean(ImperativeRepository.class));
	}

	@Test
	void reactiveRepositoryMatchesWithNoConfiguredType() {
		this.contextRunner.withUserConfiguration(ReactiveRepository.class)
			.run((context) -> assertThat(context).hasSingleBean(ReactiveRepository.class));
	}

	@Test
	void imperativeRepositoryMatchesWithAutoConfiguredType() {
		this.contextRunner.withUserConfiguration(ImperativeRepository.class)
			.withPropertyValues("spring.data.test.repositories.type:auto")
			.run((context) -> assertThat(context).hasSingleBean(ImperativeRepository.class));
	}

	@Test
	void reactiveRepositoryMatchesWithAutoConfiguredType() {
		this.contextRunner.withUserConfiguration(ReactiveRepository.class)
			.withPropertyValues("spring.data.test.repositories.type:auto")
			.run((context) -> assertThat(context).hasSingleBean(ReactiveRepository.class));
	}

	@Test
	void imperativeRepositoryMatchesWithImperativeConfiguredType() {
		this.contextRunner.withUserConfiguration(ImperativeRepository.class)
			.withPropertyValues("spring.data.test.repositories.type:imperative")
			.run((context) -> assertThat(context).hasSingleBean(ImperativeRepository.class));
	}

	@Test
	void reactiveRepositoryMatchesWithReactiveConfiguredType() {
		this.contextRunner.withUserConfiguration(ReactiveRepository.class)
			.withPropertyValues("spring.data.test.repositories.type:reactive")
			.run((context) -> assertThat(context).hasSingleBean(ReactiveRepository.class));
	}

	@Test
	void imperativeRepositoryDoesNotMatchWithReactiveConfiguredType() {
		this.contextRunner.withUserConfiguration(ImperativeRepository.class)
			.withPropertyValues("spring.data.test.repositories.type:reactive")
			.run((context) -> assertThat(context).doesNotHaveBean(ImperativeRepository.class));
	}

	@Test
	void reactiveRepositoryDoesNotMatchWithImperativeConfiguredType() {
		this.contextRunner.withUserConfiguration(ReactiveRepository.class)
			.withPropertyValues("spring.data.test.repositories.type:imperative")
			.run((context) -> assertThat(context).doesNotHaveBean(ReactiveRepository.class));
	}

	@Test
	void imperativeRepositoryDoesNotMatchWithNoneConfiguredType() {
		this.contextRunner.withUserConfiguration(ImperativeRepository.class)
			.withPropertyValues("spring.data.test.repositories.type:none")
			.run((context) -> assertThat(context).doesNotHaveBean(ImperativeRepository.class));
	}

	@Test
	void reactiveRepositoryDoesNotMatchWithNoneConfiguredType() {
		this.contextRunner.withUserConfiguration(ReactiveRepository.class)
			.withPropertyValues("spring.data.test.repositories.type:none")
			.run((context) -> assertThat(context).doesNotHaveBean(ReactiveRepository.class));
	}

	@Test
	void failsFastWhenConfiguredTypeIsUnknown() {
		this.contextRunner.withUserConfiguration(ReactiveRepository.class)
			.withPropertyValues("spring.data.test.repositories.type:abcde")
			.run((context) -> assertThat(context).hasFailed());
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnRepositoryType(store = "test", type = RepositoryType.IMPERATIVE)
	static class ImperativeRepository {

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnRepositoryType(store = "test", type = RepositoryType.REACTIVE)
	static class ReactiveRepository {

	}

}
