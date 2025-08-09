/*
 * Copyright 2012-present the original author or authors.
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.JarEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.net.util.UrlDecoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JarUrl}.
 *
 * @author Phillip Webb
 */
class JarUrlTests {

	@TempDir
	File temp;

	File jarFile;

	String jarFileUrlPath;

	@BeforeEach
	void setup() throws MalformedURLException {
		this.jarFile = new File(this.temp, "my.jar");
		this.jarFileUrlPath = this.jarFile.toURI().toURL().toString().substring("file:".length()).replace("!", "%21");
	}

	@Test
	void createWithFileReturnsUrl() {
		URL url = JarUrl.create(this.jarFile);
		assertThat(url).hasToString("jar:file:%s!/".formatted(this.jarFileUrlPath));
	}

	@Test
	void createWithFileAndEntryReturnsUrl() {
		JarEntry entry = new JarEntry("lib.jar");
		URL url = JarUrl.create(this.jarFile, entry);
		assertThat(url).hasToString("jar:nested:%s/!lib.jar!/".formatted(this.jarFileUrlPath));
	}

	@Test
	void createWithFileAndNullEntryReturnsUrl() {
		URL url = JarUrl.create(this.jarFile, (JarEntry) null);
		assertThat(url).hasToString("jar:file:%s!/".formatted(this.jarFileUrlPath));
	}

	@Test
	void createWithFileAndNameReturnsUrl() {
		URL url = JarUrl.create(this.jarFile, "lib.jar");
		assertThat(url).hasToString("jar:nested:%s/!lib.jar!/".formatted(this.jarFileUrlPath));
	}

	@Test
	void createWithFileAndNullNameReturnsUrl() {
		URL url = JarUrl.create(this.jarFile, (String) null);
		assertThat(url).hasToString("jar:file:%s!/".formatted(this.jarFileUrlPath));
	}

	@Test
	void createWithFileNameAndPathReturnsUrl() {
		URL url = JarUrl.create(this.jarFile, "lib.jar", "com/example/My.class");
		assertThat(url).hasToString("jar:nested:%s/!lib.jar!/com/example/My.class".formatted(this.jarFileUrlPath));
	}

	@Test
	void createWithReservedCharsInName() throws Exception {
		String badFolderName = "foo#bar!/baz/!oof";
		this.temp = new File(this.temp, badFolderName);
		setup();
		URL url = JarUrl.create(this.jarFile, "lib.jar", "com/example/My.class");
		assertThat(url).hasToString("jar:nested:%s/!lib.jar!/com/example/My.class".formatted(this.jarFileUrlPath));
		assertThat(UrlDecoder.decode(url.toString())).contains(badFolderName);
	}

}
