/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.loader.tools.layer.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link FilteredResourceStrategy}.
 *
 * @author Madhura Bhave
 */
class FilteredResourceStrategyTests {

	@Test
	void createWhenFiltersNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new FilteredResourceStrategy("custom", null));
	}

	@Test
	void createWhenFiltersEmptyShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new FilteredResourceStrategy("custom", Collections.emptyList()));
	}

	@Test
	void getLayerShouldReturnLayerName() {
		FilteredResourceStrategy strategy = new FilteredResourceStrategy("custom",
				Collections.singletonList(new TestFilter1()));
		assertThat(strategy.getLayer().toString()).isEqualTo("custom");
	}

	@Test
	void getMatchingLayerWhenFilterMatchesIncludes() {
		FilteredResourceStrategy strategy = new FilteredResourceStrategy("custom",
				Collections.singletonList(new TestFilter1()));
		assertThat(strategy.getMatchingLayer("ABCD").toString()).isEqualTo("custom");
	}

	@Test
	void matchesWhenFilterMatchesIncludesAndExcludesFromSameFilter() {
		FilteredResourceStrategy strategy = new FilteredResourceStrategy("custom",
				Collections.singletonList(new TestFilter1()));
		assertThat(strategy.getMatchingLayer("AZ")).isNull();
	}

	@Test
	void matchesWhenFilterMatchesIncludesAndExcludesFromAnotherFilter() {
		List<ResourceFilter> filters = new ArrayList<>();
		filters.add(new TestFilter1());
		filters.add(new TestFilter2());
		FilteredResourceStrategy strategy = new FilteredResourceStrategy("custom", filters);
		assertThat(strategy.getMatchingLayer("AY")).isNull();
	}

	private static class TestFilter1 implements ResourceFilter {

		@Override
		public boolean isResourceIncluded(String resourceName) {
			return resourceName.startsWith("A");
		}

		@Override
		public boolean isResourceExcluded(String resourceName) {
			return resourceName.endsWith("Z");
		}

	}

	private static class TestFilter2 implements ResourceFilter {

		@Override
		public boolean isResourceIncluded(String resourceName) {
			return resourceName.startsWith("B");
		}

		@Override
		public boolean isResourceExcluded(String resourceName) {
			return resourceName.endsWith("Y");
		}

	}

}
