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

import java.io.IOError;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Objects;

import org.springframework.boot.loader.net.protocol.nested.NestedLocation;
import org.springframework.boot.loader.zip.ZipContent;

/**
 * {@link Path} implementation for {@link NestedLocation nested} jar files.
 *
 * @author Phillip Webb
 * @see NestedFileSystemProvider
 */
final class NestedPath implements Path {

	private final NestedFileSystem fileSystem;

	private final String nestedEntryName;

	private volatile Boolean entryExists;

	/**
     * Constructs a new NestedPath object with the given NestedFileSystem and nested entry name.
     * 
     * @param fileSystem the NestedFileSystem to be associated with the NestedPath
     * @param nestedEntryName the name of the nested entry within the NestedFileSystem
     * @throws IllegalArgumentException if fileSystem is null
     */
    NestedPath(NestedFileSystem fileSystem, String nestedEntryName) {
		if (fileSystem == null) {
			throw new IllegalArgumentException("'filesSystem' must not be null");
		}
		this.fileSystem = fileSystem;
		this.nestedEntryName = (nestedEntryName != null && !nestedEntryName.isBlank()) ? nestedEntryName : null;
	}

	/**
     * Returns the path of the JAR file.
     *
     * @return the path of the JAR file
     */
    Path getJarPath() {
		return this.fileSystem.getJarPath();
	}

	/**
     * Returns the name of the nested entry.
     *
     * @return the name of the nested entry
     */
    String getNestedEntryName() {
		return this.nestedEntryName;
	}

	/**
     * Returns the NestedFileSystem associated with this NestedPath.
     *
     * @return the NestedFileSystem associated with this NestedPath
     */
    @Override
	public NestedFileSystem getFileSystem() {
		return this.fileSystem;
	}

	/**
     * Returns whether the path is absolute.
     *
     * @return {@code true} if the path is absolute, {@code false} otherwise
     */
    @Override
	public boolean isAbsolute() {
		return true;
	}

	/**
     * Returns the root path.
     *
     * @return the root path
     */
    @Override
	public Path getRoot() {
		return null;
	}

	/**
     * Returns the file name of the current NestedPath object.
     *
     * @return the file name as a Path object
     */
    @Override
	public Path getFileName() {
		return this;
	}

	/**
     * Returns the parent path of this nested path.
     *
     * @return the parent path, or null if this nested path has no parent
     */
    @Override
	public Path getParent() {
		return null;
	}

	/**
     * Returns the count of names.
     *
     * @return the count of names
     */
    @Override
	public int getNameCount() {
		return 1;
	}

	/**
     * Returns the name of the path at the specified index.
     *
     * @param index the index of the path
     * @return the name of the path
     * @throws IllegalArgumentException if the index is not 0
     */
    @Override
	public Path getName(int index) {
		if (index != 0) {
			throw new IllegalArgumentException("Nested paths only have a single element");
		}
		return this;
	}

	/**
     * Returns a subpath of the current path.
     * 
     * @param beginIndex the index of the starting element of the subpath
     * @param endIndex the index of the ending element of the subpath
     * @return the subpath from beginIndex (inclusive) to endIndex (exclusive)
     * @throws IllegalArgumentException if beginIndex or endIndex is not 0 or 1
     */
    @Override
	public Path subpath(int beginIndex, int endIndex) {
		if (beginIndex != 0 || endIndex != 1) {
			throw new IllegalArgumentException("Nested paths only have a single element");
		}
		return this;
	}

	/**
     * Returns true if this path starts with the specified path.
     * 
     * @param other the path to compare with
     * @return true if this path starts with the specified path, false otherwise
     */
    @Override
	public boolean startsWith(Path other) {
		return equals(other);
	}

	/**
     * Checks if this path ends with the specified path.
     * 
     * @param other the path to compare with
     * @return true if this path ends with the specified path, false otherwise
     */
    @Override
	public boolean endsWith(Path other) {
		return equals(other);
	}

	/**
     * Returns the normalized form of this path.
     * 
     * @return the normalized form of this path
     */
    @Override
	public Path normalize() {
		return this;
	}

	/**
     * Resolves the given path against this nested path.
     * 
     * @param other the path to be resolved against this nested path
     * @return the resolved path
     * @throws UnsupportedOperationException if unable to resolve nested path
     */
    @Override
	public Path resolve(Path other) {
		throw new UnsupportedOperationException("Unable to resolve nested path");
	}

