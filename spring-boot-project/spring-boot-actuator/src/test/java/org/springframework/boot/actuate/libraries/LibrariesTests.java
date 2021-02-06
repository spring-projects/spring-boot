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
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link Libraries}.
 *
 * @author Phil Clay
 */
class LibrariesTests {

	@Test
	void unfiltered() {
		Libraries libraries = Libraries.builder()
				.addLibraries("categoryA",
						Arrays.asList(Collections.singletonMap("key1", "value1"),
								Collections.singletonMap("key2", "value2")))
				.addLibrary("categoryA", Collections.singletonMap("key3", "value3"))
				.addLibrary("categoryB", Collections.singletonMap("key4", "value4")).build();

		assertThat(libraries.getDetails()).containsExactly(
				entry("categoryA", Arrays.asList(Collections.singletonMap("key1", "value1"),
						Collections.singletonMap("key2", "value2"), Collections.singletonMap("key3", "value3"))),
				entry("categoryB", Arrays.asList(Collections.singletonMap("key4", "value4"))));
		assertThat(libraries.get("categoryB")).containsOnly(Collections.singletonMap("key4", "value4"));

	}

	@Test
	void filtered() {
		Libraries libraries = Libraries.builder()
				.addLibraries("categoryA",
						Arrays.asList(Collections.singletonMap("key1", "value1"),
								Collections.singletonMap("key2", "value2")))
				.addLibrary("categoryA", Collections.singletonMap("key3", "value3"))
				.addLibrary("categoryB", Collections.singletonMap("key4", "value4"))
				.addIncludesFilter((library) -> library.get("key2") != null).build();

		assertThat(libraries.getDetails()).containsExactly(
				entry("categoryA", Arrays.asList(Collections.singletonMap("key2", "value2"))),
				entry("categoryB", Collections.emptyList()));

		assertThat(libraries.get("categoryA")).containsOnly(Collections.singletonMap("key2", "value2"));
	}

}
