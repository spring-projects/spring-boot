/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.loader.nio.file;

import java.io.IOException;
import java.net.URI;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.loader.net.protocol.nested.NestedLocation;

/**
 * {@link FileSystem} implementation for {@link NestedLocation nested} jar files.
 *
 * @author Phillip Webb
 * @see NestedFileSystemProvider
 */
class NestedFileSystem extends FileSystem {

	private static final Set<String> SUPPORTED_FILE_ATTRIBUTE_VIEWS = Set.of("basic");

	private static final String FILE_SYSTEMS_CLASS_NAME = FileSystems.class.getName();

	private static final Object EXISTING_FILE_SYSTEM = new Object();

	private final NestedFileSystemProvider provider;

	private final Path jarPath;

	private volatile boolean closed;

	private final Map<String, Object> zipFileSystems = new HashMap<>();

	/**
     * Constructs a new NestedFileSystem with the specified provider and jarPath.
     * 
     * @param provider the NestedFileSystemProvider to be used for this NestedFileSystem
     * @param jarPath the Path to the JAR file to be used for this NestedFileSystem
     * @throws IllegalArgumentException if provider or jarPath is null
     */
    NestedFileSystem(NestedFileSystemProvider provider, Path jarPath) {
		if (provider == null || jarPath == null) {
			throw new IllegalArgumentException("Provider and JarPath must not be null");
		}
		this.provider = provider;
		this.jarPath = jarPath;
	}

	/**
     * Installs a zip file system if necessary for the specified nested entry name.
     * 
     * @param nestedEntryName the name of the nested entry
     */
    void installZipFileSystemIfNecessary(String nestedEntryName) {
		try {
			boolean seen;
			synchronized (this.zipFileSystems) {
				seen = this.zipFileSystems.putIfAbsent(nestedEntryName, EXISTING_FILE_SYSTEM) != null;
			}
			if (!seen) {
				URI uri = new URI("jar:nested:" + this.jarPath.toUri().getPath() + "/!" + nestedEntryName);
				if (!hasFileSystem(uri)) {
					FileSystem zipFileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
					synchronized (this.zipFileSystems) {
						this.zipFileSystems.put(nestedEntryName, zipFileSystem);
					}
				}
			}
		}
		catch (Exception ex) {
			// Ignore
		}
	}

	/**
     * Checks if a file system exists for the given URI.
     * 
     * @param uri the URI to check for a file system
     * @return true if a file system exists for the given URI, false otherwise
     */
    private boolean hasFileSystem(URI uri) {
		try {
			FileSystems.getFileSystem(uri);
			return true;
		}
		catch (FileSystemNotFoundException ex) {
			return isCreatingNewFileSystem();
		}
	}

	/**
     * Checks if a new file system is being created.
     * 
     * @return true if a new file system is being created, false otherwise
     */
    private boolean isCreatingNewFileSystem() {
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		if (stack != null) {
			for (StackTraceElement element : stack) {
				if (FILE_SYSTEMS_CLASS_NAME.equals(element.getClassName())) {
					return "newFileSystem".equals(element.getMethodName());
				}
			}
		}
		return false;
	}

	/**
     * Returns the provider that created this file system.
     *
     * @return the provider that created this file system
     */
    @Override
	public FileSystemProvider provider() {
		return this.provider;
	}

	/**
     * Returns the path of the JAR file.
     *
     * @return the path of the JAR file
     */
    Path getJarPath() {
		return this.jarPath;
	}

	/**
     * Closes the NestedFileSystem and releases any resources associated with it.
     * If the NestedFileSystem is already closed, this method does nothing.
     * 
     * @throws IOException if an I/O error occurs while closing the NestedFileSystem
     */
    @Override
	public void close() throws IOException {
		if (this.closed) {
			return;
		}
		this.closed = true;
		synchronized (this.zipFileSystems) {
			this.zipFileSystems.values()
				.stream()
				.filter(FileSystem.class::isInstance)
				.map(FileSystem.class::cast)
				.forEach(this::closeZipFileSystem);
		}
		this.provider.removeFileSystem(this);
	}

	/**
     * Closes the specified ZipFileSystem.
     * 
     * @param zipFileSystem the ZipFileSystem to be closed
     */
    private void closeZipFileSystem(FileSystem zipFileSystem) {
		try {
			zipFileSystem.close();
		}
		catch (Exception ex) {
			// Ignore
		}
	}

