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

package org.springframework.boot.loader.nio.file;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.LinkOption;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.ReadOnlyFileSystemException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.loader.net.protocol.nested.NestedLocation;

/**
 * {@link FileSystemProvider} implementation for {@link NestedLocation nested} jar files.
 *
 * @author Phillip Webb
 * @since 3.2.0
 */
public class NestedFileSystemProvider extends FileSystemProvider {

	private Map<Path, NestedFileSystem> fileSystems = new HashMap<>();

	/**
	 * Returns the scheme of the file system provider.
	 * @return the scheme of the file system provider
	 */
	@Override
	public String getScheme() {
		return "nested";
	}

	/**
	 * Creates a new file system for the specified URI and environment.
	 * @param uri The URI of the file system.
	 * @param env The environment variables.
	 * @return The newly created file system.
	 * @throws IOException If an I/O error occurs.
	 * @throws FileSystemAlreadyExistsException If a file system already exists for the
	 * specified URI.
	 */
	@Override
	public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
		NestedLocation location = NestedLocation.fromUri(uri);
		Path jarPath = location.path();
		synchronized (this.fileSystems) {
			if (this.fileSystems.containsKey(jarPath)) {
				throw new FileSystemAlreadyExistsException();
			}
			NestedFileSystem fileSystem = new NestedFileSystem(this, location.path());
			this.fileSystems.put(location.path(), fileSystem);
			return fileSystem;
		}
	}

	/**
	 * Returns the FileSystem object for the specified URI.
	 * @param uri The URI of the file system.
	 * @return The FileSystem object associated with the specified URI.
	 * @throws FileSystemNotFoundException If the file system is not found.
	 */
	@Override
	public FileSystem getFileSystem(URI uri) {
		NestedLocation location = NestedLocation.fromUri(uri);
		synchronized (this.fileSystems) {
			NestedFileSystem fileSystem = this.fileSystems.get(location.path());
			if (fileSystem == null) {
				throw new FileSystemNotFoundException();
			}
			return fileSystem;
		}
	}

	/**
	 * Returns the path for the specified URI.
	 * @param uri the URI for which to retrieve the path
	 * @return the path corresponding to the specified URI
	 */
	@Override
	public Path getPath(URI uri) {
		NestedLocation location = NestedLocation.fromUri(uri);
		synchronized (this.fileSystems) {
			NestedFileSystem fileSystem = this.fileSystems.computeIfAbsent(location.path(),
					(path) -> new NestedFileSystem(this, path));
			fileSystem.installZipFileSystemIfNecessary(location.nestedEntryName());
			return fileSystem.getPath(location.nestedEntryName());
		}
	}

	/**
	 * Removes the specified NestedFileSystem from the list of file systems.
	 * @param fileSystem the NestedFileSystem to be removed
	 */
	void removeFileSystem(NestedFileSystem fileSystem) {
		synchronized (this.fileSystems) {
			this.fileSystems.remove(fileSystem.getJarPath());
		}
	}

	/**
	 * Creates a new byte channel for reading and writing to a file.
	 * @param path the path to the file
	 * @param options the set of options specifying how the file is opened
	 * @param attrs the file attributes to set when creating the file
	 * @return a new SeekableByteChannel for the file
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
			throws IOException {
		NestedPath nestedPath = NestedPath.cast(path);
		return new NestedByteChannel(nestedPath.getJarPath(), nestedPath.getNestedEntryName());
	}

	/**
	 * Returns a new directory stream for the given directory, using the specified filter.
	 * @param dir the directory to create the directory stream for
	 * @param filter the filter to apply to the directory stream
	 * @return a new directory stream for the given directory
	 * @throws IOException if an I/O error occurs
	 * @throws NotDirectoryException if the specified path is not a directory
	 */
	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
		throw new NotDirectoryException(NestedPath.cast(dir).toString());
	}

	/**
	 * Creates a new directory at the specified path with the given attributes.
	 * @param dir the path of the directory to be created
	 * @param attrs the attributes to set for the new directory
	 * @throws IOException if an I/O error occurs while creating the directory
	 * @throws ReadOnlyFileSystemException if the file system is read-only and cannot
	 * create the directory
	 */
	@Override
	public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
		throw new ReadOnlyFileSystemException();
	}

	/**
	 * Deletes the specified file or directory.
	 * @param path the path of the file or directory to be deleted
	 * @throws IOException if an I/O error occurs
	 * @throws ReadOnlyFileSystemException if the file system is read-only
	 */
	@Override
	public void delete(Path path) throws IOException {
		throw new ReadOnlyFileSystemException();
	}

	/**
	 * Copies a file from the source path to the target path.
	 * @param source the path of the file to be copied
	 * @param target the path where the file should be copied to
	 * @param options options specifying how the copy should be performed
	 * @throws IOException if an I/O error occurs during the copy operation
	 * @throws ReadOnlyFileSystemException if the file system is read-only and the copy
	 * operation is not allowed
	 */
	@Override
	public void copy(Path source, Path target, CopyOption... options) throws IOException {
		throw new ReadOnlyFileSystemException();
	}

	/**
	 * Moves a file or directory from the source path to the target path.
	 * @param source the path of the file or directory to be moved
	 * @param target the target path where the file or directory will be moved to
	 * @param options options specifying how the move operation should be performed
	 * @throws IOException if an I/O error occurs during the move operation
	 * @throws ReadOnlyFileSystemException if the file system is read-only and the move
	 * operation is not supported
	 */
	@Override
	public void move(Path source, Path target, CopyOption... options) throws IOException {
		throw new ReadOnlyFileSystemException();
	}

	/**
	 * Compares two paths and determines if they refer to the same file.
	 * @param path the first path to compare
	 * @param path2 the second path to compare
	 * @return true if the two paths refer to the same file, false otherwise
	 * @throws IOException if an I/O error occurs while comparing the paths
	 */
	@Override
	public boolean isSameFile(Path path, Path path2) throws IOException {
		return path.equals(path2);
	}

	/**
	 * Returns whether or not the specified file is hidden.
	 * @param path the path to the file
	 * @return true if the file is hidden, false otherwise
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public boolean isHidden(Path path) throws IOException {
		return false;
	}

	/**
	 * Returns the {@code FileStore} object representing the file store where the
	 * specified path is located.
	 * @param path the path for which the file store is to be returned
	 * @return the {@code FileStore} object representing the file store where the
	 * specified path is located
	 * @throws IOException if an I/O error occurs
	 * @throws IllegalArgumentException if the specified path does not exist
	 */
	@Override
	public FileStore getFileStore(Path path) throws IOException {
		NestedPath nestedPath = NestedPath.cast(path);
		nestedPath.assertExists();
		return new NestedFileStore(nestedPath.getFileSystem());
	}

	/**
	 * Checks the access permissions for the specified path.
	 * @param path the path to check access permissions for
	 * @param modes the access modes to check
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public void checkAccess(Path path, AccessMode... modes) throws IOException {
		Path jarPath = getJarPath(path);
		jarPath.getFileSystem().provider().checkAccess(jarPath, modes);
	}

	/**
	 * Returns the file attribute view of the specified type for the given path.
	 * @param path the path to the file
	 * @param type the class representing the file attribute view
	 * @param options options indicating how symbolic links are handled
	 * @return the file attribute view of the specified type for the given path
	 * @throws UnsupportedOperationException if the file system provider does not support
	 * the file attribute view
	 * @throws IllegalArgumentException if the given path is invalid or the type parameter
	 * is null
	 * @throws SecurityException if a security manager is present and it denies access to
	 * the file
	 */
	@Override
	public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
		Path jarPath = getJarPath(path);
		return jarPath.getFileSystem().provider().getFileAttributeView(jarPath, type, options);
	}

	/**
	 * Reads the attributes of a file or directory specified by the given path.
	 * @param path the path to the file or directory
	 * @param type the class representing the type of attributes to be read
	 * @param options options indicating how symbolic links are handled
	 * @return the attributes of the file or directory
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options)
			throws IOException {
		Path jarPath = getJarPath(path);
		return jarPath.getFileSystem().provider().readAttributes(jarPath, type, options);
	}

	/**
	 * Reads the attributes of a file or directory specified by the given path.
	 * @param path the path to the file or directory
	 * @param attributes the attributes to read
	 * @param options options indicating how symbolic links are handled
	 * @return a map containing the attributes of the file or directory
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
		Path jarPath = getJarPath(path);
		return jarPath.getFileSystem().provider().readAttributes(jarPath, attributes, options);
	}

	/**
	 * Returns the path of the JAR file associated with the given path.
	 * @param path the path for which to retrieve the JAR file path
	 * @return the path of the JAR file associated with the given path
	 */
	protected Path getJarPath(Path path) {
		return NestedPath.cast(path).getJarPath();
	}

	/**
	 * Sets the value of the specified attribute for the file or directory identified by
	 * the given path.
	 * @param path the path to the file or directory
	 * @param attribute the attribute to set
	 * @param value the value to set for the attribute
	 * @param options options indicating how the file or directory should be handled
	 * @throws IOException if an I/O error occurs
	 * @throws ReadOnlyFileSystemException if the file system is read-only
	 */
	@Override
	public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
		throw new ReadOnlyFileSystemException();
	}

}
