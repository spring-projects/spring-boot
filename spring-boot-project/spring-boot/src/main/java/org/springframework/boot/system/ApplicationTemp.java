/*
 * Copyright 2012-2022 the original author or authors.
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
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.util.EnumSet;
import java.util.HexFormat;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Provides access to an application specific temporary directory. Generally speaking
 * different Spring Boot applications will get different locations, however, simply
 * restarting an application will give the same location.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public class ApplicationTemp {

	private static final FileAttribute<?>[] NO_FILE_ATTRIBUTES = {};

	private static final EnumSet<PosixFilePermission> DIRECTORY_PERMISSIONS = EnumSet.of(PosixFilePermission.OWNER_READ,
			PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE);

	private final Class<?> sourceClass;

	private volatile Path path;

	/**
	 * Create a new {@link ApplicationTemp} instance.
	 */
	public ApplicationTemp() {
		this(null);
	}

	/**
	 * Create a new {@link ApplicationTemp} instance for the specified source class.
	 * @param sourceClass the source class or {@code null}
	 */
	public ApplicationTemp(Class<?> sourceClass) {
		this.sourceClass = sourceClass;
	}

	@Override
	public String toString() {
		return getDir().getAbsolutePath();
	}

	/**
	 * Return the directory to be used for application specific temp files.
	 * @return the application temp directory
	 */
	public File getDir() {
		return getPath().toFile();
	}

	/**
	 * Return a subdirectory of the application temp.
	 * @param subDir the subdirectory name
	 * @return a subdirectory
	 */
	public File getDir(String subDir) {
		return createDirectory(getPath().resolve(subDir)).toFile();
	}

	private Path getPath() {
		if (this.path == null) {
			synchronized (this) {
				String hash = HexFormat.of().withUpperCase().formatHex(generateHash(this.sourceClass));
				this.path = createDirectory(getTempDirectory().resolve(hash));
			}
		}
		return this.path;
	}

	private Path createDirectory(Path path) {
		try {
			if (!Files.exists(path)) {
				Files.createDirectory(path, getFileAttributes(path.getFileSystem(), DIRECTORY_PERMISSIONS));
			}
			return path;
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to create application temp directory " + path, ex);
		}
	}

	private FileAttribute<?>[] getFileAttributes(FileSystem fileSystem, EnumSet<PosixFilePermission> ownerReadWrite) {
		if (!fileSystem.supportedFileAttributeViews().contains("posix")) {
			return NO_FILE_ATTRIBUTES;
		}
		return new FileAttribute<?>[] { PosixFilePermissions.asFileAttribute(ownerReadWrite) };
	}

	private Path getTempDirectory() {
		String property = System.getProperty("java.io.tmpdir");
		Assert.state(StringUtils.hasLength(property), "No 'java.io.tmpdir' property set");
		Path tempDirectory = Paths.get(property);
		Assert.state(Files.exists(tempDirectory), () -> "Temp directory '" + tempDirectory + "' does not exist");
		Assert.state(Files.isDirectory(tempDirectory),
				() -> "Temp location '" + tempDirectory + "' is not a directory");
		return tempDirectory;
	}

	private byte[] generateHash(Class<?> sourceClass) {
		ApplicationHome home = new ApplicationHome(sourceClass);
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-1");
			update(digest, home.getSource());
			update(digest, home.getDir());
			update(digest, System.getProperty("user.dir"));
			update(digest, System.getProperty("java.home"));
			update(digest, System.getProperty("java.class.path"));
			update(digest, System.getProperty("sun.java.command"));
			update(digest, System.getProperty("sun.boot.class.path"));
			return digest.digest();
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private void update(MessageDigest digest, Object source) {
		if (source != null) {
			digest.update(getUpdateSourceBytes(source));
		}
	}

	private byte[] getUpdateSourceBytes(Object source) {
		if (source instanceof File file) {
			return getUpdateSourceBytes(file.getAbsolutePath());
		}
		return source.toString().getBytes();
	}

}
