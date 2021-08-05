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

package org.springframework.boot.actuate.endpoint;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ProducibleOperationArgumentResolver}.
 *
 * @author Andy Wilkinson
 */
class ProducibleOperationArgumentResolverTests {

	private static final String V2_JSON = ApiVersion.V2.getProducedMimeType().toString();

	private static final String V3_JSON = ApiVersion.V3.getProducedMimeType().toString();

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
		assertThat(new ProducibleOperationArgumentResolver(acceptHeader(V2_JSON)).resolve(ApiVersion.class))
				.isEqualTo(ApiVersion.V2);
		assertThat(new ProducibleOperationArgumentResolver(acceptHeader(V3_JSON)).resolve(ApiVersion.class))
				.isEqualTo(ApiVersion.V3);
	}

	@Test
	void whenMultipleValuesAreAcceptableThenHighestOrdinalIsReturned() {
		assertThat(resolve(acceptHeader(V2_JSON, V3_JSON))).isEqualTo(ApiVersion.V3);
	}

	@Test
	void whenMultipleValuesAreAcceptableAsSingleHeaderThenHighestOrdinalIsReturned() {
		assertThat(resolve(acceptHeader(V2_JSON + "," + V3_JSON))).isEqualTo(ApiVersion.V3);
	}

	private Supplier<List<String>> acceptHeader(String... types) {
		List<String> value = Arrays.asList(types);
		return () -> (value.isEmpty() ? null : value);
	}

	private ApiVersion resolve(Supplier<List<String>> accepts) {
		return new ProducibleOperationArgumentResolver(accepts).resolve(ApiVersion.class);
	}

}
