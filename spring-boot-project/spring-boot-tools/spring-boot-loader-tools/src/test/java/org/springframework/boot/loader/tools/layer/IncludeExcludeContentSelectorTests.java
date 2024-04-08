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

package org.springframework.boot.loader.tools.layer;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.loader.tools.Layer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link IncludeExcludeContentSelector}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class IncludeExcludeContentSelectorTests {

	private static final Layer LAYER = new Layer("test");

	@Test
	void createWhenLayerIsNullThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(
					() -> new IncludeExcludeContentSelector<>(null, Collections.emptyList(), Collections.emptyList()))
			.withMessage("Layer must not be null");
	}

	@Test
	void createWhenFactoryIsNullThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new IncludeExcludeContentSelector<>(LAYER, null, null, null))
			.withMessage("FilterFactory must not be null");
	}

	@Test
	void getLayerReturnsLayer() {
		IncludeExcludeContentSelector<?> selector = new IncludeExcludeContentSelector<>(LAYER, null, null);
		assertThat(selector.getLayer()).isEqualTo(LAYER);
	}

	@Test
	void containsWhenEmptyIncludesAndEmptyExcludesReturnsTrue() {
		List<String> includes = Collections.emptyList();
		List<String> excludes = Collections.emptyList();
		IncludeExcludeContentSelector<String> selector = new IncludeExcludeContentSelector<>(LAYER, includes, excludes,
				TestContentsFilter::new);
		assertThat(selector.contains("A")).isTrue();
	}

	@Test
	void containsWhenNullIncludesAndEmptyExcludesReturnsTrue() {
		List<String> includes = null;
		List<String> excludes = null;
		IncludeExcludeContentSelector<String> selector = new IncludeExcludeContentSelector<>(LAYER, includes, excludes,
				TestContentsFilter::new);
		assertThat(selector.contains("A")).isTrue();
	}

	@Test
	void containsWhenEmptyIncludesAndNotExcludedReturnsTrue() {
		List<String> includes = Collections.emptyList();
		List<String> excludes = List.of("B");
		IncludeExcludeContentSelector<String> selector = new IncludeExcludeContentSelector<>(LAYER, includes, excludes,
				TestContentsFilter::new);
		assertThat(selector.contains("A")).isTrue();
	}

	@Test
	void containsWhenEmptyIncludesAndExcludedReturnsFalse() {
		List<String> includes = Collections.emptyList();
		List<String> excludes = List.of("A");
		IncludeExcludeContentSelector<String> selector = new IncludeExcludeContentSelector<>(LAYER, includes, excludes,
				TestContentsFilter::new);
		assertThat(selector.contains("A")).isFalse();
	}

	@Test
	void containsWhenIncludedAndEmptyExcludesReturnsTrue() {
		List<String> includes = List.of("A", "B");
		List<String> excludes = Collections.emptyList();
		IncludeExcludeContentSelector<String> selector = new IncludeExcludeContentSelector<>(LAYER, includes, excludes,
				TestContentsFilter::new);
		assertThat(selector.contains("B")).isTrue();
	}

	@Test
	void containsWhenIncludedAndNotExcludedReturnsTrue() {
		List<String> includes = List.of("A", "B");
		List<String> excludes = List.of("C", "D");
		IncludeExcludeContentSelector<String> selector = new IncludeExcludeContentSelector<>(LAYER, includes, excludes,
				TestContentsFilter::new);
		assertThat(selector.contains("B")).isTrue();
	}

	@Test
	void containsWhenIncludedAndExcludedReturnsFalse() {
		List<String> includes = List.of("A", "B");
		List<String> excludes = List.of("C", "D");
		IncludeExcludeContentSelector<String> selector = new IncludeExcludeContentSelector<>(LAYER, includes, excludes,
				TestContentsFilter::new);
		assertThat(selector.contains("C")).isFalse();
	}

	/**
	 * {@link ContentFilter} used for testing.
	 */
	static class TestContentsFilter implements ContentFilter<String> {

		private final String match;

		TestContentsFilter(String match) {
			this.match = match;
		}

		@Override
		public boolean matches(String item) {
			return this.match.equals(item);
		}

	}

}
