/*
 * Copyright 2012-2021 the original author or authors.
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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link NamedContributorsMapAdapter}.
 *
 * @author Phillip Webb
 */
class NamedContributorsMapAdapterTests {

	@Test
	void createWhenMapIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new TestNamedContributorsMapAdapter<>(null, Function.identity()))
				.withMessage("Map must not be null");
	}

	@Test
	void createWhenValueAdapterIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new TestNamedContributorsMapAdapter<>(Collections.emptyMap(), null))
				.withMessage("ValueAdapter must not be null");
	}

	@Test
	void createWhenMapContainsNullValueThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new TestNamedContributorsMapAdapter<>(Collections.singletonMap("test", null),
						Function.identity()))
				.withMessage("Map must not contain null values");
	}

	@Test
	void createWhenMapContainsNullKeyThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new TestNamedContributorsMapAdapter<>(Collections.singletonMap(null, "test"),
						Function.identity()))
				.withMessage("Map must not contain null keys");
	}

	@Test
	void createWhenMapContainsKeyWithSlashThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new TestNamedContributorsMapAdapter<>(Collections.singletonMap("test/key", "test"),
						Function.identity()))
				.withMessage("Map keys must not contain a '/'");
	}

	@Test
	void iterateReturnsAdaptedEntries() {
		TestNamedContributorsMapAdapter<String> adapter = createAdapter();
		Iterator<NamedContributor<String>> iterator = adapter.iterator();
		NamedContributor<String> one = iterator.next();
		NamedContributor<String> two = iterator.next();
		assertThat(iterator.hasNext()).isFalse();
		assertThat(one.getName()).isEqualTo("one");
		assertThat(one.getContributor()).isEqualTo("eno");
		assertThat(two.getName()).isEqualTo("two");
		assertThat(two.getContributor()).isEqualTo("owt");
	}

	@Test
	void getContributorReturnsAdaptedEntry() {
		TestNamedContributorsMapAdapter<String> adapter = createAdapter();
		assertThat(adapter.getContributor("one")).isEqualTo("eno");
		assertThat(adapter.getContributor("two")).isEqualTo("owt");
	}

	@Test
	void getContributorWhenNotInMapReturnsNull() {
		TestNamedContributorsMapAdapter<String> adapter = createAdapter();
		assertThat(adapter.getContributor("missing")).isNull();
	}

	private TestNamedContributorsMapAdapter<String> createAdapter() {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("one", "one");
		map.put("two", "two");
		TestNamedContributorsMapAdapter<String> adapter = new TestNamedContributorsMapAdapter<>(map, this::reverse);
		return adapter;
	}

	private String reverse(CharSequence charSequence) {
		return new StringBuilder(charSequence).reverse().toString();
	}

	static class TestNamedContributorsMapAdapter<V> extends NamedContributorsMapAdapter<V, String> {

		TestNamedContributorsMapAdapter(Map<String, V> map, Function<V, ? extends String> valueAdapter) {
			super(map, valueAdapter);
		}

	}

}
