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

package org.springframework.boot.loader.tools.layer.library;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.loader.tools.Library;
import org.springframework.boot.loader.tools.LibraryCoordinates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CoordinateFilter}.
 *
 * @author Madhura Bhave
 * @author Scott Frederick
 */
class CoordinateFilterTests {

	@Test
	void isLibraryIncludedWhenGroupIdIsNullAndToMatchHasWildcard() {
		List<String> includes = Collections.singletonList("*:*");
		CoordinateFilter filter = new CoordinateFilter(includes, Collections.emptyList());
		Library library = mock(Library.class);
		given(library.getCoordinates()).willReturn(new LibraryCoordinates(null, null, null));
		assertThat(filter.isLibraryIncluded(library)).isTrue();
	}

	@Test
	void isLibraryIncludedWhenArtifactIdIsNullAndToMatchHasWildcard() {
		List<String> includes = Collections.singletonList("org.acme:*");
		CoordinateFilter filter = new CoordinateFilter(includes, Collections.emptyList());
		Library library = mock(Library.class);
		given(library.getCoordinates()).willReturn(new LibraryCoordinates("org.acme", null, null));
		assertThat(filter.isLibraryIncluded(library)).isTrue();
	}

	@Test
	void isLibraryIncludedWhenVersionIsNullAndToMatchHasWildcard() {
		List<String> includes = Collections.singletonList("org.acme:something:*");
		CoordinateFilter filter = new CoordinateFilter(includes, Collections.emptyList());
		Library library = mock(Library.class);
		given(library.getCoordinates()).willReturn(new LibraryCoordinates("org.acme", "something", null));
		assertThat(filter.isLibraryIncluded(library)).isTrue();
	}

	@Test
	void isLibraryIncludedWhenGroupIdDoesNotMatch() {
		List<String> includes = Collections.singletonList("org.acme:*");
		CoordinateFilter filter = new CoordinateFilter(includes, Collections.emptyList());
		Library library = mock(Library.class);
		given(library.getCoordinates()).willReturn(new LibraryCoordinates("other.foo", null, null));
		assertThat(filter.isLibraryIncluded(library)).isFalse();
	}

	@Test
	void isLibraryIncludedWhenArtifactIdDoesNotMatch() {
		List<String> includes = Collections.singletonList("org.acme:test:*");
		CoordinateFilter filter = new CoordinateFilter(includes, Collections.emptyList());
		Library library = mock(Library.class);
		given(library.getCoordinates()).willReturn(new LibraryCoordinates("org.acme", "other", null));
		assertThat(filter.isLibraryIncluded(library)).isFalse();
	}

	@Test
	void isLibraryIncludedWhenArtifactIdMatches() {
		List<String> includes = Collections.singletonList("org.acme:test:*");
		CoordinateFilter filter = new CoordinateFilter(includes, Collections.emptyList());
		Library library = mock(Library.class);
		given(library.getCoordinates()).willReturn(new LibraryCoordinates("org.acme", "test", null));
		assertThat(filter.isLibraryIncluded(library)).isTrue();
	}

	@Test
	void isLibraryIncludedWhenVersionDoesNotMatch() {
		List<String> includes = Collections.singletonList("org.acme:test:*SNAPSHOT");
		CoordinateFilter filter = new CoordinateFilter(includes, Collections.emptyList());
		Library library = mock(Library.class);
		given(library.getCoordinates()).willReturn(new LibraryCoordinates("org.acme", "test", "1.0.0"));
		assertThat(filter.isLibraryIncluded(library)).isFalse();
	}

	@Test
	void isLibraryIncludedWhenVersionMatches() {
		List<String> includes = Collections.singletonList("org.acme:test:*SNAPSHOT");
		CoordinateFilter filter = new CoordinateFilter(includes, Collections.emptyList());
		Library library = mock(Library.class);
		given(library.getCoordinates()).willReturn(new LibraryCoordinates("org.acme", "test", "1.0.0-SNAPSHOT"));
		assertThat(filter.isLibraryIncluded(library)).isTrue();
	}

}
