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
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.devtools.filewatch.ChangedFile.Type;
import org.springframework.util.Assert;

/**
 * A snapshot of a directory at a given point in time.
 *
 * @author Phillip Webb
 */
class DirectorySnapshot {

	private static final Set<String> DOTS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(".", "..")));

	private final File directory;

	private final Date time;

	private Set<FileSnapshot> files;

	/**
	 * Create a new {@link DirectorySnapshot} for the given directory.
	 * @param directory the source directory
	 */
	DirectorySnapshot(File directory) {
		Assert.notNull(directory, "Directory must not be null");
		Assert.isTrue(!directory.isFile(), () -> "Directory '" + directory + "' must not be a file");
		this.directory = directory;
		this.time = new Date();
		Set<FileSnapshot> files = new LinkedHashSet<>();
		collectFiles(directory, files);
		this.files = Collections.unmodifiableSet(files);
	}

	private void collectFiles(File source, Set<FileSnapshot> result) {
		File[] children = source.listFiles();
		if (children != null) {
			for (File child : children) {
				if (child.isDirectory() && !DOTS.contains(child.getName())) {
					collectFiles(child, result);
				}
				else if (child.isFile()) {
					result.add(new FileSnapshot(child));
				}
			}
		}
	}

	ChangedFiles getChangedFiles(DirectorySnapshot snapshot, FileFilter triggerFilter) {
		Assert.notNull(snapshot, "Snapshot must not be null");
		File directory = this.directory;
		Assert.isTrue(snapshot.directory.equals(directory),
				() -> "Snapshot source directory must be '" + directory + "'");
		Set<ChangedFile> changes = new LinkedHashSet<>();
		Map<File, FileSnapshot> previousFiles = getFilesMap();
		for (FileSnapshot currentFile : snapshot.files) {
			if (acceptChangedFile(triggerFilter, currentFile)) {
				FileSnapshot previousFile = previousFiles.remove(currentFile.getFile());
				if (previousFile == null) {
					changes.add(new ChangedFile(directory, currentFile.getFile(), Type.ADD));
				}
				else if (!previousFile.equals(currentFile)) {
					changes.add(new ChangedFile(directory, currentFile.getFile(), Type.MODIFY));
				}
			}
		}
		for (FileSnapshot previousFile : previousFiles.values()) {
			if (acceptChangedFile(triggerFilter, previousFile)) {
				changes.add(new ChangedFile(directory, previousFile.getFile(), Type.DELETE));
			}
		}
		return new ChangedFiles(directory, changes);
	}

	private boolean acceptChangedFile(FileFilter triggerFilter, FileSnapshot file) {
		return (triggerFilter == null || !triggerFilter.accept(file.getFile()));
	}

	private Map<File, FileSnapshot> getFilesMap() {
		Map<File, FileSnapshot> files = new LinkedHashMap<>();
		for (FileSnapshot file : this.files) {
			files.put(file.getFile(), file);
		}
		return files;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (obj instanceof DirectorySnapshot other) {
			return equals(other, null);
		}
		return super.equals(obj);
	}

	boolean equals(DirectorySnapshot other, FileFilter filter) {
		if (this.directory.equals(other.directory)) {
			Set<FileSnapshot> ourFiles = filter(this.files, filter);
			Set<FileSnapshot> otherFiles = filter(other.files, filter);
			return ourFiles.equals(otherFiles);
		}
		return false;
	}

	private Set<FileSnapshot> filter(Set<FileSnapshot> source, FileFilter filter) {
		if (filter == null) {
			return source;
		}
		Set<FileSnapshot> filtered = new LinkedHashSet<>();
		for (FileSnapshot file : source) {
			if (filter.accept(file.getFile())) {
				filtered.add(file);
			}
		}
		return filtered;
	}

	@Override
	public int hashCode() {
		int hashCode = this.directory.hashCode();
		hashCode = 31 * hashCode + this.files.hashCode();
		return hashCode;
	}

	/**
	 * Return the source directory of this snapshot.
	 * @return the source directory
	 */
	File getDirectory() {
		return this.directory;
	}

	@Override
	public String toString() {
		return this.directory + " snapshot at " + this.time;
	}

}
