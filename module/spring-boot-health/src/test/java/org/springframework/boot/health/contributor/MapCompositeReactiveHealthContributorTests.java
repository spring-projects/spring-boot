/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.boot.health.contributor.ReactiveHealthContributors.Entry;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MapCompositeReactiveHealthContributor}.
 *
 * @author Phillip Webb
 */
class MapCompositeReactiveHealthContributorTests
		extends MapCompositeTests<CompositeReactiveHealthContributor, ReactiveHealthContributor, Entry> {

	@Override
	@SuppressWarnings("NullAway") // Test null check
	protected CompositeReactiveHealthContributor create(Map<String, String> map,
			@Nullable Function<String, ReactiveHealthContributor> valueAdapter) {
		return new MapCompositeReactiveHealthContributor<>(map, valueAdapter);
	}

	@Override
	protected Stream<Entry> stream(CompositeReactiveHealthContributor composite) {
		return composite.stream();
	}

	@Override
	protected @Nullable ReactiveHealthContributor getContributor(CompositeReactiveHealthContributor composite,
			String name) {
		return composite.getContributor(name);
	}

	@Override
	protected ReactiveHealthContributor createContributor(String data) {
		return (ReactiveHealthIndicator) () -> Mono.just(Health.up().withDetail("data", data).build());
	}

	@Override
	protected @Nullable String getData(ReactiveHealthContributor contributor) {
		Health health = ((ReactiveHealthIndicator) contributor).health().block();
		assertThat(health).isNotNull();
		return (String) health.getDetails().get("data");
	}

	@Override
	protected String getName(Entry entry) {
		return entry.name();
	}

	@Override
	protected ReactiveHealthContributor getContributor(Entry entry) {
		return entry.contributor();
	}

}
