/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

import java.util.Map;
import java.util.function.Function;

/**
 * A {@link HealthContributor} that is composed of other {@link HealthContributor}
 * instances.
 *
 * @author Phillip Webb
 * @author Guirong Hu
 * @since 4.0.0
 * @see CompositeReactiveHealthContributor
 */
public non-sealed interface CompositeHealthContributor extends HealthContributor, HealthContributors {

	/**
	 * Factory method that will create a {@link CompositeHealthContributor} from the
	 * specified map.
	 * @param map the source map
	 * @return a composite health contributor instance
	 */
	static CompositeHealthContributor fromMap(Map<String, ? extends HealthContributor> map) {
		return fromMap(map, Function.identity());
	}

	/**
	 * Factory method that will create a {@link CompositeHealthContributor} from the
	 * specified map.
	 * @param <V> the value type
	 * @param map the source map
	 * @param valueAdapter function used to adapt the map value
	 * @return a composite health contributor instance
	 */
	static <V> CompositeHealthContributor fromMap(Map<String, V> map,
			Function<V, ? extends HealthContributor> valueAdapter) {
		return new MapCompositeHealthContributor<>(map, valueAdapter);
	}

}
