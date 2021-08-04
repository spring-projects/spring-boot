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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;

import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.io.IOBiConsumer;
import org.springframework.boot.buildpack.platform.io.TarArchive;
import org.springframework.boot.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ImageBuildpack}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
class ImageBuildpackTests extends AbstractJsonTests {

	private String longFilePath;

	@BeforeEach
	void setUp() {
		StringBuilder path = new StringBuilder();
		new Random().ints('a', 'z' + 1).limit(100).forEach((i) -> path.append((char) i));
		this.longFilePath = path.toString();
	}

	@Test
	void resolveWhenFullyQualifiedReferenceReturnsBuilder() throws Exception {
		Image image = Image.of(getContent("buildpack-image.json"));
		BuildpackResolverContext resolverContext = mock(BuildpackResolverContext.class);
		given(resolverContext.fetchImage(any(), any())).willReturn(image);
		willAnswer(this::withMockLayers).given(resolverContext).exportImageLayers(any(), any());
		BuildpackReference reference = BuildpackReference.of("docker://example/buildpack1:latest");
		Buildpack buildpack = ImageBuildpack.resolve(resolverContext, reference);
		assertThat(buildpack.getCoordinates()).hasToString("example/hello-universe@0.0.1");
		assertHasExpectedLayers(buildpack);
	}

	@Test
	void resolveWhenUnqualifiedReferenceReturnsBuilder() throws Exception {
		Image image = Image.of(getContent("buildpack-image.json"));
		BuildpackResolverContext resolverContext = mock(BuildpackResolverContext.class);
		given(resolverContext.fetchImage(any(), any())).willReturn(image);
		willAnswer(this::withMockLayers).given(resolverContext).exportImageLayers(any(), any());
		BuildpackReference reference = BuildpackReference.of("example/buildpack1:latest");
		Buildpack buildpack = ImageBuildpack.resolve(resolverContext, reference);
		assertThat(buildpack.getCoordinates()).hasToString("example/hello-universe@0.0.1");
		assertHasExpectedLayers(buildpack);
	}

	@Test
	void resolveWhenWhenImageNotPulledThrowsException() throws Exception {
		BuildpackResolverContext resolverContext = mock(BuildpackResolverContext.class);
		given(resolverContext.fetchImage(any(), any())).willThrow(IOException.class);
		BuildpackReference reference = BuildpackReference.of("docker://example/buildpack1:latest");
		assertThatIllegalArgumentException().isThrownBy(() -> ImageBuildpack.resolve(resolverContext, reference))
				.withMessageContaining("Error pulling buildpack image")
				.withMessageContaining("example/buildpack1:latest");
	}

	@Test
	void resolveWhenMissingMetadataLabelThrowsException() throws Exception {
		Image image = Image.of(getContent("image.json"));
		BuildpackResolverContext resolverContext = mock(BuildpackResolverContext.class);
		given(resolverContext.fetchImage(any(), any())).willReturn(image);
		BuildpackReference reference = BuildpackReference.of("docker://example/buildpack1:latest");
		assertThatIllegalArgumentException().isThrownBy(() -> ImageBuildpack.resolve(resolverContext, reference))
				.withMessageContaining("No 'io.buildpacks.buildpackage.metadata' label found");
	}

	@Test
	void resolveWhenFullyQualifiedReferenceWithInvalidImageReferenceThrowsException() {
		BuildpackReference reference = BuildpackReference.of("docker://buildpack@0.0.1");
		BuildpackResolverContext resolverContext = mock(BuildpackResolverContext.class);
		assertThatIllegalArgumentException().isThrownBy(() -> ImageBuildpack.resolve(resolverContext, reference))
				.withMessageContaining("Unable to parse image reference \"buildpack@0.0.1\"");
	}

	@Test
	void resolveWhenUnqualifiedReferenceWithInvalidImageReferenceReturnsNull() {
		BuildpackReference reference = BuildpackReference.of("buildpack@0.0.1");
		BuildpackResolverContext resolverContext = mock(BuildpackResolverContext.class);
		Buildpack buildpack = ImageBuildpack.resolve(resolverContext, reference);
		assertThat(buildpack).isNull();
	}

	private Object withMockLayers(InvocationOnMock invocation) {
		try {
			IOBiConsumer<String, TarArchive> consumer = invocation.getArgument(1);
			TarArchive archive = (out) -> {
				try (TarArchiveOutputStream tarOut = new TarArchiveOutputStream(out)) {
					tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
					writeTarEntry(tarOut, "/cnb/");
					writeTarEntry(tarOut, "/cnb/buildpacks/");
					writeTarEntry(tarOut, "/cnb/buildpacks/example_buildpack/");
					writeTarEntry(tarOut, "/cnb/buildpacks/example_buildpack/0.0.1/");
					writeTarEntry(tarOut, "/cnb/buildpacks/example_buildpack/0.0.1/buildpack.toml");
					writeTarEntry(tarOut, "/cnb/buildpacks/example_buildpack/0.0.1/" + this.longFilePath);
					tarOut.finish();
				}
			};
			consumer.accept("test", archive);
		}
		catch (IOException ex) {
			fail("Error writing mock layers", ex);
		}
		return null;
	}

	private void writeTarEntry(TarArchiveOutputStream tarOut, String name) throws IOException {
		TarArchiveEntry entry = new TarArchiveEntry(name);
		tarOut.putArchiveEntry(entry);
		tarOut.closeArchiveEntry();
	}

	private void assertHasExpectedLayers(Buildpack buildpack) throws IOException {
		List<ByteArrayOutputStream> layers = new ArrayList<>();
		buildpack.apply((layer) -> {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			layer.writeTo(out);
			layers.add(out);
		});
		assertThat(layers).hasSize(1);
		byte[] content = layers.get(0).toByteArray();
		List<TarArchiveEntry> entries = new ArrayList<>();
		try (TarArchiveInputStream tar = new TarArchiveInputStream(new ByteArrayInputStream(content))) {
			TarArchiveEntry entry = tar.getNextTarEntry();
			while (entry != null) {
				entries.add(entry);
				entry = tar.getNextTarEntry();
			}
		}
		assertThat(entries).extracting("name", "mode").containsExactlyInAnyOrder(
				tuple("cnb/", TarArchiveEntry.DEFAULT_DIR_MODE),
				tuple("cnb/buildpacks/", TarArchiveEntry.DEFAULT_DIR_MODE),
				tuple("cnb/buildpacks/example_buildpack/", TarArchiveEntry.DEFAULT_DIR_MODE),
				tuple("cnb/buildpacks/example_buildpack/0.0.1/", TarArchiveEntry.DEFAULT_DIR_MODE),
				tuple("cnb/buildpacks/example_buildpack/0.0.1/buildpack.toml", TarArchiveEntry.DEFAULT_FILE_MODE),
				tuple("cnb/buildpacks/example_buildpack/0.0.1/" + this.longFilePath,
						TarArchiveEntry.DEFAULT_FILE_MODE));
	}

}
