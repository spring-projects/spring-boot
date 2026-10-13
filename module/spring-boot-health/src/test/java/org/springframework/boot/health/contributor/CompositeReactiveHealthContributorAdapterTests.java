/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

/**
 * Tests for {@link CompositeReactiveHealthContributorAdapter}.
 *
 * @author Phillip Webb
 */
class CompositeReactiveHealthContributorAdapterTests extends ReactiveHealthContributorsAdapterTests {

	@Override
	protected CompositeReactiveHealthContributorAdapter createAdapter(CompositeReactiveHealthContributor delegate) {
		return new CompositeReactiveHealthContributorAdapter(delegate);
	}

}
