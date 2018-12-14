/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.properties.source;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ConfigurationPropertyState}.
 *
 * @author Phillip Webb
 */
public class ConfigurationPropertyStateTests {

	@Test
	public void searchWhenIterableIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> ConfigurationPropertyState.search(null, (e) -> true))
				.withMessageContaining("Source must not be null");
	}

	@Test
	public void searchWhenPredicateIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> ConfigurationPropertyState.search(Collections.emptyList(), null))
				.withMessageContaining("Predicate must not be null");
	}

	@Test
	public void searchWhenContainsItemShouldReturnPresent() {
		List<String> source = Arrays.asList("a", "b", "c");
		ConfigurationPropertyState result = ConfigurationPropertyState.search(source,
				"b"::equals);
		assertThat(result).isEqualTo(ConfigurationPropertyState.PRESENT);
	}

	@Test
	public void searchWhenContainsNoItemShouldReturnAbsent() {
		List<String> source = Arrays.asList("a", "x", "c");
		ConfigurationPropertyState result = ConfigurationPropertyState.search(source,
				"b"::equals);
		assertThat(result).isEqualTo(ConfigurationPropertyState.ABSENT);
	}

}
