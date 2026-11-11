/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.gson.autoconfigure;

import java.util.List;
import java.util.stream.Stream;

import com.google.gson.Strictness;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GsonProperties}.
 *
 * @author Andy Wilkinson
 */
class GsonPropertiesTests {

	@Test
	void valuesOfOurStrictnessMatchValuesOfGsonsStrictness() {
		assertThat(namesOf(GsonProperties.Strictness.values())).isEqualTo(namesOf(Strictness.values()));
	}

	private List<String> namesOf(Enum<?>[] input) {
		return Stream.of(input).map(Enum::name).toList();
	}

}
