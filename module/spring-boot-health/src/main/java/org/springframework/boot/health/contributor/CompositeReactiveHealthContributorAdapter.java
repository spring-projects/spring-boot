/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

/**
 * Adapts a {@link CompositeReactiveHealthContributor} to a
 * {@link CompositeHealthContributor}.
 *
 * @author Phillip Webb
 * @see CompositeReactiveHealthContributor#asHealthContributor()
 */
class CompositeReactiveHealthContributorAdapter extends ReactiveHealthContributorsAdapter
		implements CompositeHealthContributor {

	CompositeReactiveHealthContributorAdapter(ReactiveHealthContributors delegate) {
		super(delegate);
	}

}
