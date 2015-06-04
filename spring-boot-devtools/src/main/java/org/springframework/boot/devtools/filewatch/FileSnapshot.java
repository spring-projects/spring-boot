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

	public FileSnapshot(File file) {
		Assert.notNull(file, "File must not be null");
		Assert.isTrue(file.isFile() || !file.exists(), "File must not be a folder");
		this.file = file;
		this.exists = file.exists();
		this.length = file.length();
		this.lastModified = file.lastModified();
	}

	public File getFile() {
		return this.file;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (obj instanceof FileSnapshot) {
			FileSnapshot other = (FileSnapshot) obj;
			boolean equals = this.file.equals(other.file);
			equals &= this.exists == other.exists;
			equals &= this.length == other.length;
			equals &= this.lastModified == other.lastModified;
			return equals;
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		int hashCode = this.file.hashCode();
		hashCode = 31 * hashCode + (this.exists ? 1231 : 1237);
		hashCode = 31 * hashCode + (int) (this.length ^ (this.length >>> 32));
		hashCode = 31 * hashCode + (int) (this.lastModified ^ (this.lastModified >>> 32));
		return hashCode;
	}

	@Override
	public String toString() {
		return this.file.toString();
	}

}
