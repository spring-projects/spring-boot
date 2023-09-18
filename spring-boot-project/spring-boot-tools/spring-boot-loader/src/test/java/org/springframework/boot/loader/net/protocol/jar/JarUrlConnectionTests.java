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

package org.springframework.boot.loader.net.protocol.jar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.Permission;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.net.protocol.Handlers;
import org.springframework.boot.loader.testsupport.TestJar;
import org.springframework.boot.loader.zip.AssertFileChannelDataBlocksClosed;
import org.springframework.boot.loader.zip.ZipContent;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link JarUrlConnection}.
 *
 * @author Phillip Webb
 */
@AssertFileChannelDataBlocksClosed
class JarUrlConnectionTests {

	@TempDir
	File temp;

	private File file;

	private URL url;

	@BeforeAll
	static void registerHandlers() {
		Handlers.register();
	}

	@BeforeEach
	@AfterEach
	void reset() {
		JarUrlConnection.clearCache();
		Optimizations.disable();
	}

	@BeforeEach
	void setup() throws Exception {
		this.file = new File(this.temp, "test.jar");
		TestJar.create(this.file);
		this.url = JarUrl.create(this.file, "nested.jar");
	}

	@Test
	void getJarFileReturnsJarFile() throws Exception {
		JarUrlConnection connection = JarUrlConnection.open(this.url);
		JarFile jarFile = connection.getJarFile();
		assertThat(jarFile).isNotNull();
		assertThat(jarFile.getEntry("3.dat")).isNotNull();
	}

	@Test
	void getJarEntryReturnsJarEntry() throws Exception {
		URL url = JarUrl.create(this.file, "nested.jar", "3.dat");
		JarUrlConnection connection = JarUrlConnection.open(url);
		JarEntry entry = connection.getJarEntry();
		assertThat(entry).isNotNull();
		assertThat(entry.getName()).isEqualTo("3.dat");
	}

	@Test
	void getJarEntryWhenHasNoEntryNameReturnsNull() throws Exception {
		JarUrlConnection connection = JarUrlConnection.open(this.url);
		JarEntry entry = connection.getJarEntry();
		assertThat(entry).isNull();
	}

	@Test
	void getContentLengthReturnsContentLength() throws Exception {
		JarUrlConnection connection = JarUrlConnection.open(this.url);
		try (ZipContent content = ZipContent.open(this.file.toPath())) {
			int expected = content.getEntry("nested.jar").getUncompressedSize();
			assertThat(connection.getContentLength()).isEqualTo(expected);
		}
	}

	@Test
	void getContentLengthWhenLengthIsLargerThanMaxIntReturnsMinusOne() {
		JarUrlConnection connection = mock(JarUrlConnection.class);
		given(connection.getContentLength()).willCallRealMethod();
		given(connection.getContentLengthLong()).willReturn((long) Integer.MAX_VALUE + 1);
		assertThat(connection.getContentLength()).isEqualTo(-1);
	}

	@Test
	void getContentLengthLongWhenHasNoEntryReturnsSizeOfJar() throws Exception {
		JarUrlConnection connection = JarUrlConnection.open(this.url);
		try (ZipContent content = ZipContent.open(this.file.toPath())) {
			int expected = content.getEntry("nested.jar").getUncompressedSize();
			assertThat(connection.getContentLengthLong()).isEqualTo(expected);
		}
	}

	@Test
	void getContentLengthLongWhenHasEntryReturnsEntrySize() throws Exception {
		URL url = JarUrl.create(this.file, "nested.jar", "3.dat");
		JarUrlConnection connection = JarUrlConnection.open(url);
		assertThat(connection.getContentLengthLong()).isEqualTo(1);
	}

	@Test
	void getContentLengthLongWhenCannotConnectReturnsMinusOne() throws IOException {
		JarUrlConnection connection = mock(JarUrlConnection.class);
		willThrow(IOException.class).given(connection).connect();
		given(connection.getContentLengthLong()).willCallRealMethod();
		assertThat(connection.getContentLengthLong()).isEqualTo(-1);
	}

