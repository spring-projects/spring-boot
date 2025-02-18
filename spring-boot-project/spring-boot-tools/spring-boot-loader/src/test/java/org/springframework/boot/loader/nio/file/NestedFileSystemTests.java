/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.loader.nio.file;

import java.io.File;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link NestedFileSystem}.
 *
 * @author Phillip Webb
 */
class NestedFileSystemTests {

	@TempDir
	File temp;

	private NestedFileSystemProvider provider;

	private Path jarPath;

	private NestedFileSystem fileSystem;

	@BeforeEach
	void setup() {
		this.provider = new NestedFileSystemProvider();
		this.jarPath = new File(this.temp, "test.jar").toPath();
		this.fileSystem = new NestedFileSystem(this.provider, this.jarPath);
	}

	@Test
	void providerReturnsProvider() {
		assertThat(this.fileSystem.provider()).isSameAs(this.provider);
	}

	@Test
	void getJarPathReturnsJarPath() {
		assertThat(this.fileSystem.getJarPath()).isSameAs(this.jarPath);
	}

	@Test
	void closeClosesFileSystem() throws Exception {
		this.fileSystem.close();
		assertThat(this.fileSystem.isOpen()).isFalse();
	}

	@Test
	void closeWhenAlreadyClosedDoesNothing() throws Exception {
		this.fileSystem.close();
		this.fileSystem.close();
		assertThat(this.fileSystem.isOpen()).isFalse();
	}

	@Test
	void isOpenWhenOpenReturnsTrue() {
		assertThat(this.fileSystem.isOpen()).isTrue();
	}

	@Test
	void isOpenWhenClosedReturnsFalse() throws Exception {
		this.fileSystem.close();
		assertThat(this.fileSystem.isOpen()).isFalse();
	}

	@Test
	void isReadOnlyReturnsTrue() {
		assertThat(this.fileSystem.isReadOnly()).isTrue();
	}

	@Test
	void getSeparatorReturnsSeparator() {
		assertThat(this.fileSystem.getSeparator()).isEqualTo("/!");
	}

	@Test
	void getRootDirectoryWhenOpenReturnsEmptyIterable() {
		assertThat(this.fileSystem.getRootDirectories()).isEmpty();
	}

	@Test
	void getRootDirectoryWhenClosedThrowsException() throws Exception {
		this.fileSystem.close();
		assertThatExceptionOfType(ClosedFileSystemException.class)
			.isThrownBy(() -> this.fileSystem.getRootDirectories());
	}

	@Test
	void supportedFileAttributeViewsWhenOpenReturnsBasic() {
		assertThat(this.fileSystem.supportedFileAttributeViews()).containsExactly("basic");
	}

	@Test
	void supportedFileAttributeViewsWhenClosedThrowsException() throws Exception {
		this.fileSystem.close();
		assertThatExceptionOfType(ClosedFileSystemException.class)
			.isThrownBy(() -> this.fileSystem.supportedFileAttributeViews());
	}

	@Test
	void getPathWhenClosedThrowsException() throws Exception {
		this.fileSystem.close();
		assertThatExceptionOfType(ClosedFileSystemException.class)
			.isThrownBy(() -> this.fileSystem.getPath("nested.jar"));
	}

	@Test
	void getPathWhenFirstIsNull() {
		Path path = this.fileSystem.getPath(null);
		assertThat(path.toString()).endsWith(File.separator + "test.jar");
	}

	@Test
	void getPathWhenFirstIsBlank() {
		Path path = this.fileSystem.getPath("");
		assertThat(path.toString()).endsWith(File.separator + "test.jar");
	}

	@Test
	void getPathWhenMoreIsNotEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.fileSystem.getPath("nested.jar", "another.jar"))
			.withMessage("Nested paths must contain a single element");
	}

	@Test
	void getPathReturnsPath() {
		assertThat(this.fileSystem.getPath("nested.jar")).isInstanceOf(NestedPath.class);
	}

	@Test
	void getPathMatchThrowsException() {
		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> this.fileSystem.getPathMatcher("*"))
			.withMessage("Nested paths do not support path matchers");
	}

	@Test
	void getUserPrincipalLookupServiceThrowsException() {
		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> this.fileSystem.getUserPrincipalLookupService())
			.withMessage("Nested paths do not have a user principal lookup service");
	}

	@Test
	void newWatchServiceThrowsException() {
		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> this.fileSystem.newWatchService())
			.withMessage("Nested paths do not support the WatchService");
	}

	@Test
	void toStringReturnsString() {
		assertThat(this.fileSystem).hasToString(this.jarPath.toAbsolutePath().toString());
	}

	@Test
	void equalsAndHashCode() {
		Path jp1 = new File(this.temp, "test1.jar").toPath();
		Path jp2 = new File(this.temp, "test1.jar").toPath();
		Path jp3 = new File(this.temp, "test2.jar").toPath();
		NestedFileSystem f1 = new NestedFileSystem(this.provider, jp1);
		NestedFileSystem f2 = new NestedFileSystem(this.provider, jp1);
		NestedFileSystem f3 = new NestedFileSystem(this.provider, jp2);
		NestedFileSystem f4 = new NestedFileSystem(this.provider, jp3);
		assertThat(f1.hashCode()).isEqualTo(f2.hashCode());
		assertThat(f1).isEqualTo(f1).isEqualTo(f2).isEqualTo(f3).isNotEqualTo(f4);
	}

}
