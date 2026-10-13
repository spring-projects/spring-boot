/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.registry;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.health.contributor.ReactiveHealthContributor;
import org.springframework.boot.health.contributor.ReactiveHealthContributors;
import org.springframework.boot.health.contributor.ReactiveHealthContributors.Entry;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;

import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultReactiveHealthContributorRegistry}.
 *
 * @author Phillip Webb
 */
class DefaultReactiveHealthContributorRegistryTests
		extends AbstractHealthContributorRegistryTests<ReactiveHealthContributor, ReactiveHealthContributors.Entry> {

	@Override
	protected AbstractRegistry<ReactiveHealthContributor, Entry> createRegistry(
			Collection<? extends HealthContributorNameValidator> nameValidators,
			@Nullable Consumer<BiConsumer<String, ReactiveHealthContributor>> initialRegistrations) {
		return new DefaultReactiveHealthContributorRegistry(nameValidators, initialRegistrations);
	}

	@Override
	protected ReactiveHealthContributor mockHealthIndicator() {
		return mock(ReactiveHealthIndicator.class);
	}

	@Override
	protected String name(Entry entry) {
		return entry.name();
	}

	@Override
	protected ReactiveHealthContributor contributor(Entry entry) {
		return entry.contributor();
	}

}
