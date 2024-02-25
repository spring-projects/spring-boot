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

package org.springframework.boot.loader.launch;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.springframework.boot.loader.net.protocol.jar.JarUrl;

/**
 * {@link Archive} implementation backed by a {@link JarFile}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class JarFileArchive implements Archive {

	private static final String UNPACK_MARKER = "UNPACK:";

	private static final FileAttribute<?>[] NO_FILE_ATTRIBUTES = {};

	private static final FileAttribute<?>[] DIRECTORY_PERMISSION_ATTRIBUTES = asFileAttributes(
			PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE);

	private static final FileAttribute<?>[] FILE_PERMISSION_ATTRIBUTES = asFileAttributes(
			PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);

	private static final Path TEMP = Paths.get(System.getProperty("java.io.tmpdir"));

	private final File file;

	private final JarFile jarFile;

	private volatile Path tempUnpackDirectory;

	/**
     * Constructs a new JarFileArchive using the specified file.
     * 
     * @param file the file to be used for creating the JarFileArchive
     * @throws IOException if an I/O error occurs while creating the JarFileArchive
     */
    JarFileArchive(File file) throws IOException {
		this(file, new JarFile(file));
	}

	/**
     * Constructs a new JarFileArchive with the specified file and JarFile.
     * 
     * @param file the file representing the Jar archive
     * @param jarFile the JarFile instance representing the Jar archive
     */
    private JarFileArchive(File file, JarFile jarFile) {
		this.file = file;
		this.jarFile = jarFile;
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
     * Returns a set of URLs representing the classpath entries in the JAR file.
     * 
     * @param includeFilter         a predicate to filter the entries to include in the classpath
     * @param directorySearchFilter a predicate to filter the entries to search for nested JAR files
     * @return a set of URLs representing the classpath entries
     * @throws IOException if an I/O error occurs while accessing the JAR file
     */
    @Override
	public Set<URL> getClassPathUrls(Predicate<Entry> includeFilter, Predicate<Entry> directorySearchFilter)
			throws IOException {
		return this.jarFile.stream()
			.map(JarArchiveEntry::new)
			.filter(includeFilter)
			.map(this::getNestedJarUrl)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
     * Returns the URL of a nested JAR file within the given JarArchiveEntry.
     * 
     * @param archiveEntry the JarArchiveEntry representing the nested JAR file
     * @return the URL of the nested JAR file
     * @throws UncheckedIOException if an I/O error occurs while getting the URL
     */
    private URL getNestedJarUrl(JarArchiveEntry archiveEntry) {
		try {
			JarEntry jarEntry = archiveEntry.jarEntry();
			String comment = jarEntry.getComment();
			if (comment != null && comment.startsWith(UNPACK_MARKER)) {
				return getUnpackedNestedJarUrl(jarEntry);
			}
			return JarUrl.create(this.file, jarEntry);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/**
     * Retrieves the URL of an unpacked nested JAR file.
     * 
     * @param jarEntry The JAR entry representing the nested JAR file.
     * @return The URL of the unpacked nested JAR file.
     * @throws IOException If an I/O error occurs during the process.
     */
    private URL getUnpackedNestedJarUrl(JarEntry jarEntry) throws IOException {
		String name = jarEntry.getName();
		if (name.lastIndexOf('/') != -1) {
			name = name.substring(name.lastIndexOf('/') + 1);
		}
		Path path = getTempUnpackDirectory().resolve(name);
		if (!Files.exists(path) || Files.size(path) != jarEntry.getSize()) {
			unpack(jarEntry, path);
		}
		return path.toUri().toURL();
	}

	/**
     * Returns the temporary unpack directory for the JarFileArchive.
     * If the temporary unpack directory has already been set, it will be returned.
     * Otherwise, a new temporary unpack directory will be created and set.
     * 
     * @return the temporary unpack directory
     */
    private Path getTempUnpackDirectory() {
		Path tempUnpackDirectory = this.tempUnpackDirectory;
		if (tempUnpackDirectory != null) {
			return tempUnpackDirectory;
		}
		synchronized (TEMP) {
			tempUnpackDirectory = this.tempUnpackDirectory;
			if (tempUnpackDirectory == null) {
				tempUnpackDirectory = createUnpackDirectory(TEMP);
				this.tempUnpackDirectory = tempUnpackDirectory;
			}
		}
		return tempUnpackDirectory;
	}

	/**
     * Creates an unpack directory for the JarFileArchive.
     * 
     * @param parent the parent directory where the unpack directory will be created
     * @return the path of the created unpack directory
     * @throws IllegalStateException if the unpack directory cannot be created after 100 attempts
     */
    private Path createUnpackDirectory(Path parent) {
		int attempts = 0;
		String fileName = Paths.get(this.jarFile.getName()).getFileName().toString();
		while (attempts++ < 100) {
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
     * Creates a directory at the specified path.
     * 
     * @param path the path where the directory should be created
     * @throws IOException if an I/O error occurs while creating the directory
     */
    private void createDirectory(Path path) throws IOException {
		Files.createDirectory(path, getFileAttributes(path, DIRECTORY_PERMISSION_ATTRIBUTES));
	}

	/**
     * Unpacks a specific entry from the JAR file and saves it to the specified path.
     * 
     * @param entry The entry to unpack from the JAR file.
     * @param path The path where the unpacked file will be saved.
     * @throws IOException If an I/O error occurs during the unpacking process.
     */
    private void unpack(JarEntry entry, Path path) throws IOException {
		createFile(path);
		path.toFile().deleteOnExit();
		try (InputStream in = this.jarFile.getInputStream(entry)) {
			Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/**
     * Creates a new file at the specified path.
     * 
     * @param path the path where the file should be created
     * @throws IOException if an I/O error occurs while creating the file
     */
    private void createFile(Path path) throws IOException {
		Files.createFile(path, getFileAttributes(path, FILE_PERMISSION_ATTRIBUTES));
	}

	/**
     * Returns an array of file attributes for the given path.
     * 
     * @param path The path for which to retrieve the file attributes.
     * @param permissionAttributes The file attributes to be returned.
     * @return An array of file attributes for the given path, or NO_FILE_ATTRIBUTES if the file system does not support POSIX.
     */
    private FileAttribute<?>[] getFileAttributes(Path path, FileAttribute<?>[] permissionAttributes) {
		return (!supportsPosix(path.getFileSystem())) ? NO_FILE_ATTRIBUTES : permissionAttributes;
	}

	/**
     * Checks if the given file system supports POSIX file attribute views.
     * 
     * @param fileSystem the file system to check
     * @return {@code true} if the file system supports POSIX file attribute views, {@code false} otherwise
     */
    private boolean supportsPosix(FileSystem fileSystem) {
		return fileSystem.supportedFileAttributeViews().contains("posix");
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
     * Returns a string representation of the JarFileArchive object.
     * 
     * @return a string representation of the JarFileArchive object
     */
    @Override
	public String toString() {
		return this.file.toString();
	}

	/**
     * Converts an array of PosixFilePermission objects into an array of FileAttribute objects.
     * 
     * @param permissions the PosixFilePermission objects to convert
     * @return an array of FileAttribute objects representing the converted permissions
     */
    private static FileAttribute<?>[] asFileAttributes(PosixFilePermission... permissions) {
		return new FileAttribute<?>[] { PosixFilePermissions.asFileAttribute(Set.of(permissions)) };
	}

	/**
	 * {@link Entry} implementation backed by a {@link JarEntry}.
	 */
	private record JarArchiveEntry(JarEntry jarEntry) implements Entry {

		/**
     * Returns the name of the jar entry.
     *
     * @return the name of the jar entry
     */
    @Override
		public String name() {
			return this.jarEntry.getName();
		}

		/**
     * Returns a boolean value indicating whether the current entry in the JAR file is a directory.
     *
     * @return {@code true} if the current entry is a directory, {@code false} otherwise.
     */
    @Override
		public boolean isDirectory() {
			return this.jarEntry.isDirectory();
		}

	}

}
