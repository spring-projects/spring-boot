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

package org.springframework.boot.buildpack.platform.io;

import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link FilePermissions}.
 *
 * @author Scott Frederick
 */
class FilePermissionsTests {

	@Test
	void posixPermissionsToUmask() {
		Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwxrw-r--");
		assertThat(FilePermissions.posixPermissionsToUmask(permissions)).isEqualTo(0764);
	}

	@Test
	void posixPermissionsToUmaskWithEmptyPermissions() {
		Set<PosixFilePermission> permissions = Collections.emptySet();
		assertThat(FilePermissions.posixPermissionsToUmask(permissions)).isEqualTo(0);
	}

	@Test
	void posixPermissionsToUmaskWithNullPermissions() {
		assertThatIllegalArgumentException().isThrownBy(() -> FilePermissions.posixPermissionsToUmask(null));
	}

}
