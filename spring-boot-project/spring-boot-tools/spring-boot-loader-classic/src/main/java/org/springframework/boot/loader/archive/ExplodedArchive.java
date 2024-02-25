/*
 * Copyright 2012-2023 the original author or authors.
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

	private final File manifestFile;

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

	/**
	 * Returns the manifest file located in the specified root directory.
	 * @param root the root directory where the manifest file is located
	 * @return the manifest file
	 */
	private File getManifestFile(File root) {
		File metaInf = new File(root, "META-INF");
		return new File(metaInf, "MANIFEST.MF");
	}

	/**
	 * Returns the URL of the root directory of this ExplodedArchive.
	 * @return the URL of the root directory
	 * @throws MalformedURLException if the URL is malformed
	 */
	@Override
	public URL getUrl() throws MalformedURLException {
		return this.root.toURI().toURL();
	}

	/**
	 * Retrieves the manifest of the exploded archive.
	 * @return The manifest of the exploded archive.
	 * @throws IOException If an I/O error occurs while reading the manifest file.
	 */
	@Override
	public Manifest getManifest() throws IOException {
		if (this.manifest == null && this.manifestFile.exists()) {
			try (FileInputStream inputStream = new FileInputStream(this.manifestFile)) {
				this.manifest = new Manifest(inputStream);
			}
		}
		return this.manifest;
	}

	/**
	 * Returns an iterator over the nested archives within this exploded archive.
	 * @param searchFilter the filter used to search for specific entries within the
	 * nested archives
	 * @param includeFilter the filter used to include specific entries within the nested
	 * archives
	 * @return an iterator over the nested archives
	 * @throws IOException if an I/O error occurs while accessing the nested archives
	 */
	@Override
	public Iterator<Archive> getNestedArchives(EntryFilter searchFilter, EntryFilter includeFilter) throws IOException {
		return new ArchiveIterator(this.root, this.recursive, searchFilter, includeFilter);
	}

	/**
	 * Returns an iterator over the entries in this ExplodedArchive.
	 * @return an iterator over the entries in this ExplodedArchive
	 * @deprecated This method is deprecated since version 2.3.10 and will not be removed.
	 * Use {@link EntryIterator} instead.
	 */
	@Override
	@Deprecated(since = "2.3.10", forRemoval = false)
	public Iterator<Entry> iterator() {
		return new EntryIterator(this.root, this.recursive, null, null);
	}

	/**
	 * Retrieves a nested archive from the given entry.
	 * @param entry the entry from which to retrieve the nested archive
	 * @return the nested archive
	 */
	protected Archive getNestedArchive(Entry entry) {
		File file = ((FileEntry) entry).getFile();
		return (file.isDirectory() ? new ExplodedArchive(file) : new SimpleJarFileArchive((FileEntry) entry));
	}

	/**
	 * Returns whether the archive is exploded.
	 * @return {@code true} if the archive is exploded, {@code false} otherwise.
	 */
	@Override
	public boolean isExploded() {
		return true;
	}

	/**
	 * Returns a string representation of the object.
	 * @return the URL as a string if available, or "exploded archive" if an exception
	 * occurs
	 */
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

		private final String rootUrl;

		/**
		 * Constructs a new AbstractIterator object with the specified parameters.
		 * @param root the root directory or file from which to start iterating
		 * @param recursive true if the iteration should be recursive, false otherwise
		 * @param searchFilter the filter used to determine which entries to search for
		 * @param includeFilter the filter used to determine which entries to include in
		 * the iteration
		 */
		AbstractIterator(File root, boolean recursive, EntryFilter searchFilter, EntryFilter includeFilter) {
			this.root = root;
			this.rootUrl = this.root.toURI().getPath();
			this.recursive = recursive;
			this.searchFilter = searchFilter;
			this.includeFilter = includeFilter;
			this.stack.add(listFiles(root));
			this.current = poll();
		}

		/**
		 * Returns true if there is a next element in the iterator, false otherwise.
		 * @return true if there is a next element, false otherwise
		 */
		@Override
		public boolean hasNext() {
			return this.current != null;
		}

		/**
		 * Returns the next element in the iteration.
		 * @throws NoSuchElementException if there are no more elements to iterate over
		 * @return the next element in the iteration
		 */
		@Override
		public T next() {
			FileEntry entry = this.current;
			if (entry == null) {
				throw new NoSuchElementException();
			}
			this.current = poll();
			return adapt(entry);
		}

		/**
		 * Retrieves the next file entry from the stack.
		 * @return The next file entry from the stack, or null if there are no more
		 * entries.
		 */
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

		/**
		 * Retrieves the FileEntry object for the given File.
		 * @param file The File object for which to retrieve the FileEntry.
		 * @return The FileEntry object representing the given File.
		 * @throws IllegalStateException if the URL of the FileEntry cannot be created.
		 */
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

		/**
		 * Checks if the given FileEntry is listable.
		 * @param entry the FileEntry to check
		 * @return true if the entry is listable, false otherwise
		 */
		private boolean isListable(FileEntry entry) {
			return entry.isDirectory() && (this.recursive || entry.getFile().getParentFile().equals(this.root))
					&& (this.searchFilter == null || this.searchFilter.matches(entry))
					&& (this.includeFilter == null || !this.includeFilter.matches(entry));
		}

		/**
		 * Returns an iterator over the files in the specified directory.
		 * @param file the directory to list files from
		 * @return an iterator over the files in the directory, or an empty iterator if
		 * the directory is empty or does not exist
		 */
		private Iterator<File> listFiles(File file) {
			File[] files = file.listFiles();
			if (files == null) {
				return Collections.emptyIterator();
			}
			Arrays.sort(files, entryComparator);
			return Arrays.asList(files).iterator();
		}

		/**
		 * Removes the last element returned by the iterator. This operation is not
		 * supported and will always throw an UnsupportedOperationException.
		 * @throws UnsupportedOperationException if the remove operation is not supported
		 */
		@Override
		public void remove() {
			throw new UnsupportedOperationException("remove");
		}

		/**
		 * Adapts a FileEntry to a specific type T.
		 * @param entry the FileEntry to be adapted
		 * @return the adapted object of type T
		 */
		protected abstract T adapt(FileEntry entry);

	}

	/**
	 * EntryIterator class.
	 */
	private static class EntryIterator extends AbstractIterator<Entry> {

		/**
		 * Constructs a new EntryIterator object with the specified parameters.
		 * @param root the root directory or file from which to start iterating
		 * @param recursive true if the iteration should be recursive, false otherwise
		 * @param searchFilter the filter used to determine which entries to search for
		 * @param includeFilter the filter used to determine which entries to include in
		 * the iteration
		 */
		EntryIterator(File root, boolean recursive, EntryFilter searchFilter, EntryFilter includeFilter) {
			super(root, recursive, searchFilter, includeFilter);
		}

		/**
		 * Adapts a FileEntry to an Entry.
		 * @param entry the FileEntry to be adapted
		 * @return the adapted Entry
		 */
		@Override
		protected Entry adapt(FileEntry entry) {
			return entry;
		}

	}

	/**
	 * ArchiveIterator class.
	 */
	private static class ArchiveIterator extends AbstractIterator<Archive> {

		/**
		 * Constructs a new ArchiveIterator object with the specified root directory,
		 * recursive flag, search filter, and include filter.
		 * @param root the root directory from which to start iterating
		 * @param recursive a boolean flag indicating whether to iterate recursively
		 * through subdirectories
		 * @param searchFilter the filter used to determine which entries to search for
		 * @param includeFilter the filter used to determine which entries to include in
		 * the iteration
		 */
		ArchiveIterator(File root, boolean recursive, EntryFilter searchFilter, EntryFilter includeFilter) {
			super(root, recursive, searchFilter, includeFilter);
		}

		/**
		 * Adapts a FileEntry to an Archive.
		 * @param entry the FileEntry to be adapted
		 * @return the adapted Archive
		 */
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

		/**
		 * Constructs a new FileEntry object with the specified name, file, and URL.
		 * @param name the name of the file entry
		 * @param file the file associated with the file entry
		 * @param url the URL associated with the file entry
		 */
		FileEntry(String name, File file, URL url) {
			this.name = name;
			this.file = file;
			this.url = url;
		}

		/**
		 * Returns the file associated with this FileEntry.
		 * @return the file associated with this FileEntry
		 */
		File getFile() {
			return this.file;
		}

		/**
		 * Returns a boolean value indicating whether the FileEntry object represents a
		 * directory.
		 * @return true if the FileEntry object represents a directory, false otherwise.
		 */
		@Override
		public boolean isDirectory() {
			return this.file.isDirectory();
		}

		/**
		 * Returns the name of the FileEntry.
		 * @return the name of the FileEntry
		 */
		@Override
		public String getName() {
			return this.name;
		}

		/**
		 * Returns the URL associated with this FileEntry.
		 * @return the URL associated with this FileEntry
		 */
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

		/**
		 * Constructs a SimpleJarFileArchive object with the specified FileEntry.
		 * @param file the FileEntry representing the file to be archived
		 */
		SimpleJarFileArchive(FileEntry file) {
			this.url = file.getUrl();
		}

		/**
		 * Returns the URL of the SimpleJarFileArchive.
		 * @return the URL of the SimpleJarFileArchive
		 * @throws MalformedURLException if the URL is malformed
		 */
		@Override
		public URL getUrl() throws MalformedURLException {
			return this.url;
		}

		/**
		 * Retrieves the manifest file associated with this SimpleJarFileArchive.
		 * @return The manifest file, or null if it does not exist.
		 * @throws IOException If an I/O error occurs while retrieving the manifest file.
		 */
		@Override
		public Manifest getManifest() throws IOException {
			return null;
		}

		/**
		 * Returns an empty iterator of Archive objects representing nested archives
		 * within this SimpleJarFileArchive.
		 * @param searchFilter the filter used to search for specific entries within the
		 * nested archives
		 * @param includeFilter the filter used to include specific entries within the
		 * nested archives
		 * @return an empty iterator of Archive objects
		 * @throws IOException if an I/O error occurs while accessing the nested archives
		 */
		@Override
		public Iterator<Archive> getNestedArchives(EntryFilter searchFilter, EntryFilter includeFilter)
				throws IOException {
			return Collections.emptyIterator();
		}

		/**
		 * Returns an iterator over the entries in this SimpleJarFileArchive.
		 * @return an iterator over the entries in this SimpleJarFileArchive
		 * @deprecated This method has been deprecated since version 2.3.10 and will not
		 * be removed in future versions. It is recommended to use an alternative method
		 * instead.
		 */
		@Override
		@Deprecated(since = "2.3.10", forRemoval = false)
		public Iterator<Entry> iterator() {
			return Collections.emptyIterator();
		}

		/**
		 * Returns a string representation of the object.
		 * @return the URL of the jar archive if available, otherwise returns "jar
		 * archive"
		 */
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
