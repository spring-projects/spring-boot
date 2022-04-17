/*
 * Copyright 2012-2021 the original author or authors.
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

	public JarFileArchive(File file) throws IOException {
		this(file, file.toURI().toURL());
	}

	public JarFileArchive(File file, URL url) throws IOException {
		this(new JarFile(file));
		this.url = url;
	}

	public JarFileArchive(JarFile jarFile) {
		this.jarFile = jarFile;
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
	public Iterator<Archive> getNestedArchives(EntryFilter searchFilter, EntryFilter includeFilter) throws IOException {
		return new NestedArchiveIterator(this.jarFile.iterator(), searchFilter, includeFilter);
	}

	@Override
	@Deprecated
	public Iterator<Entry> iterator() {
		return new EntryIterator(this.jarFile.iterator(), null, null);
	}

	@Override
	public void close() throws IOException {
		this.jarFile.close();
	}

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

	private Path getTempUnpackDirectory() {
		if (this.tempUnpackDirectory == null) {
			Path tempDirectory = Paths.get(System.getProperty("java.io.tmpdir"));
			this.tempUnpackDirectory = createUnpackDirectory(tempDirectory);
		}
		return this.tempUnpackDirectory;
	}

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
			}
		}
		throw new IllegalStateException("Failed to create unpack directory in directory '" + parent + "'");
	}

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

	private void createDirectory(Path path) throws IOException {
		Files.createDirectory(path, getFileAttributes(path.getFileSystem(), DIRECTORY_PERMISSIONS));
	}

	private void createFile(Path path) throws IOException {
		Files.createFile(path, getFileAttributes(path.getFileSystem(), FILE_PERMISSIONS));
	}

	private FileAttribute<?>[] getFileAttributes(FileSystem fileSystem, EnumSet<PosixFilePermission> ownerReadWrite) {
		if (!fileSystem.supportedFileAttributeViews().contains("posix")) {
			return NO_FILE_ATTRIBUTES;
		}
		return new FileAttribute<?>[] { PosixFilePermissions.asFileAttribute(ownerReadWrite) };
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

	/**
	 * Abstract base class for iterator implementations.
	 */
	private abstract static class AbstractIterator<T> implements Iterator<T> {

		private final Iterator<JarEntry> iterator;

		private final EntryFilter searchFilter;

		private final EntryFilter includeFilter;

		private Entry current;

		AbstractIterator(Iterator<JarEntry> iterator, EntryFilter searchFilter, EntryFilter includeFilter) {
			this.iterator = iterator;
			this.searchFilter = searchFilter;
			this.includeFilter = includeFilter;
			this.current = poll();
		}

		@Override
		public boolean hasNext() {
			return this.current != null;
		}

		@Override
		public T next() {
			T result = adapt(this.current);
			this.current = poll();
			return result;
		}

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

		protected abstract T adapt(Entry entry);

	}

	/**
	 * {@link Archive.Entry} iterator implementation backed by {@link JarEntry}.
	 */
	private static class EntryIterator extends AbstractIterator<Entry> {

		EntryIterator(Iterator<JarEntry> iterator, EntryFilter searchFilter, EntryFilter includeFilter) {
			super(iterator, searchFilter, includeFilter);
		}

		@Override
		protected Entry adapt(Entry entry) {
			return entry;
		}

	}

	/**
	 * Nested {@link Archive} iterator implementation backed by {@link JarEntry}.
	 */
	private class NestedArchiveIterator extends AbstractIterator<Archive> {

		NestedArchiveIterator(Iterator<JarEntry> iterator, EntryFilter searchFilter, EntryFilter includeFilter) {
			super(iterator, searchFilter, includeFilter);
		}

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

		JarFileEntry(JarEntry jarEntry) {
			this.jarEntry = jarEntry;
		}

		JarEntry getJarEntry() {
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
