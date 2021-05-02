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
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TarGzipBuildpack}.
 *
 * @author Scott Frederick
 */
class TarGzipBuildpackTests {

	private File buildpackDir;

	private TestTarGzip testTarGzip;

	private BuildpackResolverContext resolverContext;

	@BeforeEach
	void setUp(@TempDir File temp) {
		this.buildpackDir = new File(temp, "buildpack");
		this.buildpackDir.mkdirs();
		this.testTarGzip = new TestTarGzip(this.buildpackDir);
		this.resolverContext = mock(BuildpackResolverContext.class);
	}

	@Test
	void resolveWhenFilePathReturnsBuildpack() throws Exception {
		Path compressedArchive = this.testTarGzip.createArchive();
		BuildpackReference reference = BuildpackReference.of(compressedArchive.toString());
		Buildpack buildpack = TarGzipBuildpack.resolve(this.resolverContext, reference);
		assertThat(buildpack).isNotNull();
		assertThat(buildpack.getCoordinates()).hasToString("example/buildpack1@0.0.1");
		this.testTarGzip.assertHasExpectedLayers(buildpack);
	}

	@Test
	void resolveWhenFileUrlReturnsBuildpack() throws Exception {
		Path compressedArchive = this.testTarGzip.createArchive();
		BuildpackReference reference = BuildpackReference.of("file://" + compressedArchive.toString());
		Buildpack buildpack = TarGzipBuildpack.resolve(this.resolverContext, reference);
		assertThat(buildpack).isNotNull();
		assertThat(buildpack.getCoordinates()).hasToString("example/buildpack1@0.0.1");
		this.testTarGzip.assertHasExpectedLayers(buildpack);
	}

	@Test
	void resolveWhenArchiveWithoutDescriptorThrowsException() throws Exception {
		Path compressedArchive = this.testTarGzip.createEmptyArchive();
		BuildpackReference reference = BuildpackReference.of(compressedArchive.toString());
		assertThatIllegalArgumentException().isThrownBy(() -> TarGzipBuildpack.resolve(this.resolverContext, reference))
				.withMessageContaining("Buildpack descriptor 'buildpack.toml' is required")
				.withMessageContaining(compressedArchive.toString());
	}

	@Test
	void resolveWhenArchiveWithDirectoryReturnsNull() {
		BuildpackReference reference = BuildpackReference.of(this.buildpackDir.getAbsolutePath());
		Buildpack buildpack = TarGzipBuildpack.resolve(this.resolverContext, reference);
		assertThat(buildpack).isNull();
	}

	@Test
	void resolveWhenArchiveThatDoesNotExistReturnsNull() {
		BuildpackReference reference = BuildpackReference.of("/test/i/am/missing/buildpack.tar");
		Buildpack buildpack = TarGzipBuildpack.resolve(this.resolverContext, reference);
		assertThat(buildpack).isNull();
	}

}
