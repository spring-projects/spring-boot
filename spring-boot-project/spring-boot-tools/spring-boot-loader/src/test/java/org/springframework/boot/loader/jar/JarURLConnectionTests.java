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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.TestJarCreator;
import org.springframework.boot.loader.jar.JarURLConnection.JarEntryName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link JarURLConnection}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Rostyslav Dudka
 */
class JarURLConnectionTests {

	private File rootJarFile;

	private JarFile jarFile;

	@BeforeEach
	void setup(@TempDir File tempDir) throws Exception {
		this.rootJarFile = new File(tempDir, "root.jar");
		TestJarCreator.createTestJar(this.rootJarFile);
		this.jarFile = new JarFile(this.rootJarFile);
	}

	@AfterEach
	void tearDown() throws Exception {
		this.jarFile.close();
	}

	@Test
	void connectionToRootUsingAbsoluteUrl() throws Exception {
		URL url = new URL("jar:" + this.rootJarFile.toURI().toURL() + "!/");
		Object content = JarURLConnection.get(url, this.jarFile).getContent();
		assertThat(JarFileWrapper.unwrap((java.util.jar.JarFile) content)).isSameAs(this.jarFile);
	}

	@Test
	void connectionToRootUsingRelativeUrl() throws Exception {
		URL url = new URL("jar:file:" + getRelativePath() + "!/");
		Object content = JarURLConnection.get(url, this.jarFile).getContent();
		assertThat(JarFileWrapper.unwrap((java.util.jar.JarFile) content)).isSameAs(this.jarFile);
	}

	@Test
	void connectionToEntryUsingAbsoluteUrl() throws Exception {
		URL url = new URL("jar:" + this.rootJarFile.toURI().toURL() + "!/1.dat");
		try (InputStream input = JarURLConnection.get(url, this.jarFile).getInputStream()) {
			assertThat(input).hasSameContentAs(new ByteArrayInputStream(new byte[] { 1 }));
		}
	}

	@Test
	void connectionToEntryUsingRelativeUrl() throws Exception {
		URL url = new URL("jar:file:" + getRelativePath() + "!/1.dat");
		try (InputStream input = JarURLConnection.get(url, this.jarFile).getInputStream()) {
			assertThat(input).hasSameContentAs(new ByteArrayInputStream(new byte[] { 1 }));
		}
	}

	@Test
	void connectionToEntryUsingAbsoluteUrlWithFileColonSlashSlashPrefix() throws Exception {
		URL url = new URL("jar:" + this.rootJarFile.toURI().toURL() + "!/1.dat");
		try (InputStream input = JarURLConnection.get(url, this.jarFile).getInputStream()) {
			assertThat(input).hasSameContentAs(new ByteArrayInputStream(new byte[] { 1 }));
		}
	}

	@Test
	void connectionToEntryUsingAbsoluteUrlForNestedEntry() throws Exception {
		URL url = new URL("jar:" + this.rootJarFile.toURI().toURL() + "!/nested.jar!/3.dat");
		JarURLConnection connection = JarURLConnection.get(url, this.jarFile);
		try (InputStream input = connection.getInputStream()) {
			assertThat(input).hasSameContentAs(new ByteArrayInputStream(new byte[] { 3 }));
		}
		connection.getJarFile().close();
	}

	@Test
	void connectionToEntryUsingRelativeUrlForNestedEntry() throws Exception {
		URL url = new URL("jar:file:" + getRelativePath() + "!/nested.jar!/3.dat");
		JarURLConnection connection = JarURLConnection.get(url, this.jarFile);
		try (InputStream input = connection.getInputStream()) {
			assertThat(input).hasSameContentAs(new ByteArrayInputStream(new byte[] { 3 }));
		}
		connection.getJarFile().close();
	}

	@Test
	void connectionToEntryUsingAbsoluteUrlForEntryFromNestedJarFile() throws Exception {
		URL url = new URL("jar:" + this.rootJarFile.toURI().toURL() + "!/nested.jar!/3.dat");
		try (JarFile nested = this.jarFile.getNestedJarFile(this.jarFile.getEntry("nested.jar"))) {
			try (InputStream input = JarURLConnection.get(url, nested).getInputStream()) {
				assertThat(input).hasSameContentAs(new ByteArrayInputStream(new byte[] { 3 }));
			}
		}
	}

	@Test
	void connectionToEntryUsingRelativeUrlForEntryFromNestedJarFile() throws Exception {
		URL url = new URL("jar:file:" + getRelativePath() + "!/nested.jar!/3.dat");
		try (JarFile nested = this.jarFile.getNestedJarFile(this.jarFile.getEntry("nested.jar"))) {
			try (InputStream input = JarURLConnection.get(url, nested).getInputStream()) {
				assertThat(input).hasSameContentAs(new ByteArrayInputStream(new byte[] { 3 }));
			}
		}
	}

