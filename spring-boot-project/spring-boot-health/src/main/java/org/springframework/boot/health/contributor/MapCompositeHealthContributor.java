/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.health.contributor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.util.Assert;

/**
 * {@link CompositeHealthContributor} backed by a map with values adapted as necessary.
 *
 * @param <V> the value type
 * @author Phillip Webb
 * @see CompositeHealthContributor#fromMap(Map, Function)
 */
class MapCompositeHealthContributor<V> implements CompositeHealthContributor {

	private final Map<String, HealthContributor> contributors;

	MapCompositeHealthContributor(Map<String, V> map, Function<V, ? extends HealthContributor> valueAdapter) {
		Assert.notNull(map, "'map' must not be null");
		Assert.notNull(valueAdapter, "'valueAdapter' must not be null");
		LinkedHashMap<String, HealthContributor> contributors = new LinkedHashMap<>();
		map.forEach((key, value) -> {
			Assert.notNull(key, "'map' must not contain null keys");
			Assert.notNull(value, "'map' must not contain null values");
			Assert.isTrue(!key.contains("/"), "'map' keys must not contain a '/'");
			contributors.put(key, valueAdapter.apply(value));
		});
		this.contributors = Collections.unmodifiableMap(contributors);
	}

	@Override
	public HealthContributor getContributor(String name) {
		return this.contributors.get(name);
	}

	@Override
	public Stream<Entry> stream() {
		return this.contributors.entrySet().stream().map((entry) -> new Entry(entry.getKey(), entry.getValue()));
	}

}
