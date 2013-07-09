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

package org.springframework.launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

/**
 * {@link Archive} implementation backed by an exploded archive directory.
 * 
 * @author Phillip Webb
 */
public class ExplodedArchive implements Archive {

	private static final Set<String> SKIPPED_NAMES = new HashSet<String>(Arrays.asList(
			".", ".."));

	private static final Object MANIFEST_ENTRY_NAME = "META-INF/MANIFEST.MF";

	private File root;

	private Map<String, Entry> entries = new LinkedHashMap<String, Entry>();

	private Manifest manifest;

	public ExplodedArchive(File root) {
		if (!root.exists() || !root.isDirectory()) {
			throw new IllegalArgumentException("Invalid source folder " + root);
		}
		this.root = root;
		buildEntries(root);
		this.entries = Collections.unmodifiableMap(this.entries);
	}

	private ExplodedArchive(File root, Map<String, Entry> entries) {
		this.root = root;
		this.entries = Collections.unmodifiableMap(entries);
	}

	private void buildEntries(File file) {
		if (!file.equals(this.root)) {
			String name = file.getAbsolutePath().substring(
					this.root.getAbsolutePath().length() + 1);
			if (file.isDirectory()) {
				name += "/";
			}
			this.entries.put(name, new FileEntry(name, file));
		}
		if (file.isDirectory()) {
			for (File child : file.listFiles()) {
				if (!SKIPPED_NAMES.contains(child.getName())) {
					buildEntries(child);
				}
			}
		}
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
	public Iterable<Entry> getEntries() {
		return this.entries.values();
	}

	@Override
	public URL getUrl() throws MalformedURLException {
		FilteredURLStreamHandler handler = new FilteredURLStreamHandler();
		return new URL("file", "", -1, this.root.getAbsolutePath() + "/", handler);
		// return this.root.toURI().toURL();
	}

	@Override
	public Archive getNestedArchive(Entry entry) throws IOException {
		File file = ((FileEntry) entry).getFile();
		return (file.isDirectory() ? new ExplodedArchive(file) : new JarFileArchive(file));
	}

	@Override
	public Archive getFilteredArchive(EntryFilter filter) throws IOException {
		Map<String, Entry> filteredEntries = new LinkedHashMap<String, Archive.Entry>();
		for (Map.Entry<String, Entry> entry : this.entries.entrySet()) {
			String filteredName = filter.apply(entry.getKey(), entry.getValue());
			if (filteredName != null) {
				filteredEntries.put(filteredName, new FileEntry(filteredName,
						((FileEntry) entry.getValue()).getFile()));
			}
		}
		return new ExplodedArchive(this.root, filteredEntries);
	}

	private class FileEntry implements Entry {

		private final String name;
		private final File file;

		public FileEntry(String name, File file) {
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
		public String getName() {
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
					ExplodedArchive.this.root.getAbsolutePath().length() + 1);
			if (ExplodedArchive.this.entries.containsKey(name)) {
				return new URL(url.toString()).openConnection();
			}
			return new FileNotFoundURLConnection(url, name);
		}
	}

	/**
	 * {@link URLConnection} used to represent a filtered file.
	 */
	private static class FileNotFoundURLConnection extends URLConnection {

		private String name;

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
