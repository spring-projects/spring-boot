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

package org.springframework.boot.web.servlet.server;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Logic to extract URLs of static resource jars (those containing
 * {@code "META-INF/resources"} directories).
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class StaticResourceJars {

	/**
	 * Retrieves a list of URLs representing the resource jars.
	 * @return a list of URLs representing the resource jars
	 */
	List<URL> getUrls() {
		ClassLoader classLoader = getClass().getClassLoader();
		if (classLoader instanceof URLClassLoader urlClassLoader) {
			return getUrlsFrom(urlClassLoader.getURLs());
		}
		else {
			return getUrlsFrom(Stream.of(ManagementFactory.getRuntimeMXBean().getClassPath().split(File.pathSeparator))
				.map(this::toUrl)
				.toArray(URL[]::new));
		}
	}

	/**
	 * Retrieves a list of URLs from the given array of URLs.
	 * @param urls the array of URLs to retrieve URLs from
	 * @return a list of URLs extracted from the given array of URLs
	 */
	List<URL> getUrlsFrom(URL... urls) {
		List<URL> resourceJarUrls = new ArrayList<>();
		for (URL url : urls) {
			addUrl(resourceJarUrls, url);
		}
		return resourceJarUrls;
	}

	/**
	 * Converts a class path entry to a URL.
	 * @param classPathEntry the class path entry to convert
	 * @return the URL representation of the class path entry
	 * @throws IllegalArgumentException if the URL could not be created from the class
	 * path entry
	 */
	private URL toUrl(String classPathEntry) {
		try {
			return new File(classPathEntry).toURI().toURL();
		}
		catch (MalformedURLException ex) {
			throw new IllegalArgumentException("URL could not be created from '" + classPathEntry + "'", ex);
		}
	}

	/**
	 * Converts a URL to a File object.
	 * @param url the URL to convert
	 * @return the File object representing the URL
	 * @throws IllegalStateException if failed to create File from the URL
	 */
	private File toFile(URL url) {
		try {
			return new File(url.toURI());
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException("Failed to create File from URL '" + url + "'");
		}
		catch (IllegalArgumentException ex) {
			return null;
		}
	}

	/**
	 * Adds a URL to the given list of URLs.
	 * @param urls the list of URLs to add the URL to
	 * @param url the URL to be added
	 * @throws IllegalStateException if an IOException occurs
	 */
	private void addUrl(List<URL> urls, URL url) {
		try {
			if (!"file".equals(url.getProtocol())) {
				addUrlConnection(urls, url, url.openConnection());
			}
			else {
				File file = toFile(url);
				if (file != null) {
					addUrlFile(urls, url, file);
				}
				else {
					addUrlConnection(urls, url, url.openConnection());
				}
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * Adds the given URL to the list of URLs if the file is a directory with a
	 * "META-INF/resources" subdirectory or if the file is a resources JAR file.
	 * @param urls the list of URLs to add the URL to
	 * @param url the URL to add to the list
	 * @param file the file to check if it is a directory or a resources JAR file
	 */
	private void addUrlFile(List<URL> urls, URL url, File file) {
		if ((file.isDirectory() && new File(file, "META-INF/resources").isDirectory()) || isResourcesJar(file)) {
			urls.add(url);
		}
	}

	/**
	 * Adds the given URL connection to the list of URLs if it is a resources JAR
	 * connection.
	 * @param urls the list of URLs to add the connection to
	 * @param url the URL associated with the connection
	 * @param connection the URL connection to be added
	 */
	private void addUrlConnection(List<URL> urls, URL url, URLConnection connection) {
		if (connection instanceof JarURLConnection jarURLConnection && isResourcesJar(jarURLConnection)) {
			urls.add(url);
		}
	}

	/**
	 * Checks if the given JarURLConnection represents a resources JAR.
	 * @param connection the JarURLConnection to check
	 * @return true if the connection represents a resources JAR, false otherwise
	 */
	private boolean isResourcesJar(JarURLConnection connection) {
		try {
			return isResourcesJar(connection.getJarFile(), !connection.getUseCaches());
		}
		catch (IOException ex) {
			return false;
		}
	}

	/**
	 * Checks if the given file is a resources JAR file.
	 * @param file the file to be checked
	 * @return true if the file is a resources JAR file, false otherwise
	 */
	private boolean isResourcesJar(File file) {
		try {
			return isResourcesJar(new JarFile(file), true);
		}
		catch (IOException | InvalidPathException ex) {
			return false;
		}
	}

	/**
	 * Checks if the given JarFile is a resources jar.
	 * @param jarFile the JarFile to check
	 * @param closeJarFile true if the JarFile should be closed after checking, false
	 * otherwise
	 * @return true if the JarFile is a resources jar, false otherwise
	 * @throws IOException if an I/O error occurs while accessing the JarFile
	 */
	private boolean isResourcesJar(JarFile jarFile, boolean closeJarFile) throws IOException {
		try {
			return jarFile.getName().endsWith(".jar") && (jarFile.getJarEntry("META-INF/resources") != null);
		}
		finally {
			if (closeJarFile) {
				jarFile.close();
			}
		}
	}

}
