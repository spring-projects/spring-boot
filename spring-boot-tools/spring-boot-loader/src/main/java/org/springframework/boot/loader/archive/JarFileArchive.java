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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;

import org.springframework.boot.loader.data.RandomAccessData.ResourceAccess;
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

	private static final AsciiBytes UNPACK_MARKER = new AsciiBytes("UNPACK:");

	private static final int BUFFER_SIZE = 32 * 1024;

	private final JarFile jarFile;

	private final List<Entry> entries;

	private URL url;

	public JarFileArchive(File file) throws IOException {
		this(file, null);
	}

	public JarFileArchive(File file, URL url) throws IOException {
		this(new JarFile(file));
		this.url = url;
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
		if (this.url != null) {
			return this.url;
		}
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
		if (data.getComment().startsWith(UNPACK_MARKER)) {
			return getUnpackedNestedArchive(data);
		}
		JarFile jarFile = this.jarFile.getNestedJarFile(data);
		return new JarFileArchive(jarFile);
	}

	private Archive getUnpackedNestedArchive(JarEntryData data) throws IOException {
		AsciiBytes hash = data.getComment().substring(UNPACK_MARKER.length());
		String name = data.getName().toString();
		if (name.lastIndexOf("/") != -1) {
			name = name.substring(name.lastIndexOf("/") + 1);
		}
		File file = new File(getTempUnpackFolder(), hash.toString() + "-" + name);
		if (!file.exists() || file.length() != data.getSize()) {
			unpack(data, file);
		}
		return new JarFileArchive(file, file.toURI().toURL());
	}

	private File getTempUnpackFolder() {
		File tempFolder = new File(System.getProperty("java.io.tmpdir"));
		File unpackFolder = new File(tempFolder, "spring-boot-libs");
		unpackFolder.mkdirs();
		return unpackFolder;
	}

	private void unpack(JarEntryData data, File file) throws IOException {
		InputStream inputStream = data.getData().getInputStream(ResourceAccess.ONCE);
		try {
			OutputStream outputStream = new FileOutputStream(file);
			try {
				byte[] buffer = new byte[BUFFER_SIZE];
				int bytesRead = -1;
				while ((bytesRead = inputStream.read(buffer)) != -1) {
					outputStream.write(buffer, 0, bytesRead);
				}
				outputStream.flush();
			}
			finally {
				outputStream.close();
			}
		}
		finally {
			inputStream.close();
		}
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
