/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.docker.compose.core;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

/**
 * A reference to a Docker Compose file (usually named {@code compose.yaml}).
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 * @see #of(File)
 * @see #of(Collection)
 * @see #find(File)
 */
public final class DockerComposeFile {

	private static final List<String> SEARCH_ORDER = List.of("compose.yaml", "compose.yml", "docker-compose.yaml",
			"docker-compose.yml");

	private final List<File> files;

	private DockerComposeFile(List<File> files) {
		Assert.isTrue(!files.isEmpty(), "'files' must not be empty");
		this.files = files.stream().map(DockerComposeFile::toCanonicalFile).toList();
	}

	private static File toCanonicalFile(File file) {
		try {
			return file.getCanonicalFile();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/**
	 * Returns the source Docker Compose files.
	 * @return the source Docker Compose files
	 * @since 3.4.0
	 */
	public List<File> getFiles() {
		return this.files;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		DockerComposeFile other = (DockerComposeFile) obj;
		return this.files.equals(other.files);
	}

	@Override
	public int hashCode() {
		return this.files.hashCode();
	}

	@Override
	public String toString() {
		if (this.files.size() == 1) {
			return this.files.get(0).getPath();
		}
		return this.files.stream().map(File::toString).collect(Collectors.joining(", "));
	}

	/**
	 * Find the Docker Compose file by searching in the given working directory. Files are
	 * considered in the same order that {@code docker compose} uses, namely:
	 * <ul>
	 * <li>{@code compose.yaml}</li>
	 * <li>{@code compose.yml}</li>
	 * <li>{@code docker-compose.yaml}</li>
	 * <li>{@code docker-compose.yml}</li>
	 * </ul>
	 * @param workingDirectory the working directory to search or {@code null} to use the
	 * current directory
	 * @return the located file or {@code null} if no Docker Compose file can be found
	 */
	public static DockerComposeFile find(File workingDirectory) {
		File base = (workingDirectory != null) ? workingDirectory : new File(".");
		if (!base.exists()) {
			return null;
		}
		Assert.state(base.isDirectory(), () -> "'%s' is not a directory".formatted(base));
		Path basePath = base.toPath();
		for (String candidate : SEARCH_ORDER) {
			Path resolved = basePath.resolve(candidate);
			if (Files.exists(resolved)) {
				return of(resolved.toAbsolutePath().toFile());
			}
		}
		return null;
	}

	/**
	 * Create a new {@link DockerComposeFile} for the given {@link File}.
	 * @param file the source file
	 * @return the Docker Compose file
	 */
	public static DockerComposeFile of(File file) {
		Assert.notNull(file, "'file' must not be null");
		Assert.isTrue(file.exists(), () -> "'file' [%s] must exist".formatted(file));
		Assert.isTrue(file.isFile(), () -> "'file' [%s] must be a normal file".formatted(file));
		return new DockerComposeFile(Collections.singletonList(file));
	}

	/**
	 * Creates a new {@link DockerComposeFile} for the given {@link File files}.
	 * @param files the source files
	 * @return the Docker Compose file
	 * @since 3.4.0
	 */
	public static DockerComposeFile of(Collection<? extends File> files) {
		Assert.notNull(files, "'files' must not be null");
		for (File file : files) {
			Assert.notNull(file, "'files' must not contain null elements");
			Assert.isTrue(file.exists(), () -> "'files' content [%s] must exist".formatted(file));
			Assert.isTrue(file.isFile(), () -> "'files' content [%s] must be a normal file".formatted(file));
		}
		return new DockerComposeFile(List.copyOf(files));
	}

}
