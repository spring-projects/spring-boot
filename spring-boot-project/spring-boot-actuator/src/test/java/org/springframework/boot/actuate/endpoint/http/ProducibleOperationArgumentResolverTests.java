/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.http;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ProducibleOperationArgumentResolver}.
 *
 * @author Andy Wilkinson
 */
class ProducibleOperationArgumentResolverTests {

	@Test
	void whenAcceptHeaderIsEmptyThenHighestOrdinalIsReturned() {
		assertThat(resolve(acceptHeader())).isEqualTo(ApiVersion.V3);
	}

	@Test
	void whenEverythingIsAcceptableThenHighestOrdinalIsReturned() {
		assertThat(resolve(acceptHeader("*/*"))).isEqualTo(ApiVersion.V3);
	}

	@Test
	void whenNothingIsAcceptableThenNullIsReturned() {
		assertThat(resolve(acceptHeader("image/png"))).isEqualTo(null);
	}

	@Test
	void whenSingleValueIsAcceptableThenMatchingEnumValueIsReturned() {
		assertThat(new ProducibleOperationArgumentResolver(acceptHeader(ActuatorMediaType.V2_JSON))
				.resolve(ApiVersion.class)).isEqualTo(ApiVersion.V2);
		assertThat(new ProducibleOperationArgumentResolver(acceptHeader(ActuatorMediaType.V3_JSON))
				.resolve(ApiVersion.class)).isEqualTo(ApiVersion.V3);
	}

	@Test
	void whenMultipleValuesAreAcceptableThenHighestOrdinalIsReturned() {
		assertThat(resolve(acceptHeader(ActuatorMediaType.V2_JSON, ActuatorMediaType.V3_JSON)))
				.isEqualTo(ApiVersion.V3);
	}

	@Test
	void whenMultipleValuesAreAcceptableAsSingleHeaderThenHighestOrdinalIsReturned() {
		assertThat(resolve(acceptHeader(ActuatorMediaType.V2_JSON + "," + ActuatorMediaType.V3_JSON)))
				.isEqualTo(ApiVersion.V3);
	}

	private Map<String, List<String>> acceptHeader(String... types) {
		List<String> value = Arrays.asList(types);
		return value.isEmpty() ? Collections.emptyMap() : Collections.singletonMap("Accept", value);
	}

	private ApiVersion resolve(Map<String, List<String>> httpHeaders) {
		return new ProducibleOperationArgumentResolver(httpHeaders).resolve(ApiVersion.class);
	}

}
