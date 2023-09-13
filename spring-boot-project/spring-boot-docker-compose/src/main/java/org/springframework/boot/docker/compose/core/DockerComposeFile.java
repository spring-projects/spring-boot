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

package org.springframework.boot.docker.compose.core;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.util.Assert;

/**
 * A reference to a docker compose file (usually named {@code compose.yaml}).
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 * @see #of(File)
 * @see #find(File)
 */
public final class DockerComposeFile {

	private static final List<String> SEARCH_ORDER = List.of("compose.yaml", "compose.yml", "docker-compose.yaml",
			"docker-compose.yml");

	private final File file;

	private DockerComposeFile(File file) {
		try {
			this.file = file.getCanonicalFile();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
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
		return this.file.equals(other.file);
	}

	@Override
	public int hashCode() {
		return this.file.hashCode();
	}

	@Override
	public String toString() {
		return this.file.toString();
	}

	/**
	 * Find the docker compose file by searching in the given working directory. Files are
	 * considered in the same order that {@code docker compose} uses, namely:
	 * <ul>
	 * <li>{@code compose.yaml}</li>
	 * <li>{@code compose.yml}</li>
	 * <li>{@code docker-compose.yaml}</li>
	 * <li>{@code docker-compose.yml}</li>
	 * </ul>
	 * @param workingDirectory the working directory to search or {@code null} to use the
	 * current directory
	 * @return the located file or {@code null} if no docker compose file can be found
	 */
	public static DockerComposeFile find(File workingDirectory) {
		File base = (workingDirectory != null) ? workingDirectory : new File(".");
		if (!base.exists()) {
			return null;
		}
		Assert.isTrue(base.isDirectory(), () -> "'%s' is not a directory".formatted(base));
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
	 * @return the docker compose file
	 */
	public static DockerComposeFile of(File file) {
		Assert.notNull(file, "File must not be null");
		Assert.isTrue(file.exists(), () -> "Docker Compose file '%s' does not exist".formatted(file));
		Assert.isTrue(file.isFile(), () -> "Docker compose file '%s' is not a file".formatted(file));
		return new DockerComposeFile(file);
	}

}
