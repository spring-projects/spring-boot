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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;

import org.springframework.boot.loader.jar.JarEntryData;
import org.springframework.boot.loader.jar.JarEntryFilter;
import org.springframework.boot.loader.jar.JarFile;
import org.springframework.boot.loader.util.AsciiBytes;

/**
 * {@link Archive} implementation backed by a {@link JarFile}.
 * 
 * @author Phillip Webb
 */
public class JarFileArchive extends Archive {

	private final JarFile jarFile;

	private final List<Entry> entries;

	public JarFileArchive(File file) throws IOException {
		this(new JarFile(file));
	}

	public JarFileArchive(JarFile jarFile) {
		this.jarFile = jarFile;
		ArrayList<Entry> jarFileEntries = new ArrayList<Entry>();
		for (JarEntryData data : jarFile) {
			jarFileEntries.add(new JarFileEntry(data));
		}
		this.entries = Collections.unmodifiableList(jarFileEntries);
	}

	@Override
	public URL getUrl() throws MalformedURLException {
		return this.jarFile.getUrl();
	}

	@Override
	public Manifest getManifest() throws IOException {
		return this.jarFile.getManifest();
	}

	@Override
	public List<Archive> getNestedArchives(EntryFilter filter) throws IOException {
		List<Archive> nestedArchives = new ArrayList<Archive>();
		for (Entry entry : getEntries()) {
			if (filter.matches(entry)) {
				nestedArchives.add(getNestedArchive(entry));
			}
		}
		return Collections.unmodifiableList(nestedArchives);
	}

	@Override
	public Collection<Entry> getEntries() {
		return Collections.unmodifiableCollection(this.entries);
	}

	protected Archive getNestedArchive(Entry entry) throws IOException {
		JarEntryData data = ((JarFileEntry) entry).getJarEntryData();
		JarFile jarFile = this.jarFile.getNestedJarFile(data);
		return new JarFileArchive(jarFile);
	}

	@Override
	public Archive getFilteredArchive(final EntryRenameFilter filter) throws IOException {
		JarFile filteredJar = this.jarFile.getFilteredJarFile(new JarEntryFilter() {
			@Override
			public AsciiBytes apply(AsciiBytes name, JarEntryData entryData) {
				return filter.apply(name, new JarFileEntry(entryData));
			}
		});
		return new JarFileArchive(filteredJar);
	}

	/**
	 * {@link Archive.Entry} implementation backed by a {@link JarEntry}.
	 */
	private static class JarFileEntry implements Entry {

		private final JarEntryData entryData;

		public JarFileEntry(JarEntryData entryData) {
			this.entryData = entryData;
		}

		public JarEntryData getJarEntryData() {
			return this.entryData;
		}

		@Override
		public boolean isDirectory() {
			return this.entryData.isDirectory();
		}

		@Override
		public AsciiBytes getName() {
			return this.entryData.getName();
		}

	}

}
