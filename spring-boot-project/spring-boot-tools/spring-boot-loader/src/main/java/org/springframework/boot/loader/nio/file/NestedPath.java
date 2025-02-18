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

	NestedPath(NestedFileSystem fileSystem, String nestedEntryName) {
		if (fileSystem == null) {
			throw new IllegalArgumentException("'filesSystem' must not be null");
		}
		this.fileSystem = fileSystem;
		this.nestedEntryName = (nestedEntryName != null && !nestedEntryName.isBlank()) ? nestedEntryName : null;
	}

	Path getJarPath() {
		return this.fileSystem.getJarPath();
	}

	String getNestedEntryName() {
		return this.nestedEntryName;
	}

	@Override
	public NestedFileSystem getFileSystem() {
		return this.fileSystem;
	}

	@Override
	public boolean isAbsolute() {
		return true;
	}

	@Override
	public Path getRoot() {
		return null;
	}

	@Override
	public Path getFileName() {
		return this;
	}

	@Override
	public Path getParent() {
		return null;
	}

	@Override
	public int getNameCount() {
		return 1;
	}

	@Override
	public Path getName(int index) {
		if (index != 0) {
			throw new IllegalArgumentException("Nested paths only have a single element");
		}
		return this;
	}

	@Override
	public Path subpath(int beginIndex, int endIndex) {
		if (beginIndex != 0 || endIndex != 1) {
			throw new IllegalArgumentException("Nested paths only have a single element");
		}
		return this;
	}

	@Override
	public boolean startsWith(Path other) {
		return equals(other);
	}

	@Override
	public boolean endsWith(Path other) {
		return equals(other);
	}

	@Override
	public Path normalize() {
		return this;
	}

	@Override
	public Path resolve(Path other) {
		throw new UnsupportedOperationException("Unable to resolve nested path");
	}

	@Override
	public Path relativize(Path other) {
		throw new UnsupportedOperationException("Unable to relativize nested path");
	}

	@Override
	public URI toUri() {
		try {
			String uri = "nested:" + this.fileSystem.getJarPath().toUri().getRawPath();
			if (this.nestedEntryName != null) {
				uri += "/!" + UriPathEncoder.encode(this.nestedEntryName);
			}
			return new URI(uri);
		}
		catch (URISyntaxException ex) {
			throw new IOError(ex);
		}
	}

	@Override
	public Path toAbsolutePath() {
		return this;
	}

	@Override
	public Path toRealPath(LinkOption... options) throws IOException {
		return this;
	}

	@Override
	public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) throws IOException {
		throw new UnsupportedOperationException("Nested paths cannot be watched");
	}

	@Override
	public int compareTo(Path other) {
		NestedPath otherNestedPath = cast(other);
		return this.nestedEntryName.compareTo(otherNestedPath.nestedEntryName);
	}

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

	@Override
	public int hashCode() {
		return Objects.hash(this.fileSystem, this.nestedEntryName);
	}

	@Override
	public String toString() {
		String string = this.fileSystem.getJarPath().toString();
		if (this.nestedEntryName != null) {
			string += this.fileSystem.getSeparator() + this.nestedEntryName;
		}
		return string;
	}

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

	static NestedPath cast(Path path) {
		if (path instanceof NestedPath nestedPath) {
			return nestedPath;
		}
		throw new ProviderMismatchException();
	}

}
