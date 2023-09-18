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

package org.springframework.boot.loader.launch;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.springframework.boot.loader.launch.Archive.Entry;

/**
 * Base class for a {@link Launcher} backed by an executable archive.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Scott Frederick
 * @since 3.2.0
 * @see JarLauncher
 * @see WarLauncher
 */
public abstract class ExecutableArchiveLauncher extends Launcher {

	private static final String START_CLASS_ATTRIBUTE = "Start-Class";

	protected static final String BOOT_CLASSPATH_INDEX_ATTRIBUTE = "Spring-Boot-Classpath-Index";

	protected static final String DEFAULT_CLASSPATH_INDEX_FILE_NAME = "classpath.idx";

	private final Archive archive;

	private final ClassPathIndexFile classPathIndex;

	public ExecutableArchiveLauncher() throws Exception {
		this(Archive.create(Launcher.class));
	}

	protected ExecutableArchiveLauncher(Archive archive) throws Exception {
		this.archive = archive;
		this.classPathIndex = getClassPathIndex(this.archive);
	}

	ClassPathIndexFile getClassPathIndex(Archive archive) throws IOException {
		if (!archive.isExploded()) {
			return null; // Regular archives already have a defined order
		}
		String location = getClassPathIndexFileLocation(archive);
		return ClassPathIndexFile.loadIfPossible(archive.getRootDirectory(), location);
	}

	private String getClassPathIndexFileLocation(Archive archive) throws IOException {
		Manifest manifest = archive.getManifest();
		Attributes attributes = (manifest != null) ? manifest.getMainAttributes() : null;
		String location = (attributes != null) ? attributes.getValue(BOOT_CLASSPATH_INDEX_ATTRIBUTE) : null;
		return (location != null) ? location : getEntryPathPrefix() + DEFAULT_CLASSPATH_INDEX_FILE_NAME;
	}

	@Override
	protected ClassLoader createClassLoader(Collection<URL> urls) throws Exception {
		if (this.classPathIndex != null) {
			urls = new ArrayList<>(urls);
			urls.addAll(this.classPathIndex.getUrls());
		}
		return super.createClassLoader(urls);
	}

	@Override
	protected final Archive getArchive() {
		return this.archive;
	}

	@Override
	protected String getMainClass() throws Exception {
		Manifest manifest = this.archive.getManifest();
		String mainClass = (manifest != null) ? manifest.getMainAttributes().getValue(START_CLASS_ATTRIBUTE) : null;
		if (mainClass == null) {
			throw new IllegalStateException("No 'Start-Class' manifest entry specified in " + this);
		}
		return mainClass;
	}

	@Override
	protected Set<URL> getClassPathUrls() throws Exception {
		return this.archive.getClassPathUrls(this::isIncludedOnClassPathAndNotIndexed, this::isSearchedDirectory);
	}

	private boolean isIncludedOnClassPathAndNotIndexed(Entry entry) {
		if (!isIncludedOnClassPath(entry)) {
			return false;
		}
		return (this.classPathIndex == null) || !this.classPathIndex.containsEntry(entry.name());
	}

	/**
	 * Determine if the specified directory entry is a candidate for further searching.
	 * @param entry the entry to check
	 * @return {@code true} if the entry is a candidate for further searching
	 */
	protected boolean isSearchedDirectory(Archive.Entry entry) {
		return ((getEntryPathPrefix() == null) || entry.name().startsWith(getEntryPathPrefix()))
				&& !isIncludedOnClassPath(entry);
	}

	/**
	 * Determine if the specified entry is a nested item that should be added to the
	 * classpath.
	 * @param entry the entry to check
	 * @return {@code true} if the entry is a nested item (jar or directory)
	 */
	protected abstract boolean isIncludedOnClassPath(Archive.Entry entry);

	/**
	 * Return the path prefix for relevant entries in the archive.
	 * @return the entry path prefix
	 */
	protected abstract String getEntryPathPrefix();

}