	/**
     * Returns a boolean value indicating whether the NestedFileSystem is open or closed.
     *
     * @return true if the NestedFileSystem is open, false if it is closed.
     */
    @Override
	public boolean isOpen() {
		return !this.closed;
	}

	/**
     * Returns a boolean value indicating whether the NestedFileSystem is read-only.
     *
     * @return true if the NestedFileSystem is read-only, false otherwise.
     */
    @Override
	public boolean isReadOnly() {
		return true;
	}

	/**
     * Returns the separator used in the NestedFileSystem class.
     *
     * @return the separator as a String
     */
    @Override
	public String getSeparator() {
		return "/!";
	}

	/**
     * Returns an empty set of root directories.
     * 
     * @return an empty set of root directories
     * @throws IllegalStateException if the NestedFileSystem is closed
     */
    @Override
	public Iterable<Path> getRootDirectories() {
		assertNotClosed();
		return Collections.emptySet();
	}

	/**
     * Returns an iterable collection of the file stores available in this nested file system.
     * 
     * @return an iterable collection of the file stores available in this nested file system
     * 
     * @throws IllegalStateException if the nested file system is closed
     */
    @Override
	public Iterable<FileStore> getFileStores() {
		assertNotClosed();
		return Collections.emptySet();
	}

	/**
     * Returns a set of strings representing the supported file attribute views for this file system.
     * 
     * @return a set of strings representing the supported file attribute views
     * @throws FileSystemException if the file system is closed
     */
    @Override
	public Set<String> supportedFileAttributeViews() {
		assertNotClosed();
		return SUPPORTED_FILE_ATTRIBUTE_VIEWS;
	}

	/**
     * Returns a Path object representing a nested path in the file system.
     * 
     * @param first the first element of the nested path
     * @param more additional elements of the nested path
     * @return a Path object representing the nested path
     * @throws IllegalStateException if the file system is closed
     * @throws IllegalArgumentException if the nested path contains more than one element
     */
    @Override
	public Path getPath(String first, String... more) {
		assertNotClosed();
		if (more.length != 0) {
			throw new IllegalArgumentException("Nested paths must contain a single element");
		}
		return new NestedPath(this, first);
	}

	/**
     * Returns a PathMatcher object for matching paths in the specified syntax and pattern.
     * 
     * @param syntaxAndPattern the syntax and pattern to be used for matching paths
     * @return a PathMatcher object for matching paths
     * @throws UnsupportedOperationException if nested paths do not support path matchers
     */
    @Override
	public PathMatcher getPathMatcher(String syntaxAndPattern) {
		throw new UnsupportedOperationException("Nested paths do not support path matchers");
	}

	/**
     * Returns the user principal lookup service for this nested file system.
     * 
     * @return the user principal lookup service for this nested file system
     * @throws UnsupportedOperationException if nested paths do not have a user principal lookup service
     */
    @Override
	public UserPrincipalLookupService getUserPrincipalLookupService() {
		throw new UnsupportedOperationException("Nested paths do not have a user principal lookup service");
	}

	/**
     * Creates a new WatchService for monitoring file system events.
     *
     * @return a new WatchService instance
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedOperationException if nested paths do not support the WatchService
     */
    @Override
	public WatchService newWatchService() throws IOException {
		throw new UnsupportedOperationException("Nested paths do not support the WatchService");
	}

	/**
     * Compares this NestedFileSystem object to the specified object for equality.
     * 
     * @param obj the object to compare to
     * @return true if the objects are equal, false otherwise
     */
    @Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		NestedFileSystem other = (NestedFileSystem) obj;
		return this.jarPath.equals(other.jarPath);
	}

	/**
     * Returns the hash code value for the object. The hash code is generated based on the jarPath attribute of the NestedFileSystem object.
     *
     * @return the hash code value for the object
     */
    @Override
	public int hashCode() {
		return this.jarPath.hashCode();
	}

	/**
     * Returns the absolute path of the JAR file.
     *
     * @return the absolute path of the JAR file
     */
    @Override
	public String toString() {
		return this.jarPath.toAbsolutePath().toString();
	}

	/**
     * Checks if the NestedFileSystem is closed.
     * 
     * @throws ClosedFileSystemException if the NestedFileSystem is closed.
     */
    private void assertNotClosed() {
		if (this.closed) {
			throw new ClosedFileSystemException();
		}
	}

}
