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
	void posixPermissionsToUmaskWithNullPermissions() {
		assertThatIllegalArgumentException().isThrownBy(() -> FilePermissions.posixPermissionsToUmask(null));
	}

}
