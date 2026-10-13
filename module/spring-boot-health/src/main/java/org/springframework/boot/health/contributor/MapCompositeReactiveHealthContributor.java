/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * {@link CompositeReactiveHealthContributor} backed by a map with values adapted as
 * necessary.
 *
 * @param <V> the value type
 * @author Phillip Webb
 * @see CompositeReactiveHealthContributor#fromMap(Map, Function)
 */
class MapCompositeReactiveHealthContributor<V> implements CompositeReactiveHealthContributor {

	private final Map<String, ReactiveHealthContributor> contributors;

	MapCompositeReactiveHealthContributor(Map<String, V> map,
			Function<V, ? extends ReactiveHealthContributor> valueAdapter) {
		Assert.notNull(map, "'map' must not be null");
		Assert.notNull(valueAdapter, "'valueAdapter' must not be null");
		LinkedHashMap<String, ReactiveHealthContributor> contributors = new LinkedHashMap<>();
		map.forEach((key, value) -> {
			Assert.notNull(key, "'map' must not contain null keys");
			Assert.notNull(value, "'map' must not contain null values");
			Assert.isTrue(!key.contains("/"), "'map' keys must not contain a '/'");
			contributors.put(key, valueAdapter.apply(value));
		});
		this.contributors = Collections.unmodifiableMap(contributors);
	}

	@Override
	public @Nullable ReactiveHealthContributor getContributor(String name) {
		return this.contributors.get(name);
	}

	@Override
	public Stream<Entry> stream() {
		return this.contributors.entrySet().stream().map((entry) -> new Entry(entry.getKey(), entry.getValue()));
	}

}
