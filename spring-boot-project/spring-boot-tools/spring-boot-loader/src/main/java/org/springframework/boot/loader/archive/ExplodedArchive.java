/*
 * Copyright 2012-2020 the original author or authors.
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
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.jar.Manifest;

/**
 * {@link Archive} implementation backed by an exploded archive directory.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @since 1.0.0
 */
public class ExplodedArchive implements Archive {

	private static final Set<String> SKIPPED_NAMES = new HashSet<>(Arrays.asList(".", ".."));

	private final File root;

	private final boolean recursive;

	private File manifestFile;

	private Manifest manifest;

	/**
	 * Create a new {@link ExplodedArchive} instance.
	 * @param root the root directory
	 */
	public ExplodedArchive(File root) {
		this(root, true);
	}

	/**
	 * Create a new {@link ExplodedArchive} instance.
	 * @param root the root directory
	 * @param recursive if recursive searching should be used to locate the manifest.
	 * Defaults to {@code true}, directories with a large tree might want to set this to
	 * {@code false}.
	 */
	public ExplodedArchive(File root, boolean recursive) {
		if (!root.exists() || !root.isDirectory()) {
			throw new IllegalArgumentException("Invalid source directory " + root);
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
			try (FileInputStream inputStream = new FileInputStream(this.manifestFile)) {
				this.manifest = new Manifest(inputStream);
			}
		}
		return this.manifest;
	}

	@Override
	public Iterator<Archive> getNestedArchives(EntryFilter searchFilter, EntryFilter includeFilter) throws IOException {
		return new ArchiveIterator(this.root, this.recursive, searchFilter, includeFilter);
	}

	@Override
	@Deprecated
	public Iterator<Entry> iterator() {
		return new EntryIterator(this.root, this.recursive, null, null);
	}

	protected Archive getNestedArchive(Entry entry) throws IOException {
		File file = ((FileEntry) entry).getFile();
		return (file.isDirectory() ? new ExplodedArchive(file) : new SimpleJarFileArchive((FileEntry) entry));
	}

	@Override
	public boolean isExploded() {
		return true;
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
	private abstract static class AbstractIterator<T> implements Iterator<T> {

		private static final Comparator<File> entryComparator = Comparator.comparing(File::getAbsolutePath);

		private final File root;

		private final boolean recursive;

		private final EntryFilter searchFilter;

		private final EntryFilter includeFilter;

		private final Deque<Iterator<File>> stack = new LinkedList<>();

		private FileEntry current;

		private String rootUrl;

		AbstractIterator(File root, boolean recursive, EntryFilter searchFilter, EntryFilter includeFilter) {
			this.root = root;
			this.rootUrl = this.root.toURI().getPath();
			this.recursive = recursive;
			this.searchFilter = searchFilter;
			this.includeFilter = includeFilter;
			this.stack.add(listFiles(root));
			this.current = poll();
		}

		@Override
		public boolean hasNext() {
			return this.current != null;
		}

		@Override
		public T next() {
			FileEntry entry = this.current;
			if (entry == null) {
				throw new NoSuchElementException();
			}
			this.current = poll();
			return adapt(entry);
		}

		private FileEntry poll() {
			while (!this.stack.isEmpty()) {
				while (this.stack.peek().hasNext()) {
					File file = this.stack.peek().next();
					if (SKIPPED_NAMES.contains(file.getName())) {
						continue;
					}
					FileEntry entry = getFileEntry(file);
					if (isListable(entry)) {
						this.stack.addFirst(listFiles(file));
					}
					if (this.includeFilter == null || this.includeFilter.matches(entry)) {
						return entry;
					}
				}
				this.stack.poll();
			}
			return null;
		}

		private FileEntry getFileEntry(File file) {
			URI uri = file.toURI();
			String name = uri.getPath().substring(this.rootUrl.length());
			try {
				return new FileEntry(name, file, uri.toURL());
			}
			catch (MalformedURLException ex) {
				throw new IllegalStateException(ex);
			}
		}

		private boolean isListable(FileEntry entry) {
			return entry.isDirectory() && (this.recursive || entry.getFile().getParentFile().equals(this.root))
					&& (this.searchFilter == null || this.searchFilter.matches(entry))
					&& (this.includeFilter == null || !this.includeFilter.matches(entry));
		}

		private Iterator<File> listFiles(File file) {
			File[] files = file.listFiles();
			if (files == null) {
				return Collections.emptyIterator();
			}
			Arrays.sort(files, entryComparator);
			return Arrays.asList(files).iterator();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("remove");
		}

		protected abstract T adapt(FileEntry entry);

	}

	private static class EntryIterator extends AbstractIterator<Entry> {

		EntryIterator(File root, boolean recursive, EntryFilter searchFilter, EntryFilter includeFilter) {
			super(root, recursive, searchFilter, includeFilter);
		}

		@Override
		protected Entry adapt(FileEntry entry) {
			return entry;
		}

	}

	private static class ArchiveIterator extends AbstractIterator<Archive> {

		ArchiveIterator(File root, boolean recursive, EntryFilter searchFilter, EntryFilter includeFilter) {
			super(root, recursive, searchFilter, includeFilter);
		}

		@Override
		protected Archive adapt(FileEntry entry) {
			File file = entry.getFile();
			return (file.isDirectory() ? new ExplodedArchive(file) : new SimpleJarFileArchive(entry));
		}

	}

	/**
	 * {@link Entry} backed by a File.
	 */
	private static class FileEntry implements Entry {

		private final String name;

		private final File file;

		private final URL url;

		FileEntry(String name, File file, URL url) {
			this.name = name;
			this.file = file;
			this.url = url;
		}

		File getFile() {
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

		URL getUrl() {
			return this.url;
		}

	}

	/**
	 * {@link Archive} implementation backed by a simple JAR file that doesn't itself
	 * contain nested archives.
	 */
	private static class SimpleJarFileArchive implements Archive {

		private final URL url;

		SimpleJarFileArchive(FileEntry file) {
			this.url = file.getUrl();
		}

		@Override
		public URL getUrl() throws MalformedURLException {
			return this.url;
		}

		@Override
		public Manifest getManifest() throws IOException {
			return null;
		}

		@Override
		public Iterator<Archive> getNestedArchives(EntryFilter searchFilter, EntryFilter includeFilter)
				throws IOException {
			return Collections.emptyIterator();
		}

		@Override
		@Deprecated
		public Iterator<Entry> iterator() {
			return Collections.emptyIterator();
		}

		@Override
		public String toString() {
			try {
				return getUrl().toString();
			}
			catch (Exception ex) {
				return "jar archive";
			}
		}

	}

}
