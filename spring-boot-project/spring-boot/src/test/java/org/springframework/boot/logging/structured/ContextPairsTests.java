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

package org.springframework.boot.logging.structured;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link ContextPairs}.
 *
 * @author Phillip Webb
 */
class ContextPairsTests {

	@Test
	void flatWhenIncludeFalseDoesNothing() {
		ContextPairs contextPairs = new ContextPairs(false, null);
		Map<String, String> map = Map.of("spring", "boot");
		Map<String, Object> actual = apply(contextPairs.flat(".", (pairs) -> pairs.addMapEntries((item) -> map)));
		assertThat(actual.isEmpty());
	}

	@Test
	void flatIncludesName() {
		ContextPairs contextPairs = new ContextPairs(true, null);
		Map<String, String> map = Map.of("spring", "boot");
		Map<String, Object> actual = apply(contextPairs.flat(".", (pairs) -> pairs.addMapEntries((item) -> map)));
		assertThat(actual).containsExactlyEntriesOf(map);
	}

	@Test
	void flatWhenPrefixAppliesPrefix() {
		ContextPairs contextPairs = new ContextPairs(true, "the");
		Map<String, String> map = Map.of("spring", "boot");
		Map<String, Object> actual = apply(contextPairs.flat("_", (pairs) -> pairs.addMapEntries((item) -> map)));
		assertThat(actual).containsOnly(entry("the_spring", "boot"));
	}

	@Test
	void flatWhenPrefixEndingWithDelimeterAppliesPrefix() {
		ContextPairs contextPairs = new ContextPairs(true, "the_");
		Map<String, String> map = Map.of("spring", "boot");
		Map<String, Object> actual = apply(contextPairs.flat("_", (pairs) -> pairs.addMapEntries((item) -> map)));
		assertThat(actual).containsOnly(entry("the_spring", "boot"));
	}

	@Test
	void flatWhenPrefixAndNameStartingWithDelimeterAppliesPrefix() {
		ContextPairs contextPairs = new ContextPairs(true, "the");
		Map<String, String> map = Map.of("_spring", "boot");
		Map<String, Object> actual = apply(contextPairs.flat("_", (pairs) -> pairs.addMapEntries((item) -> map)));
		assertThat(actual).containsOnly(entry("the_spring", "boot"));
	}

	@Test
	void flatWhenJoinerJoins() {
		ContextPairs contextPairs = new ContextPairs(true, "the");
		Map<String, String> map = Map.of("spring", "boot");
		Map<String, Object> actual = apply(
				contextPairs.flat((prefix, name) -> prefix + name, (pairs) -> pairs.addMapEntries((item) -> map)));
		assertThat(actual).containsOnly(entry("thespring", "boot"));
	}

	@Test
	void flatWhenJoinerReturnsNullFilters() {
		ContextPairs contextPairs = new ContextPairs(true, "the");
		Map<String, String> map = Map.of("spring", "boot");
		Map<String, Object> actual = apply(
				contextPairs.flat((prefix, name) -> null, (pairs) -> pairs.addMapEntries((item) -> map)));
		assertThat(actual).isEmpty();
	}

	@Test
	void nestedWhenIncludeFalseDoesNothing() {
		ContextPairs contextPairs = new ContextPairs(false, null);
		Map<String, String> map = Map.of("spring", "boot");
		Map<String, Object> actual = apply(contextPairs.nested((pairs) -> pairs.addMapEntries((item) -> map)));
		assertThat(actual.isEmpty());
	}

	@Test
	void nestedExpandsNames() {
		ContextPairs contextPairs = new ContextPairs(true, null);
		Map<String, String> map = new LinkedHashMap<>();
		map.put("a1.b1.c1", "A1B1C1");
		map.put("a1.b2.c1", "A1B2C1");
		map.put("a1.b1.c2", "A1B1C2");
		Map<String, Object> actual = apply(contextPairs.nested((pairs) -> pairs.addMapEntries((item) -> map)));
		Map<String, Object> expected = new LinkedHashMap<>();
		Map<String, Object> a1 = new LinkedHashMap<>();
		Map<String, Object> b1 = new LinkedHashMap<>();
		Map<String, Object> b2 = new LinkedHashMap<>();
		expected.put("a1", a1);
		a1.put("b1", b1);
		a1.put("b2", b2);
		b1.put("c1", "A1B1C1");
		b1.put("c2", "A1B1C2");
		b2.put("c1", "A1B2C1");
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void nestedWhenDuplicateInParentThrowsException() {
		ContextPairs contextPairs = new ContextPairs(true, null);
		Map<String, String> map = new LinkedHashMap<>();
		map.put("a1.b1.c1", "A1B1C1");
		map.put("a1.b1", "A1B1");
		assertThatIllegalStateException()
			.isThrownBy(() -> apply(contextPairs.nested((pairs) -> pairs.addMapEntries((item) -> map))))
			.withMessage("Duplicate nested pairs added under 'a1.b1'");
	}

	@Test
	void nestedWhenDuplicateInLeafThrowsException() {
		ContextPairs contextPairs = new ContextPairs(true, null);
		Map<String, String> map = new LinkedHashMap<>();
		map.put("a1.b1", "A1B1");
		map.put("a1.b1.c1", "A1B1C1");
		assertThatIllegalStateException()
			.isThrownBy(() -> apply(contextPairs.nested((pairs) -> pairs.addMapEntries((item) -> map))))
			.withMessage("Duplicate nested pairs added under 'a1.b1'");
	}

	@Test
	void nestedWhenPrefixAppliesPrefix() {
		ContextPairs contextPairs = new ContextPairs(true, "a1");
		Map<String, String> map = new LinkedHashMap<>();
		map.put("b1.c1", "A1B1C1");
		map.put("b2.c1", "A1B2C1");
		map.put("b1.c2", "A1B1C2");
		Map<String, Object> actual = apply(contextPairs.nested((pairs) -> pairs.addMapEntries((item) -> map)));
		Map<String, Object> expected = new LinkedHashMap<>();
		Map<String, Object> a1 = new LinkedHashMap<>();
		Map<String, Object> b1 = new LinkedHashMap<>();
		Map<String, Object> b2 = new LinkedHashMap<>();
		expected.put("a1", a1);
		a1.put("b1", b1);
		a1.put("b2", b2);
		b1.put("c1", "A1B1C1");
		b1.put("c2", "A1B1C2");
		b2.put("c1", "A1B2C1");
		assertThat(actual).isEqualTo(expected);
	}

	Map<String, Object> apply(BiConsumer<?, BiConsumer<String, Object>> action) {
		Map<String, Object> result = new LinkedHashMap<>();
		action.accept(null, result::put);
		return result;
	}

}
