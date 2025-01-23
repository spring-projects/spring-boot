/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.autoconfigure.gson;

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
