/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.health.contributor.HealthContributors.Entry;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MapCompositeHealthContributor}.
 *
 * @author Phillip Webb
 */
class MapCompositeHealthContributorTests
		extends MapCompositeTests<CompositeHealthContributor, HealthContributor, Entry> {

	@Override
	@SuppressWarnings("NullAway") // Test null check
	protected CompositeHealthContributor create(Map<String, String> map,
			@Nullable Function<String, HealthContributor> valueAdapter) {
		return new MapCompositeHealthContributor<>(map, valueAdapter);
	}

	@Override
	protected Stream<Entry> stream(CompositeHealthContributor composite) {
		return composite.stream();
	}

	@Override
	protected @Nullable HealthContributor getContributor(CompositeHealthContributor composite, String name) {
		return composite.getContributor(name);
	}

	@Override
	protected HealthContributor createContributor(String data) {
		return (HealthIndicator) () -> Health.up().withDetail("data", data).build();
	}

	@Override
	protected @Nullable String getData(HealthContributor contributor) {
		Health health = ((HealthIndicator) contributor).health();
		assertThat(health).isNotNull();
		return (String) health.getDetails().get("data");
	}

	@Override
	protected String getName(Entry entry) {
		return entry.name();
	}

	@Override
	protected HealthContributor getContributor(Entry entry) {
		return entry.contributor();
	}

}
