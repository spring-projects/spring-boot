/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.autoconfigure.contributor;

import java.util.stream.Stream;

import reactor.core.publisher.Mono;

import org.springframework.boot.health.autoconfigure.contributor.CompositeReactiveHealthContributorConfigurationTests.TestReactiveHealthIndicator;
import org.springframework.boot.health.contributor.AbstractReactiveHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.ReactiveHealthContributor;
import org.springframework.boot.health.contributor.ReactiveHealthContributors;

/**
 * Tests for {@link CompositeReactiveHealthContributorConfiguration}.
 *
 * @author Phillip Webb
 */
class CompositeReactiveHealthContributorConfigurationTests extends
		AbstractCompositeHealthContributorConfigurationTests<ReactiveHealthContributor, TestReactiveHealthIndicator> {

	@Override
	protected AbstractCompositeHealthContributorConfiguration<ReactiveHealthContributor, TestReactiveHealthIndicator, TestBean> newComposite() {
		return new TestCompositeReactiveHealthContributorConfiguration();
	}

	@Override
	protected Stream<String> allNamesFromComposite(ReactiveHealthContributor composite) {
		return ((ReactiveHealthContributors) composite).stream().map(ReactiveHealthContributors.Entry::name);
	}

	static class TestCompositeReactiveHealthContributorConfiguration
			extends CompositeReactiveHealthContributorConfiguration<TestReactiveHealthIndicator, TestBean> {

		TestCompositeReactiveHealthContributorConfiguration() {
			super(TestReactiveHealthIndicator::new);
		}

	}

	static class TestReactiveHealthIndicator extends AbstractReactiveHealthIndicator {

		TestReactiveHealthIndicator(TestBean testBean) {
		}

		@Override
		protected Mono<Health> doHealthCheck(Health.Builder builder) {
			return Mono.just(builder.up().build());
		}

	}

}
