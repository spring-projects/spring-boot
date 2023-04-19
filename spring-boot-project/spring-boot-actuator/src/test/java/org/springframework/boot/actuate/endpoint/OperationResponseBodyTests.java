/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link OperationResponseBody}.
 *
 * @author Phillip Webb
 */
class OperationResponseBodyTests {

	@Test
	void ofMapReturnsOperationResponseBody() {
		LinkedHashMap<String, String> map = new LinkedHashMap<>();
		map.put("one", "1");
		map.put("two", "2");
		Map<String, String> mapDescriptor = OperationResponseBody.of(map);
		assertThat(mapDescriptor).containsExactly(entry("one", "1"), entry("two", "2"));
		assertThat(mapDescriptor).isInstanceOf(OperationResponseBody.class);
	}

}
