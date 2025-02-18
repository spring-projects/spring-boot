/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.observation;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation.Context;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertiesObservationFilterPredicate}.
 *
 * @author Moritz Halbritter
 */
class PropertiesObservationFilterPredicateTests {

	@Test
	void shouldDoNothingIfKeyValuesAreEmpty() {
		PropertiesObservationFilterPredicate filter = createFilter();
		Context mapped = mapContext(filter, "a", "alpha");
		assertThat(mapped.getLowCardinalityKeyValues()).containsExactly(KeyValue.of("a", "alpha"));
	}

	@Test
	void shouldAddKeyValues() {
		PropertiesObservationFilterPredicate filter = createFilter("b", "beta");
		Context mapped = mapContext(filter, "a", "alpha");
		assertThat(mapped.getLowCardinalityKeyValues()).containsExactly(KeyValue.of("a", "alpha"),
				KeyValue.of("b", "beta"));
	}

	@Test
	void shouldFilter() {
		PropertiesObservationFilterPredicate predicate = createPredicate("spring.security");
		Context context = new Context();
		assertThat(predicate.test("spring.security.filterchains", context)).isFalse();
		assertThat(predicate.test("spring.security", context)).isFalse();
		assertThat(predicate.test("spring.data", context)).isTrue();
		assertThat(predicate.test("spring", context)).isTrue();
	}

	@Test
	void filterShouldFallbackToAll() {
		PropertiesObservationFilterPredicate predicate = createPredicate("all");
		Context context = new Context();
		assertThat(predicate.test("spring.security.filterchains", context)).isFalse();
		assertThat(predicate.test("spring.security", context)).isFalse();
		assertThat(predicate.test("spring.data", context)).isFalse();
		assertThat(predicate.test("spring", context)).isFalse();
	}

	@Test
	void shouldNotFilterIfDisabledNamesIsEmpty() {
		PropertiesObservationFilterPredicate predicate = createPredicate();
		Context context = new Context();
		assertThat(predicate.test("spring.security.filterchains", context)).isTrue();
		assertThat(predicate.test("spring.security", context)).isTrue();
		assertThat(predicate.test("spring.data", context)).isTrue();
		assertThat(predicate.test("spring", context)).isTrue();
	}

	private static Context mapContext(PropertiesObservationFilterPredicate filter, String... initialKeyValues) {
		Context context = new Context();
		context.addLowCardinalityKeyValues(KeyValues.of(initialKeyValues));
		return filter.map(context);
	}

	private static PropertiesObservationFilterPredicate createFilter(String... keyValues) {
		ObservationProperties properties = new ObservationProperties();
		for (int i = 0; i < keyValues.length; i += 2) {
			properties.getKeyValues().put(keyValues[i], keyValues[i + 1]);
		}
		return new PropertiesObservationFilterPredicate(properties);
	}

	private static PropertiesObservationFilterPredicate createPredicate(String... disabledNames) {
		ObservationProperties properties = new ObservationProperties();
		for (String name : disabledNames) {
			properties.getEnable().put(name, false);
		}
		return new PropertiesObservationFilterPredicate(properties);
	}

}
