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
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.loader.TestJarCreator;
import org.springframework.boot.loader.archive.Archive.Entry;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

/**
 * Tests for {@link JarFileArchive}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class JarFileArchiveTests {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private File rootJarFile;

	private JarFileArchive archive;

	private String rootJarFileUrl;

	@Before
	public void setup() throws Exception {
		setup(false);
	}

	private void setup(boolean unpackNested) throws Exception {
		this.rootJarFile = this.temporaryFolder.newFile();
		this.rootJarFileUrl = this.rootJarFile.toURI().toString();
		TestJarCreator.createTestJar(this.rootJarFile, unpackNested);
		this.archive = new JarFileArchive(this.rootJarFile);
	}

	@Test
	public void getManifest() throws Exception {
		assertThat(this.archive.getManifest().getMainAttributes().getValue("Built-By")).isEqualTo("j1");
	}

	@Test
	public void getEntries() {
		Map<String, Archive.Entry> entries = getEntriesMap(this.archive);
		assertThat(entries.size()).isEqualTo(11);
	}

	@Test
	public void getUrl() throws Exception {
		URL url = this.archive.getUrl();
		assertThat(url.toString()).isEqualTo("jar:" + this.rootJarFileUrl + "!/");
	}

	@Test
	public void getNestedArchive() throws Exception {
		Entry entry = getEntriesMap(this.archive).get("nested.jar");
		Archive nested = this.archive.getNestedArchive(entry);
		assertThat(nested.getUrl().toString()).isEqualTo("jar:" + this.rootJarFileUrl + "!/nested.jar!/");
	}

	@Test
	public void getNestedUnpackedArchive() throws Exception {
		setup(true);
		Entry entry = getEntriesMap(this.archive).get("nested.jar");
		Archive nested = this.archive.getNestedArchive(entry);
		assertThat(nested.getUrl().toString()).startsWith("file:");
		assertThat(nested.getUrl().toString()).endsWith("/nested.jar");
	}

	@Test
	public void unpackedLocationsAreUniquePerArchive() throws Exception {
		setup(true);
		Entry entry = getEntriesMap(this.archive).get("nested.jar");
		URL firstNested = this.archive.getNestedArchive(entry).getUrl();
		setup(true);
		entry = getEntriesMap(this.archive).get("nested.jar");
		URL secondNested = this.archive.getNestedArchive(entry).getUrl();
		assertThat(secondNested).isNotEqualTo(firstNested);
	}

	@Test
	public void unpackedLocationsFromSameArchiveShareSameParent() throws Exception {
		setup(true);
		File nested = new File(
				this.archive.getNestedArchive(getEntriesMap(this.archive).get("nested.jar")).getUrl().toURI());
		File anotherNested = new File(
				this.archive.getNestedArchive(getEntriesMap(this.archive).get("another-nested.jar")).getUrl().toURI());
		assertThat(nested.getParent()).isEqualTo(anotherNested.getParent());
	}

	@Test
	public void zip64ArchivesAreHandledGracefully() throws IOException {
		File file = this.temporaryFolder.newFile("test.jar");
		FileCopyUtils.copy(writeZip64Jar(), file);
		this.thrown.expectMessage(equalTo("Zip64 archives are not supported"));
		new JarFileArchive(file);
	}

	@Test
	public void nestedZip64ArchivesAreHandledGracefully() throws IOException {
		File file = this.temporaryFolder.newFile("test.jar");
		JarOutputStream output = new JarOutputStream(new FileOutputStream(file));
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
		output.close();
		JarFileArchive jarFileArchive = new JarFileArchive(file);
		this.thrown.expectMessage(equalTo("Failed to get nested archive for entry nested/zip64.jar"));
		jarFileArchive.getNestedArchive(getEntriesMap(jarFileArchive).get("nested/zip64.jar"));
	}

	private byte[] writeZip64Jar() throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		JarOutputStream jarOutput = new JarOutputStream(bytes);
		for (int i = 0; i < 65537; i++) {
			jarOutput.putNextEntry(new JarEntry(i + ".dat"));
			jarOutput.closeEntry();
		}
		jarOutput.close();
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
