/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.loader.archive;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.loader.TestJarCreator;
import org.springframework.boot.loader.archive.Archive.Entry;
import org.springframework.boot.loader.util.AsciiBytes;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link JarFileArchive}.
 *
 * @author Phillip Webb
 */
public class JarFileArchiveTests {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private File rootJarFile;

	private JarFileArchive archive;

	private String rootJarFileUrl;

	@Before
	public void setup() throws Exception {
		setup(false);
	}

	private void setup(boolean unpackNested) throws Exception {
		this.rootJarFile = this.temporaryFolder.newFile();
		this.rootJarFileUrl = rootJarFile.toURI().toString();
		System.out.println(rootJarFileUrl);
		TestJarCreator.createTestJar(this.rootJarFile, unpackNested);
		this.archive = new JarFileArchive(this.rootJarFile);
	}

	@Test
	public void getManifest() throws Exception {
		assertThat(this.archive.getManifest().getMainAttributes().getValue("Built-By"),
				equalTo("j1"));
	}

	@Test
	public void getEntries() throws Exception {
		Map<String, Archive.Entry> entries = getEntriesMap(this.archive);
		assertThat(entries.size(), equalTo(9));
	}

	@Test
	public void getUrl() throws Exception {
		URL url = this.archive.getUrl();
		assertThat(url.toString(), equalTo("jar:" + this.rootJarFileUrl + "!/"));
	}

	@Test
	public void getNestedArchive() throws Exception {
		Entry entry = getEntriesMap(this.archive).get("nested.jar");
		Archive nested = this.archive.getNestedArchive(entry);
		assertThat(nested.getUrl().toString(), equalTo("jar:" + this.rootJarFileUrl
				+ "!/nested.jar!/"));
	}

	@Test
	public void getNestedUnpackedArchive() throws Exception {
		setup(true);
		Entry entry = getEntriesMap(this.archive).get("nested.jar");
		Archive nested = this.archive.getNestedArchive(entry);
		assertThat(nested.getUrl().toString(), startsWith("file:"));
		assertThat(nested.getUrl().toString(), endsWith(".jar"));
	}

	@Test
	public void getFilteredArchive() throws Exception {
		Archive filteredArchive = this.archive
				.getFilteredArchive(new Archive.EntryRenameFilter() {
					@Override
					public AsciiBytes apply(AsciiBytes entryName, Entry entry) {
						if (entryName.toString().equals("1.dat")) {
							return entryName;
						}
						return null;
					}
				});
		Map<String, Entry> entries = getEntriesMap(filteredArchive);
		assertThat(entries.size(), equalTo(1));
	}

	private Map<String, Archive.Entry> getEntriesMap(Archive archive) {
		Map<String, Archive.Entry> entries = new HashMap<String, Archive.Entry>();
		for (Archive.Entry entry : archive.getEntries()) {
			entries.put(entry.getName().toString(), entry);
		}
		return entries;
	}
}
