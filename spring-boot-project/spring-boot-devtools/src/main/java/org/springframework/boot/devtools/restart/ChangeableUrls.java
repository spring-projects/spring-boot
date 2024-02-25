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

package org.springframework.boot.devtools.restart;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;

import org.springframework.boot.devtools.logger.DevToolsLogFactory;
import org.springframework.boot.devtools.settings.DevToolsSettings;
import org.springframework.core.log.LogMessage;
import org.springframework.util.StringUtils;

/**
 * A filtered collection of URLs which can change after the application has started.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
final class ChangeableUrls implements Iterable<URL> {

	private static final Log logger = DevToolsLogFactory.getLog(ChangeableUrls.class);

	private final List<URL> urls;

	/**
     * Constructs a new ChangeableUrls object with the given URLs.
     * 
     * @param urls the URLs to be included in the ChangeableUrls object
     */
    private ChangeableUrls(URL... urls) {
		DevToolsSettings settings = DevToolsSettings.get();
		List<URL> reloadableUrls = new ArrayList<>(urls.length);
		for (URL url : urls) {
			if ((settings.isRestartInclude(url) || isDirectoryUrl(url.toString())) && !settings.isRestartExclude(url)) {
				reloadableUrls.add(url);
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Matching URLs for reloading : " + reloadableUrls);
		}
		this.urls = Collections.unmodifiableList(reloadableUrls);
	}

	/**
     * Checks if the given URL string represents a directory.
     * 
     * @param urlString the URL string to be checked
     * @return true if the URL string represents a directory, false otherwise
     */
    private boolean isDirectoryUrl(String urlString) {
		return urlString.startsWith("file:") && urlString.endsWith("/");
	}

	/**
     * Returns an iterator over the elements in this ChangeableUrls object.
     *
     * @return an iterator over the elements in this ChangeableUrls object
     */
    @Override
	public Iterator<URL> iterator() {
		return this.urls.iterator();
	}

	/**
     * Returns the size of the list of URLs.
     *
     * @return the size of the list of URLs
     */
    int size() {
		return this.urls.size();
	}

	/**
     * Converts the list of URLs to an array of URLs.
     * 
     * @return an array of URLs containing the same elements as the list of URLs
     */
    URL[] toArray() {
		return this.urls.toArray(new URL[0]);
	}

	/**
     * Returns an unmodifiable list of URLs.
     *
     * @return an unmodifiable list of URLs
     */
    List<URL> toList() {
		return Collections.unmodifiableList(this.urls);
	}

	/**
     * Returns a string representation of the ChangeableUrls object.
     * 
     * @return a string representation of the ChangeableUrls object
     */
    @Override
	public String toString() {
		return this.urls.toString();
	}

	/**
     * Retrieves a list of changeable URLs from the given class loader.
     * 
     * @param classLoader the class loader from which to retrieve the URLs
     * @return a ChangeableUrls object containing the list of changeable URLs
     */
    static ChangeableUrls fromClassLoader(ClassLoader classLoader) {
		List<URL> urls = new ArrayList<>();
		for (URL url : urlsFromClassLoader(classLoader)) {
			urls.add(url);
			urls.addAll(getUrlsFromClassPathOfJarManifestIfPossible(url));
		}
		return fromUrls(urls);
	}

	/**
     * Returns an array of URLs from the given ClassLoader.
     * If the ClassLoader is an instance of URLClassLoader, it returns the URLs from the URLClassLoader.
     * Otherwise, it splits the class path obtained from the runtime MXBean and converts each path to a URL.
     * 
     * @param classLoader the ClassLoader to retrieve URLs from
     * @return an array of URLs from the ClassLoader
     */
    private static URL[] urlsFromClassLoader(ClassLoader classLoader) {
		if (classLoader instanceof URLClassLoader urlClassLoader) {
			return urlClassLoader.getURLs();
		}
		return Stream.of(ManagementFactory.getRuntimeMXBean().getClassPath().split(File.pathSeparator))
			.map(ChangeableUrls::toURL)
			.toArray(URL[]::new);
	}

	/**
     * Converts a class path entry to a URL.
     * 
     * @param classPathEntry the class path entry to convert
     * @return the URL representation of the class path entry
     * @throws IllegalArgumentException if the URL could not be created from the class path entry
     */
    private static URL toURL(String classPathEntry) {
		try {
			return new File(classPathEntry).toURI().toURL();
		}
		catch (MalformedURLException ex) {
			throw new IllegalArgumentException("URL could not be created from '" + classPathEntry + "'", ex);
		}
	}

	/**
     * Retrieves a list of URLs from the classpath of a JAR file's manifest, if possible.
     * 
     * @param url The URL of the JAR file.
     * @return A list of URLs from the classpath of the JAR file's manifest, or an empty list if not found.
     * @throws IllegalStateException If failed to read the Class-Path attribute from the manifest.
     */
    private static List<URL> getUrlsFromClassPathOfJarManifestIfPossible(URL url) {
		try {
			File file = new File(url.toURI());
			if (file.isFile()) {
				try (JarFile jarFile = new JarFile(file)) {
					try {
						return getUrlsFromManifestClassPathAttribute(url, jarFile);
					}
					catch (IOException ex) {
						throw new IllegalStateException(
								"Failed to read Class-Path attribute from manifest of jar " + url, ex);
					}
				}
			}
		}
		catch (Exception ex) {
			// Assume it's not a jar and continue
		}
		return Collections.emptyList();
	}

	/**
     * Retrieves a list of URLs from the Class-Path attribute in the manifest file of a JAR file.
     * 
     * @param jarUrl The URL of the JAR file.
     * @param jarFile The JarFile object representing the JAR file.
     * @return A list of URLs extracted from the Class-Path attribute.
     * @throws IOException If an I/O error occurs while reading the manifest file.
     * @throws IllegalStateException If the Class-Path attribute contains a malformed URL.
     */
    private static List<URL> getUrlsFromManifestClassPathAttribute(URL jarUrl, JarFile jarFile) throws IOException {
		Manifest manifest = jarFile.getManifest();
		if (manifest == null) {
			return Collections.emptyList();
		}
		String classPath = manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
		if (!StringUtils.hasText(classPath)) {
			return Collections.emptyList();
		}
		String[] entries = StringUtils.delimitedListToStringArray(classPath, " ");
		List<URL> urls = new ArrayList<>(entries.length);
		List<URL> nonExistentEntries = new ArrayList<>();
		for (String entry : entries) {
			try {
				URL referenced = new URL(jarUrl, entry);
				if (new File(referenced.getFile()).exists()) {
					urls.add(referenced);
				}
				else {
					referenced = new URL(jarUrl, URLDecoder.decode(entry, StandardCharsets.UTF_8));
					if (new File(referenced.getFile()).exists()) {
						urls.add(referenced);
					}
					else {
						nonExistentEntries.add(referenced);
					}
				}
			}
			catch (MalformedURLException ex) {
				throw new IllegalStateException("Class-Path attribute contains malformed URL", ex);
			}
		}
		if (!nonExistentEntries.isEmpty()) {
			logger.info(LogMessage.of(() -> "The Class-Path manifest attribute in " + jarFile.getName()
					+ " referenced one or more files that do not exist: "
					+ StringUtils.collectionToCommaDelimitedString(nonExistentEntries)));
		}
		return urls;
	}

	/**
     * Converts a collection of URLs to an instance of ChangeableUrls.
     * 
     * @param urls the collection of URLs to be converted
     * @return an instance of ChangeableUrls containing the converted URLs
     */
    static ChangeableUrls fromUrls(Collection<URL> urls) {
		return fromUrls(new ArrayList<>(urls).toArray(new URL[urls.size()]));
	}

	/**
     * Creates a new instance of ChangeableUrls with the given URLs.
     * 
     * @param urls the URLs to be used for creating the ChangeableUrls instance
     * @return a new instance of ChangeableUrls
     */
    static ChangeableUrls fromUrls(URL... urls) {
		return new ChangeableUrls(urls);
	}

}
