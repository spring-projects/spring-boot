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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link LibraryCoordinates}.
 *
 * @author Scott Frederick
 */
class LibraryCoordinatesTests {

	@Test
	void parseCoordinatesWithAllElements() {
		LibraryCoordinates coordinates = new LibraryCoordinates("com.acme:my-library:1.0.0");
		assertThat(coordinates.getGroupId()).isEqualTo("com.acme");
		assertThat(coordinates.getArtifactId()).isEqualTo("my-library");
		assertThat(coordinates.getVersion()).isEqualTo("1.0.0");
	}

	@Test
	void parseCoordinatesWithoutVersion() {
		LibraryCoordinates coordinates = new LibraryCoordinates("com.acme:my-library");
		assertThat(coordinates.getGroupId()).isEqualTo("com.acme");
		assertThat(coordinates.getArtifactId()).isEqualTo("my-library");
		assertThat(coordinates.getVersion()).isNull();
	}

	@Test
	void parseCoordinatesWithEmptyElements() {
		LibraryCoordinates coordinates = new LibraryCoordinates(":my-library:");
		assertThat(coordinates.getGroupId()).isEqualTo("");
		assertThat(coordinates.getArtifactId()).isEqualTo("my-library");
		assertThat(coordinates.getVersion()).isNull();
	}

	@Test
	void parseCoordinatesWithExtraElements() {
		LibraryCoordinates coordinates = new LibraryCoordinates("com.acme:my-library:1.0.0.BUILD-SNAPSHOT:11111");
		assertThat(coordinates.getGroupId()).isEqualTo("com.acme");
		assertThat(coordinates.getArtifactId()).isEqualTo("my-library");
		assertThat(coordinates.getVersion()).isEqualTo("1.0.0.BUILD-SNAPSHOT");
	}

	@Test
	void parseCoordinatesWithoutMinimumElements() {
		assertThatIllegalArgumentException().isThrownBy(() -> new LibraryCoordinates("com.acme"));
	}

	@Test
	void toStringReturnsString() {
		assertThat(new LibraryCoordinates("com.acme:my-library:1.0.0")).hasToString("com.acme:my-library:1.0.0");
		assertThat(new LibraryCoordinates("com.acme:my-library")).hasToString("com.acme:my-library:");
	}

}
