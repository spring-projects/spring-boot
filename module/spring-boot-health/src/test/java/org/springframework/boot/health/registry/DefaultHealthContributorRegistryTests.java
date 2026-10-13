/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.registry;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.health.contributor.HealthContributors;
import org.springframework.boot.health.contributor.HealthContributors.Entry;
import org.springframework.boot.health.contributor.HealthIndicator;

import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultHealthContributorRegistry}.
 *
 * @author Phillip Webb
 */
class DefaultHealthContributorRegistryTests
		extends AbstractHealthContributorRegistryTests<HealthContributor, HealthContributors.Entry> {

	@Override
	protected AbstractRegistry<HealthContributor, Entry> createRegistry(
			Collection<? extends HealthContributorNameValidator> nameValidators,
			@Nullable Consumer<BiConsumer<String, HealthContributor>> initialRegistrations) {
		return new DefaultHealthContributorRegistry(nameValidators, initialRegistrations);
	}

	@Override
	protected HealthContributor mockHealthIndicator() {
		return mock(HealthIndicator.class);
	}

	@Override
	protected String name(Entry entry) {
		return entry.name();
	}

	@Override
	protected HealthContributor contributor(Entry entry) {
		return entry.contributor();
	}

}
