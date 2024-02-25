/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.loader;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.ExplodedArchive;

/**
 * Base class for executable archive {@link Launcher}s.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Scott Frederick
 * @since 1.0.0
 */
public abstract class ExecutableArchiveLauncher extends Launcher {

	private static final String START_CLASS_ATTRIBUTE = "Start-Class";

	protected static final String BOOT_CLASSPATH_INDEX_ATTRIBUTE = "Spring-Boot-Classpath-Index";

	protected static final String DEFAULT_CLASSPATH_INDEX_FILE_NAME = "classpath.idx";

	private final Archive archive;

	private final ClassPathIndexFile classPathIndex;

	/**
     * Constructs a new ExecutableArchiveLauncher.
     * 
     * This constructor creates an ExecutableArchiveLauncher object by initializing the archive and classPathIndex
     * properties. It throws an IllegalStateException if any exception occurs during the process.
     * 
     * @throws IllegalStateException if an exception occurs during the creation of the archive or classPathIndex
     */
    public ExecutableArchiveLauncher() {
		try {
			this.archive = createArchive();
			this.classPathIndex = getClassPathIndex(this.archive);
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
     * Constructs a new ExecutableArchiveLauncher with the specified Archive.
     * 
     * @param archive the Archive to be used by the launcher
     * @throws IllegalStateException if an exception occurs while initializing the launcher
     */
    protected ExecutableArchiveLauncher(Archive archive) {
		try {
			this.archive = archive;
			this.classPathIndex = getClassPathIndex(this.archive);
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
     * Retrieves the class path index file for the given archive.
     * 
     * @param archive the archive for which to retrieve the class path index file
     * @return the class path index file if it exists, null otherwise
     * @throws IOException if an I/O error occurs while retrieving the class path index file
     */
    protected ClassPathIndexFile getClassPathIndex(Archive archive) throws IOException {
		// Only needed for exploded archives, regular ones already have a defined order
		if (archive instanceof ExplodedArchive) {
			String location = getClassPathIndexFileLocation(archive);
			return ClassPathIndexFile.loadIfPossible(archive.getUrl(), location);
		}
		return null;
	}

	/**
     * Returns the location of the classpath index file for the given archive.
     * 
     * @param archive the archive for which to retrieve the classpath index file location
     * @return the location of the classpath index file
     * @throws IOException if an I/O error occurs while retrieving the manifest or attributes
     */
    private String getClassPathIndexFileLocation(Archive archive) throws IOException {
		Manifest manifest = archive.getManifest();
		Attributes attributes = (manifest != null) ? manifest.getMainAttributes() : null;
		String location = (attributes != null) ? attributes.getValue(BOOT_CLASSPATH_INDEX_ATTRIBUTE) : null;
		return (location != null) ? location : getArchiveEntryPathPrefix() + DEFAULT_CLASSPATH_INDEX_FILE_NAME;
	}

	/**
     * Returns the main class specified in the manifest of the executable archive.
     * 
     * @return the main class name
     * @throws Exception if an error occurs while retrieving the main class
     * @throws IllegalStateException if no 'Start-Class' manifest entry is specified in the executable archive
     */
    @Override
	protected String getMainClass() throws Exception {
		Manifest manifest = this.archive.getManifest();
		String mainClass = null;
		if (manifest != null) {
			mainClass = manifest.getMainAttributes().getValue(START_CLASS_ATTRIBUTE);
		}
		if (mainClass == null) {
			throw new IllegalStateException("No 'Start-Class' manifest entry specified in " + this);
		}
		return mainClass;
	}

	/**
     * Creates a class loader for the given archives.
     * 
     * @param archives the iterator of archives
     * @return the created class loader
     * @throws Exception if an error occurs while creating the class loader
     */
    @Override
	protected ClassLoader createClassLoader(Iterator<Archive> archives) throws Exception {
		List<URL> urls = new ArrayList<>(guessClassPathSize());
		while (archives.hasNext()) {
			urls.add(archives.next().getUrl());
		}
		if (this.classPathIndex != null) {
			urls.addAll(this.classPathIndex.getUrls());
		}
		return createClassLoader(urls.toArray(new URL[0]));
	}

	/**
     * Returns the estimated size of the class path.
     * 
     * @return The estimated size of the class path.
     */
    private int guessClassPathSize() {
		if (this.classPathIndex != null) {
			return this.classPathIndex.size() + 10;
		}
		return 50;
	}

	/**
     * Returns an iterator over the class path archives.
     * 
     * @return an iterator over the class path archives
     * @throws Exception if an error occurs while getting the class path archives
     */
    @Override
	protected Iterator<Archive> getClassPathArchivesIterator() throws Exception {
		Archive.EntryFilter searchFilter = this::isSearchCandidate;
		Iterator<Archive> archives = this.archive.getNestedArchives(searchFilter,
				(entry) -> isNestedArchive(entry) && !isEntryIndexed(entry));
		if (isPostProcessingClassPathArchives()) {
			archives = applyClassPathArchivePostProcessing(archives);
		}
		return archives;
	}

	/**
     * Checks if the given entry is indexed in the class path index.
     *
     * @param entry the entry to check
     * @return true if the entry is indexed, false otherwise
     */
    private boolean isEntryIndexed(Archive.Entry entry) {
		if (this.classPathIndex != null) {
			return this.classPathIndex.containsEntry(entry.getName());
		}
		return false;
	}

	/**
     * Applies post-processing to the classpath archives.
     * 
     * @param archives the iterator of archives to be processed
     * @return the iterator of processed archives
     * @throws Exception if an error occurs during post-processing
     */
    private Iterator<Archive> applyClassPathArchivePostProcessing(Iterator<Archive> archives) throws Exception {
		List<Archive> list = new ArrayList<>();
		while (archives.hasNext()) {
			list.add(archives.next());
		}
		postProcessClassPathArchives(list);
		return list.iterator();
	}

	/**
	 * Determine if the specified entry is a candidate for further searching.
	 * @param entry the entry to check
	 * @return {@code true} if the entry is a candidate for further searching
	 * @since 2.3.0
	 */
	protected boolean isSearchCandidate(Archive.Entry entry) {
		if (getArchiveEntryPathPrefix() == null) {
			return true;
		}
		return entry.getName().startsWith(getArchiveEntryPathPrefix());
	}

	/**
	 * Determine if the specified entry is a nested item that should be added to the
	 * classpath.
	 * @param entry the entry to check
	 * @return {@code true} if the entry is a nested item (jar or directory)
	 */
	protected abstract boolean isNestedArchive(Archive.Entry entry);

	/**
	 * Return if post-processing needs to be applied to the archives. For back
	 * compatibility this method returns {@code true}, but subclasses that don't override
	 * {@link #postProcessClassPathArchives(List)} should provide an implementation that
	 * returns {@code false}.
	 * @return if the {@link #postProcessClassPathArchives(List)} method is implemented
	 * @since 2.3.0
	 */
	protected boolean isPostProcessingClassPathArchives() {
		return true;
	}

	/**
	 * Called to post-process archive entries before they are used. Implementations can
	 * add and remove entries.
	 * @param archives the archives
	 * @throws Exception if the post-processing fails
	 * @see #isPostProcessingClassPathArchives()
	 */
	protected void postProcessClassPathArchives(List<Archive> archives) throws Exception {
	}

	/**
	 * Return the path prefix for entries in the archive.
	 * @return the path prefix
	 */
	protected String getArchiveEntryPathPrefix() {
		return null;
	}

	/**
     * Returns a boolean value indicating whether the archive is exploded.
     * 
     * @return {@code true} if the archive is exploded, {@code false} otherwise.
     */
    @Override
	protected boolean isExploded() {
		return this.archive.isExploded();
	}

	/**
     * Returns the archive associated with this ExecutableArchiveLauncher.
     *
     * @return the archive associated with this ExecutableArchiveLauncher
     */
    @Override
	protected final Archive getArchive() {
		return this.archive;
	}

}
