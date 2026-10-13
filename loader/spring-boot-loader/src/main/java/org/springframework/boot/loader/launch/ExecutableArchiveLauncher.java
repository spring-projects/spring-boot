/*
 * Copyright 2012-present the original author or authors.
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
