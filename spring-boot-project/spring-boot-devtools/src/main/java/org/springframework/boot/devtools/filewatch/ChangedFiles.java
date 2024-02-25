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
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

/**
 * A collections of files from a specific source directory that have changed.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @see FileChangeListener
 * @see ChangedFiles
 */
public final class ChangedFiles implements Iterable<ChangedFile> {

	private final File sourceDirectory;

	private final Set<ChangedFile> files;

	/**
	 * Creates a new instance of the ChangedFiles class with the specified source
	 * directory and set of changed files.
	 * @param sourceDirectory the source directory from which the files were changed
	 * @param files the set of changed files
	 */
	public ChangedFiles(File sourceDirectory, Set<ChangedFile> files) {
		this.sourceDirectory = sourceDirectory;
		this.files = Collections.unmodifiableSet(files);
	}

	/**
	 * The source directory being watched.
	 * @return the source directory
	 */
	public File getSourceDirectory() {
		return this.sourceDirectory;
	}

	/**
	 * Returns an iterator over the elements in this ChangedFiles object.
	 * @return an iterator over the elements in this ChangedFiles object
	 */
	@Override
	public Iterator<ChangedFile> iterator() {
		return getFiles().iterator();
	}

	/**
	 * The files that have been changed.
	 * @return the changed files
	 */
	public Set<ChangedFile> getFiles() {
		return this.files;
	}

	/**
	 * Compares this ChangedFiles object to the specified object for equality.
	 * @param obj the object to compare to
	 * @return true if the objects are equal, false otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj instanceof ChangedFiles other) {
			return this.sourceDirectory.equals(other.sourceDirectory) && this.files.equals(other.files);
		}
		return super.equals(obj);
	}

	/**
	 * Returns the hash code value for the ChangedFiles object. The hash code is computed
	 * based on the hash code of the files list.
	 * @return the hash code value for the ChangedFiles object
	 */
	@Override
	public int hashCode() {
		return this.files.hashCode();
	}

	/**
	 * Returns a string representation of the ChangedFiles object.
	 * @return a string representation of the ChangedFiles object, including the source
	 * directory and files
	 */
	@Override
	public String toString() {
		return this.sourceDirectory + " " + this.files;
	}

}
