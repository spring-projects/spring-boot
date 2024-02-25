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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;

import org.springframework.boot.loader.jar.JarFile;

/**
 * {@link Archive} implementation backed by a {@link JarFile}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.0.0
 */
public class JarFileArchive implements Archive {

	private static final String UNPACK_MARKER = "UNPACK:";

	private static final int BUFFER_SIZE = 32 * 1024;

	private static final FileAttribute<?>[] NO_FILE_ATTRIBUTES = {};

	private static final EnumSet<PosixFilePermission> DIRECTORY_PERMISSIONS = EnumSet.of(PosixFilePermission.OWNER_READ,
			PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE);

	private static final EnumSet<PosixFilePermission> FILE_PERMISSIONS = EnumSet.of(PosixFilePermission.OWNER_READ,
			PosixFilePermission.OWNER_WRITE);

	private final JarFile jarFile;

	private URL url;

	private Path tempUnpackDirectory;

	/**
     * Constructs a new JarFileArchive using the specified file.
     * 
     * @param file the file representing the jar archive
     * @throws IOException if an I/O error occurs while reading the file
     */
    public JarFileArchive(File file) throws IOException {
		this(file, file.toURI().toURL());
	}

	/**
     * Constructs a new JarFileArchive with the specified file and URL.
     * 
     * @param file the file representing the JAR file
     * @param url the URL representing the location of the JAR file
     * @throws IOException if an I/O error occurs while reading the JAR file
     */
    public JarFileArchive(File file, URL url) throws IOException {
		this(new JarFile(file));
		this.url = url;
	}

	/**
     * Constructs a new JarFileArchive with the specified JarFile.
     * 
     * @param jarFile the JarFile to be used for the archive
     */
    public JarFileArchive(JarFile jarFile) {
		this.jarFile = jarFile;
	}

	/**
     * Returns the URL of the JarFileArchive.
     * 
     * @return the URL of the JarFileArchive
     * @throws MalformedURLException if the URL is malformed
     */
    @Override
	public URL getUrl() throws MalformedURLException {
		if (this.url != null) {
			return this.url;
		}
		return this.jarFile.getUrl();
	}

	/**
     * Returns the manifest of this JarFileArchive.
     * 
     * @return the manifest of this JarFileArchive
     * @throws IOException if an I/O error occurs while reading the manifest
     */
    @Override
	public Manifest getManifest() throws IOException {
		return this.jarFile.getManifest();
	}

	/**
     * Returns an iterator over the nested archives within this JarFileArchive that match the given search filter and include filter.
     *
     * @param searchFilter the filter used to search for nested archives
     * @param includeFilter the filter used to include nested archives in the iterator
     * @return an iterator over the nested archives
     * @throws IOException if an I/O error occurs while accessing the nested archives
     */
    @Override
	public Iterator<Archive> getNestedArchives(EntryFilter searchFilter, EntryFilter includeFilter) throws IOException {
		return new NestedArchiveIterator(this.jarFile.iterator(), searchFilter, includeFilter);
	}

	/**
     * Returns an iterator over the entries in this JarFileArchive.
     * 
     * @return an iterator over the entries in this JarFileArchive
     * 
     * @deprecated This method is deprecated since version 2.3.10 and will not be removed.
     *             Use {@link #entryIterator()} instead.
     */
    @Override
	@Deprecated(since = "2.3.10", forRemoval = false)
	public Iterator<Entry> iterator() {
		return new EntryIterator(this.jarFile.iterator(), null, null);
	}

	/**
     * Closes the JarFileArchive.
     * 
     * @throws IOException if an I/O error occurs while closing the JarFileArchive
     */
    @Override
	public void close() throws IOException {
		this.jarFile.close();
	}

	/**
     * Retrieves a nested archive from the given entry.
     * 
     * @param entry The entry from which to retrieve the nested archive.
     * @return The nested archive.
     * @throws IOException If an I/O error occurs while retrieving the nested archive.
     * @throws IllegalStateException If an exception occurs while getting the nested archive.
     */
    protected Archive getNestedArchive(Entry entry) throws IOException {
		JarEntry jarEntry = ((JarFileEntry) entry).getJarEntry();
		if (jarEntry.getComment().startsWith(UNPACK_MARKER)) {
			return getUnpackedNestedArchive(jarEntry);
		}
		try {
			JarFile jarFile = this.jarFile.getNestedJarFile(jarEntry);
			return new JarFileArchive(jarFile);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to get nested archive for entry " + entry.getName(), ex);
		}
	}

