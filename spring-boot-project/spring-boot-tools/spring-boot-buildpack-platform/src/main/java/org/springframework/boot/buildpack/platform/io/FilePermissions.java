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

package org.springframework.boot.buildpack.platform.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collection;

import org.springframework.util.Assert;

/**
 * Utilities for dealing with file permissions and attributes.
 *
 * @author Scott Frederick
 * @since 2.5.0
 */
public final class FilePermissions {

	private FilePermissions() {
	}

	/**
	 * Return the integer representation of the file permissions for a path, where the
	 * integer value conforms to the
	 * <a href="https://en.wikipedia.org/wiki/Umask">umask</a> octal notation.
	 * @param path the file path
	 * @return the integer representation
	 * @throws IOException if path permissions cannot be read
	 */
	public static int umaskForPath(Path path) throws IOException {
		Assert.notNull(path, "Path must not be null");
		PosixFileAttributeView attributeView = Files.getFileAttributeView(path, PosixFileAttributeView.class);
		Assert.state(attributeView != null, "Unsupported file type for retrieving Posix attributes");
		return posixPermissionsToUmask(attributeView.readAttributes().permissions());
	}

	/**
	 * Return the integer representation of a set of Posix file permissions, where the
	 * integer value conforms to the
	 * <a href="https://en.wikipedia.org/wiki/Umask">umask</a> octal notation.
	 * @param permissions the set of {@code PosixFilePermission}s
	 * @return the integer representation
	 */
	public static int posixPermissionsToUmask(Collection<PosixFilePermission> permissions) {
		Assert.notNull(permissions, "Permissions must not be null");
		int owner = permissionToUmask(permissions, PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.OWNER_READ);
		int group = permissionToUmask(permissions, PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.GROUP_READ);
		int other = permissionToUmask(permissions, PosixFilePermission.OTHERS_EXECUTE, PosixFilePermission.OTHERS_WRITE,
				PosixFilePermission.OTHERS_READ);
		return Integer.parseInt("" + owner + group + other, 8);
	}

	private static int permissionToUmask(Collection<PosixFilePermission> permissions, PosixFilePermission execute,
			PosixFilePermission write, PosixFilePermission read) {
		int value = 0;
		if (permissions.contains(execute)) {
			value += 1;
		}
		if (permissions.contains(write)) {
			value += 2;
		}
		if (permissions.contains(read)) {
			value += 4;
		}
		return value;
	}

}
