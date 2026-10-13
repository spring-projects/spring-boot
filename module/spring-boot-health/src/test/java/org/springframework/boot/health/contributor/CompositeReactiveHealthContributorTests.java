/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CompositeReactiveHealthContributor}.
 *
 * @author Phillip Webb
 */
class CompositeReactiveHealthContributorTests {

	@Test
	void asHealthContributorReturnsAdaptedContributor() {
		ReactiveHealthIndicator indicator = () -> Mono.just(Health.up().withDetail("spring", "boot").build());
		Map<String, ReactiveHealthContributor> reactiveContributors = Map.of("test", indicator);
		CompositeReactiveHealthContributor contributor = CompositeReactiveHealthContributor
			.fromMap(reactiveContributors);
		CompositeHealthContributor adapted = contributor.asHealthContributor();
		HealthIndicator byName = (HealthIndicator) adapted.getContributor("test");
		assertThat(byName).isNotNull();
		Health health = byName.health(true);
		assertThat(health).isNotNull();
		assertThat(health.getDetails()).containsEntry("spring", "boot");
		HealthContributors.Entry entry = adapted.iterator().next();
		assertThat(entry.name()).isEqualTo("test");
		HealthIndicator byEntry = (HealthIndicator) entry.contributor();
		health = byEntry.health(true);
		assertThat(health).isNotNull();
		assertThat(health.getDetails()).containsEntry("spring", "boot");
	}

	@Test
	void fromMapReturnsCompositeReactiveHealthContributorMapAdapter() {
		Map<String, ReactiveHealthContributor> map = new LinkedHashMap<>();
		ReactiveHealthIndicator indicator = () -> Mono.just(Health.down().build());
		map.put("test", indicator);
		CompositeReactiveHealthContributor composite = CompositeReactiveHealthContributor.fromMap(map);
		assertThat(composite).isInstanceOf(MapCompositeReactiveHealthContributor.class);
		ReactiveHealthContributors.Entry entry = composite.iterator().next();
		assertThat(entry.name()).isEqualTo("test");
		assertThat(entry.contributor()).isSameAs(indicator);
	}

	@Test
	void fromMapWithAdapterReturnsCompositeReactiveHealthContributorMapAdapter() {
		Map<String, ReactiveHealthContributor> map = new LinkedHashMap<>();
		ReactiveHealthIndicator downIndicator = () -> Mono.just(Health.down().build());
		ReactiveHealthIndicator upIndicator = () -> Mono.just(Health.up().build());
		map.put("test", downIndicator);
		CompositeReactiveHealthContributor composite = CompositeReactiveHealthContributor.fromMap(map,
				(value) -> upIndicator);
		assertThat(composite).isInstanceOf(MapCompositeReactiveHealthContributor.class);
		ReactiveHealthContributors.Entry entry = composite.iterator().next();
		assertThat(entry.name()).isEqualTo("test");
		assertThat(entry.contributor()).isSameAs(upIndicator);
	}

}