	/**
     * Retrieves an unpacked nested archive from the given JarEntry.
     * 
     * @param jarEntry The JarEntry representing the nested archive.
     * @return The unpacked nested archive.
     * @throws IOException If an I/O error occurs during the unpacking process.
     */
    private Archive getUnpackedNestedArchive(JarEntry jarEntry) throws IOException {
		String name = jarEntry.getName();
		if (name.lastIndexOf('/') != -1) {
			name = name.substring(name.lastIndexOf('/') + 1);
		}
		Path path = getTempUnpackDirectory().resolve(name);
		if (!Files.exists(path) || Files.size(path) != jarEntry.getSize()) {
			unpack(jarEntry, path);
		}
		return new JarFileArchive(path.toFile(), path.toUri().toURL());
	}

	/**
     * Returns the temporary unpack directory for the JarFileArchive.
     * If the temporary unpack directory has not been set, it creates a new one in the system's temporary directory.
     * 
     * @return the temporary unpack directory
     */
    private Path getTempUnpackDirectory() {
		if (this.tempUnpackDirectory == null) {
			Path tempDirectory = Paths.get(System.getProperty("java.io.tmpdir"));
			this.tempUnpackDirectory = createUnpackDirectory(tempDirectory);
		}
		return this.tempUnpackDirectory;
	}

	/**
     * Creates an unpack directory for the JarFileArchive.
     * 
     * @param parent the parent directory where the unpack directory will be created
     * @return the path of the created unpack directory
     * @throws IllegalStateException if the unpack directory cannot be created after 1000 attempts
     */
    private Path createUnpackDirectory(Path parent) {
		int attempts = 0;
		while (attempts++ < 1000) {
			String fileName = Paths.get(this.jarFile.getName()).getFileName().toString();
			Path unpackDirectory = parent.resolve(fileName + "-spring-boot-libs-" + UUID.randomUUID());
			try {
				createDirectory(unpackDirectory);
				return unpackDirectory;
			}
			catch (IOException ex) {
				// Ignore
			}
		}
		throw new IllegalStateException("Failed to create unpack directory in directory '" + parent + "'");
	}

