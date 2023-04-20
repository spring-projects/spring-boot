/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.web;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.ApiVersion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link EndpointMediaTypes}.
 *
 * @author Phillip Webb
 */
class EndpointMediaTypesTests {

	private static final String V2_JSON = ApiVersion.V2.getProducedMimeType().toString();

	private static final String V3_JSON = ApiVersion.V3.getProducedMimeType().toString();

	@Test
	void defaultReturnsExpectedProducedAndConsumedTypes() {
		assertThat(EndpointMediaTypes.DEFAULT.getProduced()).containsExactly(V3_JSON, V2_JSON, "application/json");
		assertThat(EndpointMediaTypes.DEFAULT.getConsumed()).containsExactly(V3_JSON, V2_JSON, "application/json");
	}

	@Test
	void createWhenProducedIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new EndpointMediaTypes(null, Collections.emptyList()))
			.withMessageContaining("Produced must not be null");
	}

	@Test
	void createWhenConsumedIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new EndpointMediaTypes(Collections.emptyList(), null))
			.withMessageContaining("Consumed must not be null");
	}

	@Test
	void createFromProducedAndConsumedUsesSameListForBoth() {
		EndpointMediaTypes types = new EndpointMediaTypes("spring/framework", "spring/boot");
		assertThat(types.getProduced()).containsExactly("spring/framework", "spring/boot");
		assertThat(types.getConsumed()).containsExactly("spring/framework", "spring/boot");
	}

	@Test
	void getProducedShouldReturnProduced() {
		List<String> produced = Arrays.asList("a", "b", "c");
		EndpointMediaTypes types = new EndpointMediaTypes(produced, Collections.emptyList());
		assertThat(types.getProduced()).isEqualTo(produced);
	}

	@Test
	void getConsumedShouldReturnConsumed() {
		List<String> consumed = Arrays.asList("a", "b", "c");
		EndpointMediaTypes types = new EndpointMediaTypes(Collections.emptyList(), consumed);
		assertThat(types.getConsumed()).isEqualTo(consumed);
	}

}
