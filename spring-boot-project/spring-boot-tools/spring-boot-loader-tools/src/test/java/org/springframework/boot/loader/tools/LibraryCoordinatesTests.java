/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.loader.tools;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LibraryCoordinates}.
 *
 * @author Phillip Webb
 */
class LibraryCoordinatesTests {

	@Test
	void ofCreateLibraryCoordinates() {
		LibraryCoordinates coordinates = LibraryCoordinates.of("g", "a", "v");
		assertThat(coordinates.getGroupId()).isEqualTo("g");
		assertThat(coordinates.getArtifactId()).isEqualTo("a");
		assertThat(coordinates.getVersion()).isEqualTo("v");
		assertThat(coordinates.toString()).isEqualTo("g:a:v");
	}

	@Test
	void toStandardNotationStringWhenCoordinatesAreNull() {
		assertThat(LibraryCoordinates.toStandardNotationString(null)).isEqualTo("::");
	}

	@Test
	void toStandardNotationStringWhenCoordinatesElementsNull() {
		assertThat(LibraryCoordinates.toStandardNotationString(mock(LibraryCoordinates.class))).isEqualTo("::");
	}

	@Test
	void toStandardNotationString() {
		LibraryCoordinates coordinates = mock(LibraryCoordinates.class);
		given(coordinates.getGroupId()).willReturn("a");
		given(coordinates.getArtifactId()).willReturn("b");
		given(coordinates.getVersion()).willReturn("c");
		assertThat(LibraryCoordinates.toStandardNotationString(coordinates)).isEqualTo("a:b:c");
	}

}
