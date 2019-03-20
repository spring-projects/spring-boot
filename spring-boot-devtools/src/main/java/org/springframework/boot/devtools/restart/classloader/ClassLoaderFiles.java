/*
 * Copyright 2012-2015 the original author or authors.
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
 * {@link ClassLoaderFile} items grouped by source folders.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @see ClassLoaderFile
 * @see ClassLoaderRepository
 */
public class ClassLoaderFiles implements ClassLoaderFileRepository, Serializable {

	private static final long serialVersionUID = 1;

	private final Map<String, SourceFolder> sourceFolders;

	/**
	 * Create a new {@link ClassLoaderFiles} instance.
	 */
	public ClassLoaderFiles() {
		this.sourceFolders = new LinkedHashMap<String, SourceFolder>();
	}

	/**
	 * Create a new {@link ClassLoaderFiles} instance.
	 * @param classLoaderFiles the source classloader files.
	 */
	public ClassLoaderFiles(ClassLoaderFiles classLoaderFiles) {
		Assert.notNull(classLoaderFiles, "ClassLoaderFiles must not be null");
		this.sourceFolders = new LinkedHashMap<String, SourceFolder>(
				classLoaderFiles.sourceFolders);
	}

	/**
	 * Add all elements items from the specified {@link ClassLoaderFiles} to this
	 * instance.
	 * @param files the files to add
	 */
	public void addAll(ClassLoaderFiles files) {
		Assert.notNull(files, "Files must not be null");
		for (SourceFolder folder : files.getSourceFolders()) {
			for (Map.Entry<String, ClassLoaderFile> entry : folder.getFilesEntrySet()) {
				addFile(folder.getName(), entry.getKey(), entry.getValue());
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
	 * @param sourceFolder the source folder of the file
	 * @param name the name of the file
	 * @param file the file to add
	 */
	public void addFile(String sourceFolder, String name, ClassLoaderFile file) {
		Assert.notNull(sourceFolder, "SourceFolder must not be null");
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(file, "File must not be null");
		removeAll(name);
		getOrCreateSourceFolder(sourceFolder).add(name, file);
	}

	private void removeAll(String name) {
		for (SourceFolder sourceFolder : this.sourceFolders.values()) {
			sourceFolder.remove(name);
		}
	}

	/**
	 * Get or create a {@link SourceFolder} with the given name.
	 * @param name the name of the folder
	 * @return an existing or newly added {@link SourceFolder}
	 */
	protected final SourceFolder getOrCreateSourceFolder(String name) {
		SourceFolder sourceFolder = this.sourceFolders.get(name);
		if (sourceFolder == null) {
			sourceFolder = new SourceFolder(name);
			this.sourceFolders.put(name, sourceFolder);
		}
		return sourceFolder;
	}

	/**
	 * Return all {@link SourceFolder SourceFolders} that have been added to the
	 * collection.
	 * @return a collection of {@link SourceFolder} items
	 */
	public Collection<SourceFolder> getSourceFolders() {
		return Collections.unmodifiableCollection(this.sourceFolders.values());
	}

	/**
	 * Return the size of the collection.
	 * @return the size of the collection
	 */
	public int size() {
		int size = 0;
		for (SourceFolder sourceFolder : this.sourceFolders.values()) {
			size += sourceFolder.getFiles().size();
		}
		return size;
	}

	@Override
	public ClassLoaderFile getFile(String name) {
		for (SourceFolder sourceFolder : this.sourceFolders.values()) {
			ClassLoaderFile file = sourceFolder.get(name);
			if (file != null) {
				return file;
			}
		}
		return null;
	}

	/**
	 * An individual source folder that is being managed by the collection.
	 */
	public static class SourceFolder implements Serializable {

		private static final long serialVersionUID = 1;

		private final String name;

		private final Map<String, ClassLoaderFile> files = new LinkedHashMap<String, ClassLoaderFile>();

		SourceFolder(String name) {
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
		 * Return the name of the source folder.
		 * @return the name of the source folder
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * Return all {@link ClassLoaderFile ClassLoaderFiles} in the collection that are
		 * contained in this source folder.
		 * @return the files contained in the source folder
		 */
		public Collection<ClassLoaderFile> getFiles() {
			return Collections.unmodifiableCollection(this.files.values());
		}

	}

}
