/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

import java.util.Map;
import java.util.function.Function;

/**
 * A {@link ReactiveHealthContributor} that is composed of other
 * {@link ReactiveHealthContributor} instances.
 *
 * @author Phillip Webb
 * @since 4.0.0
 * @see CompositeHealthContributor
 */
public non-sealed interface CompositeReactiveHealthContributor
		extends ReactiveHealthContributor, ReactiveHealthContributors {

	@Override
	default CompositeHealthContributor asHealthContributor() {
		return new CompositeReactiveHealthContributorAdapter(this);
	}

	/**
	 * Factory method that will create a {@link CompositeReactiveHealthContributor} from
	 * the specified map.
	 * @param map the source map
	 * @return a composite health contributor instance
	 */
	static CompositeReactiveHealthContributor fromMap(Map<String, ? extends ReactiveHealthContributor> map) {
		return fromMap(map, Function.identity());
	}

	/**
	 * Factory method that will create a {@link CompositeReactiveHealthContributor} from
	 * the specified map.
	 * @param <V> the value type
	 * @param map the source map
	 * @param valueAdapter function used to adapt the map value
	 * @return a composite health contributor instance
	 */
	static <V> CompositeReactiveHealthContributor fromMap(Map<String, V> map,
			Function<V, ? extends ReactiveHealthContributor> valueAdapter) {
		return new MapCompositeReactiveHealthContributor<>(map, valueAdapter);
	}

}