	@Test
	void getContentTypeWhenHasNoEntryReturnsJavaJar() throws Exception {
		JarUrlConnection connection = JarUrlConnection.open(this.url);
		assertThat(connection.getContentType()).isEqualTo("x-java/jar");
	}

	@Test
	void getContentTypeWhenHasKnownStreamReturnsDeducedType() throws Exception {
		String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ok></ok>";
		try (JarOutputStream out = new JarOutputStream(new FileOutputStream(this.file))) {
			out.putNextEntry(new ZipEntry("test.dat"));
			out.write(content.getBytes(StandardCharsets.UTF_8));
			out.closeEntry();
		}
		JarUrlConnection connection = JarUrlConnection
			.open(new URL("jar:file:" + this.file.getAbsolutePath() + "!/test.dat"));
		assertThat(connection.getContentType()).isEqualTo("application/xml");
	}

	@Test
	void getContentTypeWhenNotKnownInStreamButKnownNameReturnsDeducedType() throws Exception {
		String content = "nothinguseful";
		try (JarOutputStream out = new JarOutputStream(new FileOutputStream(this.file))) {
			out.putNextEntry(new ZipEntry("test.xml"));
			out.write(content.getBytes(StandardCharsets.UTF_8));
			out.closeEntry();
		}
		JarUrlConnection connection = JarUrlConnection
			.open(new URL("jar:file:" + this.file.getAbsolutePath() + "!/test.xml"));
		assertThat(connection.getContentType()).isEqualTo("application/xml");
	}

	@Test
	void getContentTypeWhenCannotBeDeducedReturnsContentUnknown() throws Exception {
		String content = "nothinguseful";
		try (JarOutputStream out = new JarOutputStream(new FileOutputStream(this.file))) {
			out.putNextEntry(new ZipEntry("test.dat"));
			out.write(content.getBytes(StandardCharsets.UTF_8));
			out.closeEntry();
		}
		JarUrlConnection connection = JarUrlConnection
			.open(new URL("jar:file:" + this.file.getAbsolutePath() + "!/test.dat"));
		assertThat(connection.getContentType()).isEqualTo("content/unknown");
	}

	@Test
	void getHeaderFieldDelegatesToJarFileConnection() throws Exception {
		JarUrlConnection connection = JarUrlConnection.open(this.url);
		URLConnection jarFileConnection = mock(URLConnection.class);
		given(jarFileConnection.getHeaderField("test")).willReturn("test");
		ReflectionTestUtils.setField(connection, "jarFileConnection", jarFileConnection);
		assertThat(connection.getHeaderField("test")).isEqualTo("test");
	}

	@Test
	void getContentWhenHasEntryReturnsContentFromEntry() throws Exception {
		String content = "hello";
		try (JarOutputStream out = new JarOutputStream(new FileOutputStream(this.file))) {
			out.putNextEntry(new ZipEntry("test.txt"));
			out.write(content.getBytes(StandardCharsets.UTF_8));
			out.closeEntry();
		}
		JarUrlConnection connection = JarUrlConnection
			.open(new URL("jar:file:" + this.file.getAbsolutePath() + "!/test.txt"));
		assertThat(connection.getContent()).isInstanceOf(FilterInputStream.class);
	}

	@Test
	void getContentWhenHasNoEntryReturnsJarFile() throws Exception {
		JarUrlConnection connection = JarUrlConnection.open(this.url);
		assertThat(connection.getContent()).isInstanceOf(JarFile.class);
	}

	@Test
	void getPermissionReturnJarConnectionPermission() throws IOException {
		JarUrlConnection connection = JarUrlConnection.open(this.url);
		URLConnection jarFileConnection = mock(URLConnection.class);
		Permission permission = mock(Permission.class);
		given(jarFileConnection.getPermission()).willReturn(permission);
		ReflectionTestUtils.setField(connection, "jarFileConnection", jarFileConnection);
		assertThat(connection.getPermission()).isSameAs(permission);
	}

