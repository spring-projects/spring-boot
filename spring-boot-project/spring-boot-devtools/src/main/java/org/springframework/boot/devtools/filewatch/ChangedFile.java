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
import org.springframework.util.StringUtils;

/**
 * A single file that has changed.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @see ChangedFiles
 */
public final class ChangedFile {

	private final File sourceDirectory;

	private final File file;

	private final Type type;

	/**
	 * Create a new {@link ChangedFile} instance.
	 * @param sourceDirectory the source directory
	 * @param file the file
	 * @param type the type of change
	 */
	public ChangedFile(File sourceDirectory, File file, Type type) {
		Assert.notNull(sourceDirectory, "SourceDirectory must not be null");
		Assert.notNull(file, "File must not be null");
		Assert.notNull(type, "Type must not be null");
		this.sourceDirectory = sourceDirectory;
		this.file = file;
		this.type = type;
	}

	/**
	 * Return the file that was changed.
	 * @return the file
	 */
	public File getFile() {
		return this.file;
	}

	/**
	 * Return the type of change.
	 * @return the type of change
	 */
	public Type getType() {
		return this.type;
	}

	/**
	 * Return the name of the file relative to the source directory.
	 * @return the relative name
	 */
	public String getRelativeName() {
		File directory = this.sourceDirectory.getAbsoluteFile();
		File file = this.file.getAbsoluteFile();
		String directoryName = StringUtils.cleanPath(directory.getPath());
		String fileName = StringUtils.cleanPath(file.getPath());
		Assert.state(fileName.startsWith(directoryName),
				() -> "The file " + fileName + " is not contained in the source directory " + directoryName);
		return fileName.substring(directoryName.length() + 1);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (obj instanceof ChangedFile other) {
			return this.file.equals(other.file) && this.type.equals(other.type);
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return this.file.hashCode() * 31 + this.type.hashCode();
	}

	@Override
	public String toString() {
		return this.file + " (" + this.type + ")";
	}

	/**
	 * Change types.
	 */
	public enum Type {

		/**
		 * A new file has been added.
		 */
		ADD,

		/**
		 * An existing file has been modified.
		 */
		MODIFY,

		/**
		 * An existing file has been deleted.
		 */
		DELETE

	}

}
