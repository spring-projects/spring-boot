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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

	private final Lock pathLock = new ReentrantLock();

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

	/**
	 * Returns the absolute path of the directory.
	 * @return the absolute path of the directory
	 */
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

	/**
	 * Returns the path of the temporary directory for the application. If the path is not
	 * already set, it generates a hash based on the source class and creates a directory
	 * with that hash in the temporary directory.
	 * @return the path of the temporary directory
	 */
	private Path getPath() {
		if (this.path == null) {
			this.pathLock.lock();
			try {
				if (this.path == null) {
					String hash = HexFormat.of().withUpperCase().formatHex(generateHash(this.sourceClass));
					this.path = createDirectory(getTempDirectory().resolve(hash));
				}
			}
			finally {
				this.pathLock.unlock();
			}
		}
		return this.path;
	}

	/**
	 * Creates a directory at the specified path if it does not already exist.
	 * @param path the path at which to create the directory
	 * @return the created directory path
	 * @throws IllegalStateException if unable to create the directory
	 */
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

	/**
	 * Returns an array of file attributes for the given file system and owner read/write
	 * permissions.
	 * @param fileSystem the file system to retrieve the file attributes from
	 * @param ownerReadWrite the owner read/write permissions to be used as file
	 * attributes
	 * @return an array of file attributes
	 */
	private FileAttribute<?>[] getFileAttributes(FileSystem fileSystem, EnumSet<PosixFilePermission> ownerReadWrite) {
		if (!fileSystem.supportedFileAttributeViews().contains("posix")) {
			return NO_FILE_ATTRIBUTES;
		}
		return new FileAttribute<?>[] { PosixFilePermissions.asFileAttribute(ownerReadWrite) };
	}

	/**
	 * Returns the path to the temporary directory.
	 * @return the path to the temporary directory
	 * @throws IllegalStateException if the 'java.io.tmpdir' property is not set, or if
	 * the temporary directory does not exist or is not a directory
	 */
	private Path getTempDirectory() {
		String property = System.getProperty("java.io.tmpdir");
		Assert.state(StringUtils.hasLength(property), "No 'java.io.tmpdir' property set");
		Path tempDirectory = Paths.get(property);
		Assert.state(Files.exists(tempDirectory), () -> "Temp directory '" + tempDirectory + "' does not exist");
		Assert.state(Files.isDirectory(tempDirectory),
				() -> "Temp location '" + tempDirectory + "' is not a directory");
		return tempDirectory;
	}

	/**
	 * Generates a hash value based on the provided source class.
	 * @param sourceClass the class used to generate the hash
	 * @return a byte array representing the generated hash
	 * @throws IllegalStateException if an error occurs during the hash generation process
	 */
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

	/**
	 * Updates the given MessageDigest with the bytes obtained from the provided source
	 * object.
	 * @param digest the MessageDigest to be updated
	 * @param source the object from which the bytes will be obtained
	 */
	private void update(MessageDigest digest, Object source) {
		if (source != null) {
			digest.update(getUpdateSourceBytes(source));
		}
	}

	/**
	 * Returns the byte array representation of the update source.
	 * @param source the update source object
	 * @return the byte array representation of the update source
	 */
	private byte[] getUpdateSourceBytes(Object source) {
		if (source instanceof File file) {
			return getUpdateSourceBytes(file.getAbsolutePath());
		}
		return source.toString().getBytes();
	}

}