	@Test
	void getInputStreamWhenHasNoEntryThrowsException() throws Exception {
		JarUrlConnection connection = JarUrlConnection.open(this.url);
		assertThatIOException().isThrownBy(() -> connection.getInputStream()).withMessage("no entry name specified");
	}

	@Test
	void getInputStreamWhenOptimizedWithoutReadAndHasCachedJarWithEntryReturnsEmptyInputStream() throws Exception {
		JarUrlConnection setupConnection = JarUrlConnection.open(JarUrl.create(this.file, "nested.jar"));
		setupConnection.connect();
		assertThat(JarUrlConnection.jarFiles.getCached(setupConnection.getJarFileURL())).isNotNull();
		JarUrlConnection connection = JarUrlConnection.open(JarUrl.create(this.file, "nested.jar", "3.dat"));
		connection.setUseCaches(false);
		Optimizations.enable(false);
		assertThat(connection.getInputStream()).isSameAs(JarUrlConnection.emptyInputStream);
	}

	@Test
	void getInputStreamWhenNoEntryAndOptimzedThrowsException() throws Exception {
		JarUrlConnection setupConnection = JarUrlConnection.open(JarUrl.create(this.file, "nested.jar"));
		setupConnection.connect();
		assertThat(JarUrlConnection.jarFiles.getCached(setupConnection.getJarFileURL())).isNotNull();
		JarUrlConnection connection = JarUrlConnection.open(JarUrl.create(this.file, "nested.jar", "missing.dat"));
		Optimizations.enable(false);
		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(connection::getInputStream)
			.isSameAs(JarUrlConnection.FILE_NOT_FOUND_EXCEPTION);
	}

	@Test
	void getInputStreamWhenNoEntryAndNotOptimzedThrowsException() throws Exception {
		JarUrlConnection connection = JarUrlConnection.open(JarUrl.create(this.file, "nested.jar", "missing.dat"));
		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(connection::getInputStream)
			.withMessageContaining("JAR entry missing.dat not found in");
	}

	@Test
	void getInputStreamReturnsInputStream() throws IOException {
		JarUrlConnection connection = JarUrlConnection.open(JarUrl.create(this.file, "nested.jar", "3.dat"));
		try (InputStream in = connection.getInputStream()) {
			assertThat(in).hasBinaryContent(new byte[] { 3 });
		}
	}

	@Test
	void getInputStreamWhenNoCachedClosesJarFileOnClose() throws IOException {
		JarUrlConnection connection = JarUrlConnection.open(JarUrl.create(this.file, "nested.jar", "3.dat"));
		connection.setUseCaches(false);
		InputStream in = connection.getInputStream();
		JarFile jarFile = (JarFile) ReflectionTestUtils.getField(connection, "jarFile");
		jarFile = spy(jarFile);
		ReflectionTestUtils.setField(connection, "jarFile", jarFile);
		in.close();
		then(jarFile).should().close();
	}

	@Test
	void getAllowUserInteractionDelegatesToJarFileConnection() throws Exception {
		JarUrlConnection connection = JarUrlConnection.open(this.url);
		URLConnection jarFileConnection = mock(URLConnection.class);
		ReflectionTestUtils.setField(connection, "jarFileConnection", jarFileConnection);
		given(jarFileConnection.getAllowUserInteraction()).willReturn(true);
		assertThat(connection.getAllowUserInteraction()).isTrue();
		then(jarFileConnection).should().getAllowUserInteraction();
	}

	@Test
	void setAllowUserInteractionDelegatesToJarFileConnection() throws IOException {
		JarUrlConnection connection = JarUrlConnection.open(this.url);
		URLConnection jarFileConnection = mock(URLConnection.class);
		ReflectionTestUtils.setField(connection, "jarFileConnection", jarFileConnection);
		connection.setAllowUserInteraction(true);
		then(jarFileConnection).should().setAllowUserInteraction(true);
	}

