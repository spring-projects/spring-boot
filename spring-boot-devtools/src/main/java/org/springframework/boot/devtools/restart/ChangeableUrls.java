/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.devtools.restart;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.devtools.settings.DevToolsSettings;
import org.springframework.util.StringUtils;

/**
 * A filtered collection of URLs which can change after the application has started.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
final class ChangeableUrls implements Iterable<URL> {

	private static final Log logger = LogFactory.getLog(ChangeableUrls.class);

	private final List<URL> urls;

	private ChangeableUrls(URL... urls) {
		DevToolsSettings settings = DevToolsSettings.get();
		List<URL> reloadableUrls = new ArrayList<>(urls.length);
		for (URL url : urls) {
			if ((settings.isRestartInclude(url) || isFolderUrl(url.toString()))
					&& !settings.isRestartExclude(url)) {
				reloadableUrls.add(url);
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Matching URLs for reloading : " + reloadableUrls);
		}
		this.urls = Collections.unmodifiableList(reloadableUrls);
	}

	private boolean isFolderUrl(String urlString) {
		return urlString.startsWith("file:") && urlString.endsWith("/");
	}

	@Override
	public Iterator<URL> iterator() {
		return this.urls.iterator();
	}

	public int size() {
		return this.urls.size();
	}

	public URL[] toArray() {
		return this.urls.toArray(new URL[this.urls.size()]);
	}

	public List<URL> toList() {
		return Collections.unmodifiableList(this.urls);
	}

	@Override
	public String toString() {
		return this.urls.toString();
	}

	public static ChangeableUrls fromUrlClassLoader(URLClassLoader classLoader) {
		List<URL> urls = new ArrayList<>();
		for (URL url : classLoader.getURLs()) {
			urls.add(url);
			urls.addAll(getUrlsFromClassPathOfJarManifestIfPossible(url));
		}
		return fromUrls(urls);
	}

	private static List<URL> getUrlsFromClassPathOfJarManifestIfPossible(URL url) {
		JarFile jarFile = getJarFileIfPossible(url);
		if (jarFile == null) {
			return Collections.<URL>emptyList();
		}
		try {
			return getUrlsFromManifestClassPathAttribute(jarFile);
		}
		catch (IOException ex) {
			throw new IllegalStateException(
					"Failed to read Class-Path attribute from manifest of jar " + url,
					ex);
		}
	}

	private static JarFile getJarFileIfPossible(URL url) {
		try {
			File file = new File(url.toURI());
			if (file.isFile()) {
				return new JarFile(file);
			}
		}
		catch (Exception ex) {
			// Assume it's not a jar and continue
		}
		return null;
	}

	private static List<URL> getUrlsFromManifestClassPathAttribute(JarFile jarFile)
			throws IOException {
		Manifest manifest = jarFile.getManifest();
		if (manifest == null) {
			return Collections.<URL>emptyList();
		}
		String classPath = manifest.getMainAttributes()
				.getValue(Attributes.Name.CLASS_PATH);
		if (!StringUtils.hasText(classPath)) {
			return Collections.emptyList();
		}
		String[] entries = StringUtils.delimitedListToStringArray(classPath, " ");
		List<URL> urls = new ArrayList<>(entries.length);
		File parent = new File(jarFile.getName()).getParentFile();
		List<File> nonExistentEntries = new ArrayList<>();
		for (String entry : entries) {
			try {
				File referenced = new File(parent, entry);
				if (referenced.exists()) {
					urls.add(referenced.toURI().toURL());
				}
				else {
					nonExistentEntries.add(referenced);
				}
			}
			catch (MalformedURLException ex) {
				throw new IllegalStateException(
						"Class-Path attribute contains malformed URL", ex);
			}
		}
		if (!nonExistentEntries.isEmpty()) {
			System.out.println("The Class-Path manifest attribute in " + jarFile.getName()
					+ " referenced one or more files that do not exist: "
					+ StringUtils.collectionToCommaDelimitedString(nonExistentEntries));
		}
		return urls;
	}

	public static ChangeableUrls fromUrls(Collection<URL> urls) {
		return fromUrls(new ArrayList<>(urls).toArray(new URL[urls.size()]));
	}

	public static ChangeableUrls fromUrls(URL... urls) {
		return new ChangeableUrls(urls);
	}

}
