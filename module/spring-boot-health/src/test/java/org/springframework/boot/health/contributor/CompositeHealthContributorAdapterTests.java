/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

/**
 * Tests for {@link CompositeHealthContributorAdapter}.
 *
 * @author Phillip Webb
 */
class CompositeHealthContributorAdapterTests extends HealthContributorsAdapterTests {

	@Override
	protected CompositeHealthContributorAdapter createAdapter(CompositeHealthContributor delegate) {
		return new CompositeHealthContributorAdapter(delegate);
	}

}
