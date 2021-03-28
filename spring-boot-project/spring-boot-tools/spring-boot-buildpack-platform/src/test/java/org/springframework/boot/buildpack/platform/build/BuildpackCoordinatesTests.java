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

package org.springframework.boot.buildpack.platform.build;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link BuildpackCoordinates}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
class BuildpackCoordinatesTests extends AbstractJsonTests {

	private final Path archive = Paths.get("/buildpack/path");

	@Test
	void fromToml() throws IOException {
		BuildpackCoordinates coordinates = BuildpackCoordinates
				.fromToml(createTomlStream("example/buildpack1", "0.0.1", true, false), this.archive);
		assertThat(coordinates.getId()).isEqualTo("example/buildpack1");
		assertThat(coordinates.getVersion()).isEqualTo("0.0.1");
	}

	@Test
	void fromTomlWhenMissingDescriptorThrowsException() throws Exception {
		ByteArrayInputStream coordinates = new ByteArrayInputStream("".getBytes());
		assertThatIllegalArgumentException().isThrownBy(() -> BuildpackCoordinates.fromToml(coordinates, this.archive))
				.withMessageContaining("Buildpack descriptor 'buildpack.toml' is required")
				.withMessageContaining(this.archive.toString());
	}

	@Test
	void fromTomlWhenMissingIDThrowsException() throws Exception {
		InputStream coordinates = createTomlStream(null, null, true, false);
		assertThatIllegalArgumentException().isThrownBy(() -> BuildpackCoordinates.fromToml(coordinates, this.archive))
				.withMessageContaining("Buildpack descriptor must contain ID")
				.withMessageContaining(this.archive.toString());
	}

	@Test
	void fromTomlWhenMissingVersionThrowsException() throws Exception {
		InputStream coordinates = createTomlStream("example/buildpack1", null, true, false);
		assertThatIllegalArgumentException().isThrownBy(() -> BuildpackCoordinates.fromToml(coordinates, this.archive))
				.withMessageContaining("Buildpack descriptor must contain version")
				.withMessageContaining(this.archive.toString());
	}

	@Test
	void fromTomlWhenMissingStacksAndOrderThrowsException() throws Exception {
		InputStream coordinates = createTomlStream("example/buildpack1", "0.0.1", false, false);
		assertThatIllegalArgumentException().isThrownBy(() -> BuildpackCoordinates.fromToml(coordinates, this.archive))
				.withMessageContaining("Buildpack descriptor must contain either 'stacks' or 'order'")
				.withMessageContaining(this.archive.toString());
	}

	@Test
	void fromTomlWhenContainsBothStacksAndOrderThrowsException() throws Exception {
		InputStream coordinates = createTomlStream("example/buildpack1", "0.0.1", true, true);
		assertThatIllegalArgumentException().isThrownBy(() -> BuildpackCoordinates.fromToml(coordinates, this.archive))
				.withMessageContaining("Buildpack descriptor must not contain both 'stacks' and 'order'")
				.withMessageContaining(this.archive.toString());
	}

	@Test
	void fromBuildpackMetadataWhenMetadataIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> BuildpackCoordinates.fromBuildpackMetadata(null))
				.withMessage("BuildpackMetadata must not be null");
	}

	@Test
	void fromBuildpackMetadataReturnsCoordinates() throws Exception {
		BuildpackMetadata metadata = BuildpackMetadata.fromJson(getContentAsString("buildpack-metadata.json"));
		BuildpackCoordinates coordinates = BuildpackCoordinates.fromBuildpackMetadata(metadata);
		assertThat(coordinates.getId()).isEqualTo("example/hello-universe");
		assertThat(coordinates.getVersion()).isEqualTo("0.0.1");
	}

	@Test
	void ofWhenIdIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> BuildpackCoordinates.of(null, null))
				.withMessage("ID must not be empty");
	}

	@Test
	void ofReturnsCoordinates() {
		BuildpackCoordinates coordinates = BuildpackCoordinates.of("id", "1");
		assertThat(coordinates).hasToString("id@1");
	}

	@Test
	void getIdReturnsId() {
		BuildpackCoordinates coordinates = BuildpackCoordinates.of("id", "1");
		assertThat(coordinates.getId()).isEqualTo("id");
	}

	@Test
	void getVersionReturnsVersion() {
		BuildpackCoordinates coordinates = BuildpackCoordinates.of("id", "1");
		assertThat(coordinates.getVersion()).isEqualTo("1");
	}

	@Test
	void getVersionWhenVersionIsNullReturnsNull() {
		BuildpackCoordinates coordinates = BuildpackCoordinates.of("id", null);
		assertThat(coordinates.getVersion()).isNull();
	}

	@Test
	void toStringReturnsNiceString() {
		BuildpackCoordinates coordinates = BuildpackCoordinates.of("id", "1");
		assertThat(coordinates).hasToString("id@1");
	}

	@Test
	void equalsAndHashCode() {
		BuildpackCoordinates c1a = BuildpackCoordinates.of("id", "1");
		BuildpackCoordinates c1b = BuildpackCoordinates.of("id", "1");
		BuildpackCoordinates c2 = BuildpackCoordinates.of("id", "2");
		assertThat(c1a).isEqualTo(c1a).isEqualTo(c1b).isNotEqualTo(c2);
		assertThat(c1a.hashCode()).isEqualTo(c1b.hashCode());
	}

	private InputStream createTomlStream(String id, String version, boolean includeStacks, boolean includeOrder) {
		StringBuilder builder = new StringBuilder();
		builder.append("[buildpack]\n");
		if (id != null) {
			builder.append("id = \"").append(id).append("\"\n");
		}
		if (version != null) {
			builder.append("version = \"").append(version).append("\"\n");
		}
		builder.append("name = \"Example buildpack\"\n");
		builder.append("homepage = \"https://github.com/example/example-buildpack\"\n");
		if (includeStacks) {
			builder.append("[[stacks]]\n");
			builder.append("id = \"io.buildpacks.stacks.bionic\"\n");
		}
		if (includeOrder) {
			builder.append("[[order]]\n");
			builder.append("group = [ { id = \"example/buildpack2\", version=\"0.0.2\" } ]\n");
		}
		return new ByteArrayInputStream(builder.toString().getBytes(StandardCharsets.UTF_8));
	}

}
