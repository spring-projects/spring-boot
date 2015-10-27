/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot;

import java.io.File;
import java.security.MessageDigest;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Provides access to an application specific temporary directory. Generally speaking
 * different Spring Boot applications will get different locations, however, simply
 * restarting an application will give the same location.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public class ApplicationTemp {

	private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

	private final Class<?> sourceClass;

	private volatile File dir;

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
	 * Return a sub-directory of the application temp.
	 * @param subDir the sub-directory name
	 * @return a sub-directory
	 */
	public File getDir(String subDir) {
		File dir = new File(getDir(), subDir);
		dir.mkdirs();
		return dir;
	}

	/**
	 * Return the directory to be used for application specific temp files.
	 * @return the application temp directory
	 */
	public File getDir() {
		if (this.dir == null) {
			synchronized (this) {
				byte[] hash = generateHash(this.sourceClass);
				this.dir = new File(getTempDirectory(), toHexString(hash));
				this.dir.mkdirs();
				Assert.state(this.dir.exists(),
						"Unable to create temp directory " + this.dir);
			}
		}
		return this.dir;
	}

	private File getTempDirectory() {
		String property = System.getProperty("java.io.tmpdir");
		Assert.state(StringUtils.hasLength(property), "No 'java.io.tmpdir' property set");
		File file = new File(property);
		Assert.state(file.exists(), "Temp directory" + file + " does not exist");
		Assert.state(file.isDirectory(), "Temp location " + file + " is not a directory");
		return file;
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
		if (source instanceof File) {
			return getUpdateSourceBytes(((File) source).getAbsolutePath());
		}
		return source.toString().getBytes();
	}

	private String toHexString(byte[] bytes) {
		char[] hex = new char[bytes.length * 2];
		for (int i = 0; i < bytes.length; i++) {
			int b = bytes[i] & 0xFF;
			hex[i * 2] = HEX_CHARS[b >>> 4];
			hex[i * 2 + 1] = HEX_CHARS[b & 0x0F];
		}
		return new String(hex);
	}

}
