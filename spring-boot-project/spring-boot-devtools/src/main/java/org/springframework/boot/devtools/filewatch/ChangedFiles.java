/*
 * Copyright 2012-2018 the original author or authors.
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
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

/**
 * A collections of files from a specific source folder that have changed.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @see FileChangeListener
 * @see ChangedFiles
 */
public final class ChangedFiles implements Iterable<ChangedFile> {

	private final File sourceFolder;

	private final Set<ChangedFile> files;

	public ChangedFiles(File sourceFolder, Set<ChangedFile> files) {
		this.sourceFolder = sourceFolder;
		this.files = Collections.unmodifiableSet(files);
	}

	/**
	 * The source folder being watched.
	 * @return the source folder
	 */
	public File getSourceFolder() {
		return this.sourceFolder;
	}

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

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj instanceof ChangedFiles) {
			ChangedFiles other = (ChangedFiles) obj;
			return this.sourceFolder.equals(other.sourceFolder)
					&& this.files.equals(other.files);
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return this.files.hashCode();
	}

	@Override
	public String toString() {
		return this.sourceFolder + " " + this.files;
	}

}
