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

package org.springframework.boot.actuate.libraries;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link LibrariesEndpoint}.
 *
 * @author Phil Clay
 */
class LibrariesEndpointTests {

	@Test
	void nullContributorsThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new LibrariesEndpoint(null));
	}

	@Test
	void noContributorsIsEmpty() {
		LibrariesEndpoint endpoint = new LibrariesEndpoint(Collections.emptyList());
		Libraries libraries = endpoint.libraries(Collections.emptyList());
		assertThat(libraries.getDetails()).isEmpty();
	}

	@Test
	void unfiltered() {

		LibrariesEndpoint endpoint = new LibrariesEndpoint(Arrays.asList((builder) -> builder
				.addLibraries("categoryA",
						Arrays.asList(Collections.singletonMap("key1", "value1"),
								Collections.singletonMap("key2", "value2")))
				.addLibrary("categoryA", Collections.singletonMap("key3", "value3"))
				.addLibrary("categoryB", Collections.singletonMap("key4", "value4"))));

		Libraries libraries = endpoint.libraries(Collections.emptyList());

		assertThat(libraries.getDetails()).containsExactly(
				entry("categoryA", Arrays.asList(Collections.singletonMap("key1", "value1"),
						Collections.singletonMap("key2", "value2"), Collections.singletonMap("key3", "value3"))),
				entry("categoryB", Arrays.asList(Collections.singletonMap("key4", "value4"))));
	}

	@Test
	void filtered() {

		LibrariesEndpoint endpoint = new LibrariesEndpoint(Arrays.asList((builder) -> builder
				.addLibraries("categoryA",
						Arrays.asList(Collections.singletonMap("key1", "value1"),
								Collections.singletonMap("key2", "value2")))
				.addLibrary("categoryA", Collections.singletonMap("key3", "value3"))
				.addLibrary("categoryB", Collections.singletonMap("key4", "value4"))));

		Libraries libraries = endpoint.libraries(Arrays.asList("key2:value2"));

		assertThat(libraries.getDetails()).containsExactly(
				entry("categoryA", Arrays.asList(Collections.singletonMap("key2", "value2"))),
				entry("categoryB", Collections.emptyList()));

		assertThat(libraries.get("categoryA")).containsOnly(Collections.singletonMap("key2", "value2"));
	}

}
