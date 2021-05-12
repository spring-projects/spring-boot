/*
 * Copyright 2012-2020 the original author or authors.
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
	 * Create a new {@link ClassLoaderFiles} instance.
	 */
	public ClassLoaderFiles() {
		this.sourceDirectories = new LinkedHashMap<>();
	}

	/**
	 * Create a new {@link ClassLoaderFiles} instance.
	 * @param classLoaderFiles the source classloader files.
	 */
	public ClassLoaderFiles(ClassLoaderFiles classLoaderFiles) {
		Assert.notNull(classLoaderFiles, "ClassLoaderFiles must not be null");
		this.sourceDirectories = new LinkedHashMap<>(classLoaderFiles.sourceDirectories);
	}

	/**
	 * Add all elements items from the specified {@link ClassLoaderFiles} to this
	 * instance.
	 * @param files the files to add
	 */
	public void addAll(ClassLoaderFiles files) {
		Assert.notNull(files, "Files must not be null");
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
		Assert.notNull(sourceDirectory, "SourceDirectory must not be null");
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(file, "File must not be null");
		removeAll(name);
		getOrCreateSourceDirectory(sourceDirectory).add(name, file);
	}

	private void removeAll(String name) {
		for (SourceDirectory sourceDirectory : this.sourceDirectories.values()) {
			sourceDirectory.remove(name);
		}
	}

	/**
	 * Get or create a {@link SourceDirectory} with the given name.
	 * @param name the name of the directory
	 * @return an existing or newly added {@link SourceDirectory}
	 */
	protected final SourceDirectory getOrCreateSourceDirectory(String name) {
		SourceDirectory sourceDirectory = this.sourceDirectories.get(name);
		if (sourceDirectory == null) {
			sourceDirectory = new SourceDirectory(name);
			this.sourceDirectories.put(name, sourceDirectory);
		}
		return sourceDirectory;
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
		int size = 0;
		for (SourceDirectory sourceDirectory : this.sourceDirectories.values()) {
			size += sourceDirectory.getFiles().size();
		}
		return size;
	}

	@Override
	public ClassLoaderFile getFile(String name) {
		for (SourceDirectory sourceDirectory : this.sourceDirectories.values()) {
			ClassLoaderFile file = sourceDirectory.get(name);
			if (file != null) {
				return file;
			}
		}
		return null;
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
