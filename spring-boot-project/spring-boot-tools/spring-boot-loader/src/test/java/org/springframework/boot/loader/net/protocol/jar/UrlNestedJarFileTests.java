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
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.loader.testsupport.TestJar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

/**
 * Tests for {@link UrlNestedJarFile}.
 *
 * @author Phillip Webb
 */
class UrlNestedJarFileTests {

	@TempDir
	File temp;

	private UrlNestedJarFile jarFile;

	@Mock
	private Consumer<JarFile> closeAction;

	@BeforeEach
	void setup() throws Exception {
		MockitoAnnotations.openMocks(this);
		File file = new File(this.temp, "test.jar");
		TestJar.create(file);
		this.jarFile = new UrlNestedJarFile(file, "multi-release.jar", Runtime.version(), this.closeAction);
	}

	@AfterEach
	void cleanup() throws Exception {
		this.jarFile.close();
	}

	@Test
	void getEntryWhenNotfoundReturnsNull() {
		assertThat(this.jarFile.getEntry("missing")).isNull();
	}

	@Test
	void getEntryWhenFoundReturnsUrlJarEntry() {
		assertThat(this.jarFile.getEntry("multi-release.dat")).isInstanceOf(UrlJarEntry.class);
	}

	@Test
	void getManifestReturnsNewCopy() throws Exception {
		Manifest manifest1 = this.jarFile.getManifest();
		Manifest manifest2 = this.jarFile.getManifest();
		assertThat(manifest1).isNotSameAs(manifest2);
	}

	@Test
	void closeCallsCloseAction() throws Exception {
		this.jarFile.close();
		then(this.closeAction).should().accept(this.jarFile);
	}

}
