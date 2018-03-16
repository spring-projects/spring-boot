/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.loader.jar;

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
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.loader.TestJarCreator;
import org.springframework.boot.loader.data.RandomAccessDataFile;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link JarFile}.
 *
 * @author Phillip Webb
 * @author Martin Lau
 * @author Andy Wilkinson
 */
public class JarFileTests {

	private static final String PROTOCOL_HANDLER = "java.protocol.handler.pkgs";

	private static final String HANDLERS_PACKAGE = "org.springframework.boot.loader";

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private File rootJarFile;

	private JarFile jarFile;

	@Before
	public void setup() throws Exception {
		this.rootJarFile = this.temporaryFolder.newFile();
		TestJarCreator.createTestJar(this.rootJarFile);
		this.jarFile = new JarFile(this.rootJarFile);
	}

	@Test
	public void jdkJarFile() throws Exception {
		// Sanity checks to see how the default jar file operates
		java.util.jar.JarFile jarFile = new java.util.jar.JarFile(this.rootJarFile);
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
		assertThat(entries.hasMoreElements()).isFalse();
		URL jarUrl = new URL("jar:" + this.rootJarFile.toURI() + "!/");
		URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { jarUrl });
		assertThat(urlClassLoader.getResource("special/\u00EB.dat")).isNotNull();
		assertThat(urlClassLoader.getResource("d/9.dat")).isNotNull();
		jarFile.close();
		urlClassLoader.close();
	}

	@Test
	public void createFromFile() throws Exception {
		JarFile jarFile = new JarFile(this.rootJarFile);
		assertThat(jarFile.getName()).isNotNull();
		jarFile.close();
	}

	@Test
	public void getManifest() throws Exception {
		assertThat(this.jarFile.getManifest().getMainAttributes().getValue("Built-By"))
				.isEqualTo("j1");
	}

	@Test
	public void getManifestEntry() throws Exception {
		ZipEntry entry = this.jarFile.getJarEntry("META-INF/MANIFEST.MF");
		Manifest manifest = new Manifest(this.jarFile.getInputStream(entry));
		assertThat(manifest.getMainAttributes().getValue("Built-By")).isEqualTo("j1");
	}

	@Test
	public void getEntries() {
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
		assertThat(entries.hasMoreElements()).isFalse();
	}

	@Test
	public void getSpecialResourceViaClassLoader() throws Exception {
		URLClassLoader urlClassLoader = new URLClassLoader(
				new URL[] { this.jarFile.getUrl() });
		assertThat(urlClassLoader.getResource("special/\u00EB.dat")).isNotNull();
		urlClassLoader.close();
	}

	@Test
	public void getJarEntry() {
		java.util.jar.JarEntry entry = this.jarFile.getJarEntry("1.dat");
		assertThat(entry).isNotNull();
		assertThat(entry.getName()).isEqualTo("1.dat");
	}

	@Test
	public void getInputStream() throws Exception {
		InputStream inputStream = this.jarFile
				.getInputStream(this.jarFile.getEntry("1.dat"));
		assertThat(inputStream.available()).isEqualTo(1);
		assertThat(inputStream.read()).isEqualTo(1);
		assertThat(inputStream.available()).isEqualTo(0);
		assertThat(inputStream.read()).isEqualTo(-1);
	}

	@Test
	public void getName() {
		assertThat(this.jarFile.getName()).isEqualTo(this.rootJarFile.getPath());
	}

	@Test
	public void getSize() throws Exception {
		try (ZipFile zip = new ZipFile(this.rootJarFile)) {
			assertThat(this.jarFile.size()).isEqualTo(zip.size());
		}
	}

	@Test
	public void getEntryTime() throws Exception {
		java.util.jar.JarFile jdkJarFile = new java.util.jar.JarFile(this.rootJarFile);
		assertThat(this.jarFile.getEntry("META-INF/MANIFEST.MF").getTime())
				.isEqualTo(jdkJarFile.getEntry("META-INF/MANIFEST.MF").getTime());
		jdkJarFile.close();
	}

	@Test
	public void close() throws Exception {
		RandomAccessDataFile randomAccessDataFile = spy(
				new RandomAccessDataFile(this.rootJarFile));
		JarFile jarFile = new JarFile(randomAccessDataFile);
		jarFile.close();
		verify(randomAccessDataFile).close();
	}

	@Test
	public void getUrl() throws Exception {
		URL url = this.jarFile.getUrl();
		assertThat(url.toString()).isEqualTo("jar:" + this.rootJarFile.toURI() + "!/");
		JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
		assertThat(jarURLConnection.getJarFile()).isSameAs(this.jarFile);
		assertThat(jarURLConnection.getJarEntry()).isNull();
		assertThat(jarURLConnection.getContentLength()).isGreaterThan(1);
		assertThat(jarURLConnection.getContent()).isSameAs(this.jarFile);
		assertThat(jarURLConnection.getContentType()).isEqualTo("x-java/jar");
		assertThat(jarURLConnection.getJarFileURL().toURI())
				.isEqualTo(this.rootJarFile.toURI());
	}

	@Test
	public void createEntryUrl() throws Exception {
		URL url = new URL(this.jarFile.getUrl(), "1.dat");
		assertThat(url.toString())
				.isEqualTo("jar:" + this.rootJarFile.toURI() + "!/1.dat");
		JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
		assertThat(jarURLConnection.getJarFile()).isSameAs(this.jarFile);
		assertThat(jarURLConnection.getJarEntry())
				.isSameAs(this.jarFile.getJarEntry("1.dat"));
		assertThat(jarURLConnection.getContentLength()).isEqualTo(1);
		assertThat(jarURLConnection.getContent()).isInstanceOf(InputStream.class);
		assertThat(jarURLConnection.getContentType()).isEqualTo("content/unknown");
		assertThat(jarURLConnection.getPermission()).isInstanceOf(FilePermission.class);
		FilePermission permission = (FilePermission) jarURLConnection.getPermission();
		assertThat(permission.getActions()).isEqualTo("read");
		assertThat(permission.getName()).isEqualTo(this.rootJarFile.getPath());
	}

	@Test
	public void getMissingEntryUrl() throws Exception {
		URL url = new URL(this.jarFile.getUrl(), "missing.dat");
		assertThat(url.toString())
				.isEqualTo("jar:" + this.rootJarFile.toURI() + "!/missing.dat");
		this.thrown.expect(FileNotFoundException.class);
		((JarURLConnection) url.openConnection()).getJarEntry();
	}

	@Test
	public void getUrlStream() throws Exception {
		URL url = this.jarFile.getUrl();
		url.openConnection();
		this.thrown.expect(IOException.class);
		url.openStream();
	}

	@Test
	public void getEntryUrlStream() throws Exception {
		URL url = new URL(this.jarFile.getUrl(), "1.dat");
		url.openConnection();
		InputStream stream = url.openStream();
		assertThat(stream.read()).isEqualTo(1);
		assertThat(stream.read()).isEqualTo(-1);
	}

	@Test
	public void getNestedJarFile() throws Exception {
		JarFile nestedJarFile = this.jarFile
				.getNestedJarFile(this.jarFile.getEntry("nested.jar"));

		Enumeration<java.util.jar.JarEntry> entries = nestedJarFile.entries();
		assertThat(entries.nextElement().getName()).isEqualTo("META-INF/");
		assertThat(entries.nextElement().getName()).isEqualTo("META-INF/MANIFEST.MF");
		assertThat(entries.nextElement().getName()).isEqualTo("3.dat");
		assertThat(entries.nextElement().getName()).isEqualTo("4.dat");
		assertThat(entries.nextElement().getName()).isEqualTo("\u00E4.dat");
		assertThat(entries.hasMoreElements()).isFalse();

		InputStream inputStream = nestedJarFile
				.getInputStream(nestedJarFile.getEntry("3.dat"));
		assertThat(inputStream.read()).isEqualTo(3);
		assertThat(inputStream.read()).isEqualTo(-1);

		URL url = nestedJarFile.getUrl();
		assertThat(url.toString())
				.isEqualTo("jar:" + this.rootJarFile.toURI() + "!/nested.jar!/");
		JarURLConnection conn = (JarURLConnection) url.openConnection();
		assertThat(conn.getJarFile()).isSameAs(nestedJarFile);
		assertThat(conn.getJarFileURL().toString())
				.isEqualTo("jar:" + this.rootJarFile.toURI() + "!/nested.jar");
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

	@Test
	public void getNestedJarDirectory() throws Exception {
		JarFile nestedJarFile = this.jarFile
				.getNestedJarFile(this.jarFile.getEntry("d/"));

		Enumeration<java.util.jar.JarEntry> entries = nestedJarFile.entries();
		assertThat(entries.nextElement().getName()).isEqualTo("9.dat");
		assertThat(entries.hasMoreElements()).isFalse();

		InputStream inputStream = nestedJarFile
				.getInputStream(nestedJarFile.getEntry("9.dat"));
		assertThat(inputStream.read()).isEqualTo(9);
		assertThat(inputStream.read()).isEqualTo(-1);

		URL url = nestedJarFile.getUrl();
		assertThat(url.toString()).isEqualTo("jar:" + this.rootJarFile.toURI() + "!/d!/");
		assertThat(((JarURLConnection) url.openConnection()).getJarFile())
				.isSameAs(nestedJarFile);
	}

	@Test
	public void getNestedJarEntryUrl() throws Exception {
		JarFile nestedJarFile = this.jarFile
				.getNestedJarFile(this.jarFile.getEntry("nested.jar"));
		URL url = nestedJarFile.getJarEntry("3.dat").getUrl();
		assertThat(url.toString())
				.isEqualTo("jar:" + this.rootJarFile.toURI() + "!/nested.jar!/3.dat");
		InputStream inputStream = url.openStream();
		assertThat(inputStream).isNotNull();
		assertThat(inputStream.read()).isEqualTo(3);
	}

	@Test
	public void createUrlFromString() throws Exception {
		JarFile.registerUrlProtocolHandler();
		String spec = "jar:" + this.rootJarFile.toURI() + "!/nested.jar!/3.dat";
		URL url = new URL(spec);
		assertThat(url.toString()).isEqualTo(spec);
		InputStream inputStream = url.openStream();
		assertThat(inputStream).isNotNull();
		assertThat(inputStream.read()).isEqualTo(3);
		JarURLConnection connection = (JarURLConnection) url.openConnection();
		assertThat(connection.getURL().toString()).isEqualTo(spec);
		assertThat(connection.getJarFileURL().toString())
				.isEqualTo("jar:" + this.rootJarFile.toURI() + "!/nested.jar");
		assertThat(connection.getEntryName()).isEqualTo("3.dat");
	}

	@Test
	public void createNonNestedUrlFromString() throws Exception {
		nonNestedJarFileFromString("jar:" + this.rootJarFile.toURI() + "!/2.dat");
	}

	@Test
	public void createNonNestedUrlFromPathString() throws Exception {
		nonNestedJarFileFromString(
				"jar:" + this.rootJarFile.toPath().toUri() + "!/2.dat");
	}

	private void nonNestedJarFileFromString(String spec) throws Exception {
		JarFile.registerUrlProtocolHandler();
		URL url = new URL(spec);
		assertThat(url.toString()).isEqualTo(spec);
		InputStream inputStream = url.openStream();
		assertThat(inputStream).isNotNull();
		assertThat(inputStream.read()).isEqualTo(2);
		JarURLConnection connection = (JarURLConnection) url.openConnection();
		assertThat(connection.getURL().toString()).isEqualTo(spec);
		assertThat(connection.getJarFileURL().toURI())
				.isEqualTo(this.rootJarFile.toURI());
		assertThat(connection.getEntryName()).isEqualTo("2.dat");
	}

	@Test
	public void getDirectoryInputStream() throws Exception {
		InputStream inputStream = this.jarFile
				.getInputStream(this.jarFile.getEntry("d/"));
		assertThat(inputStream).isNotNull();
		assertThat(inputStream.read()).isEqualTo(-1);
	}

	@Test
	public void getDirectoryInputStreamWithoutSlash() throws Exception {
		InputStream inputStream = this.jarFile.getInputStream(this.jarFile.getEntry("d"));
		assertThat(inputStream).isNotNull();
		assertThat(inputStream.read()).isEqualTo(-1);
	}

	@Test
	public void sensibleToString() throws Exception {
		assertThat(this.jarFile.toString()).isEqualTo(this.rootJarFile.getPath());
		assertThat(this.jarFile.getNestedJarFile(this.jarFile.getEntry("nested.jar"))
				.toString()).isEqualTo(this.rootJarFile.getPath() + "!/nested.jar");
	}

	@Test
	public void verifySignedJar() throws Exception {
		String classpath = System.getProperty("java.class.path");
		String[] entries = classpath.split(System.getProperty("path.separator"));
		String signedJarFile = null;
		for (String entry : entries) {
			if (entry.contains("bcprov")) {
				signedJarFile = entry;
			}
		}
		assertThat(signedJarFile).isNotNull();
		java.util.jar.JarFile jarFile = new JarFile(new File(signedJarFile));
		jarFile.getManifest();
		Enumeration<JarEntry> jarEntries = jarFile.entries();
		while (jarEntries.hasMoreElements()) {
			JarEntry jarEntry = jarEntries.nextElement();
			InputStream inputStream = jarFile.getInputStream(jarEntry);
			inputStream.skip(Long.MAX_VALUE);
			inputStream.close();
			if (!jarEntry.getName().startsWith("META-INF") && !jarEntry.isDirectory()
					&& !jarEntry.getName().endsWith("TigerDigest.class")) {
				assertThat(jarEntry.getCertificates()).isNotNull();
			}
		}
		jarFile.close();
	}

	@Test
	public void jarFileWithScriptAtTheStart() throws Exception {
		File file = this.temporaryFolder.newFile();
		InputStream sourceJarContent = new FileInputStream(this.rootJarFile);
		FileOutputStream outputStream = new FileOutputStream(file);
		StreamUtils.copy("#/bin/bash", Charset.defaultCharset(), outputStream);
		FileCopyUtils.copy(sourceJarContent, outputStream);
		this.rootJarFile = file;
		this.jarFile = new JarFile(file);
		// Call some other tests to verify
		getEntries();
		getNestedJarFile();
	}

	@Test
	public void cannotLoadMissingJar() throws Exception {
		// relates to gh-1070
		JarFile nestedJarFile = this.jarFile
				.getNestedJarFile(this.jarFile.getEntry("nested.jar"));
		URL nestedUrl = nestedJarFile.getUrl();
		URL url = new URL(nestedUrl, nestedJarFile.getUrl() + "missing.jar!/3.dat");
		this.thrown.expect(FileNotFoundException.class);
		url.openConnection().getInputStream();
	}

	@Test
	public void registerUrlProtocolHandlerWithNoExistingRegistration() {
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
	public void registerUrlProtocolHandlerAddsToExistingRegistration() {
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
	public void jarFileCanBeDeletedOnceItHasBeenClosed() throws Exception {
		File temp = this.temporaryFolder.newFile();
		TestJarCreator.createTestJar(temp);
		JarFile jf = new JarFile(temp);
		jf.close();
		assertThat(temp.delete()).isTrue();
	}

}
