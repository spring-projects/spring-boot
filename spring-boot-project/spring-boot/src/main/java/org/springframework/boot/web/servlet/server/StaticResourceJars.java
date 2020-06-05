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

	List<URL> getUrls() {
		ClassLoader classLoader = getClass().getClassLoader();
		if (classLoader instanceof URLClassLoader) {
			return getUrlsFrom(((URLClassLoader) classLoader).getURLs());
		}
		else {
			return getUrlsFrom(Stream.of(ManagementFactory.getRuntimeMXBean().getClassPath().split(File.pathSeparator))
					.map(this::toUrl).toArray(URL[]::new));
		}
	}

	List<URL> getUrlsFrom(URL... urls) {
		List<URL> resourceJarUrls = new ArrayList<>();
		for (URL url : urls) {
			addUrl(resourceJarUrls, url);
		}
		return resourceJarUrls;
	}

	private URL toUrl(String classPathEntry) {
		try {
			return new File(classPathEntry).toURI().toURL();
		}
		catch (MalformedURLException ex) {
			throw new IllegalArgumentException("URL could not be created from '" + classPathEntry + "'", ex);
		}
	}

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

	private void addUrlFile(List<URL> urls, URL url, File file) {
		if ((file.isDirectory() && new File(file, "META-INF/resources").isDirectory()) || isResourcesJar(file)) {
			urls.add(url);
		}
	}

	private void addUrlConnection(List<URL> urls, URL url, URLConnection connection) {
		if (connection instanceof JarURLConnection && isResourcesJar((JarURLConnection) connection)) {
			urls.add(url);
		}
	}

	private boolean isResourcesJar(JarURLConnection connection) {
		try {
			return isResourcesJar(connection.getJarFile());
		}
		catch (IOException ex) {
			return false;
		}
	}

	private boolean isResourcesJar(File file) {
		try {
			return isResourcesJar(new JarFile(file));
		}
		catch (IOException | InvalidPathException ex) {
			return false;
		}
	}

	private boolean isResourcesJar(JarFile jar) throws IOException {
		try {
			return jar.getName().endsWith(".jar") && (jar.getJarEntry("META-INF/resources") != null);
		}
		finally {
			jar.close();
		}
	}

}
