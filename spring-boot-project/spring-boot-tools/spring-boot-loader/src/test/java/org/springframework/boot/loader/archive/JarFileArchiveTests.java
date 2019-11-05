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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.TestJarCreator;
import org.springframework.boot.loader.archive.Archive.Entry;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JarFileArchive}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Camille Vienot
 */
class JarFileArchiveTests {

	@TempDir
	File tempDir;

	private File rootJarFile;

	private JarFileArchive archive;

	private String rootJarFileUrl;

	@BeforeEach
	void setup() throws Exception {
		setup(false);
	}

	@AfterEach
	void tearDown() throws Exception {
		this.archive.close();
	}

	private void setup(boolean unpackNested) throws Exception {
		this.rootJarFile = new File(this.tempDir, "root.jar");
		this.rootJarFileUrl = this.rootJarFile.toURI().toString();
		TestJarCreator.createTestJar(this.rootJarFile, unpackNested);
		if (this.archive != null) {
			this.archive.close();
		}
		this.archive = new JarFileArchive(this.rootJarFile);
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
		URL url = this.archive.getUrl();
		assertThat(url.toString()).isEqualTo(this.rootJarFileUrl);
	}

	@Test
	void getNestedArchive() throws Exception {
		Entry entry = getEntriesMap(this.archive).get("nested.jar");
		try (Archive nested = this.archive.getNestedArchive(entry)) {
			assertThat(nested.getUrl().toString()).isEqualTo("jar:" + this.rootJarFileUrl + "!/nested.jar!/");
		}
	}

	@Test
	void getNestedUnpackedArchive() throws Exception {
		setup(true);
		Entry entry = getEntriesMap(this.archive).get("nested.jar");
		try (Archive nested = this.archive.getNestedArchive(entry)) {
			assertThat(nested.getUrl().toString()).startsWith("file:");
			assertThat(nested.getUrl().toString()).endsWith("/nested.jar");
		}
	}

	@Test
	void unpackedLocationsAreUniquePerArchive() throws Exception {
		setup(true);
		Entry entry = getEntriesMap(this.archive).get("nested.jar");
		URL firstNestedUrl;
		try (Archive firstNested = this.archive.getNestedArchive(entry)) {
			firstNestedUrl = firstNested.getUrl();
		}
		this.archive.close();
		setup(true);
		entry = getEntriesMap(this.archive).get("nested.jar");
		try (Archive secondNested = this.archive.getNestedArchive(entry)) {
			URL secondNestedUrl = secondNested.getUrl();
			assertThat(secondNestedUrl).isNotEqualTo(firstNestedUrl);
		}
	}

	@Test
	void unpackedLocationsFromSameArchiveShareSameParent() throws Exception {
		setup(true);
		try (Archive nestedArchive = this.archive.getNestedArchive(getEntriesMap(this.archive).get("nested.jar"));
				Archive anotherNestedArchive = this.archive
						.getNestedArchive(getEntriesMap(this.archive).get("another-nested.jar"))) {
			File nested = new File(nestedArchive.getUrl().toURI());
			File anotherNested = new File(anotherNestedArchive.getUrl().toURI());
			assertThat(nested.getParent()).isEqualTo(anotherNested.getParent());
		}
	}

	@Test
	void filesInZip64ArchivesAreAllListed() throws IOException {
		File file = new File(this.tempDir, "test.jar");
		FileCopyUtils.copy(writeZip64Jar(), file);
		try (JarFileArchive zip64Archive = new JarFileArchive(file)) {
			Iterator<Entry> entries = zip64Archive.iterator();
			for (int i = 0; i < 65537; i++) {
				assertThat(entries.hasNext()).as(i + "nth file is present").isTrue();
				entries.next();
			}
		}
	}

	@Test
	void nestedZip64ArchivesAreHandledGracefully() throws Exception {
		File file = new File(this.tempDir, "test.jar");
		try (JarOutputStream output = new JarOutputStream(new FileOutputStream(file))) {
			JarEntry zip64JarEntry = new JarEntry("nested/zip64.jar");
			output.putNextEntry(zip64JarEntry);
			byte[] zip64JarData = writeZip64Jar();
			zip64JarEntry.setSize(zip64JarData.length);
			zip64JarEntry.setCompressedSize(zip64JarData.length);
			zip64JarEntry.setMethod(ZipEntry.STORED);
			CRC32 crc32 = new CRC32();
			crc32.update(zip64JarData);
			zip64JarEntry.setCrc(crc32.getValue());
			output.write(zip64JarData);
			output.closeEntry();
		}
		try (JarFileArchive jarFileArchive = new JarFileArchive(file);
				Archive nestedArchive = jarFileArchive
						.getNestedArchive(getEntriesMap(jarFileArchive).get("nested/zip64.jar"))) {
			Iterator<Entry> it = nestedArchive.iterator();
			for (int i = 0; i < 65537; i++) {
				assertThat(it.hasNext()).as(i + "nth file is present").isTrue();
				it.next();
			}
		}
	}

	private byte[] writeZip64Jar() throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (JarOutputStream jarOutput = new JarOutputStream(bytes)) {
			for (int i = 0; i < 65537; i++) {
				jarOutput.putNextEntry(new JarEntry(i + ".dat"));
				jarOutput.closeEntry();
			}
		}
		return bytes.toByteArray();
	}

	private Map<String, Archive.Entry> getEntriesMap(Archive archive) {
		Map<String, Archive.Entry> entries = new HashMap<>();
		for (Archive.Entry entry : archive) {
			entries.put(entry.getName(), entry);
		}
		return entries;
	}

}
