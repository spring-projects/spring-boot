/*
 * Copyright 2012-present the original author or authors.
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
		assertThat(coordinates).hasToString("g:a:v");
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
