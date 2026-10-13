/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CompositeHealthContributor}.
 *
 * @author Phillip Webb
 * @author Guirong Hu
 */
class CompositeHealthContributorTests {

	@Test
	void fromMapReturnsCompositeHealthContributorMapAdapter() {
		Map<String, HealthContributor> map = new LinkedHashMap<>();
		HealthIndicator indicator = () -> Health.down().build();
		map.put("test", indicator);
		CompositeHealthContributor composite = CompositeHealthContributor.fromMap(map);
		assertThat(composite).isInstanceOf(MapCompositeHealthContributor.class);
		HealthContributors.Entry entry = composite.iterator().next();
		assertThat(entry.name()).isEqualTo("test");
		assertThat(entry.contributor()).isSameAs(indicator);
	}

	@Test
	void fromMapWithAdapterReturnsCompositeHealthContributorMapAdapter() {
		Map<String, HealthContributor> map = new LinkedHashMap<>();
		HealthIndicator downIndicator = () -> Health.down().build();
		HealthIndicator upIndicator = () -> Health.up().build();
		map.put("test", downIndicator);
		CompositeHealthContributor composite = CompositeHealthContributor.fromMap(map, (value) -> upIndicator);
		assertThat(composite).isInstanceOf(MapCompositeHealthContributor.class);
		HealthContributors.Entry entry = composite.iterator().next();
		assertThat(entry.name()).isEqualTo("test");
		assertThat(entry.contributor()).isSameAs(upIndicator);
	}

}
