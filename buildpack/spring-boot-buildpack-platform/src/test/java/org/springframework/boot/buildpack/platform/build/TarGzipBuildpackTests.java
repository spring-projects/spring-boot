/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.build;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
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
		BuildpackReference reference = BuildpackReference.of(compressedArchive.toUri().toString());
		Buildpack buildpack = TarGzipBuildpack.resolve(this.resolverContext, reference);
		assertThat(buildpack).as("Buildpack %s resolved from reference %s", buildpack, reference).isNotNull();
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

	@Test
	void resolveWillNotIApplyEntriesOutsideOfOutputLocation() throws Exception {
		Path compressedArchive = this.testTarGzip
			.createArchive((entryName) -> entryName.endsWith(".toml") ? entryName : "../" + entryName);
		BuildpackReference reference = BuildpackReference.of(compressedArchive.toUri().toString());
		Buildpack buildpack = TarGzipBuildpack.resolve(this.resolverContext, reference);
		assertThat(buildpack).as("Buildpack %s resolved from reference %s", buildpack, reference).isNotNull();
		assertThatIllegalStateException().isThrownBy(() -> buildpack.apply((layers) -> {
		})).withMessageContaining("Entry '../bin/' cannot be written outside of '");
	}

}
