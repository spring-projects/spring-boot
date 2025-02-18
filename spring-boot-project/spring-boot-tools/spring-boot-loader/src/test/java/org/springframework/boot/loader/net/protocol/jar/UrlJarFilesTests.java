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
import java.net.URL;
import java.net.URLConnection;
import java.util.jar.JarFile;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.net.protocol.Handlers;
import org.springframework.boot.loader.testsupport.TestJar;
import org.springframework.boot.loader.zip.AssertFileChannelDataBlocksClosed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link UrlJarFiles}.
 *
 * @author Phillip Webb
 */
@AssertFileChannelDataBlocksClosed
class UrlJarFilesTests {

	@TempDir
	File temp;

	private UrlJarFileFactory factory = mock(UrlJarFileFactory.class);

	private final UrlJarFiles jarFiles = new UrlJarFiles(this.factory);

	private File file;

	private URL url;

	@BeforeAll
	static void registerHandlers() {
		Handlers.register();
	}

	@BeforeEach
	void setup() throws Exception {
		this.file = new File(this.temp, "test.jar");
		this.url = new URL("nested:" + this.file.getAbsolutePath() + "/!nested.jar");
		TestJar.create(this.file);
	}

	@Test
	void getOrCreateWhenNotUsingCachesAlwaysCreatesNewJarFile() throws Exception {
		given(this.factory.createJarFile(any(), any())).willCallRealMethod();
		JarFile jarFile1 = this.jarFiles.getOrCreate(false, this.url);
		JarFile jarFile2 = this.jarFiles.getOrCreate(false, this.url);
		JarFile jarFile3 = this.jarFiles.getOrCreate(false, this.url);
		assertThat(jarFile1).isNotSameAs(jarFile2).isNotSameAs(jarFile3);
	}

	@Test
	void getOrCreateWhenUsingCachingReturnsCachedWhenAvailable() throws Exception {
		given(this.factory.createJarFile(any(), any())).willCallRealMethod();
		JarFile jarFile1 = this.jarFiles.getOrCreate(true, this.url);
		this.jarFiles.cacheIfAbsent(true, this.url, jarFile1);
		JarFile jarFile2 = this.jarFiles.getOrCreate(true, this.url);
		JarFile jarFile3 = this.jarFiles.getOrCreate(true, this.url);
		assertThat(jarFile1).isSameAs(jarFile2).isSameAs(jarFile3);
	}

	@Test
	void getCachedWhenNotCachedReturnsNull() {
		assertThat(this.jarFiles.getCached(this.url)).isNull();
	}

	@Test
	void getCachedWhenCachedReturnsCachedJar() throws Exception {
		given(this.factory.createJarFile(any(), any())).willCallRealMethod();
		JarFile jarFile = this.factory.createJarFile(this.url, null);
		this.jarFiles.cacheIfAbsent(true, this.url, jarFile);
		assertThat(this.jarFiles.getCached(this.url)).isSameAs(jarFile);
	}

	@Test
	void cacheIfAbsentWhenNotUsingCachesDoesNotCacheAndReturnsFalse() throws Exception {
		given(this.factory.createJarFile(any(), any())).willCallRealMethod();
		JarFile jarFile = this.factory.createJarFile(this.url, null);
		this.jarFiles.cacheIfAbsent(false, this.url, jarFile);
		assertThat(this.jarFiles.getCached(this.url)).isNull();
	}

	@Test
	void cacheIfAbsentWhenUsingCachingAndNotAlreadyCachedCachesAndReturnsTrue() throws Exception {
		given(this.factory.createJarFile(any(), any())).willCallRealMethod();
		JarFile jarFile = this.factory.createJarFile(this.url, null);
		assertThat(this.jarFiles.cacheIfAbsent(true, this.url, jarFile)).isTrue();
		assertThat(this.jarFiles.getCached(this.url)).isSameAs(jarFile);
	}

	@Test
	void cacheIfAbsentWhenUsingCachingAndAlreadyCachedLeavesCacheAndReturnsFalse() throws Exception {
		given(this.factory.createJarFile(any(), any())).willCallRealMethod();
		JarFile jarFile1 = this.factory.createJarFile(this.url, null);
		JarFile jarFile2 = this.factory.createJarFile(this.url, null);
		assertThat(this.jarFiles.cacheIfAbsent(true, this.url, jarFile1)).isTrue();
		assertThat(this.jarFiles.cacheIfAbsent(true, this.url, jarFile2)).isFalse();
		assertThat(this.jarFiles.getCached(this.url)).isSameAs(jarFile1);
	}

	@Test
	void closeIfNotCachedWhenNotCachedClosesJarFile() throws Exception {
		JarFile jarFile = mock(JarFile.class);
		this.jarFiles.closeIfNotCached(this.url, jarFile);
		then(jarFile).should().close();
	}

	@Test
	void closeIfNotCachedWhenCachedDoesNotCloseJarFile() throws Exception {
		JarFile jarFile = mock(JarFile.class);
		this.jarFiles.cacheIfAbsent(true, this.url, jarFile);
		this.jarFiles.closeIfNotCached(this.url, jarFile);
		then(jarFile).should(never()).close();
	}

	@Test
	void reconnectReconnectsAndAppliesUseCaches() throws Exception {
		JarFile jarFile = mock(JarFile.class);
		this.jarFiles.cacheIfAbsent(true, this.url, jarFile);
		URLConnection existingConnection = mock(URLConnection.class);
		given(existingConnection.getUseCaches()).willReturn(true);
		URLConnection connection = this.jarFiles.reconnect(jarFile, existingConnection);
		assertThat(connection).isNotSameAs(existingConnection);
		assertThat(connection.getUseCaches()).isTrue();
	}

	@Test
	void reconnectWhenExistingConnectionIsNullReconnects() throws Exception {
		JarFile jarFile = mock(JarFile.class);
		this.jarFiles.cacheIfAbsent(true, this.url, jarFile);
		URLConnection connection = this.jarFiles.reconnect(jarFile, null);
		assertThat(connection).isNotNull();
		assertThat(connection.getUseCaches()).isTrue();
	}

}
