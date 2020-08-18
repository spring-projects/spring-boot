/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.loader.jar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link JarFileWrapper}.
 *
 * @author Phillip Webb
 */
class JarFileWrapperTests {

	private JarFile parent;

	private JarFileWrapper wrapper;

	@BeforeEach
	void setup(@TempDir File temp) throws IOException {
		this.parent = spy(new JarFile(createTempJar(temp)));
		this.wrapper = new JarFileWrapper(this.parent);
	}

	private File createTempJar(File temp) throws IOException {
		File file = new File(temp, "temp.jar");
		new JarOutputStream(new FileOutputStream(file)).close();
		return file;
	}

	@Test
	void getUrlDelegatesToParent() throws MalformedURLException {
		this.wrapper.getUrl();
		verify(this.parent).getUrl();
	}

	@Test
	void getTypeDelegatesToParent() {
		this.wrapper.getType();
		verify(this.parent).getType();
	}

	@Test
	void getPermissionDelegatesToParent() {
		this.wrapper.getPermission();
		verify(this.parent).getPermission();
	}

	@Test
	void getManifestDelegatesToParent() throws IOException {
		this.wrapper.getManifest();
		verify(this.parent).getManifest();
	}

	@Test
	void entriesDelegatesToParent() {
		this.wrapper.entries();
		verify(this.parent).entries();
	}

	@Test
	void getJarEntryDelegatesToParent() {
		this.wrapper.getJarEntry("test");
		verify(this.parent).getJarEntry("test");
	}

	@Test
	void getEntryDelegatesToParent() {
		this.wrapper.getEntry("test");
		verify(this.parent).getEntry("test");
	}

	@Test
	void getInputStreamDelegatesToParent() throws IOException {
		this.wrapper.getInputStream();
		verify(this.parent).getInputStream();
	}

	@Test
	void getEntryInputStreamDelegatesToParent() throws IOException {
		ZipEntry entry = new ZipEntry("test");
		this.wrapper.getInputStream(entry);
		verify(this.parent).getInputStream(entry);
	}

	@Test
	void getCommentDelegatesToParent() {
		this.wrapper.getComment();
		verify(this.parent).getComment();
	}

	@Test
	void sizeDelegatesToParent() {
		this.wrapper.size();
		verify(this.parent).size();
	}

	@Test
	void toStringDelegatesToParent() {
		assertThat(this.wrapper.toString()).endsWith("/temp.jar");
	}

	@Test // gh-22991
	void wrapperMustNotImplementClose() {
		// If the wrapper overrides close then on Java 11 a FinalizableResource
		// instance will be used to perform cleanup. This can result in a lot
		// of additional memory being used since cleanup only occurs when the
		// finalizer thread runs. See gh-22991
		assertThatExceptionOfType(NoSuchMethodException.class)
				.isThrownBy(() -> JarFileWrapper.class.getDeclaredMethod("close"));
	}

}