	/**
     * Throws an UnsupportedOperationException indicating that it is unable to relativize a nested path.
     *
     * @param other the path to relativize against
     * @throws UnsupportedOperationException if the method is called
     */
    @Override
	public Path relativize(Path other) {
		throw new UnsupportedOperationException("Unable to relativize nested path");
	}

	/**
     * Returns the URI representation of the nested path.
     * The URI is constructed by appending the nested entry name to the jar path URI.
     * 
     * @return the URI representation of the nested path
     * @throws IOError if an error occurs while constructing the URI
     */
    @Override
	public URI toUri() {
		try {
			String uri = "nested:" + this.fileSystem.getJarPath().toUri().getPath();
			if (this.nestedEntryName != null) {
				uri += "/!" + this.nestedEntryName;
			}
			return new URI(uri);
		}
		catch (URISyntaxException ex) {
			throw new IOError(ex);
		}
	}

	/**
     * Returns the absolute path of the current path.
     *
     * @return the absolute path
     */
    @Override
	public Path toAbsolutePath() {
		return this;
	}

	/**
     * Returns the real path of this NestedPath object.
     * 
     * @param options the options indicating how symbolic links are handled
     * @return the real path of this NestedPath object
     * @throws IOException if an I/O error occurs
     */
    @Override
	public Path toRealPath(LinkOption... options) throws IOException {
		return this;
	}

	/**
     * Registers the specified watcher to watch for the specified events on this nested path.
     * 
     * @param watcher the watch service to register with
     * @param events the events to watch for
     * @param modifiers the modifiers to apply
     * @return the watch key representing the registration
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedOperationException if nested paths cannot be watched
     */
    @Override
	public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) throws IOException {
		throw new UnsupportedOperationException("Nested paths cannot be watched");
	}

	/**
     * Compares this NestedPath object with the specified Path object for order.
     * 
     * @param other the Path object to be compared
     * @return a negative integer, zero, or a positive integer as this NestedPath object is less than, equal to, or greater than the specified Path object
     * @throws ClassCastException if the specified object is not of type NestedPath
     */
    @Override
	public int compareTo(Path other) {
		NestedPath otherNestedPath = cast(other);
		return this.nestedEntryName.compareTo(otherNestedPath.nestedEntryName);
	}

	/**
     * Compares this NestedPath object to the specified object for equality.
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
		NestedPath other = (NestedPath) obj;
		return Objects.equals(this.fileSystem, other.fileSystem)
				&& Objects.equals(this.nestedEntryName, other.nestedEntryName);
	}

	/**
     * Returns a hash code value for the object. This method overrides the default implementation of the {@code hashCode()} method.
     * The hash code is computed based on the {@code fileSystem} and {@code nestedEntryName} properties of the object.
     *
     * @return the hash code value for the object
     */
    @Override
	public int hashCode() {
		return Objects.hash(this.fileSystem, this.nestedEntryName);
	}

	/**
     * Returns a string representation of the NestedPath object.
     * The string representation is the path of the JAR file in the file system,
     * followed by the separator and the name of the nested entry if it exists.
     * 
     * @return the string representation of the NestedPath object
     */
    @Override
	public String toString() {
		String string = this.fileSystem.getJarPath().toString();
		if (this.nestedEntryName != null) {
			string += this.fileSystem.getSeparator() + this.nestedEntryName;
		}
		return string;
	}

	/**
     * Checks if the specified nested entry exists within the jar file.
     * 
     * @throws NoSuchFileException if the jar file or the nested entry does not exist
     */
    void assertExists() throws NoSuchFileException {
		if (!Files.isRegularFile(getJarPath())) {
			throw new NoSuchFileException(toString());
		}
		Boolean entryExists = this.entryExists;
		if (entryExists == null) {
			try {
				try (ZipContent content = ZipContent.open(getJarPath(), this.nestedEntryName)) {
					entryExists = true;
				}
			}
			catch (IOException ex) {
				entryExists = false;
			}
			this.entryExists = entryExists;
		}
		if (!entryExists) {
			throw new NoSuchFileException(toString());
		}
	}

	/**
     * Casts a Path object to a NestedPath object.
     * 
     * @param path the Path object to be casted
     * @return the casted NestedPath object
     * @throws ProviderMismatchException if the provided path is not an instance of NestedPath
     */
    static NestedPath cast(Path path) {
		if (path instanceof NestedPath nestedPath) {
			return nestedPath;
		}
		throw new ProviderMismatchException();
	}

}
