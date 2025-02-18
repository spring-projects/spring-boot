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

package org.springframework.boot.loader.net.protocol.nested;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Cleaner.Cleanable;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.time.Instant;
import java.time.temporal.ChronoField;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import org.springframework.boot.loader.net.protocol.Handlers;
import org.springframework.boot.loader.ref.Cleaner;
import org.springframework.boot.loader.testsupport.TestJar;
import org.springframework.boot.loader.zip.AssertFileChannelDataBlocksClosed;
import org.springframework.boot.loader.zip.ZipContent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link NestedUrlConnection}.
 *
 * @author Phillip Webb
 */
@AssertFileChannelDataBlocksClosed
class NestedUrlConnectionTests {

	@TempDir
	File temp;

	private File jarFile;

	private URL url;

	@BeforeAll
	static void registerHandlers() {
		Handlers.register();
	}

	@BeforeEach
	void setup() throws Exception {
		this.jarFile = new File(this.temp, "test.jar");
		TestJar.create(this.jarFile);
		this.url = new URL("nested:" + this.jarFile.getAbsolutePath() + "/!nested.jar");
	}

	@Test
	void getContentLengthWhenContentLengthMoreThanMaxIntReturnsMinusOne() {
		NestedUrlConnection connection = mock(NestedUrlConnection.class);
		given(connection.getContentLength()).willCallRealMethod();
		given(connection.getContentLengthLong()).willReturn((long) Integer.MAX_VALUE + 1);
		assertThat(connection.getContentLength()).isEqualTo(-1);
	}

	@Test
	void getContentLengthGetsContentLength() throws Exception {
		NestedUrlConnection connection = new NestedUrlConnection(this.url);
		try (ZipContent zipContent = ZipContent.open(this.jarFile.toPath())) {
			int expectedSize = zipContent.getEntry("nested.jar").getUncompressedSize();
			assertThat(connection.getContentLength()).isEqualTo(expectedSize);
		}
	}

	@Test
	void getContentLengthLongReturnsContentLength() throws Exception {
		NestedUrlConnection connection = new NestedUrlConnection(this.url);
		try (ZipContent zipContent = ZipContent.open(this.jarFile.toPath())) {
			int expectedSize = zipContent.getEntry("nested.jar").getUncompressedSize();
			assertThat(connection.getContentLengthLong()).isEqualTo(expectedSize);
		}
	}

	@Test
	void getContentTypeReturnsJavaJar() throws Exception {
		NestedUrlConnection connection = new NestedUrlConnection(this.url);
		assertThat(connection.getContentType()).isEqualTo("x-java/jar");
	}

	@Test
	void getLastModifiedReturnsFileLastModified() throws Exception {
		NestedUrlConnection connection = new NestedUrlConnection(this.url);
		assertThat(connection.getLastModified()).isEqualTo(this.jarFile.lastModified());
	}

	@Test
	void getPermissionReturnsFilePermission() throws Exception {
		NestedUrlConnection connection = new NestedUrlConnection(this.url);
		Permission permission = connection.getPermission();
		assertThat(permission).isInstanceOf(FilePermission.class);
		assertThat(permission.getName()).isEqualTo(this.jarFile.getCanonicalPath());
	}

	@Test
	void getInputStreamReturnsContentOfNestedJar() throws Exception {
		NestedUrlConnection connection = new NestedUrlConnection(this.url);
		assertHasSameContentAsNestedJar(connection);
	}

	@Test
	void inputStreamCloseCleansResource() throws Exception {
		Cleaner cleaner = mock(Cleaner.class);
		Cleanable cleanable = mock(Cleanable.class);
		given(cleaner.register(any(), any())).willReturn(cleanable);
		NestedUrlConnection connection = new NestedUrlConnection(this.url, cleaner);
		connection.getInputStream().close();
		then(cleanable).should().clean();
		ArgumentCaptor<Runnable> actionCaptor = ArgumentCaptor.forClass(Runnable.class);
		then(cleaner).should().register(any(), actionCaptor.capture());
		actionCaptor.getValue().run();
	}

	@Test // gh-38204
	void getLastModifiedReturnsFileModifiedTime() throws Exception {
		NestedUrlConnection connection = new NestedUrlConnection(this.url);
		assertThat(connection.getLastModified()).isEqualTo(this.jarFile.lastModified());
	}

	@Test // gh-38204
	void getLastModifiedHeaderReturnsFileModifiedTime() throws IOException {
		NestedUrlConnection connection = new NestedUrlConnection(this.url);
		URLConnection fileConnection = this.jarFile.toURI().toURL().openConnection();
		try {
			assertThat(connection.getHeaderFieldDate("last-modified", 0))
				.isEqualTo(withoutNanos(this.jarFile.lastModified()))
				.isEqualTo(fileConnection.getHeaderFieldDate("last-modified", 0));
		}
		finally {
			fileConnection.getInputStream().close();
		}
	}

	@Test
	void createDecodesUrlPath() throws Exception {
		File withSpace = new File(this.temp, "te st");
		withSpace.mkdirs();
		this.jarFile = new File(withSpace, "test.jar");
		TestJar.create(this.jarFile);
		this.url = new URL("nested:" + this.jarFile.toURI().getRawPath() + "/!nested.jar");
		assertThat(this.url.toString()).contains("%20");
		NestedUrlConnection connection = new NestedUrlConnection(this.url);
		assertHasSameContentAsNestedJar(connection);
		assertThat(connection.getLastModified()).isEqualTo(this.jarFile.lastModified());
	}

	private void assertHasSameContentAsNestedJar(NestedUrlConnection connection) throws IOException {
		try (InputStream actual = connection.getInputStream()) {
			try (ZipContent zipContent = ZipContent.open(this.jarFile.toPath())) {
				try (InputStream expected = zipContent.getEntry("nested.jar").openContent().asInputStream()) {
					assertThat(actual).hasSameContentAs(expected);
				}
			}
		}
	}

	private long withoutNanos(long epochMilli) {
		return Instant.ofEpochMilli(epochMilli).with(ChronoField.NANO_OF_SECOND, 0).toEpochMilli();
	}

}
