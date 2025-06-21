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

package org.springframework.boot.configurationprocessor.metadata;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.configurationprocessor.metadata.ItemHint.ValueHint;
import org.springframework.boot.configurationprocessor.metadata.ItemHint.ValueProvider;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ItemHint}.
 *
 * @author Stephane Nicoll
 */
class ItemHintTests {

	@Test
	void prefixIsAppliedWithValueHint() {
		ValueHint firstValueHint = new ValueHint("one", "First.");
		ValueHint secondValueHint = new ValueHint("two", "Second.");
		ItemHint itemHint = new ItemHint("name", List.of(firstValueHint, secondValueHint), Collections.emptyList());
		ItemHint prefixedItemHint = itemHint.applyPrefix("example");
		assertThat(itemHint).isNotSameAs(prefixedItemHint);
		assertThat(prefixedItemHint.getName()).isEqualTo("example.name");
		assertThat(prefixedItemHint.getValues()).containsExactly(firstValueHint, secondValueHint);
		assertThat(prefixedItemHint.getProviders()).isEmpty();
	}

	@Test
	void prefixIsAppliedWithValueProvider() {
		ValueProvider firstValueProvider = new ValueProvider("class-reference", Map.of("target", String.class));
		ValueProvider secondValueProvider = new ValueProvider("any", Collections.emptyMap());
		ItemHint itemHint = new ItemHint("name", Collections.emptyList(),
				List.of(firstValueProvider, secondValueProvider));
		ItemHint prefixedItemHint = itemHint.applyPrefix("example");
		assertThat(itemHint).isNotSameAs(prefixedItemHint);
		assertThat(prefixedItemHint.getName()).isEqualTo("example.name");
		assertThat(prefixedItemHint.getValues()).isEmpty();
		assertThat(prefixedItemHint.getProviders()).containsExactly(firstValueProvider, secondValueProvider);
	}

	@Test
	void prefixIsAppliedWithConvention() {
		ItemHint itemHint = new ItemHint("name", Collections.emptyList(), Collections.emptyList());
		ItemHint prefixedItemHint = itemHint.applyPrefix("example.nestedType");
		assertThat(prefixedItemHint.getName()).isEqualTo("example.nested-type.name");
	}

}
