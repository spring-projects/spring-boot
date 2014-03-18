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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

import org.springframework.boot.loader.util.AsciiBytes;

/**
 * {@link Archive} implementation backed by an exploded archive directory.
 * 
 * @author Phillip Webb
 */
public class ExplodedArchive extends Archive {

	private static final Set<String> SKIPPED_NAMES = new HashSet<String>(Arrays.asList(
			".", ".."));

	private static final AsciiBytes MANIFEST_ENTRY_NAME = new AsciiBytes(
			"META-INF/MANIFEST.MF");

	private final File root;

	private Map<AsciiBytes, Entry> entries = new LinkedHashMap<AsciiBytes, Entry>();

	private Manifest manifest;

	private boolean recursive = true;

	public ExplodedArchive(File root) {
		this(root, true);
	}

	public ExplodedArchive(File root, boolean recursive) {
		if (!root.exists() || !root.isDirectory()) {
			throw new IllegalArgumentException("Invalid source folder " + root);
		}
		this.root = root;
		this.recursive = recursive;
		buildEntries(root);
		this.entries = Collections.unmodifiableMap(this.entries);
	}

	private ExplodedArchive(File root, Map<AsciiBytes, Entry> entries) {
		this.root = root;
		this.entries = Collections.unmodifiableMap(entries);
	}

	private void buildEntries(File file) {
		if (!file.equals(this.root)) {
			String name = file.toURI().getPath()
					.substring(this.root.toURI().getPath().length());
			FileEntry entry = new FileEntry(new AsciiBytes(name), file);
			this.entries.put(entry.getName(), entry);
		}
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files == null) {
				return;
			}
			for (File child : files) {
				if (!SKIPPED_NAMES.contains(child.getName())) {
					if (file.equals(this.root) || this.recursive) {
						buildEntries(child);
					}
				}
			}
		}
	}

	@Override
	public URL getUrl() throws MalformedURLException {
		FilteredURLStreamHandler handler = new FilteredURLStreamHandler();
		return new URL("file", "", -1, this.root.toURI().getPath(), handler);
	}

	@Override
	public Manifest getManifest() throws IOException {
		if (this.manifest == null && this.entries.containsKey(MANIFEST_ENTRY_NAME)) {
			FileEntry entry = (FileEntry) this.entries.get(MANIFEST_ENTRY_NAME);
			FileInputStream inputStream = new FileInputStream(entry.getFile());
			try {
				this.manifest = new Manifest(inputStream);
			}
			finally {
				inputStream.close();
			}
		}
		return this.manifest;
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
		return Collections.unmodifiableCollection(this.entries.values());
	}

	protected Archive getNestedArchive(Entry entry) throws IOException {
		File file = ((FileEntry) entry).getFile();
		return (file.isDirectory() ? new ExplodedArchive(file) : new JarFileArchive(file));
	}

	@Override
	public Archive getFilteredArchive(EntryRenameFilter filter) throws IOException {
		Map<AsciiBytes, Entry> filteredEntries = new LinkedHashMap<AsciiBytes, Archive.Entry>();
		for (Map.Entry<AsciiBytes, Entry> entry : this.entries.entrySet()) {
			AsciiBytes filteredName = filter.apply(entry.getKey(), entry.getValue());
			if (filteredName != null) {
				filteredEntries.put(filteredName, new FileEntry(filteredName,
						((FileEntry) entry.getValue()).getFile()));
			}
		}
		return new ExplodedArchive(this.root, filteredEntries);
	}

	private class FileEntry implements Entry {

		private final AsciiBytes name;

		private final File file;

		public FileEntry(AsciiBytes name, File file) {
			this.name = name;
			this.file = file;
		}

		public File getFile() {
			return this.file;
		}

		@Override
		public boolean isDirectory() {
			return this.file.isDirectory();
		}

		@Override
		public AsciiBytes getName() {
			return this.name;
		}
	}

	/**
	 * {@link URLStreamHandler} that respects filtered entries.
	 */
	private class FilteredURLStreamHandler extends URLStreamHandler {

		public FilteredURLStreamHandler() {
		}

		@Override
		protected URLConnection openConnection(URL url) throws IOException {
			String name = url.getPath().substring(
					ExplodedArchive.this.root.toURI().getPath().length());
			if (ExplodedArchive.this.entries.containsKey(new AsciiBytes(name))) {
				return new URL(url.toString()).openConnection();
			}
			return new FileNotFoundURLConnection(url, name);
		}
	}

	/**
	 * {@link URLConnection} used to represent a filtered file.
	 */
	private static class FileNotFoundURLConnection extends URLConnection {

		private final String name;

		public FileNotFoundURLConnection(URL url, String name) {
			super(url);
			this.name = name;
		}

		@Override
		public void connect() throws IOException {
			throw new FileNotFoundException(this.name);
		}

	}

}
