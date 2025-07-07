/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.devtools.restart.classloader;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.management.loading.ClassLoaderRepository;

import org.springframework.util.Assert;

/**
 * {@link ClassLoaderFileRepository} that maintains a collection of
 * {@link ClassLoaderFile} items grouped by source directories.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @see ClassLoaderFile
 * @see ClassLoaderRepository
 */
public class ClassLoaderFiles implements ClassLoaderFileRepository, Serializable {

	private static final long serialVersionUID = 1;

	private final Map<String, SourceDirectory> sourceDirectories;

	/**
	 * A flattened map of all files from all source directories for fast, O(1) lookups.
	 * The key is the file's relative path, and the value is the ClassLoaderFile.
	 */
	private final Map<String, ClassLoaderFile> filesByName;

	/**
	 * Create a new {@link ClassLoaderFiles} instance.
	 */
	public ClassLoaderFiles() {
		this.sourceDirectories = new LinkedHashMap<>();
		this.filesByName = new LinkedHashMap<>();
	}

	/**
	 * Create a new {@link ClassLoaderFiles} instance.
	 * @param classLoaderFiles the source classloader files.
	 */
	public ClassLoaderFiles(ClassLoaderFiles classLoaderFiles) {
		Assert.notNull(classLoaderFiles, "'classLoaderFiles' must not be null");
		this.sourceDirectories = new LinkedHashMap<>(classLoaderFiles.sourceDirectories);
		this.filesByName = new LinkedHashMap<>(classLoaderFiles.filesByName);
	}

	/**
	 * Add all elements items from the specified {@link ClassLoaderFiles} to this
	 * instance.
	 * @param files the files to add
	 */
	public void addAll(ClassLoaderFiles files) {
		Assert.notNull(files, "'files' must not be null");
		for (SourceDirectory directory : files.getSourceDirectories()) {
			for (Map.Entry<String, ClassLoaderFile> entry : directory.getFilesEntrySet()) {
				addFile(directory.getName(), entry.getKey(), entry.getValue());
			}
		}
	}

	/**
	 * Add a single {@link ClassLoaderFile} to the collection.
	 * @param name the name of the file
	 * @param file the file to add
	 */
	public void addFile(String name, ClassLoaderFile file) {
		addFile("", name, file);
	}

	/**
	 * Add a single {@link ClassLoaderFile} to the collection.
	 * @param sourceDirectory the source directory of the file
	 * @param name the name of the file
	 * @param file the file to add
	 */
	public void addFile(String sourceDirectory, String name, ClassLoaderFile file) {
		Assert.notNull(sourceDirectory, "'sourceDirectory' must not be null");
		Assert.notNull(name, "'name' must not be null");
		Assert.notNull(file, "'file' must not be null");
		removeAll(name);
		getOrCreateSourceDirectory(sourceDirectory).add(name, file);
		this.filesByName.put(name, file);
	}

	private void removeAll(String name) {
		for (SourceDirectory sourceDirectory : this.sourceDirectories.values()) {
			sourceDirectory.remove(name);
		}
		this.filesByName.remove(name);
	}

	/**
	 * Get or create a {@link SourceDirectory} with the given name.
	 * @param name the name of the directory
	 * @return an existing or newly added {@link SourceDirectory}
	 */
	protected final SourceDirectory getOrCreateSourceDirectory(String name) {
		return this.sourceDirectories.computeIfAbsent(name, (key) -> new SourceDirectory(name));
	}

	/**
	 * Return all {@link SourceDirectory SourceDirectories} that have been added to the
	 * collection.
	 * @return a collection of {@link SourceDirectory} items
	 */
	public Collection<SourceDirectory> getSourceDirectories() {
		return Collections.unmodifiableCollection(this.sourceDirectories.values());
	}

	/**
	 * Return the size of the collection.
	 * @return the size of the collection
	 */
	public int size() {
		return this.filesByName.size();
	}

	@Override
	public ClassLoaderFile getFile(String name) {
		return this.filesByName.get(name);
	}

	/**
	 * Returns a set of all file entries across all source directories for efficient
	 * iteration.
	 * @return a set of all file entries
	 */
	public Set<Entry<String, ClassLoaderFile>> getFileEntries() {
		return Collections.unmodifiableSet(this.filesByName.entrySet());
	}

	/**
	 * An individual source directory that is being managed by the collection.
	 */
	public static class SourceDirectory implements Serializable {

		private static final long serialVersionUID = 1;

		private final String name;

		private final Map<String, ClassLoaderFile> files = new LinkedHashMap<>();

		SourceDirectory(String name) {
			this.name = name;
		}

		public Set<Entry<String, ClassLoaderFile>> getFilesEntrySet() {
			return this.files.entrySet();
		}

		protected final void add(String name, ClassLoaderFile file) {
			this.files.put(name, file);
		}

		protected final void remove(String name) {
			this.files.remove(name);
		}

		protected final ClassLoaderFile get(String name) {
			return this.files.get(name);
		}

		/**
		 * Return the name of the source directory.
		 * @return the name of the source directory
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * Return all {@link ClassLoaderFile ClassLoaderFiles} in the collection that are
		 * contained in this source directory.
		 * @return the files contained in the source directory
		 */
		public Collection<ClassLoaderFile> getFiles() {
			return Collections.unmodifiableCollection(this.files.values());
		}

	}

}
