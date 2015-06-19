/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.loader;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.Archive.Entry;
import org.springframework.boot.loader.archive.Archive.EntryFilter;

/**
 * Base class for executable archive {@link Launcher}s.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public abstract class ExecutableArchiveLauncher extends Launcher {

	private final Archive archive;

	private final JavaAgentDetector javaAgentDetector;

	public ExecutableArchiveLauncher() {
		this(new InputArgumentsJavaAgentDetector());
	}

	public ExecutableArchiveLauncher(JavaAgentDetector javaAgentDetector) {
		try {
			this.archive = createArchive();
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
		this.javaAgentDetector = javaAgentDetector;
	}

	ExecutableArchiveLauncher(Archive archive) {
		this.javaAgentDetector = new InputArgumentsJavaAgentDetector();
		this.archive = archive;
	}

	protected final Archive getArchive() {
		return this.archive;
	}

	@Override
	protected String getMainClass() throws Exception {
		return this.archive.getMainClass();
	}

	@Override
	protected List<Archive> getClassPathArchives() throws Exception {
		List<Archive> archives = new ArrayList<Archive>(
				this.archive.getNestedArchives(new EntryFilter() {
					@Override
					public boolean matches(Entry entry) {
						return isNestedArchive(entry);
					}
				}));
		postProcessClassPathArchives(archives);
		return archives;
	}

	@Override
	protected ClassLoader createClassLoader(URL[] urls) throws Exception {
		Set<URL> copy = new LinkedHashSet<URL>(urls.length);
		ClassLoader loader = getDefaultClassLoader();
		if (loader instanceof URLClassLoader) {
			for (URL url : ((URLClassLoader) loader).getURLs()) {
				if (addDefaultClassloaderUrl(urls, url)) {
					copy.add(url);
				}
			}
		}
		Collections.addAll(copy, urls);
		return super.createClassLoader(copy.toArray(new URL[copy.size()]));
	}

	private boolean addDefaultClassloaderUrl(URL[] urls, URL url) {
		String jarUrl = "jar:" + url + "!/";
		for (URL nestedUrl : urls) {
			if (nestedUrl.equals(url) || nestedUrl.toString().equals(jarUrl)) {
				return false;
			}
		}
		return !this.javaAgentDetector.isJavaAgentJar(url);
	}

	/**
	 * Determine if the specified {@link JarEntry} is a nested item that should be added
	 * to the classpath. The method is called once for each entry.
	 * @param entry the jar entry
	 * @return {@code true} if the entry is a nested item (jar or folder)
	 */
	protected abstract boolean isNestedArchive(Archive.Entry entry);

	/**
	 * Called to post-process archive entries before they are used. Implementations can
	 * add and remove entries.
	 * @param archives the archives
	 * @throws Exception if the post processing fails
	 */
	protected void postProcessClassPathArchives(List<Archive> archives) throws Exception {
	}

	private static ClassLoader getDefaultClassLoader() {
		ClassLoader classloader = null;
		try {
			classloader = Thread.currentThread().getContextClassLoader();
		}
		catch (Throwable ex) {
			// Cannot access thread context ClassLoader - falling back to system class
			// loader...
		}
		if (classloader == null) {
			// No thread context class loader -> use class loader of this class.
			classloader = ExecutableArchiveLauncher.class.getClassLoader();
		}
		return classloader;
	}

}
