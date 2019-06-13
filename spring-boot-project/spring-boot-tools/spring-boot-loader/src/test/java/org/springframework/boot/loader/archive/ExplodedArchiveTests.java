/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.loader.archive;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.TestJarCreator;
import org.springframework.boot.loader.archive.Archive.Entry;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ExplodedArchive}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 */
class ExplodedArchiveTests {

	@TempDir
	File tempDir;

	private File rootFolder;

	private ExplodedArchive archive;

	@BeforeEach
	public void setup() throws Exception {
		createArchive();
	}

	private void createArchive() throws Exception {
		createArchive(null);
	}

	private void createArchive(String folderName) throws Exception {
		File file = new File(this.tempDir, "test.jar");
		TestJarCreator.createTestJar(file);

		this.rootFolder = (StringUtils.hasText(folderName) ? new File(this.tempDir, folderName)
				: new File(this.tempDir, UUID.randomUUID().toString()));
		JarFile jarFile = new JarFile(file);
		Enumeration<JarEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			File destination = new File(this.rootFolder.getAbsolutePath() + File.separator + entry.getName());
			destination.getParentFile().mkdirs();
			if (entry.isDirectory()) {
				destination.mkdir();
			}
			else {
				FileCopyUtils.copy(jarFile.getInputStream(entry), new FileOutputStream(destination));
			}
		}
		this.archive = new ExplodedArchive(this.rootFolder);
		jarFile.close();
	}

	@Test
	void getManifest() throws Exception {
		assertThat(this.archive.getManifest().getMainAttributes().getValue("Built-By")).isEqualTo("j1");
	}

	@Test
	void getEntries() {
		Map<String, Archive.Entry> entries = getEntriesMap(this.archive);
		assertThat(entries.size()).isEqualTo(12);
	}

	@Test
	void getUrl() throws Exception {
		assertThat(this.archive.getUrl()).isEqualTo(this.rootFolder.toURI().toURL());
	}

	@Test
	void getUrlWithSpaceInPath() throws Exception {
		createArchive("spaces in the name");
		assertThat(this.archive.getUrl()).isEqualTo(this.rootFolder.toURI().toURL());
	}

	@Test
	void getNestedArchive() throws Exception {
		Entry entry = getEntriesMap(this.archive).get("nested.jar");
		Archive nested = this.archive.getNestedArchive(entry);
		assertThat(nested.getUrl().toString()).isEqualTo(this.rootFolder.toURI() + "nested.jar");
		((JarFileArchive) nested).close();
	}

	@Test
	void nestedDirArchive() throws Exception {
		Entry entry = getEntriesMap(this.archive).get("d/");
		Archive nested = this.archive.getNestedArchive(entry);
		Map<String, Entry> nestedEntries = getEntriesMap(nested);
		assertThat(nestedEntries.size()).isEqualTo(1);
		assertThat(nested.getUrl().toString()).isEqualTo("file:" + this.rootFolder.toURI().getPath() + "d/");
	}

	@Test
	void getNonRecursiveEntriesForRoot() {
		ExplodedArchive archive = new ExplodedArchive(new File("/"), false);
		Map<String, Archive.Entry> entries = getEntriesMap(archive);
		assertThat(entries.size()).isGreaterThan(1);
	}

	@Test
	void getNonRecursiveManifest() throws Exception {
		ExplodedArchive archive = new ExplodedArchive(new File("src/test/resources/root"));
		assertThat(archive.getManifest()).isNotNull();
		Map<String, Archive.Entry> entries = getEntriesMap(archive);
		assertThat(entries.size()).isEqualTo(4);
	}

	@Test
	void getNonRecursiveManifestEvenIfNonRecursive() throws Exception {
		ExplodedArchive archive = new ExplodedArchive(new File("src/test/resources/root"), false);
		assertThat(archive.getManifest()).isNotNull();
		Map<String, Archive.Entry> entries = getEntriesMap(archive);
		assertThat(entries.size()).isEqualTo(3);
	}

	@Test
	void getResourceAsStream() throws Exception {
		ExplodedArchive archive = new ExplodedArchive(new File("src/test/resources/root"));
		assertThat(archive.getManifest()).isNotNull();
		URLClassLoader loader = new URLClassLoader(new URL[] { archive.getUrl() });
		assertThat(loader.getResourceAsStream("META-INF/spring/application.xml")).isNotNull();
		loader.close();
	}

	@Test
	void getResourceAsStreamNonRecursive() throws Exception {
		ExplodedArchive archive = new ExplodedArchive(new File("src/test/resources/root"), false);
		assertThat(archive.getManifest()).isNotNull();
		URLClassLoader loader = new URLClassLoader(new URL[] { archive.getUrl() });
		assertThat(loader.getResourceAsStream("META-INF/spring/application.xml")).isNotNull();
		loader.close();
	}

	private Map<String, Archive.Entry> getEntriesMap(Archive archive) {
		Map<String, Archive.Entry> entries = new HashMap<>();
		for (Archive.Entry entry : archive) {
			entries.put(entry.getName(), entry);
		}
		return entries;
	}

}
