/*
 * Copyright 2012-present the original author or authors.
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
