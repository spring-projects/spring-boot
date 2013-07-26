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

package org.springframework.boot.load;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;

import org.springframework.boot.load.jar.JarEntryFilter;
import org.springframework.boot.load.jar.RandomAccessJarFile;

/**
 * {@link Archive} implementation backed by a {@link RandomAccessJarFile}.
 * 
 * @author Phillip Webb
 */
public class JarFileArchive implements Archive {

	private final RandomAccessJarFile jarFile;

	private final List<Entry> entries;

	public JarFileArchive(File file) throws IOException {
		this(new RandomAccessJarFile(file));
	}

	public JarFileArchive(RandomAccessJarFile jarFile) {
		this.jarFile = jarFile;
		ArrayList<Entry> jarFileEntries = new ArrayList<Entry>();
		Enumeration<JarEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) {
			jarFileEntries.add(new JarFileEntry(entries.nextElement()));
		}
		this.entries = Collections.unmodifiableList(jarFileEntries);
	}

	@Override
	public Manifest getManifest() throws IOException {
		return this.jarFile.getManifest();
	}

	@Override
	public Iterable<Entry> getEntries() {
		return this.entries;
	}

	@Override
	public URL getUrl() throws MalformedURLException {
		return this.jarFile.getUrl();
	}

	@Override
	public Archive getNestedArchive(Entry entry) throws IOException {
		JarEntry jarEntry = ((JarFileEntry) entry).getJarEntry();
		RandomAccessJarFile jarFile = this.jarFile.getNestedJarFile(jarEntry);
		return new JarFileArchive(jarFile);
	}

	@Override
	public Archive getFilteredArchive(final EntryFilter filter) throws IOException {
		RandomAccessJarFile filteredJar = this.jarFile
				.getFilteredJarFile(new JarEntryFilter() {
					@Override
					public String apply(String name, JarEntry entry) {
						return filter.apply(name, new JarFileEntry(entry));
					}
				});
		return new JarFileArchive(filteredJar);
	}

	/**
	 * {@link Archive.Entry} implementation backed by a {@link JarEntry}.
	 */
	private static class JarFileEntry implements Entry {

		private final JarEntry jarEntry;

		public JarFileEntry(JarEntry jarEntry) {
			this.jarEntry = jarEntry;
		}

		public JarEntry getJarEntry() {
			return this.jarEntry;
		}

		@Override
		public boolean isDirectory() {
			return this.jarEntry.isDirectory();
		}

		@Override
		public String getName() {
			return this.jarEntry.getName();
		}

	}

}
