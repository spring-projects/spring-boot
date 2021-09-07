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

package org.springframework.boot.loader.jar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.TestJarCreator;
import org.springframework.boot.loader.data.RandomAccessDataFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StopWatch;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link JarFile}.
 *
 * @author Phillip Webb
 * @author Martin Lau
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
@ExtendWith(JarUrlProtocolHandler.class)
class JarFileTests {

	private static final String PROTOCOL_HANDLER = "java.protocol.handler.pkgs";

	private static final String HANDLERS_PACKAGE = "org.springframework.boot.loader";

	@TempDir
	File tempDir;

	private File rootJarFile;

	private JarFile jarFile;

	@BeforeEach
	void setup() throws Exception {
		this.rootJarFile = new File(this.tempDir, "root.jar");
		TestJarCreator.createTestJar(this.rootJarFile);
		this.jarFile = new JarFile(this.rootJarFile);
	}

	@AfterEach
	void tearDown() throws Exception {
		this.jarFile.close();
	}

	@Test
	void jdkJarFile() throws Exception {
		// Sanity checks to see how the default jar file operates
		java.util.jar.JarFile jarFile = new java.util.jar.JarFile(this.rootJarFile);
		assertThat(jarFile.getComment()).isEqualTo("outer");
		Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();
		assertThat(entries.nextElement().getName()).isEqualTo("META-INF/");
		assertThat(entries.nextElement().getName()).isEqualTo("META-INF/MANIFEST.MF");
		assertThat(entries.nextElement().getName()).isEqualTo("1.dat");
		assertThat(entries.nextElement().getName()).isEqualTo("2.dat");
		assertThat(entries.nextElement().getName()).isEqualTo("d/");
		assertThat(entries.nextElement().getName()).isEqualTo("d/9.dat");
		assertThat(entries.nextElement().getName()).isEqualTo("special/");
		assertThat(entries.nextElement().getName()).isEqualTo("special/\u00EB.dat");
		assertThat(entries.nextElement().getName()).isEqualTo("nested.jar");
		assertThat(entries.nextElement().getName()).isEqualTo("another-nested.jar");
		assertThat(entries.nextElement().getName()).isEqualTo("space nested.jar");
		assertThat(entries.nextElement().getName()).isEqualTo("multi-release.jar");
		assertThat(entries.hasMoreElements()).isFalse();
		URL jarUrl = new URL("jar:" + this.rootJarFile.toURI() + "!/");
		URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { jarUrl });
		assertThat(urlClassLoader.getResource("special/\u00EB.dat")).isNotNull();
		assertThat(urlClassLoader.getResource("d/9.dat")).isNotNull();
		urlClassLoader.close();
		jarFile.close();
	}

	@Test
	void createFromFile() throws Exception {
		JarFile jarFile = new JarFile(this.rootJarFile);
		assertThat(jarFile.getName()).isNotNull();
		jarFile.close();
	}

	@Test
	void getManifest() throws Exception {
		assertThat(this.jarFile.getManifest().getMainAttributes().getValue("Built-By")).isEqualTo("j1");
	}

	@Test
	void getManifestEntry() throws Exception {
		ZipEntry entry = this.jarFile.getJarEntry("META-INF/MANIFEST.MF");
		Manifest manifest = new Manifest(this.jarFile.getInputStream(entry));
		assertThat(manifest.getMainAttributes().getValue("Built-By")).isEqualTo("j1");
	}

	@Test
	void getEntries() {
		Enumeration<java.util.jar.JarEntry> entries = this.jarFile.entries();
		assertThat(entries.nextElement().getName()).isEqualTo("META-INF/");
		assertThat(entries.nextElement().getName()).isEqualTo("META-INF/MANIFEST.MF");
		assertThat(entries.nextElement().getName()).isEqualTo("1.dat");
		assertThat(entries.nextElement().getName()).isEqualTo("2.dat");
		assertThat(entries.nextElement().getName()).isEqualTo("d/");
		assertThat(entries.nextElement().getName()).isEqualTo("d/9.dat");
		assertThat(entries.nextElement().getName()).isEqualTo("special/");
		assertThat(entries.nextElement().getName()).isEqualTo("special/\u00EB.dat");
		assertThat(entries.nextElement().getName()).isEqualTo("nested.jar");
		assertThat(entries.nextElement().getName()).isEqualTo("another-nested.jar");
		assertThat(entries.nextElement().getName()).isEqualTo("space nested.jar");
		assertThat(entries.nextElement().getName()).isEqualTo("multi-release.jar");
		assertThat(entries.hasMoreElements()).isFalse();
	}

	@Test
	void getSpecialResourceViaClassLoader() throws Exception {
		URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { this.jarFile.getUrl() });
		assertThat(urlClassLoader.getResource("special/\u00EB.dat")).isNotNull();
		urlClassLoader.close();
	}

	@Test
	void getJarEntry() {
		java.util.jar.JarEntry entry = this.jarFile.getJarEntry("1.dat");
		assertThat(entry).isNotNull();
		assertThat(entry.getName()).isEqualTo("1.dat");
	}

	@Test
	void getJarEntryWhenClosed() throws Exception {
		this.jarFile.close();
		assertThatZipFileClosedIsThrownBy(() -> this.jarFile.getJarEntry("1.dat"));
	}

	@Test
	void getInputStream() throws Exception {
		InputStream inputStream = this.jarFile.getInputStream(this.jarFile.getEntry("1.dat"));
		assertThat(inputStream.available()).isEqualTo(1);
		assertThat(inputStream.read()).isEqualTo(1);
		assertThat(inputStream.available()).isEqualTo(0);
		assertThat(inputStream.read()).isEqualTo(-1);
	}

	@Test
	void getInputStreamWhenClosed() throws Exception {
		ZipEntry entry = this.jarFile.getEntry("1.dat");
		this.jarFile.close();
		assertThatZipFileClosedIsThrownBy(() -> this.jarFile.getInputStream(entry));
	}

	@Test
	void getComment() {
		assertThat(this.jarFile.getComment()).isEqualTo("outer");
	}

	@Test
	void getCommentWhenClosed() throws Exception {
		this.jarFile.close();
		assertThatZipFileClosedIsThrownBy(() -> this.jarFile.getComment());
	}

	@Test
	void getName() {
		assertThat(this.jarFile.getName()).isEqualTo(this.rootJarFile.getPath());
	}

	@Test
	void size() throws Exception {
		try (ZipFile zip = new ZipFile(this.rootJarFile)) {
			assertThat(this.jarFile.size()).isEqualTo(zip.size());
		}
	}

	@Test
	void sizeWhenClosed() throws Exception {
		this.jarFile.close();
		assertThatZipFileClosedIsThrownBy(() -> this.jarFile.size());
	}

	@Test
	void getEntryTime() throws Exception {
		java.util.jar.JarFile jdkJarFile = new java.util.jar.JarFile(this.rootJarFile);
		assertThat(this.jarFile.getEntry("META-INF/MANIFEST.MF").getTime())
				.isEqualTo(jdkJarFile.getEntry("META-INF/MANIFEST.MF").getTime());
		jdkJarFile.close();
	}

	@Test
	void close() throws Exception {
		RandomAccessDataFile randomAccessDataFile = spy(new RandomAccessDataFile(this.rootJarFile));
		JarFile jarFile = new JarFile(randomAccessDataFile);
		jarFile.close();
		verify(randomAccessDataFile).close();
	}

	@Test
	void getUrl() throws Exception {
		URL url = this.jarFile.getUrl();
		assertThat(url.toString()).isEqualTo("jar:" + this.rootJarFile.toURI() + "!/");
		JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
		assertThat(JarFileWrapper.unwrap(jarURLConnection.getJarFile())).isSameAs(this.jarFile);
		assertThat(jarURLConnection.getJarEntry()).isNull();
		assertThat(jarURLConnection.getContentLength()).isGreaterThan(1);
		assertThat(JarFileWrapper.unwrap((java.util.jar.JarFile) jarURLConnection.getContent())).isSameAs(this.jarFile);
		assertThat(jarURLConnection.getContentType()).isEqualTo("x-java/jar");
		assertThat(jarURLConnection.getJarFileURL().toURI()).isEqualTo(this.rootJarFile.toURI());
	}

	@Test
	void createEntryUrl() throws Exception {
		URL url = new URL(this.jarFile.getUrl(), "1.dat");
		assertThat(url.toString()).isEqualTo("jar:" + this.rootJarFile.toURI() + "!/1.dat");
		JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
		assertThat(JarFileWrapper.unwrap(jarURLConnection.getJarFile())).isSameAs(this.jarFile);
		assertThat(jarURLConnection.getJarEntry()).isSameAs(this.jarFile.getJarEntry("1.dat"));
		assertThat(jarURLConnection.getContentLength()).isEqualTo(1);
		assertThat(jarURLConnection.getContent()).isInstanceOf(InputStream.class);
		assertThat(jarURLConnection.getContentType()).isEqualTo("content/unknown");
		assertThat(jarURLConnection.getPermission()).isInstanceOf(FilePermission.class);
		FilePermission permission = (FilePermission) jarURLConnection.getPermission();
		assertThat(permission.getActions()).isEqualTo("read");
		assertThat(permission.getName()).isEqualTo(this.rootJarFile.getPath());
	}

	@Test
	void getMissingEntryUrl() throws Exception {
		URL url = new URL(this.jarFile.getUrl(), "missing.dat");
		assertThat(url.toString()).isEqualTo("jar:" + this.rootJarFile.toURI() + "!/missing.dat");
		assertThatExceptionOfType(FileNotFoundException.class)
				.isThrownBy(((JarURLConnection) url.openConnection())::getJarEntry);
	}

	@Test
	void getUrlStream() throws Exception {
		URL url = this.jarFile.getUrl();
		url.openConnection();
		assertThatIOException().isThrownBy(url::openStream);
	}

	@Test
	void getEntryUrlStream() throws Exception {
		URL url = new URL(this.jarFile.getUrl(), "1.dat");
		url.openConnection();
		InputStream stream = url.openStream();
		assertThat(stream.read()).isEqualTo(1);
		assertThat(stream.read()).isEqualTo(-1);
	}

	@Test
	void getNestedJarFile() throws Exception {
		try (JarFile nestedJarFile = this.jarFile.getNestedJarFile(this.jarFile.getEntry("nested.jar"))) {
			assertThat(nestedJarFile.getComment()).isEqualTo("nested");
			Enumeration<java.util.jar.JarEntry> entries = nestedJarFile.entries();
			assertThat(entries.nextElement().getName()).isEqualTo("META-INF/");
			assertThat(entries.nextElement().getName()).isEqualTo("META-INF/MANIFEST.MF");
			assertThat(entries.nextElement().getName()).isEqualTo("3.dat");
			assertThat(entries.nextElement().getName()).isEqualTo("4.dat");
			assertThat(entries.nextElement().getName()).isEqualTo("\u00E4.dat");
			assertThat(entries.hasMoreElements()).isFalse();

			InputStream inputStream = nestedJarFile.getInputStream(nestedJarFile.getEntry("3.dat"));
			assertThat(inputStream.read()).isEqualTo(3);
			assertThat(inputStream.read()).isEqualTo(-1);

			URL url = nestedJarFile.getUrl();
			assertThat(url.toString()).isEqualTo("jar:" + this.rootJarFile.toURI() + "!/nested.jar!/");
			JarURLConnection conn = (JarURLConnection) url.openConnection();
			assertThat(JarFileWrapper.unwrap(conn.getJarFile())).isSameAs(nestedJarFile);
			assertThat(conn.getJarFileURL().toString()).isEqualTo("jar:" + this.rootJarFile.toURI() + "!/nested.jar");
			assertThat(conn.getInputStream()).isNotNull();
			JarInputStream jarInputStream = new JarInputStream(conn.getInputStream());
			assertThat(jarInputStream.getNextJarEntry().getName()).isEqualTo("3.dat");
			assertThat(jarInputStream.getNextJarEntry().getName()).isEqualTo("4.dat");
			assertThat(jarInputStream.getNextJarEntry().getName()).isEqualTo("\u00E4.dat");
			jarInputStream.close();
			assertThat(conn.getPermission()).isInstanceOf(FilePermission.class);
			FilePermission permission = (FilePermission) conn.getPermission();
			assertThat(permission.getActions()).isEqualTo("read");
			assertThat(permission.getName()).isEqualTo(this.rootJarFile.getPath());
		}
	}

	@Test
	void getNestedJarDirectory() throws Exception {
		try (JarFile nestedJarFile = this.jarFile.getNestedJarFile(this.jarFile.getEntry("d/"))) {
			Enumeration<java.util.jar.JarEntry> entries = nestedJarFile.entries();
			assertThat(entries.nextElement().getName()).isEqualTo("9.dat");
			assertThat(entries.hasMoreElements()).isFalse();

			try (InputStream inputStream = nestedJarFile.getInputStream(nestedJarFile.getEntry("9.dat"))) {
				assertThat(inputStream.read()).isEqualTo(9);
				assertThat(inputStream.read()).isEqualTo(-1);
			}

			URL url = nestedJarFile.getUrl();
			assertThat(url.toString()).isEqualTo("jar:" + this.rootJarFile.toURI() + "!/d!/");
			JarURLConnection connection = (JarURLConnection) url.openConnection();
			assertThat(JarFileWrapper.unwrap(connection.getJarFile())).isSameAs(nestedJarFile);
		}
	}

	@Test
	void getNestedJarEntryUrl() throws Exception {
		try (JarFile nestedJarFile = this.jarFile.getNestedJarFile(this.jarFile.getEntry("nested.jar"))) {
			URL url = nestedJarFile.getJarEntry("3.dat").getUrl();
			assertThat(url.toString()).isEqualTo("jar:" + this.rootJarFile.toURI() + "!/nested.jar!/3.dat");
			try (InputStream inputStream = url.openStream()) {
				assertThat(inputStream).isNotNull();
				assertThat(inputStream.read()).isEqualTo(3);
			}
		}
	}

	@Test
	void createUrlFromString() throws Exception {
		String spec = "jar:" + this.rootJarFile.toURI() + "!/nested.jar!/3.dat";
		URL url = new URL(spec);
		assertThat(url.toString()).isEqualTo(spec);
		JarURLConnection connection = (JarURLConnection) url.openConnection();
		try (InputStream inputStream = connection.getInputStream()) {
			assertThat(inputStream).isNotNull();
			assertThat(inputStream.read()).isEqualTo(3);
			assertThat(connection.getURL().toString()).isEqualTo(spec);
			assertThat(connection.getJarFileURL().toString())
					.isEqualTo("jar:" + this.rootJarFile.toURI() + "!/nested.jar");
			assertThat(connection.getEntryName()).isEqualTo("3.dat");
			connection.getJarFile().close();
		}
	}

	@Test
	void createNonNestedUrlFromString() throws Exception {
		nonNestedJarFileFromString("jar:" + this.rootJarFile.toURI() + "!/2.dat");
	}

	@Test
	void createNonNestedUrlFromPathString() throws Exception {
		nonNestedJarFileFromString("jar:" + this.rootJarFile.toPath().toUri() + "!/2.dat");
	}

	private void nonNestedJarFileFromString(String spec) throws Exception {
		JarFile.registerUrlProtocolHandler();
		URL url = new URL(spec);
		assertThat(url.toString()).isEqualTo(spec);
		JarURLConnection connection = (JarURLConnection) url.openConnection();
		try (InputStream inputStream = connection.getInputStream()) {
			assertThat(inputStream).isNotNull();
			assertThat(inputStream.read()).isEqualTo(2);
			assertThat(connection.getURL().toString()).isEqualTo(spec);
			assertThat(connection.getJarFileURL().toURI()).isEqualTo(this.rootJarFile.toURI());
			assertThat(connection.getEntryName()).isEqualTo("2.dat");
		}
		connection.getJarFile().close();
	}

	@Test
	void getDirectoryInputStream() throws Exception {
		InputStream inputStream = this.jarFile.getInputStream(this.jarFile.getEntry("d/"));
		assertThat(inputStream).isNotNull();
		assertThat(inputStream.read()).isEqualTo(-1);
	}

	@Test
	void getDirectoryInputStreamWithoutSlash() throws Exception {
		InputStream inputStream = this.jarFile.getInputStream(this.jarFile.getEntry("d"));
		assertThat(inputStream).isNotNull();
		assertThat(inputStream.read()).isEqualTo(-1);
	}

	@Test
	void sensibleToString() throws Exception {
		assertThat(this.jarFile.toString()).isEqualTo(this.rootJarFile.getPath());
		try (JarFile nested = this.jarFile.getNestedJarFile(this.jarFile.getEntry("nested.jar"))) {
			assertThat(nested.toString()).isEqualTo(this.rootJarFile.getPath() + "!/nested.jar");
		}
	}

	@Test
	void verifySignedJar() throws Exception {
		File signedJarFile = getSignedJarFile();
		assertThat(signedJarFile).exists();
		try (java.util.jar.JarFile expected = new java.util.jar.JarFile(signedJarFile)) {
			try (JarFile actual = new JarFile(signedJarFile)) {
				StopWatch stopWatch = new StopWatch();
				Enumeration<JarEntry> actualEntries = actual.entries();
				while (actualEntries.hasMoreElements()) {
					JarEntry actualEntry = actualEntries.nextElement();
					java.util.jar.JarEntry expectedEntry = expected.getJarEntry(actualEntry.getName());
					StreamUtils.drain(expected.getInputStream(expectedEntry));
					if (!actualEntry.getName().equals("META-INF/MANIFEST.MF")) {
						assertThat(actualEntry.getCertificates()).as(actualEntry.getName())
								.isEqualTo(expectedEntry.getCertificates());
						assertThat(actualEntry.getCodeSigners()).as(actualEntry.getName())
								.isEqualTo(expectedEntry.getCodeSigners());
					}
				}
				assertThat(stopWatch.getTotalTimeSeconds()).isLessThan(3.0);
			}
		}
	}

	private File getSignedJarFile() {
		String[] entries = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
		for (String entry : entries) {
			if (entry.contains("bcprov")) {
				return new File(entry);
			}
		}
		return null;
	}

	@Test
	void jarFileWithScriptAtTheStart() throws Exception {
		File file = new File(this.tempDir, "test.jar");
		InputStream sourceJarContent = new FileInputStream(this.rootJarFile);
		FileOutputStream outputStream = new FileOutputStream(file);
		StreamUtils.copy("#/bin/bash", Charset.defaultCharset(), outputStream);
		FileCopyUtils.copy(sourceJarContent, outputStream);
		this.rootJarFile = file;
		this.jarFile.close();
		this.jarFile = new JarFile(file);
		// Call some other tests to verify
		getEntries();
		getNestedJarFile();
	}

	@Test
	void cannotLoadMissingJar() throws Exception {
		// relates to gh-1070
		try (JarFile nestedJarFile = this.jarFile.getNestedJarFile(this.jarFile.getEntry("nested.jar"))) {
			URL nestedUrl = nestedJarFile.getUrl();
			URL url = new URL(nestedUrl, nestedJarFile.getUrl() + "missing.jar!/3.dat");
			assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(url.openConnection()::getInputStream);
		}
	}

	@Test
	void registerUrlProtocolHandlerWithNoExistingRegistration() {
		String original = System.getProperty(PROTOCOL_HANDLER);
		try {
			System.clearProperty(PROTOCOL_HANDLER);
			JarFile.registerUrlProtocolHandler();
			String protocolHandler = System.getProperty(PROTOCOL_HANDLER);
			assertThat(protocolHandler).isEqualTo(HANDLERS_PACKAGE);
		}
		finally {
			if (original == null) {
				System.clearProperty(PROTOCOL_HANDLER);
			}
			else {
				System.setProperty(PROTOCOL_HANDLER, original);
			}
		}
	}

	@Test
	void registerUrlProtocolHandlerAddsToExistingRegistration() {
		String original = System.getProperty(PROTOCOL_HANDLER);
		try {
			System.setProperty(PROTOCOL_HANDLER, "com.example");
			JarFile.registerUrlProtocolHandler();
			String protocolHandler = System.getProperty(PROTOCOL_HANDLER);
			assertThat(protocolHandler).isEqualTo("com.example|" + HANDLERS_PACKAGE);
		}
		finally {
			if (original == null) {
				System.clearProperty(PROTOCOL_HANDLER);
			}
			else {
				System.setProperty(PROTOCOL_HANDLER, original);
			}
		}
	}

	@Test
	void jarFileCanBeDeletedOnceItHasBeenClosed() throws Exception {
		File jar = new File(this.tempDir, "test.jar");
		TestJarCreator.createTestJar(jar);
		JarFile jf = new JarFile(jar);
		jf.close();
		assertThat(jar.delete()).isTrue();
	}

	@Test
	void createUrlFromStringWithContextWhenNotFound() throws Exception {
		// gh-12483
		JarURLConnection.setUseFastExceptions(true);
		try {
			try (JarFile nested = this.jarFile.getNestedJarFile(this.jarFile.getEntry("nested.jar"))) {
				URL context = nested.getUrl();
				new URL(context, "jar:" + this.rootJarFile.toURI() + "!/nested.jar!/3.dat").openConnection()
						.getInputStream().close();
				assertThatExceptionOfType(FileNotFoundException.class)
						.isThrownBy(new URL(context, "jar:" + this.rootJarFile.toURI() + "!/no.dat")
								.openConnection()::getInputStream);
			}
		}
		finally {
			JarURLConnection.setUseFastExceptions(false);
		}
	}

	@Test
	void multiReleaseEntry() throws Exception {
		try (JarFile multiRelease = this.jarFile.getNestedJarFile(this.jarFile.getEntry("multi-release.jar"))) {
			ZipEntry entry = multiRelease.getEntry("multi-release.dat");
			assertThat(entry.getName()).isEqualTo("multi-release.dat");
			InputStream inputStream = multiRelease.getInputStream(entry);
			assertThat(inputStream.available()).isEqualTo(1);
			assertThat(inputStream.read()).isEqualTo(getJavaVersion());
		}
	}

	@Test
	void zip64JarThatExceedsZipEntryLimitCanBeRead() throws Exception {
		File zip64Jar = new File(this.tempDir, "zip64.jar");
		FileCopyUtils.copy(zip64Jar(), zip64Jar);
		try (JarFile zip64JarFile = new JarFile(zip64Jar)) {
			List<JarEntry> entries = Collections.list(zip64JarFile.entries());
			assertThat(entries).hasSize(65537);
			for (int i = 0; i < entries.size(); i++) {
				JarEntry entry = entries.get(i);
				InputStream entryInput = zip64JarFile.getInputStream(entry);
				assertThat(entryInput).hasContent("Entry " + (i + 1));
			}
		}
	}

	@Test
	void zip64JarThatExceedsZipSizeLimitCanBeRead() throws Exception {
		Assumptions.assumeTrue(this.tempDir.getFreeSpace() > 6 * 1024 * 1024 * 1024, "Insufficient disk space");
		File zip64Jar = new File(this.tempDir, "zip64.jar");
		File entry = new File(this.tempDir, "entry.dat");
		CRC32 crc32 = new CRC32();
		try (FileOutputStream entryOut = new FileOutputStream(entry)) {
			byte[] data = new byte[1024 * 1024];
			new Random().nextBytes(data);
			for (int i = 0; i < 1024; i++) {
				entryOut.write(data);
				crc32.update(data);
			}
		}
		try (JarOutputStream jarOutput = new JarOutputStream(new FileOutputStream(zip64Jar))) {
			for (int i = 0; i < 6; i++) {
				JarEntry storedEntry = new JarEntry("huge-" + i);
				storedEntry.setSize(entry.length());
				storedEntry.setCompressedSize(entry.length());
				storedEntry.setCrc(crc32.getValue());
				storedEntry.setMethod(ZipEntry.STORED);
				jarOutput.putNextEntry(storedEntry);
				try (FileInputStream entryIn = new FileInputStream(entry)) {
					StreamUtils.copy(entryIn, jarOutput);
				}
				jarOutput.closeEntry();
			}
		}
		try (JarFile zip64JarFile = new JarFile(zip64Jar)) {
			assertThat(Collections.list(zip64JarFile.entries())).hasSize(6);
		}
	}

	@Test
	void nestedZip64JarCanBeRead() throws Exception {
		File outer = new File(this.tempDir, "outer.jar");
		try (JarOutputStream jarOutput = new JarOutputStream(new FileOutputStream(outer))) {
			JarEntry nestedEntry = new JarEntry("nested-zip64.jar");
			byte[] contents = zip64Jar();
			nestedEntry.setSize(contents.length);
			nestedEntry.setCompressedSize(contents.length);
			CRC32 crc32 = new CRC32();
			crc32.update(contents);
			nestedEntry.setCrc(crc32.getValue());
			nestedEntry.setMethod(ZipEntry.STORED);
			jarOutput.putNextEntry(nestedEntry);
			jarOutput.write(contents);
			jarOutput.closeEntry();
		}
		try (JarFile outerJarFile = new JarFile(outer)) {
			try (JarFile nestedZip64JarFile = outerJarFile
					.getNestedJarFile(outerJarFile.getJarEntry("nested-zip64.jar"))) {
				List<JarEntry> entries = Collections.list(nestedZip64JarFile.entries());
				assertThat(entries).hasSize(65537);
				for (int i = 0; i < entries.size(); i++) {
					JarEntry entry = entries.get(i);
					InputStream entryInput = nestedZip64JarFile.getInputStream(entry);
					assertThat(entryInput).hasContent("Entry " + (i + 1));
				}
			}
		}
	}

	private byte[] zip64Jar() throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		JarOutputStream jarOutput = new JarOutputStream(bytes);
		for (int i = 0; i < 65537; i++) {
			jarOutput.putNextEntry(new JarEntry(i + ".dat"));
			jarOutput.write(("Entry " + (i + 1)).getBytes(StandardCharsets.UTF_8));
			jarOutput.closeEntry();
		}
		jarOutput.close();
		return bytes.toByteArray();
	}

	@Test
	void jarFileEntryWithEpochTimeOfZeroShouldNotFail() throws Exception {
		File file = new File(this.tempDir, "timed.jar");
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		try (JarOutputStream jarOutputStream = new JarOutputStream(fileOutputStream)) {
			jarOutputStream.setComment("outer");
			JarEntry entry = new JarEntry("1.dat");
			entry.setLastModifiedTime(FileTime.from(Instant.EPOCH));
			ReflectionTestUtils.setField(entry, "xdostime", 0);
			jarOutputStream.putNextEntry(entry);
			jarOutputStream.write(new byte[] { (byte) 1 });
			jarOutputStream.closeEntry();
		}
		try (JarFile jar = new JarFile(file)) {
			Enumeration<java.util.jar.JarEntry> entries = jar.entries();
			JarEntry entry = entries.nextElement();
			assertThat(entry.getLastModifiedTime().toInstant()).isEqualTo(Instant.EPOCH);
			assertThat(entry.getName()).isEqualTo("1.dat");
		}
	}

	@Test
	void iterator() {
		Iterator<JarEntry> iterator = this.jarFile.iterator();
		List<String> names = new ArrayList<>();
		while (iterator.hasNext()) {
			names.add(iterator.next().getName());
		}
		assertThat(names).hasSize(12).contains("1.dat");
	}

	@Test
	void iteratorWhenClosed() throws IOException {
		this.jarFile.close();
		assertThatZipFileClosedIsThrownBy(() -> this.jarFile.iterator());
	}

	@Test
	void iteratorWhenClosedLater() throws IOException {
		Iterator<JarEntry> iterator = this.jarFile.iterator();
		iterator.next();
		this.jarFile.close();
		assertThatZipFileClosedIsThrownBy(() -> iterator.hasNext());
	}

	@Test
	void stream() {
		Stream<String> stream = this.jarFile.stream().map(JarEntry::getName);
		assertThat(stream).hasSize(12).contains("1.dat");

	}

	private void assertThatZipFileClosedIsThrownBy(ThrowingCallable throwingCallable) {
		assertThatIllegalStateException().isThrownBy(throwingCallable).withMessage("zip file closed");
	}

	private int getJavaVersion() {
		try {
			Object runtimeVersion = Runtime.class.getMethod("version").invoke(null);
			return (int) runtimeVersion.getClass().getMethod("major").invoke(runtimeVersion);
		}
		catch (Throwable ex) {
			return 8;
		}
	}

}