	@Test
	void getUseCachesDelegatesToJarFileConnection() throws Exception {
		JarUrlConnection connection = JarUrlConnection.open(this.url);
		URLConnection jarFileConnection = mock(URLConnection.class);
		ReflectionTestUtils.setField(connection, "jarFileConnection", jarFileConnection);
		given(jarFileConnection.getUseCaches()).willReturn(true);
		assertThat(connection.getUseCaches()).isTrue();
		then(jarFileConnection).should().getUseCaches();
	}

	@Test
	void setUseCachesDelegatesToJarFileConnection() throws Exception {
		JarUrlConnection connection = JarUrlConnection.open(this.url);
		URLConnection jarFileConnection = mock(URLConnection.class);
		ReflectionTestUtils.setField(connection, "jarFileConnection", jarFileConnection);
		connection.setUseCaches(true);
		then(jarFileConnection).should().setUseCaches(true);
	}

	@Test
	void getDefaultUseCachesDelegatesToJarFileConnection() throws Exception {
		JarUrlConnection connection = JarUrlConnection.open(this.url);
		URLConnection jarFileConnection = mock(URLConnection.class);
		ReflectionTestUtils.setField(connection, "jarFileConnection", jarFileConnection);
		given(jarFileConnection.getDefaultUseCaches()).willReturn(true);
		assertThat(connection.getDefaultUseCaches()).isTrue();
		then(jarFileConnection).should().getDefaultUseCaches();
	}

	@Test
	void setDefaultUseCachesDelegatesToJarFileConnection() throws Exception {
		JarUrlConnection connection = JarUrlConnection.open(this.url);
		URLConnection jarFileConnection = mock(URLConnection.class);
		ReflectionTestUtils.setField(connection, "jarFileConnection", jarFileConnection);
		connection.setDefaultUseCaches(true);
		then(jarFileConnection).should().setDefaultUseCaches(true);
	}

	@Test
	void setIfModifiedSinceDelegatesToJarFileConnection() throws Exception {
		JarUrlConnection connection = JarUrlConnection.open(this.url);
		URLConnection jarFileConnection = mock(URLConnection.class);
		ReflectionTestUtils.setField(connection, "jarFileConnection", jarFileConnection);
		connection.setIfModifiedSince(123L);
		then(jarFileConnection).should().setIfModifiedSince(123L);
	}

	@Test
	void getRequestPropertyDelegatesToJarFileConnection() throws Exception {
		JarUrlConnection connection = JarUrlConnection.open(this.url);
		URLConnection jarFileConnection = mock(URLConnection.class);
		ReflectionTestUtils.setField(connection, "jarFileConnection", jarFileConnection);
		given(jarFileConnection.getRequestProperty("test")).willReturn("test");
		assertThat(connection.getRequestProperty("test")).isEqualTo("test");
		then(jarFileConnection).should().getRequestProperty("test");
	}

	@Test
	void setRequestPropertyDelegatesToJarFileConnection() throws Exception {
		JarUrlConnection connection = JarUrlConnection.open(this.url);
		URLConnection jarFileConnection = mock(URLConnection.class);
		ReflectionTestUtils.setField(connection, "jarFileConnection", jarFileConnection);
		connection.setRequestProperty("test", "testvalue");
		then(jarFileConnection).should().setRequestProperty("test", "testvalue");
	}

	@Test
	void addRequestPropertyDelegatesToJarFileConnection() throws Exception {
		JarUrlConnection connection = JarUrlConnection.open(this.url);
		URLConnection jarFileConnection = mock(URLConnection.class);
		ReflectionTestUtils.setField(connection, "jarFileConnection", jarFileConnection);
		connection.addRequestProperty("test", "testvalue");
		then(jarFileConnection).should().addRequestProperty("test", "testvalue");
	}

	@Test
	void getRequestPropertiesDelegatesToJarFileConnection() throws Exception {
		JarUrlConnection connection = JarUrlConnection.open(this.url);
		URLConnection jarFileConnection = mock(URLConnection.class);
		ReflectionTestUtils.setField(connection, "jarFileConnection", jarFileConnection);
		Map<String, List<String>> properties = Map.of("test", List.of("testvalue"));
		given(jarFileConnection.getRequestProperties()).willReturn(properties);
		assertThat(connection.getRequestProperties()).isEqualTo(properties);
		then(jarFileConnection).should().getRequestProperties();
	}

