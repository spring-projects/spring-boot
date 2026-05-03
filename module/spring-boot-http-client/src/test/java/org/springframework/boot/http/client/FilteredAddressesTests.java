/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.http.client;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link FilteredAddresses}.
 *
 * @author Phillip Webb
 */
class FilteredAddressesTests {

	@Test
	void toListOrElseThrowWhenNotEmptyReturnsResult() {
		FilteredAddresses<String> result = FilteredAddresses.of(Stream.of("127.0.0.1"), (address) -> true);
		assertThat(result.toList().orElseThrow("localhost", mock())).containsExactly("127.0.0.1");
	}

	@Test
	void toListOrElseThrowWhenEmptyThrowsException() {
		FilteredAddresses<String> result = FilteredAddresses.of(Stream.of("127.0.0.1"), (address) -> false);
		assertThatExceptionOfType(FilteredHostException.class)
			.isThrownBy(() -> result.toList().orElseThrow("localhost", mock()))
			.withMessage("Filtered host 'localhost'");
	}

	@Test
	void toArrayOrElseThrowWhenNotEmptyReturnsResult() {
		FilteredAddresses<String> result = FilteredAddresses.of(Stream.of("127.0.0.1"), (address) -> true);
		assertThat(result.toArray(String[]::new).orElseThrow("localhost", mock())).containsExactly("127.0.0.1");
	}

	@Test
	void toArrayOrElseThrowWhenEmptyThrowsException() {
		FilteredAddresses<String> result = FilteredAddresses.of(Stream.of("127.0.0.1"), (address) -> false);
		assertThatExceptionOfType(FilteredHostException.class)
			.isThrownBy(() -> result.toArray(String[]::new).orElseThrow("localhost", mock()))
			.withMessage("Filtered host 'localhost'");
	}

	@Test
	void getOrElseThrowWhenNotEmptyReturnsResult() {
		FilteredAddresses<String> result = FilteredAddresses.of(Stream.of("127.0.0.1"), (address) -> true);
		assertThat(result.get().orElseThrow("localhost", mock())).isEqualTo("127.0.0.1");
	}

	@Test
	void getOrElseThrowWhenEmptyThrowsException() {
		FilteredAddresses<String> result = FilteredAddresses.of(Stream.of("127.0.0.1"), (address) -> false);
		assertThatExceptionOfType(FilteredHostException.class)
			.isThrownBy(() -> result.get().orElseThrow("localhost", mock()))
			.withMessage("Filtered host 'localhost'");
	}

}
