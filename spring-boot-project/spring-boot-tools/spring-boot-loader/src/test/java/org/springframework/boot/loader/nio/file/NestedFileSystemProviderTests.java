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

package org.springframework.boot.loader.nio.file;

import java.io.File;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.ReadOnlyFileSystemException;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.testsupport.TestJar;
import org.springframework.boot.loader.zip.AssertFileChannelDataBlocksClosed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link NestedFileSystemProvider}.
 *
 * @author Phillip Webb
 */
@AssertFileChannelDataBlocksClosed
class NestedFileSystemProviderTests {

	@TempDir
	File temp;

	private File file;

	private TestNestedFileSystemProvider provider = new TestNestedFileSystemProvider();

	private String uriPrefix;

	@BeforeEach
	void setup() throws Exception {
		this.file = new File(this.temp, "test.jar");
		TestJar.create(this.file);
		this.uriPrefix = "nested:" + this.file.toURI().getPath() + "/!";
	}

	@Test
	void getSchemeReturnsScheme() {
		assertThat(this.provider.getScheme()).isEqualTo("nested");
	}

	@Test
	void newFilesSystemWhenBadUrlThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.provider.newFileSystem(new URI("bad:notreal"), Collections.emptyMap()))
			.withMessageContaining("must use 'nested' scheme");
	}

	@Test
	void newFileSystemWhenAlreadyExistsThrowsException() throws Exception {
		this.provider.newFileSystem(new URI(this.uriPrefix + "nested.jar"), null);
		assertThatExceptionOfType(FileSystemAlreadyExistsException.class)
			.isThrownBy(() -> this.provider.newFileSystem(new URI(this.uriPrefix + "other.jar"), null));
	}

	@Test
	void newFileSystemReturnsFileSystem() throws Exception {
		FileSystem fileSystem = this.provider.newFileSystem(new URI(this.uriPrefix + "nested.jar"), null);
		assertThat(fileSystem).isInstanceOf(NestedFileSystem.class);
	}

	@Test
	void getFileSystemWhenBadUrlThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.provider.getFileSystem(new URI("bad:notreal")))
			.withMessageContaining("must use 'nested' scheme");
	}

	@Test
	void getFileSystemWhenNotCreatedThrowsException() {
		assertThatExceptionOfType(FileSystemNotFoundException.class)
			.isThrownBy(() -> this.provider.getFileSystem(new URI(this.uriPrefix + "nested.jar")));
	}

	@Test
	void getFileSystemReturnsFileSystem() throws Exception {
		FileSystem expected = this.provider.newFileSystem(new URI(this.uriPrefix + "nested.jar"), null);
		assertThat(this.provider.getFileSystem(new URI(this.uriPrefix + "nested.jar"))).isSameAs(expected);
	}

	@Test
	void getPathWhenFileSystemExistsReturnsPath() throws Exception {
		URI uri = new URI(this.uriPrefix + "nested.jar");
		this.provider.newFileSystem(uri, null);
		assertThat(this.provider.getPath(uri)).isInstanceOf(NestedPath.class);
	}

	@Test
	void getPathWhenFileSystemDoesNtExistReturnsPath() throws Exception {
		URI uri = new URI(this.uriPrefix + "nested.jar");
		assertThat(this.provider.getPath(uri)).isInstanceOf(NestedPath.class);
	}

	@Test
	void newByteChannelReturnsByteChannel() throws Exception {
		URI uri = new URI(this.uriPrefix + "nested.jar");
		Path path = this.provider.getPath(uri);
		try (SeekableByteChannel byteChannel = this.provider.newByteChannel(path, Set.of(StandardOpenOption.READ))) {
			assertThat(byteChannel).isInstanceOf(NestedByteChannel.class);
		}
	}

	@Test
	void newDirectoryStreamThrowsException() throws Exception {
		URI uri = new URI(this.uriPrefix + "nested.jar");
		Path path = this.provider.getPath(uri);
		assertThatExceptionOfType(NotDirectoryException.class)
			.isThrownBy(() -> this.provider.newDirectoryStream(path, null));
	}

	@Test
	void createDirectoryThrowsException() throws Exception {
		URI uri = new URI(this.uriPrefix + "nested.jar");
		Path path = this.provider.getPath(uri);
		assertThatExceptionOfType(ReadOnlyFileSystemException.class)
			.isThrownBy(() -> this.provider.createDirectory(path));
	}

	@Test
	void deleteThrowsException() throws Exception {
		URI uri = new URI(this.uriPrefix + "nested.jar");
		Path path = this.provider.getPath(uri);
		assertThatExceptionOfType(ReadOnlyFileSystemException.class).isThrownBy(() -> this.provider.delete(path));
	}

	@Test
	void copyThrowsException() throws Exception {
		URI uri = new URI(this.uriPrefix + "nested.jar");
		Path path = this.provider.getPath(uri);
		assertThatExceptionOfType(ReadOnlyFileSystemException.class).isThrownBy(() -> this.provider.copy(path, path));
	}

	@Test
	void moveThrowsException() throws Exception {
		URI uri = new URI(this.uriPrefix + "nested.jar");
		Path path = this.provider.getPath(uri);
		assertThatExceptionOfType(ReadOnlyFileSystemException.class).isThrownBy(() -> this.provider.move(path, path));
	}

	@Test
	void isSameFileWhenSameReturnsTrue() throws Exception {
		Path p1 = this.provider.getPath(new URI(this.uriPrefix + "nested.jar"));
		Path p2 = this.provider.getPath(new URI(this.uriPrefix + "nested.jar"));
		assertThat(this.provider.isSameFile(p1, p1)).isTrue();
		assertThat(this.provider.isSameFile(p1, p2)).isTrue();
	}

	@Test
	void isSameFileWhenDifferentReturnsFalse() throws Exception {
		Path p1 = this.provider.getPath(new URI(this.uriPrefix + "nested.jar"));
		Path p2 = this.provider.getPath(new URI(this.uriPrefix + "other.jar"));
		assertThat(this.provider.isSameFile(p1, p2)).isFalse();
	}

	@Test
	void isHiddenReturnsFalse() throws Exception {
		Path path = this.provider.getPath(new URI(this.uriPrefix + "nested.jar"));
		assertThat(this.provider.isHidden(path)).isFalse();
	}

	@Test
	void getFileStoreWhenFileDoesNotExistThrowsException() throws Exception {
		Path path = this.provider.getPath(new URI(this.uriPrefix + "missing.jar"));
		assertThatExceptionOfType(NoSuchFileException.class).isThrownBy(() -> this.provider.getFileStore(path));
	}

	@Test
	void getFileStoreReturnsFileStore() throws Exception {
		Path path = this.provider.getPath(new URI(this.uriPrefix + "nested.jar"));
		assertThat(this.provider.getFileStore(path)).isInstanceOf(NestedFileStore.class);
	}

	@Test
	void checkAccessDelegatesToJarPath() throws Exception {
		Path path = this.provider.getPath(new URI(this.uriPrefix + "nested.jar"));
		Path jarPath = mockJarPath();
		this.provider.setMockJarPath(jarPath);
		this.provider.checkAccess(path);
		then(jarPath.getFileSystem().provider()).should().checkAccess(jarPath);
	}

	@Test
	void getFileAttributeViewDelegatesToJarPath() throws Exception {
		Path path = this.provider.getPath(new URI(this.uriPrefix + "nested.jar"));
		Path jarPath = mockJarPath();
		this.provider.setMockJarPath(jarPath);
		this.provider.getFileAttributeView(path, BasicFileAttributeView.class);
		then(jarPath.getFileSystem().provider()).should().getFileAttributeView(jarPath, BasicFileAttributeView.class);
	}

	@Test
	void readAttributesWithTypeDelegatesToJarPath() throws Exception {
		Path path = this.provider.getPath(new URI(this.uriPrefix + "nested.jar"));
		Path jarPath = mockJarPath();
		this.provider.setMockJarPath(jarPath);
		this.provider.readAttributes(path, BasicFileAttributes.class);
		then(jarPath.getFileSystem().provider()).should().readAttributes(jarPath, BasicFileAttributes.class);
	}

	@Test
	void readAttributesWithNameDelegatesToJarPath() throws Exception {
		Path path = this.provider.getPath(new URI(this.uriPrefix + "nested.jar"));
		Path jarPath = mockJarPath();
		this.provider.setMockJarPath(jarPath);
		this.provider.readAttributes(path, "basic");
		then(jarPath.getFileSystem().provider()).should().readAttributes(jarPath, "basic");
	}

	@Test
	void setAttributeThrowsException() throws Exception {
		Path path = this.provider.getPath(new URI(this.uriPrefix + "nested.jar"));
		assertThatExceptionOfType(ReadOnlyFileSystemException.class)
			.isThrownBy(() -> this.provider.setAttribute(path, "test", "test"));
	}

	private Path mockJarPath() {
		Path path = mock(Path.class);
		FileSystem fileSystem = mock(FileSystem.class);
		given(path.getFileSystem()).willReturn(fileSystem);
		FileSystemProvider provider = mock(FileSystemProvider.class);
		given(fileSystem.provider()).willReturn(provider);
		return path;
	}

	static class TestNestedFileSystemProvider extends NestedFileSystemProvider {

		private Path mockJarPath;

		@Override
		protected Path getJarPath(Path path) {
			return (this.mockJarPath != null) ? this.mockJarPath : super.getJarPath(path);
		}

		void setMockJarPath(Path mockJarPath) {
			this.mockJarPath = mockJarPath;
		}

	}

}
