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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;

import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.io.IOBiConsumer;
import org.springframework.boot.buildpack.platform.io.TarArchive;
import org.springframework.boot.buildpack.platform.json.AbstractJsonTests;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
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
	void resolveWhenFullyQualifiedReferenceWithInvalidImageReferenceThrowsException() throws Exception {
		BuildpackReference reference = BuildpackReference.of("docker://buildpack@0.0.1");
		BuildpackResolverContext resolverContext = mock(BuildpackResolverContext.class);
		assertThatIllegalArgumentException().isThrownBy(() -> ImageBuildpack.resolve(resolverContext, reference))
				.withMessageContaining("Unable to parse image reference \"buildpack@0.0.1\"");
	}

	@Test
	void resolveWhenUnqualifiedReferenceWithInvalidImageReferenceReturnsNull() throws Exception {
		BuildpackReference reference = BuildpackReference.of("buildpack@0.0.1");
		BuildpackResolverContext resolverContext = mock(BuildpackResolverContext.class);
		Buildpack buildpack = ImageBuildpack.resolve(resolverContext, reference);
		assertThat(buildpack).isNull();
	}

	private Object withMockLayers(InvocationOnMock invocation) throws Exception {
		IOBiConsumer<String, TarArchive> consumer = invocation.getArgument(1);
		TarArchive archive = (out) -> FileCopyUtils.copy(getClass().getResourceAsStream("layer.tar"), out);
		consumer.accept("test", archive);
		return null;
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
		List<String> names = new ArrayList<>();
		try (TarArchiveInputStream tar = new TarArchiveInputStream(new ByteArrayInputStream(content))) {
			TarArchiveEntry entry = tar.getNextTarEntry();
			while (entry != null) {
				names.add(entry.getName());
				entry = tar.getNextTarEntry();
			}
		}
		assertThat(names).containsExactly("etc/apt/sources.list");
	}

}
