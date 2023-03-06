/*
 * Copyright 2012-2023 the original author or authors.
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
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
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.io.IOBiConsumer;
import org.springframework.boot.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
	void resolveWhenFullyQualifiedReferenceReturnsBuildpack() throws Exception {
		Image image = Image.of(getContent("buildpack-image.json"));
		ImageReference imageReference = ImageReference.of("example/buildpack1:1.0.0");
		BuildpackResolverContext resolverContext = mock(BuildpackResolverContext.class);
		given(resolverContext.getBuildpackLayersMetadata()).willReturn(BuildpackLayersMetadata.fromJson("{}"));
		given(resolverContext.fetchImage(eq(imageReference), eq(ImageType.BUILDPACK))).willReturn(image);
		willAnswer(this::withMockLayers).given(resolverContext).exportImageLayers(eq(imageReference), any());
		BuildpackReference reference = BuildpackReference.of("docker://example/buildpack1:1.0.0");
		Buildpack buildpack = ImageBuildpack.resolve(resolverContext, reference);
		assertThat(buildpack.getCoordinates()).hasToString("example/hello-universe@0.0.1");
		assertAppliesExpectedLayers(buildpack);
	}

	@Test
	void resolveWhenUnqualifiedReferenceReturnsBuildpack() throws Exception {
		Image image = Image.of(getContent("buildpack-image.json"));
		ImageReference imageReference = ImageReference.of("example/buildpack1:1.0.0");
		BuildpackResolverContext resolverContext = mock(BuildpackResolverContext.class);
		given(resolverContext.getBuildpackLayersMetadata()).willReturn(BuildpackLayersMetadata.fromJson("{}"));
		given(resolverContext.fetchImage(eq(imageReference), eq(ImageType.BUILDPACK))).willReturn(image);
		willAnswer(this::withMockLayers).given(resolverContext).exportImageLayers(eq(imageReference), any());
		BuildpackReference reference = BuildpackReference.of("example/buildpack1:1.0.0");
		Buildpack buildpack = ImageBuildpack.resolve(resolverContext, reference);
		assertThat(buildpack.getCoordinates()).hasToString("example/hello-universe@0.0.1");
		assertAppliesExpectedLayers(buildpack);
	}

	@Test
	void resolveReferenceWithoutTagUsesLatestTag() throws Exception {
		Image image = Image.of(getContent("buildpack-image.json"));
		ImageReference imageReference = ImageReference.of("example/buildpack1:latest");
		BuildpackResolverContext resolverContext = mock(BuildpackResolverContext.class);
		given(resolverContext.getBuildpackLayersMetadata()).willReturn(BuildpackLayersMetadata.fromJson("{}"));
		given(resolverContext.fetchImage(eq(imageReference), eq(ImageType.BUILDPACK))).willReturn(image);
		willAnswer(this::withMockLayers).given(resolverContext).exportImageLayers(eq(imageReference), any());
		BuildpackReference reference = BuildpackReference.of("example/buildpack1");
		Buildpack buildpack = ImageBuildpack.resolve(resolverContext, reference);
		assertThat(buildpack.getCoordinates()).hasToString("example/hello-universe@0.0.1");
		assertAppliesExpectedLayers(buildpack);
	}

	@Test
	void resolveReferenceWithDigestUsesDigest() throws Exception {
		Image image = Image.of(getContent("buildpack-image.json"));
		String digest = "sha256:4acb6bfd6c4f0cabaf7f3690e444afe51f1c7de54d51da7e63fac709c56f1c30";
		ImageReference imageReference = ImageReference.of("example/buildpack1@" + digest);
		BuildpackResolverContext resolverContext = mock(BuildpackResolverContext.class);
		given(resolverContext.getBuildpackLayersMetadata()).willReturn(BuildpackLayersMetadata.fromJson("{}"));
		given(resolverContext.fetchImage(eq(imageReference), eq(ImageType.BUILDPACK))).willReturn(image);
		willAnswer(this::withMockLayers).given(resolverContext).exportImageLayers(eq(imageReference), any());
		BuildpackReference reference = BuildpackReference.of("example/buildpack1@" + digest);
		Buildpack buildpack = ImageBuildpack.resolve(resolverContext, reference);
		assertThat(buildpack.getCoordinates()).hasToString("example/hello-universe@0.0.1");
		assertAppliesExpectedLayers(buildpack);
	}

	@Test
	void resolveWhenBuildpackExistsInBuilderSkipsLayers() throws Exception {
		Image image = Image.of(getContent("buildpack-image.json"));
		ImageReference imageReference = ImageReference.of("example/buildpack1:1.0.0");
		BuildpackResolverContext resolverContext = mock(BuildpackResolverContext.class);
		given(resolverContext.getBuildpackLayersMetadata())
			.willReturn(BuildpackLayersMetadata.fromJson(getContentAsString("buildpack-layers-metadata.json")));
		given(resolverContext.fetchImage(eq(imageReference), eq(ImageType.BUILDPACK))).willReturn(image);
		willAnswer(this::withMockLayers).given(resolverContext).exportImageLayers(eq(imageReference), any());
		BuildpackReference reference = BuildpackReference.of("docker://example/buildpack1:1.0.0");
		Buildpack buildpack = ImageBuildpack.resolve(resolverContext, reference);
		assertThat(buildpack.getCoordinates()).hasToString("example/hello-universe@0.0.1");
		assertAppliesNoLayers(buildpack);
	}

	@Test
	void resolveWhenWhenImageNotPulledThrowsException() throws Exception {
		BuildpackResolverContext resolverContext = mock(BuildpackResolverContext.class);
		given(resolverContext.fetchImage(any(), any())).willThrow(IOException.class);
		BuildpackReference reference = BuildpackReference.of("docker://example/buildpack1");
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
			IOBiConsumer<String, Path> consumer = invocation.getArgument(1);
			File tarFile = File.createTempFile("create-builder-test-", null);
			FileOutputStream out = new FileOutputStream(tarFile);
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
			consumer.accept("test", tarFile.toPath());
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

	private void assertAppliesExpectedLayers(Buildpack buildpack) throws IOException {
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
		assertThat(entries).extracting("name", "mode")
			.containsExactlyInAnyOrder(tuple("cnb/", TarArchiveEntry.DEFAULT_DIR_MODE),
					tuple("cnb/buildpacks/", TarArchiveEntry.DEFAULT_DIR_MODE),
					tuple("cnb/buildpacks/example_buildpack/", TarArchiveEntry.DEFAULT_DIR_MODE),
					tuple("cnb/buildpacks/example_buildpack/0.0.1/", TarArchiveEntry.DEFAULT_DIR_MODE),
					tuple("cnb/buildpacks/example_buildpack/0.0.1/buildpack.toml", TarArchiveEntry.DEFAULT_FILE_MODE),
					tuple("cnb/buildpacks/example_buildpack/0.0.1/" + this.longFilePath,
							TarArchiveEntry.DEFAULT_FILE_MODE));
	}

	private void assertAppliesNoLayers(Buildpack buildpack) throws IOException {
		List<ByteArrayOutputStream> layers = new ArrayList<>();
		buildpack.apply((layer) -> {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			layer.writeTo(out);
			layers.add(out);
		});
		assertThat(layers).isEmpty();
	}

}
