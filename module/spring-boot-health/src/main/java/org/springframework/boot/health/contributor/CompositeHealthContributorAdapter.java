/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

/**
 * Adapts a {@link CompositeHealthContributor} to a
 * {@link CompositeReactiveHealthContributor}.
 *
 * @author Phillip Webb
 * @see ReactiveHealthContributor#adapt(HealthContributor)
 */
class CompositeHealthContributorAdapter extends HealthContributorsAdapter
		implements CompositeReactiveHealthContributor {

	CompositeHealthContributorAdapter(HealthContributors delegate) {
		super(delegate);
	}

}
