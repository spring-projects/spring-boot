/*
 * Copyright 2012-2016 the original author or authors.
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
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.jar.Manifest;

/**
 * {@link Archive} implementation backed by an exploded archive directory.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class ExplodedArchive implements Archive {

	private static final Set<String> SKIPPED_NAMES = new HashSet<String>(
			Arrays.asList(".", ".."));

	private final File root;

	private final boolean recursive;

	private File manifestFile;

	private Manifest manifest;

	/**
	 * Create a new {@link ExplodedArchive} instance.
	 * @param root the root folder
	 */
	public ExplodedArchive(File root) {
		this(root, true);
	}

	/**
	 * Create a new {@link ExplodedArchive} instance.
	 * @param root the root folder
	 * @param recursive if recursive searching should be used to locate the manifest.
	 * Defaults to {@code true}, folders with a large tree might want to set this to
	 * {@code
	 * false}.
	 */
	public ExplodedArchive(File root, boolean recursive) {
		if (!root.exists() || !root.isDirectory()) {
			throw new IllegalArgumentException("Invalid source folder " + root);
		}
		this.root = root;
		this.recursive = recursive;
		this.manifestFile = getManifestFile(root);
	}

	private File getManifestFile(File root) {
		File metaInf = new File(root, "META-INF");
		return new File(metaInf, "MANIFEST.MF");
	}

	@Override
	public URL getUrl() throws MalformedURLException {
		return this.root.toURI().toURL();
	}

	@Override
	public Manifest getManifest() throws IOException {
		if (this.manifest == null && this.manifestFile.exists()) {
			FileInputStream inputStream = new FileInputStream(this.manifestFile);
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
		for (Entry entry : this) {
			if (filter.matches(entry)) {
				nestedArchives.add(getNestedArchive(entry));
			}
		}
		return Collections.unmodifiableList(nestedArchives);
	}

	@Override
	public Iterator<Entry> iterator() {
		return new FileEntryIterator(this.root, this.recursive);
	}

	protected Archive getNestedArchive(Entry entry) throws IOException {
		File file = ((FileEntry) entry).getFile();
		return (file.isDirectory() ? new ExplodedArchive(file)
				: new JarFileArchive(file));
	}

	@Override
	public String toString() {
		try {
			return getUrl().toString();
		}
		catch (Exception ex) {
			return "exploded archive";
		}
	}

	/**
	 * File based {@link Entry} {@link Iterator}.
	 */
	private static class FileEntryIterator implements Iterator<Entry> {

		private final Comparator<File> entryComparator = new EntryComparator();

		private final File root;

		private final boolean recursive;

		private final Deque<Iterator<File>> stack = new LinkedList<Iterator<File>>();

		private File current;

		FileEntryIterator(File root, boolean recursive) {
			this.root = root;
			this.recursive = recursive;
			this.stack.add(listFiles(root));
			this.current = poll();
		}

		@Override
		public boolean hasNext() {
			return this.current != null;
		}

		@Override
		public Entry next() {
			if (this.current == null) {
				throw new NoSuchElementException();
			}
			File file = this.current;
			if (file.isDirectory()
					&& (this.recursive || file.getParentFile().equals(this.root))) {
				this.stack.addFirst(listFiles(file));
			}
			this.current = poll();
			String name = file.toURI().getPath()
					.substring(this.root.toURI().getPath().length());
			return new FileEntry(name, file);
		}

		private Iterator<File> listFiles(File file) {
			File[] files = file.listFiles();
			if (files == null) {
				return Collections.<File>emptyList().iterator();
			}
			Arrays.sort(files, this.entryComparator);
			return Arrays.asList(files).iterator();
		}

		private File poll() {
			while (!this.stack.isEmpty()) {
				while (this.stack.peek().hasNext()) {
					File file = this.stack.peek().next();
					if (!SKIPPED_NAMES.contains(file.getName())) {
						return file;
					}
				}
				this.stack.poll();
			}
			return null;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("remove");
		}

		/**
		 * {@link Comparator} that orders {@link File} entries by their absolute paths.
		 */
		private static class EntryComparator implements Comparator<File> {

			@Override
			public int compare(File o1, File o2) {
				return o1.getAbsolutePath().compareTo(o2.getAbsolutePath());
			}

		}

	}

	/**
	 * {@link Entry} backed by a File.
	 */
	private static class FileEntry implements Entry {

		private final String name;

		private final File file;

		FileEntry(String name, File file) {
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

}