	/**
     * Unpacks a specific entry from the JAR file and saves it to the specified path.
     * 
     * @param entry The entry to be unpacked from the JAR file.
     * @param path The path where the unpacked file will be saved.
     * @throws IOException If an I/O error occurs during the unpacking process.
     */
    private void unpack(JarEntry entry, Path path) throws IOException {
		createFile(path);
		path.toFile().deleteOnExit();
		try (InputStream inputStream = this.jarFile.getInputStream(entry);
				OutputStream outputStream = Files.newOutputStream(path, StandardOpenOption.WRITE,
						StandardOpenOption.TRUNCATE_EXISTING)) {
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}
			outputStream.flush();
		}
	}

	/**
     * Creates a directory at the specified path.
     * 
     * @param path the path where the directory should be created
     * @throws IOException if an I/O error occurs while creating the directory
     */
    private void createDirectory(Path path) throws IOException {
		Files.createDirectory(path, getFileAttributes(path.getFileSystem(), DIRECTORY_PERMISSIONS));
	}

	/**
     * Creates a new file at the specified path.
     * 
     * @param path the path where the file should be created
     * @throws IOException if an I/O error occurs while creating the file
     */
    private void createFile(Path path) throws IOException {
		Files.createFile(path, getFileAttributes(path.getFileSystem(), FILE_PERMISSIONS));
	}

	/**
     * Retrieves the file attributes for a given file system and set of owner read/write permissions.
     * 
     * @param fileSystem The file system to retrieve the attributes from.
     * @param ownerReadWrite The set of owner read/write permissions.
     * @return An array of file attributes.
     */
    private FileAttribute<?>[] getFileAttributes(FileSystem fileSystem, EnumSet<PosixFilePermission> ownerReadWrite) {
		if (!fileSystem.supportedFileAttributeViews().contains("posix")) {
			return NO_FILE_ATTRIBUTES;
		}
		return new FileAttribute<?>[] { PosixFilePermissions.asFileAttribute(ownerReadWrite) };
	}

	/**
     * Returns a string representation of the JarFileArchive object.
     * 
     * @return the URL of the JarFileArchive if available, otherwise returns "jar archive"
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

	/**
	 * Abstract base class for iterator implementations.
	 */
	private abstract static class AbstractIterator<T> implements Iterator<T> {

		private final Iterator<JarEntry> iterator;

		private final EntryFilter searchFilter;

		private final EntryFilter includeFilter;

		private Entry current;

		/**
         * Constructs a new AbstractIterator with the specified iterator, search filter, and include filter.
         * 
         * @param iterator the iterator to be used for iterating over JarEntry objects
         * @param searchFilter the filter used to determine if a JarEntry should be considered during iteration
         * @param includeFilter the filter used to determine if a JarEntry should be included in the iteration results
         */
        AbstractIterator(Iterator<JarEntry> iterator, EntryFilter searchFilter, EntryFilter includeFilter) {
			this.iterator = iterator;
			this.searchFilter = searchFilter;
			this.includeFilter = includeFilter;
			this.current = poll();
		}

		/**
         * Returns true if there is a next element in the iterator, false otherwise.
         * 
         * @return true if there is a next element, false otherwise
         */
        @Override
		public boolean hasNext() {
			return this.current != null;
		}

		/**
         * Returns the next element in the iteration.
         * 
         * @return the next element in the iteration
         * @throws NoSuchElementException if there are no more elements to iterate over
         */
        @Override
		public T next() {
			T result = adapt(this.current);
			this.current = poll();
			return result;
		}

		/**
         * Returns the next entry in the iterator that matches the search and include filters.
         * 
         * @return the next entry that matches the filters, or null if no more entries are available
         */
        private Entry poll() {
			while (this.iterator.hasNext()) {
				JarFileEntry candidate = new JarFileEntry(this.iterator.next());
				if ((this.searchFilter == null || this.searchFilter.matches(candidate))
						&& (this.includeFilter == null || this.includeFilter.matches(candidate))) {
					return candidate;
				}
			}
			return null;
		}

		/**
         * Adapts the given entry to a specific type.
         *
         * @param entry the entry to be adapted
         * @return the adapted entry of type T
         */
        protected abstract T adapt(Entry entry);

	}

	/**
	 * {@link Archive.Entry} iterator implementation backed by {@link JarEntry}.
	 */
	private static class EntryIterator extends AbstractIterator<Entry> {

		/**
         * Constructs a new EntryIterator object with the specified iterator, search filter, and include filter.
         * 
         * @param iterator the iterator to be used for iterating over JarEntry objects
         * @param searchFilter the filter used to determine if a JarEntry should be included in the iteration
         * @param includeFilter the filter used to determine if a JarEntry should be included in the final result
         */
        EntryIterator(Iterator<JarEntry> iterator, EntryFilter searchFilter, EntryFilter includeFilter) {
			super(iterator, searchFilter, includeFilter);
		}

		/**
         * Adapts the given entry.
         * 
         * @param entry the entry to be adapted
         * @return the adapted entry
         */
        @Override
		protected Entry adapt(Entry entry) {
			return entry;
		}

	}

	/**
	 * Nested {@link Archive} iterator implementation backed by {@link JarEntry}.
	 */
	private class NestedArchiveIterator extends AbstractIterator<Archive> {

		/**
         * Constructs a new NestedArchiveIterator with the specified iterator, search filter, and include filter.
         * 
         * @param iterator the iterator to be used for iterating over the Jar entries
         * @param searchFilter the filter used to determine which entries to search for
         * @param includeFilter the filter used to determine which entries to include in the iteration
         */
        NestedArchiveIterator(Iterator<JarEntry> iterator, EntryFilter searchFilter, EntryFilter includeFilter) {
			super(iterator, searchFilter, includeFilter);
		}

		/**
         * Adapts the given entry to an Archive object.
         * 
         * @param entry the entry to be adapted
         * @return the adapted Archive object
         * @throws IllegalStateException if an IOException occurs during the adaptation process
         */
        @Override
		protected Archive adapt(Entry entry) {
			try {
				return getNestedArchive(entry);
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		}

	}

	/**
	 * {@link Archive.Entry} implementation backed by a {@link JarEntry}.
	 */
	private static class JarFileEntry implements Entry {

		private final JarEntry jarEntry;

		/**
         * Constructs a new JarFileEntry object with the specified JarEntry.
         * 
         * @param jarEntry the JarEntry object to be associated with this JarFileEntry
         */
        JarFileEntry(JarEntry jarEntry) {
			this.jarEntry = jarEntry;
		}

		/**
         * Returns the JarEntry associated with this JarFileEntry.
         *
         * @return the JarEntry object associated with this JarFileEntry
         */
        JarEntry getJarEntry() {
			return this.jarEntry;
		}

		/**
         * Returns a boolean value indicating whether the JarFileEntry is a directory.
         * 
         * @return true if the JarFileEntry is a directory, false otherwise
         */
        @Override
		public boolean isDirectory() {
			return this.jarEntry.isDirectory();
		}

		/**
         * Returns the name of the JarFileEntry.
         *
         * @return the name of the JarFileEntry
         */
        @Override
		public String getName() {
			return this.jarEntry.getName();
		}

	}

}