	@Test
	void connectWhenConnectedDoesNotReconnect() throws Exception {
		JarUrlConnection connection = JarUrlConnection.open(this.url);
		connection.connect();
		ReflectionTestUtils.setField(connection, "jarFile", null);
		connection.connect();
		assertThat(ReflectionTestUtils.getField(connection, "jarFile")).isNull();
	}

	@Test
	void connectWhenHasNotFoundSupplierThrowsException() throws Exception {
		JarUrlConnection connection = JarUrlConnection.open(JarUrl.create(this.file, "nested.jar", "missing.dat"));
		assertThat(connection).extracting("notFound").isNotNull();
		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(connection::connect)
			.withMessageContaining("JAR entry missing.dat not found in");
	}

	@Test
	void connectWhenOptimizationsEnabledAndHasCachedJarWithoutEntryThrowsException() throws Exception {
		JarUrlConnection setupConnection = JarUrlConnection.open(JarUrl.create(this.file, "nested.jar"));
		setupConnection.connect();
		assertThat(JarUrlConnection.jarFiles.getCached(setupConnection.getJarFileURL())).isNotNull();
		JarUrlConnection connection = JarUrlConnection.open(JarUrl.create(this.file, "nested.jar", "missing.dat"));
		Optimizations.enable(true);
		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(connection::connect)
			.isSameAs(JarUrlConnection.FILE_NOT_FOUND_EXCEPTION);
	}

	@Test
	void connectWhenHasNoEntryConnects() throws Exception {
		JarUrlConnection setupConnection = JarUrlConnection.open(this.url);
		setupConnection.connect();
		assertThat(setupConnection.getJarFile()).isNotNull();
	}

	@Test
	void connectWhenEntryDoesNotExistAndOptimizationsEnabledThrowsException() throws Exception {
		JarUrlConnection connection = JarUrlConnection.open(JarUrl.create(this.file, "nested.jar", "missing.dat"));
		Optimizations.enable(true);
		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(connection::connect)
			.isSameAs(JarUrlConnection.FILE_NOT_FOUND_EXCEPTION);
	}

	@Test
	void connectWhenEntryDoesNotExistAndNoOptimizationsEnabledThrowsException() throws Exception {
		JarUrlConnection connection = JarUrlConnection.open(JarUrl.create(this.file, "nested.jar", "missing.dat"));
		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(connection::connect)
			.withMessageContaining("JAR entry missing.dat not found in");
	}

	@Test
	void connectWhenEntryExists() throws Exception {
		JarUrlConnection connection = JarUrlConnection.open(JarUrl.create(this.file, "nested.jar", "3.dat"));
		connection.connect();
		assertThat(connection.getJarEntry()).isNotNull();
	}

	@Test
	void connectWhenAddedToCacheReconnects() throws IOException {
		JarUrlConnection connection = JarUrlConnection.open(this.url);
		Object originalConnection = ReflectionTestUtils.getField(connection, "jarFileConnection");
		connection.connect();
		assertThat(connection).extracting("jarFileConnection").isNotSameAs(originalConnection);
	}

	@Test
	void openWhenNestedAndInCachedWithoutEntryAndOptimzationsEnabledReturnsNoFoundConnection() throws Exception {
		JarUrlConnection setupConnection = JarUrlConnection.open(JarUrl.create(this.file, "nested.jar"));
		setupConnection.connect();
		assertThat(JarUrlConnection.jarFiles.getCached(setupConnection.getJarFileURL())).isNotNull();
		Optimizations.enable(true);
		JarUrlConnection connection = JarUrlConnection.open(JarUrl.create(this.file, "nested.jar", "missing.dat"));
		assertThat(connection).isSameAs(JarUrlConnection.NOT_FOUND_CONNECTION);
	}

	@Test
	void openReturnsConnection() throws Exception {
		JarUrlConnection connection = JarUrlConnection.open(this.url);
		assertThat(connection).isNotNull();
	}

}
