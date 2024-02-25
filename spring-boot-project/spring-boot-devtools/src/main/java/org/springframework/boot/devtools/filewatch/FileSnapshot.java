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

package org.springframework.boot.devtools.filewatch;

import java.io.File;

import org.springframework.util.Assert;

/**
 * A snapshot of a File at a given point in time.
 *
 * @author Phillip Webb
 */
class FileSnapshot {

	private final File file;

	private final boolean exists;

	private final long length;

	private final long lastModified;

	/**
     * Constructs a new FileSnapshot object with the given file.
     * 
     * @param file the file to create a snapshot of
     * @throws IllegalArgumentException if the file is null or if the file is a directory
     */
    FileSnapshot(File file) {
		Assert.notNull(file, "File must not be null");
		Assert.isTrue(file.isFile() || !file.exists(), "File must not be a directory");
		this.file = file;
		this.exists = file.exists();
		this.length = file.length();
		this.lastModified = file.lastModified();
	}

	/**
     * Returns the file associated with this FileSnapshot.
     *
     * @return the file associated with this FileSnapshot
     */
    File getFile() {
		return this.file;
	}

	/**
     * Compares this FileSnapshot object to the specified object for equality.
     * 
     * @param obj the object to compare to
     * @return true if the objects are equal, false otherwise
     */
    @Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (obj instanceof FileSnapshot other) {
			boolean equals = this.file.equals(other.file);
			equals = equals && this.exists == other.exists;
			equals = equals && this.length == other.length;
			equals = equals && this.lastModified == other.lastModified;
			return equals;
		}
		return super.equals(obj);
	}

	/**
     * Returns the hash code value for this FileSnapshot object.
     * 
     * The hash code is calculated based on the file path, existence status, length, and last modified timestamp of the file.
     * 
     * @return the hash code value for this FileSnapshot object
     */
    @Override
	public int hashCode() {
		int hashCode = this.file.hashCode();
		hashCode = 31 * hashCode + Boolean.hashCode(this.exists);
		hashCode = 31 * hashCode + Long.hashCode(this.length);
		hashCode = 31 * hashCode + Long.hashCode(this.lastModified);
		return hashCode;
	}

	/**
     * Returns a string representation of the FileSnapshot object.
     * 
     * @return a string representation of the FileSnapshot object
     */
    @Override
	public String toString() {
		return this.file.toString();
	}

}
