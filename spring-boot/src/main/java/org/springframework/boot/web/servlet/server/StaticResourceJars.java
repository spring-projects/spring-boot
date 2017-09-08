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

package org.springframework.boot.web.servlet.server;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

/**
 * Logic to extract URLs of static resource jars (those containing
 * {@code "META-INF/resources"} directories).
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class StaticResourceJars {

	public final List<URL> getUrls() {
		ClassLoader classLoader = getClass().getClassLoader();
		List<URL> urls = new ArrayList<>();
		if (classLoader instanceof URLClassLoader) {
			for (URL url : ((URLClassLoader) classLoader).getURLs()) {
				addUrl(urls, url);
			}
		}
		return urls;
	}

	private void addUrl(List<URL> urls, URL url) {
		try {
			if ("file".equals(url.getProtocol())) {
				addUrlFile(urls, url, new File(url.getFile()));
			}
			else {
				addUrlConnection(urls, url, url.openConnection());
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private void addUrlFile(List<URL> urls, URL url, File file) {
		if (file.isDirectory() && new File(file, "META-INF/resources").isDirectory()) {
			urls.add(url);
		}
		else if (isResourcesJar(file)) {
			urls.add(url);
		}
	}

	private void addUrlConnection(List<URL> urls, URL url, URLConnection connection) {
		if (connection instanceof JarURLConnection) {
			if (isResourcesJar((JarURLConnection) connection)) {
				urls.add(url);
			}
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
		catch (IOException ex) {
			return false;
		}
	}

	private boolean isResourcesJar(JarFile jar) throws IOException {
		try {
			return jar.getName().endsWith(".jar")
					&& (jar.getJarEntry("META-INF/resources") != null);
		}
		finally {
			jar.close();
		}
	}

}
