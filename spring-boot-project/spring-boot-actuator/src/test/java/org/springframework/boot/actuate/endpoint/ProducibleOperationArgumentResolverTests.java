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

import org.springframework.util.MimeType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Test for {@link ProducibleOperationArgumentResolver}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ProducibleOperationArgumentResolverTests {

	private static final String V2_JSON = ApiVersion.V2.getProducedMimeType().toString();

	private static final String V3_JSON = ApiVersion.V3.getProducedMimeType().toString();

	@Test
	void whenAcceptHeaderIsEmptyThenHighestOrdinalIsReturned() {
		assertThat(resolve(acceptHeader())).isEqualTo(ApiVersion.V3);
	}

	@Test
	void whenAcceptHeaderIsEmptyAndWithDefaultThenDefaultIsReturned() {
		assertThat(resolve(acceptHeader(), WithDefault.class)).isEqualTo(WithDefault.TWO);
	}

	@Test
	void whenEverythingIsAcceptableThenHighestOrdinalIsReturned() {
		assertThat(resolve(acceptHeader("*/*"))).isEqualTo(ApiVersion.V3);
	}

	@Test
	void whenEverythingIsAcceptableWithDefaultThenDefaultIsReturned() {
		assertThat(resolve(acceptHeader("*/*"), WithDefault.class)).isEqualTo(WithDefault.TWO);
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

	@Test
	void withMultipleValuesOneOfWhichIsAllReturnsDefault() {
		assertThat(resolve(acceptHeader("one/one", "*/*"), WithDefault.class)).isEqualTo(WithDefault.TWO);
	}

	@Test
	void whenMultipleDefaultsThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> resolve(acceptHeader("one/one"), WithMultipleDefaults.class))
				.withMessageContaining("Multiple default values");
	}

	private Supplier<List<String>> acceptHeader(String... types) {
		List<String> value = Arrays.asList(types);
		return () -> (value.isEmpty() ? null : value);
	}

	private ApiVersion resolve(Supplier<List<String>> accepts) {
		return resolve(accepts, ApiVersion.class);
	}

	private <T> T resolve(Supplier<List<String>> accepts, Class<T> type) {
		return new ProducibleOperationArgumentResolver(accepts).resolve(type);
	}

	enum WithDefault implements Producible<WithDefault> {

		ONE("one/one"),

		TWO("two/two") {

			@Override
			public boolean isDefault() {
				return true;
			}

		},

		THREE("three/three");

		private final MimeType mimeType;

		WithDefault(String mimeType) {
			this.mimeType = MimeType.valueOf(mimeType);
		}

		@Override
		public MimeType getProducedMimeType() {
			return this.mimeType;
		}

	}

	enum WithMultipleDefaults implements Producible<WithMultipleDefaults> {

		ONE, TWO, THREE;

		@Override
		public boolean isDefault() {
			return true;
		}

		@Override
		public MimeType getProducedMimeType() {
			return MimeType.valueOf("image/jpeg");
		}

	}

}
