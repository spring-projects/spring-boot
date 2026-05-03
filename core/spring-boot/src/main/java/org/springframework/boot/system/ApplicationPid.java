/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.system;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * An application process ID.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public class ApplicationPid {

	private static final PosixFilePermission[] WRITE_PERMISSIONS = { PosixFilePermission.OWNER_WRITE,
			PosixFilePermission.GROUP_WRITE, PosixFilePermission.OTHERS_WRITE };

	private final @Nullable Long pid;

	public ApplicationPid() {
		this.pid = currentProcessPid();
	}

	protected ApplicationPid(@Nullable Long pid) {
		this.pid = pid;
	}

	private @Nullable Long currentProcessPid() {
		try {
			return ProcessHandle.current().pid();
		}
		catch (Throwable ex) {
			return null;
		}
	}

	/**
	 * Return if the application PID is available.
	 * @return {@code true} if the PID is available
	 * @since 3.4.0
	 */
	public boolean isAvailable() {
		return this.pid != null;
	}

	/**
	 * Return the application PID as a {@link Long}.
	 * @return the application PID or {@code null}
	 * @since 3.4.0
	 */
	public @Nullable Long toLong() {
		return this.pid;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof ApplicationPid other) {
			return ObjectUtils.nullSafeEquals(this.pid, other.pid);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.pid);
	}

	@Override
	public String toString() {
		return (this.pid != null) ? String.valueOf(this.pid) : "???";
	}

	/**
	 * Write the PID to the specified file.
	 * @param file the PID file
	 * @throws IllegalStateException if no PID is available.
	 * @throws IOException if the file cannot be written
	 */
	public void write(File file) throws IOException {
		Assert.state(this.pid != null, "No PID available");
		Path path = file.toPath();
		createParentDirectory(path);
		if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
			assertCanOverwrite(path);
		}
		Files.writeString(path, this.pid.toString(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE,
				LinkOption.NOFOLLOW_LINKS);
	}

	private void createParentDirectory(Path path) throws IOException {
		Path parent = path.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
	}

	private void assertCanOverwrite(Path file) throws IOException {
		if (!Files.isWritable(file) || !canWritePosixFile(file)) {
			throw new FileNotFoundException(file.toString() + " (permission denied)");
		}
	}

	private boolean canWritePosixFile(Path file) throws IOException {
		try {
			Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(file, LinkOption.NOFOLLOW_LINKS);
			for (PosixFilePermission permission : WRITE_PERMISSIONS) {
				if (permissions.contains(permission)) {
					return true;
				}
			}
			return false;
		}
		catch (UnsupportedOperationException ex) {
			// Assume that we can
			return true;
		}
	}

}