	@Test
	void connectionToEntryInNestedJarFromUrlThatUsesExistingUrlAsContext() throws Exception {
		URL url = new URL(new URL("jar", null, -1, this.rootJarFile.toURI().toURL() + "!/nested.jar!/", new Handler()),
				"/3.dat");
		try (JarFile nested = this.jarFile.getNestedJarFile(this.jarFile.getEntry("nested.jar"))) {
			try (InputStream input = JarURLConnection.get(url, nested).getInputStream()) {
				assertThat(input).hasSameContentAs(new ByteArrayInputStream(new byte[] { 3 }));
			}
		}
	}

	@Test
	void connectionToEntryWithSpaceNestedEntry() throws Exception {
		URL url = new URL("jar:file:" + getRelativePath() + "!/space nested.jar!/3.dat");
		JarURLConnection connection = JarURLConnection.get(url, this.jarFile);
		try (InputStream input = connection.getInputStream()) {
			assertThat(input).hasSameContentAs(new ByteArrayInputStream(new byte[] { 3 }));
		}
		connection.getJarFile().close();
	}

	@Test
	void connectionToEntryWithEncodedSpaceNestedEntry() throws Exception {
		URL url = new URL("jar:file:" + getRelativePath() + "!/space%20nested.jar!/3.dat");
		JarURLConnection connection = JarURLConnection.get(url, this.jarFile);
		try (InputStream input = connection.getInputStream()) {
			assertThat(input).hasSameContentAs(new ByteArrayInputStream(new byte[] { 3 }));
		}
		connection.getJarFile().close();
	}

	@Test
	void connectionToEntryUsingWrongAbsoluteUrlForEntryFromNestedJarFile() throws Exception {
		URL url = new URL("jar:" + this.rootJarFile.toURI().toURL() + "!/w.jar!/3.dat");
		try (JarFile nested = this.jarFile.getNestedJarFile(this.jarFile.getEntry("nested.jar"))) {
			assertThatExceptionOfType(FileNotFoundException.class)
					.isThrownBy(JarURLConnection.get(url, nested)::getInputStream);
		}
	}

	@Test
	void getContentLengthReturnsLengthOfUnderlyingEntry() throws Exception {
		URL url = new URL("jar:" + this.rootJarFile.toURI().toURL() + "!/nested.jar!/3.dat");
		try (JarFile nested = this.jarFile.getNestedJarFile(this.jarFile.getEntry("nested.jar"))) {
			JarURLConnection connection = JarURLConnection.get(url, nested);
			assertThat(connection.getContentLength()).isEqualTo(1);
		}
	}

	@Test
	void getContentLengthLongReturnsLengthOfUnderlyingEntry() throws Exception {
		URL url = new URL("jar:" + this.rootJarFile.toURI().toURL() + "!/nested.jar!/3.dat");
		try (JarFile nested = this.jarFile.getNestedJarFile(this.jarFile.getEntry("nested.jar"))) {
			JarURLConnection connection = JarURLConnection.get(url, nested);
			assertThat(connection.getContentLengthLong()).isEqualTo(1);
		}
	}

	@Test
	void getLastModifiedReturnsLastModifiedTimeOfJarEntry() throws Exception {
		URL url = new URL("jar:" + this.rootJarFile.toURI().toURL() + "!/1.dat");
		JarURLConnection connection = JarURLConnection.get(url, this.jarFile);
		assertThat(connection.getLastModified()).isEqualTo(connection.getJarEntry().getTime());
	}

	@Test
	void jarEntryBasicName() {
		assertThat(new JarEntryName(new StringSequence("a/b/C.class")).toString()).isEqualTo("a/b/C.class");
	}

	@Test
	void jarEntryNameWithSingleByteEncodedCharacters() {
		assertThat(new JarEntryName(new StringSequence("%61/%62/%43.class")).toString()).isEqualTo("a/b/C.class");
	}

	@Test
	void jarEntryNameWithDoubleByteEncodedCharacters() {
		assertThat(new JarEntryName(new StringSequence("%c3%a1/b/C.class")).toString()).isEqualTo("\u00e1/b/C.class");
	}

	@Test
	void jarEntryNameWithMixtureOfEncodedAndUnencodedDoubleByteCharacters() {
		assertThat(new JarEntryName(new StringSequence("%c3%a1/b/\u00c7.class")).toString())
				.isEqualTo("\u00e1/b/\u00c7.class");
	}

	@Test
	void openConnectionCanBeClosedWithoutClosingSourceJar() throws Exception {
		URL url = new URL("jar:" + this.rootJarFile.toURI().toURL() + "!/");
		JarURLConnection connection = JarURLConnection.get(url, this.jarFile);
		java.util.jar.JarFile connectionJarFile = connection.getJarFile();
		connectionJarFile.close();
		assertThat(this.jarFile.isClosed()).isFalse();
	}

	private String getRelativePath() {
		return this.rootJarFile.getPath().replace('\\', '/');
	}

}
