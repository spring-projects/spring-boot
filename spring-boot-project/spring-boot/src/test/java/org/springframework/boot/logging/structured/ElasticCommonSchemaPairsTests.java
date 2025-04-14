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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ElasticCommonSchemaPairs}.
 *
 * @author Phillip Webb
 */
class ElasticCommonSchemaPairsTests {

	@Test
	void nestedExpandsNames() {
		Map<String, String> map = Map.of("a1.b1.c1", "A1B1C1", "a1.b2.c1", "A1B2C1", "a1.b1.c2", "A1B1C2");
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
		assertThat(ElasticCommonSchemaPairs.nested(map)).isEqualTo(expected);
	}

	@Test
	void nestedWhenDuplicateInParentThrowsException() {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("a1.b1.c1", "A1B1C1");
		map.put("a1.b1", "A1B1");
		assertThatIllegalStateException().isThrownBy(() -> ElasticCommonSchemaPairs.nested(map))
			.withMessage("Duplicate ECS pairs added under 'a1.b1'");
	}

	@Test
	void nestedWhenDuplicateInLeafThrowsException() {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("a1.b1", "A1B1");
		map.put("a1.b1.c1", "A1B1C1");
		assertThatIllegalStateException().isThrownBy(() -> ElasticCommonSchemaPairs.nested(map))
			.withMessage("Duplicate ECS pairs added under 'a1.b1'");
	}

}
