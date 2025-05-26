/*
 * Copyright 2012-2024 the original author or authors.
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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.jar.Manifest;

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

	private final Archive archive;

	public ExecutableArchiveLauncher() throws Exception {
		this(Archive.create(Launcher.class));
	}

	protected ExecutableArchiveLauncher(Archive archive) throws Exception {
		this.archive = archive;
		this.classPathIndex = getClassPathIndex(this.archive);
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

	/**
	 * Determine if the specified directory entry is a candidate for further searching.
	 * @param entry the entry to check
	 * @return {@code true} if the entry is a candidate for further searching
	 */
	protected boolean isSearchedDirectory(Archive.Entry entry) {
		return ((getEntryPathPrefix() == null) || entry.name().startsWith(getEntryPathPrefix()))
				&& !isIncludedOnClassPath(entry);
	}

}
