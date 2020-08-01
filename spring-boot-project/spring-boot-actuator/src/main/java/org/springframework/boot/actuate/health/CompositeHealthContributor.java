/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.health;

import java.util.Map;
import java.util.function.Function;

/**
 * A {@link HealthContributor} that is composed of other {@link HealthContributor}
 * instances.
 *
 * @author Phillip Webb
 * @since 2.2.0
 * @see CompositeHealth
 * @see CompositeReactiveHealthContributor
 */
public interface CompositeHealthContributor extends HealthContributor, NamedContributors<HealthContributor> {

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
		return new CompositeHealthContributorMapAdapter<>(map, valueAdapter);
	}

}
