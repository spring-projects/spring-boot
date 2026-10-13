/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.source;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ConfigurationPropertyState}.
 *
 * @author Phillip Webb
 */
class ConfigurationPropertyStateTests {

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void searchWhenIterableIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> ConfigurationPropertyState.search(null, (e) -> true))
			.withMessageContaining("'source' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void searchWhenPredicateIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> ConfigurationPropertyState.search(Collections.emptyList(), null))
			.withMessageContaining("'predicate' must not be null");
	}

	@Test
	void searchWhenContainsItemShouldReturnPresent() {
		List<String> source = Arrays.asList("a", "b", "c");
		ConfigurationPropertyState result = ConfigurationPropertyState.search(source, "b"::equals);
		assertThat(result).isEqualTo(ConfigurationPropertyState.PRESENT);
	}

	@Test
	void searchWhenContainsNoItemShouldReturnAbsent() {
		List<String> source = Arrays.asList("a", "x", "c");
		ConfigurationPropertyState result = ConfigurationPropertyState.search(source, "b"::equals);
		assertThat(result).isEqualTo(ConfigurationPropertyState.ABSENT);
	}

}
