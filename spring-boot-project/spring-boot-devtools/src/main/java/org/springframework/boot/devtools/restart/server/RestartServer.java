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

package org.springframework.boot.devtools.restart.server;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.devtools.restart.Restarter;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile.Kind;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFiles;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFiles.SourceDirectory;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ResourceUtils;

/**
 * Server used to {@link Restarter restart} the current application with updated
 * {@link ClassLoaderFiles}.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public class RestartServer {

	private static final Log logger = LogFactory.getLog(RestartServer.class);

	private final SourceDirectoryUrlFilter sourceDirectoryUrlFilter;

	private final ClassLoader classLoader;

	/**
	 * Create a new {@link RestartServer} instance.
	 * @param sourceDirectoryUrlFilter the source filter used to link remote directory to
	 * the local classpath
	 */
	public RestartServer(SourceDirectoryUrlFilter sourceDirectoryUrlFilter) {
		this(sourceDirectoryUrlFilter, Thread.currentThread().getContextClassLoader());
	}

	/**
	 * Create a new {@link RestartServer} instance.
	 * @param sourceDirectoryUrlFilter the source filter used to link remote directory to
	 * the local classpath
	 * @param classLoader the application classloader
	 */
	public RestartServer(SourceDirectoryUrlFilter sourceDirectoryUrlFilter, ClassLoader classLoader) {
		Assert.notNull(sourceDirectoryUrlFilter, "SourceDirectoryUrlFilter must not be null");
		Assert.notNull(classLoader, "ClassLoader must not be null");
		this.sourceDirectoryUrlFilter = sourceDirectoryUrlFilter;
		this.classLoader = classLoader;
	}

	/**
	 * Update the current running application with the specified {@link ClassLoaderFiles}
	 * and trigger a reload.
	 * @param files updated class loader files
	 */
	public void updateAndRestart(ClassLoaderFiles files) {
		Set<URL> urls = new LinkedHashSet<>();
		Set<URL> classLoaderUrls = getClassLoaderUrls();
		for (SourceDirectory directory : files.getSourceDirectories()) {
			for (Entry<String, ClassLoaderFile> entry : directory.getFilesEntrySet()) {
				for (URL url : classLoaderUrls) {
					if (updateFileSystem(url, entry.getKey(), entry.getValue())) {
						urls.add(url);
					}
				}
			}
			urls.addAll(getMatchingUrls(classLoaderUrls, directory.getName()));
		}
		updateTimeStamp(urls);
		restart(urls, files);
	}

	private boolean updateFileSystem(URL url, String name, ClassLoaderFile classLoaderFile) {
		if (!isDirectoryUrl(url.toString())) {
			return false;
		}
		try {
			File directory = ResourceUtils.getFile(url);
			File file = new File(directory, name);
			if (file.exists() && file.canWrite()) {
				if (classLoaderFile.getKind() == Kind.DELETED) {
					return file.delete();
				}
				FileCopyUtils.copy(classLoaderFile.getContents(), file);
				return true;
			}
		}
		catch (IOException ex) {
			// Ignore
		}
		return false;
	}

	private boolean isDirectoryUrl(String urlString) {
		return urlString.startsWith("file:") && urlString.endsWith("/");
	}

	private Set<URL> getMatchingUrls(Set<URL> urls, String sourceDirectory) {
		Set<URL> matchingUrls = new LinkedHashSet<>();
		for (URL url : urls) {
			if (this.sourceDirectoryUrlFilter.isMatch(sourceDirectory, url)) {
				if (logger.isDebugEnabled()) {
					logger.debug("URL " + url + " matched against source directory " + sourceDirectory);
				}
				matchingUrls.add(url);
			}
		}
		return matchingUrls;
	}

	private Set<URL> getClassLoaderUrls() {
		Set<URL> urls = new LinkedHashSet<>();
		ClassLoader classLoader = this.classLoader;
		while (classLoader != null) {
			if (classLoader instanceof URLClassLoader) {
				Collections.addAll(urls, ((URLClassLoader) classLoader).getURLs());
			}
			classLoader = classLoader.getParent();
		}
		return urls;
	}

	private void updateTimeStamp(Iterable<URL> urls) {
		for (URL url : urls) {
			updateTimeStamp(url);
		}
	}

	private void updateTimeStamp(URL url) {
		try {
			URL actualUrl = ResourceUtils.extractJarFileURL(url);
			File file = ResourceUtils.getFile(actualUrl, "Jar URL");
			file.setLastModified(System.currentTimeMillis());
		}
		catch (Exception ex) {
			// Ignore
		}
	}

	/**
	 * Called to restart the application.
	 * @param urls the updated URLs
	 * @param files the updated files
	 */
	protected void restart(Set<URL> urls, ClassLoaderFiles files) {
		Restarter restarter = Restarter.getInstance();
		restarter.addUrls(urls);
		restarter.addClassLoaderFiles(files);
		restarter.restart();
	}

}
