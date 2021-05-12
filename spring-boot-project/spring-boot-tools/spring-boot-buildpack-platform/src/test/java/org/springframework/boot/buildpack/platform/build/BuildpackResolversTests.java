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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.json.AbstractJsonTests;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BuildpackResolvers}.
 *
 * @author Scott Frederick
 */
class BuildpackResolversTests extends AbstractJsonTests {

	private BuildpackResolverContext resolverContext;

	@BeforeEach
	void setup() throws Exception {
		BuilderMetadata metadata = BuilderMetadata.fromJson(getContentAsString("builder-metadata.json"));
		this.resolverContext = mock(BuildpackResolverContext.class);
		given(this.resolverContext.getBuildpackMetadata()).willReturn(metadata.getBuildpacks());
	}

	@Test
	void resolveAllWithBuilderBuildpackReferenceReturnsExpectedBuildpack() {
		BuildpackReference reference = BuildpackReference.of("urn:cnb:builder:paketo-buildpacks/spring-boot@3.5.0");
		Buildpacks buildpacks = BuildpackResolvers.resolveAll(this.resolverContext, Collections.singleton(reference));
		assertThat(buildpacks.getBuildpacks()).hasSize(1);
		assertThat(buildpacks.getBuildpacks().get(0)).isInstanceOf(BuilderBuildpack.class);
	}

	@Test
	void resolveAllWithDirectoryBuildpackReferenceReturnsExpectedBuildpack(@TempDir Path temp) throws IOException {
		FileCopyUtils.copy(getClass().getResourceAsStream("buildpack.toml"),
				Files.newOutputStream(temp.resolve("buildpack.toml")));
		BuildpackReference reference = BuildpackReference.of(temp.toAbsolutePath().toString());
		Buildpacks buildpacks = BuildpackResolvers.resolveAll(this.resolverContext, Collections.singleton(reference));
		assertThat(buildpacks.getBuildpacks()).hasSize(1);
		assertThat(buildpacks.getBuildpacks().get(0)).isInstanceOf(DirectoryBuildpack.class);
	}

	@Test
	void resolveAllWithTarGzipBuildpackReferenceReturnsExpectedBuildpack(@TempDir File temp) throws Exception {
		TestTarGzip testTarGzip = new TestTarGzip(temp);
		Path archive = testTarGzip.createArchive();
		BuildpackReference reference = BuildpackReference.of(archive.toString());
		Buildpacks buildpacks = BuildpackResolvers.resolveAll(this.resolverContext, Collections.singleton(reference));
		assertThat(buildpacks.getBuildpacks()).hasSize(1);
		assertThat(buildpacks.getBuildpacks().get(0)).isInstanceOf(TarGzipBuildpack.class);
	}

	@Test
	void resolveAllWithImageBuildpackReferenceReturnsExpectedBuildpack() throws IOException {
		Image image = Image.of(getContent("buildpack-image.json"));
		BuildpackResolverContext resolverContext = mock(BuildpackResolverContext.class);
		given(resolverContext.fetchImage(any(), any())).willReturn(image);
		BuildpackReference reference = BuildpackReference.of("docker://example/buildpack1:latest");
		Buildpacks buildpacks = BuildpackResolvers.resolveAll(resolverContext, Collections.singleton(reference));
		assertThat(buildpacks.getBuildpacks()).hasSize(1);
		assertThat(buildpacks.getBuildpacks().get(0)).isInstanceOf(ImageBuildpack.class);
	}

	@Test
	void resolveAllWithInvalidLocatorThrowsException() {
		BuildpackReference reference = BuildpackReference.of("unknown-buildpack@0.0.1");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> BuildpackResolvers.resolveAll(this.resolverContext, Collections.singleton(reference)))
				.withMessageContaining("Invalid buildpack reference")
				.withMessageContaining("'unknown-buildpack@0.0.1'");
	}

}
