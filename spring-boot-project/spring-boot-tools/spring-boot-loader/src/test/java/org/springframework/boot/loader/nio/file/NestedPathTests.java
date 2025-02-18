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
import java.net.URI;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.WatchService;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.testsupport.TestJar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link NestedPath}.
 *
 * @author Phillip Webb
 */
class NestedPathTests {

	@TempDir
	File temp;

	private NestedFileSystem fileSystem;

	private NestedFileSystemProvider provider;

	private Path jarPath;

	private NestedPath path;

	@BeforeEach
	void setup() {
		this.jarPath = new File(this.temp, "test.jar").toPath();
		this.provider = new NestedFileSystemProvider();
		this.fileSystem = new NestedFileSystem(this.provider, this.jarPath);
		this.path = new NestedPath(this.fileSystem, "nested.jar");
	}

	@Test
	void getJarPathReturnsJarPath() {
		assertThat(this.path.getJarPath()).isEqualTo(this.jarPath);
	}

	@Test
	void getNestedEntryNameReturnsNestedEntryName() {
		assertThat(this.path.getNestedEntryName()).isEqualTo("nested.jar");
	}

	@Test
	void getFileSystemReturnsFileSystem() {
		assertThat(this.path.getFileSystem()).isSameAs(this.fileSystem);
	}

	@Test
	void isAbsoluteReturnsTrue() {
		assertThat(this.path.isAbsolute()).isTrue();
	}

	@Test
	void getRootReturnsNull() {
		assertThat(this.path.getRoot()).isNull();
	}

	@Test
	void getFileNameReturnsPath() {
		assertThat(this.path.getFileName()).isSameAs(this.path);
	}

	@Test
	void getParentReturnsNull() {
		assertThat(this.path.getParent()).isNull();
	}

	@Test
	void getNameCountReturnsOne() {
		assertThat(this.path.getNameCount()).isEqualTo(1);
	}

	@Test
	void subPathWhenBeginZeroEndOneReturnsPath() {
		assertThat(this.path.subpath(0, 1)).isSameAs(this.path);
	}

	@Test
	void subPathWhenBeginIndexNotZeroThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.path.subpath(1, 1))
			.withMessage("Nested paths only have a single element");
	}

	@Test
	void subPathThenEndIndexNotOneThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.path.subpath(0, 2))
			.withMessage("Nested paths only have a single element");
	}

	@Test
	void startsWithWhenStartsWithReturnsTrue() {
		NestedPath otherPath = new NestedPath(this.fileSystem, "nested.jar");
		assertThat(this.path.startsWith(otherPath)).isTrue();
	}

	@Test
	void startsWithWhenNotStartsWithReturnsFalse() {
		NestedPath otherPath = new NestedPath(this.fileSystem, "other.jar");
		assertThat(this.path.startsWith(otherPath)).isFalse();
	}

	@Test
	void endsWithWhenEndsWithReturnsTrue() {
		NestedPath otherPath = new NestedPath(this.fileSystem, "nested.jar");
		assertThat(this.path.endsWith(otherPath)).isTrue();
	}

	@Test
	void endsWithWhenNotEndsWithReturnsFalse() {
		NestedPath otherPath = new NestedPath(this.fileSystem, "other.jar");
		assertThat(this.path.endsWith(otherPath)).isFalse();
	}

	@Test
	void normalizeReturnsPath() {
		assertThat(this.path.normalize()).isSameAs(this.path);
	}

	@Test
	void resolveThrowsException() {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> this.path.resolve(this.path))
			.withMessage("Unable to resolve nested path");
	}

	@Test
	void relativizeThrowsException() {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> this.path.relativize(this.path))
			.withMessage("Unable to relativize nested path");
	}

	@Test
	void toUriReturnsUri() throws Exception {
		assertThat(this.path.toUri())
			.isEqualTo(new URI("nested:" + this.jarPath.toUri().getRawPath() + "/!nested.jar"));
	}

	@Test
	void toUriWhenHasSpecialCharsReturnsEncodedUri() throws Exception {
		this.jarPath = new File(this.temp, "te st.jar").toPath();
		this.provider = new NestedFileSystemProvider();
		this.fileSystem = new NestedFileSystem(this.provider, this.jarPath);
		this.path = new NestedPath(this.fileSystem, "ne sted.jar");
		assertThat(this.path.toUri())
			.isEqualTo(new URI("nested:" + this.jarPath.toUri().getRawPath() + "/!ne%20sted.jar"));
	}

	@Test
	void toAbsolutePathReturnsPath() {
		assertThat(this.path.toAbsolutePath()).isSameAs(this.path);
	}

	@Test
	void toRealPathReturnsPath() throws Exception {
		assertThat(this.path.toRealPath()).isSameAs(this.path);
	}

	@Test
	void registerThrowsException() {
		WatchService watcher = mock(WatchService.class);
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> this.path.register(watcher))
			.withMessage("Nested paths cannot be watched");
	}

	@Test
	void compareToComparesOnNestedEntryName() {
		NestedPath a = new NestedPath(this.fileSystem, "a.jar");
		NestedPath b = new NestedPath(this.fileSystem, "b.jar");
		NestedPath c = new NestedPath(this.fileSystem, "c.jar");
		assertThat(new TreeSet<>(Set.of(c, a, b))).containsExactly(a, b, c);
	}

	@Test
	void hashCodeAndEquals() {
		NestedFileSystem fs2 = new NestedFileSystem(this.provider, new File(this.temp, "test2.jar").toPath());
		NestedPath p1 = new NestedPath(this.fileSystem, "a.jar");
		NestedPath p2 = new NestedPath(this.fileSystem, "a.jar");
		NestedPath p3 = new NestedPath(this.fileSystem, "c.jar");
		NestedPath p4 = new NestedPath(fs2, "c.jar");
		assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
		assertThat(p1).isEqualTo(p1).isEqualTo(p2).isNotEqualTo(p3).isNotEqualTo(p4);
	}

	@Test
	void toStringReturnsString() {
		assertThat(this.path).hasToString(this.jarPath.toString() + "/!nested.jar");
	}

	@Test
	void assertExistsWhenExists() throws Exception {
		TestJar.create(this.jarPath.toFile());
		this.path.assertExists();
	}

	@Test
	void assertExistsWhenDoesNotExistThrowsException() {
		assertThatExceptionOfType(NoSuchFileException.class).isThrownBy(this.path::assertExists);
	}

	@Test
	void castWhenNestedPathReturnsNestedPath() {
		assertThat(NestedPath.cast(this.path)).isSameAs(this.path);
	}

	@Test
	void castWhenNullThrowsException() {
		assertThatExceptionOfType(ProviderMismatchException.class).isThrownBy(() -> NestedPath.cast(null));
	}

	@Test
	void castWhenNotNestedPathThrowsException() {
		assertThatExceptionOfType(ProviderMismatchException.class).isThrownBy(() -> NestedPath.cast(this.jarPath));
	}

}
