/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link FilePermissions}.
 *
 * @author Scott Frederick
 */
class FilePermissionsTests {

	@TempDir
	Path tempDir;

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void umaskForPath() throws IOException {
		FileAttribute<Set<PosixFilePermission>> fileAttribute = PosixFilePermissions
			.asFileAttribute(PosixFilePermissions.fromString("rw-r-----"));
		Path tempFile = Files.createTempFile(this.tempDir, "umask", null, fileAttribute);
		assertThat(FilePermissions.umaskForPath(tempFile)).isEqualTo(0640);
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void umaskForPathWithNonExistentFile() {
		assertThatIOException()
			.isThrownBy(() -> FilePermissions.umaskForPath(Paths.get(this.tempDir.toString(), "does-not-exist")));
	}

	@Test
	@EnabledOnOs(OS.WINDOWS)
	void umaskForPathOnWindowsFails() throws IOException {
		Path tempFile = Files.createTempFile("umask", null);
		assertThatIllegalStateException().isThrownBy(() -> FilePermissions.umaskForPath(tempFile))
			.withMessageContaining("Unsupported file type for retrieving Posix attributes");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void umaskForPathWithNullPath() {
		assertThatIllegalArgumentException().isThrownBy(() -> FilePermissions.umaskForPath(null));
	}

	@Test
	void posixPermissionsToUmask() {
		Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwxrw-r--");
		assertThat(FilePermissions.posixPermissionsToUmask(permissions)).isEqualTo(0764);
	}

	@Test
	void posixPermissionsToUmaskWithEmptyPermissions() {
		Set<PosixFilePermission> permissions = Collections.emptySet();
		assertThat(FilePermissions.posixPermissionsToUmask(permissions)).isZero();
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void posixPermissionsToUmaskWithNullPermissions() {
		assertThatIllegalArgumentException().isThrownBy(() -> FilePermissions.posixPermissionsToUmask(null));
	}

}
