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

package org.springframework.boot.loader.net.protocol.jar;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;

import org.springframework.boot.loader.jar.NestedJarFile;
import org.springframework.boot.loader.launch.LaunchedClassLoader;

/**
 * {@link URLClassLoader} with optimized support for Jar URLs.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 3.2.0
 */
public abstract class JarUrlClassLoader extends URLClassLoader {

	private final URL[] urls;

	private final boolean hasJarUrls;

	private final Map<URL, JarFile> jarFiles = new ConcurrentHashMap<>();

	private final Set<String> undefinablePackages = Collections.newSetFromMap(new ConcurrentHashMap<>());

	/**
	 * Create a new {@link LaunchedClassLoader} instance.
	 * @param urls the URLs from which to load classes and resources
	 * @param parent the parent class loader for delegation
	 */
	public JarUrlClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
		this.urls = urls;
		this.hasJarUrls = Arrays.stream(urls).anyMatch(this::isJarUrl);
	}

	/**
	 * Finds the resource with the specified name.
	 * @param name the name of the resource
	 * @return the URL object representing the resource, or null if the resource is not
	 * found
	 */
	@Override
	public URL findResource(String name) {
		if (!this.hasJarUrls) {
			return super.findResource(name);
		}
		Optimizations.enable(false);
		try {
			return super.findResource(name);
		}
		finally {
			Optimizations.disable();
		}
	}

	/**
	 * Overrides the findResources method to provide optimized resource searching in case
	 * the class loader has JAR URLs. If the class loader does not have JAR URLs, the
	 * method falls back to the default implementation.
	 * @param name The name of the resource to be found.
	 * @return An enumeration of URLs representing the resources found.
	 * @throws IOException If an I/O error occurs while finding the resources.
	 */
	@Override
	public Enumeration<URL> findResources(String name) throws IOException {
		if (!this.hasJarUrls) {
			return super.findResources(name);
		}
		Optimizations.enable(false);
		try {
			return new OptimizedEnumeration(super.findResources(name));
		}
		finally {
			Optimizations.disable();
		}
	}

	/**
	 * Loads the specified class with the given name.
	 * @param name the name of the class to be loaded
	 * @param resolve indicates whether or not to resolve the class
	 * @return the loaded class
	 * @throws ClassNotFoundException if the class with the specified name could not be
	 * found
	 */
	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if (!this.hasJarUrls) {
			return super.loadClass(name, resolve);
		}
		Optimizations.enable(true);
		try {
			try {
				definePackageIfNecessary(name);
			}
			catch (IllegalArgumentException ex) {
				tolerateRaceConditionDueToBeingParallelCapable(ex, name);
			}
			return super.loadClass(name, resolve);
		}
		finally {
			Optimizations.disable();
		}
	}

	/**
	 * Define a package before a {@code findClass} call is made. This is necessary to
	 * ensure that the appropriate manifest for nested JARs is associated with the
	 * package.
	 * @param className the class name being found
	 */
	protected final void definePackageIfNecessary(String className) {
		if (className.startsWith("java.")) {
			return;
		}
		int lastDot = className.lastIndexOf('.');
		if (lastDot >= 0) {
			String packageName = className.substring(0, lastDot);
			if (getDefinedPackage(packageName) == null) {
				try {
					definePackage(className, packageName);
				}
				catch (IllegalArgumentException ex) {
					tolerateRaceConditionDueToBeingParallelCapable(ex, packageName);
				}
			}
		}
	}

	/**
	 * Defines a package for a given class in the specified package name. If the package
	 * is in the list of undefinable packages, the method returns without defining the
	 * package. The method checks if the class and package entries exist in the jar file
	 * associated with the given URL. If they exist and the jar file has a manifest, the
	 * package is defined using the manifest and the URL. If the package cannot be
	 * defined, it is added to the list of undefinable packages.
	 * @param className the name of the class
	 * @param packageName the name of the package
	 */
	private void definePackage(String className, String packageName) {
		if (this.undefinablePackages.contains(packageName)) {
			return;
		}
		String packageEntryName = packageName.replace('.', '/') + "/";
		String classEntryName = className.replace('.', '/') + ".class";
		for (URL url : this.urls) {
			try {
				JarFile jarFile = getJarFile(url);
				if (jarFile != null) {
					if (hasEntry(jarFile, classEntryName) && hasEntry(jarFile, packageEntryName)
							&& jarFile.getManifest() != null) {
						definePackage(packageName, jarFile.getManifest(), url);
						return;
					}
				}
			}
			catch (IOException ex) {
				// Ignore
			}
		}
		this.undefinablePackages.add(packageName);
	}

	/**
	 * Tolerates a race condition due to being parallel capable.
	 * @param ex the IllegalArgumentException indicating that the package has already been
	 * defined
	 * @param packageName the name of the package
	 * @throws AssertionError if the package has already been defined but could not be
	 * found
	 */
	private void tolerateRaceConditionDueToBeingParallelCapable(IllegalArgumentException ex, String packageName)
			throws AssertionError {
		if (getDefinedPackage(packageName) == null) {
			// This should never happen as the IllegalArgumentException indicates that the
			// package has already been defined and, therefore, getDefinedPackage(name)
			// should not have returned null.
			throw new AssertionError(
					"Package %s has already been defined but it could not be found".formatted(packageName), ex);
		}
	}

	/**
	 * Checks if the given entry exists in the specified JarFile.
	 * @param jarFile the JarFile to check
	 * @param name the name of the entry to check for
	 * @return true if the entry exists, false otherwise
	 */
	private boolean hasEntry(JarFile jarFile, String name) {
		return (jarFile instanceof NestedJarFile nestedJarFile) ? nestedJarFile.hasEntry(name)
				: jarFile.getEntry(name) != null;
	}

	/**
	 * Retrieves the JarFile associated with the given URL.
	 * @param url The URL of the JarFile to retrieve.
	 * @return The JarFile associated with the given URL, or null if the URL does not
	 * point to a JarFile.
	 * @throws IOException If an I/O error occurs while retrieving the JarFile.
	 */
	private JarFile getJarFile(URL url) throws IOException {
		JarFile jarFile = this.jarFiles.get(url);
		if (jarFile != null) {
			return jarFile;
		}
		URLConnection connection = url.openConnection();
		if (!(connection instanceof JarURLConnection)) {
			return null;
		}
		connection.setUseCaches(false);
		jarFile = ((JarURLConnection) connection).getJarFile();
		synchronized (this.jarFiles) {
			JarFile previous = this.jarFiles.putIfAbsent(url, jarFile);
			if (previous != null) {
				jarFile.close();
				jarFile = previous;
			}
		}
		return jarFile;
	}

	/**
	 * Clear any caches. This method is called reflectively by
	 * {@code ClearCachesApplicationListener}.
	 */
	public void clearCache() {
		Handler.clearCache();
		org.springframework.boot.loader.net.protocol.nested.Handler.clearCache();
		try {
			clearJarFiles();
		}
		catch (IOException ex) {
			// Ignore
		}
		for (URL url : this.urls) {
			if (isJarUrl(url)) {
				clearCache(url);
			}
		}
	}

	/**
	 * Clears the cache for the specified URL.
	 * @param url the URL for which the cache needs to be cleared
	 */
	private void clearCache(URL url) {
		try {
			URLConnection connection = url.openConnection();
			if (connection instanceof JarURLConnection jarUrlConnection) {
				clearCache(jarUrlConnection);
			}
		}
		catch (IOException ex) {
			// Ignore
		}
	}

	/**
	 * Clears the cache for the given JarURLConnection.
	 * @param connection the JarURLConnection to clear the cache for
	 * @throws IOException if an I/O error occurs while clearing the cache
	 */
	private void clearCache(JarURLConnection connection) throws IOException {
		JarFile jarFile = connection.getJarFile();
		if (jarFile instanceof NestedJarFile nestedJarFile) {
			nestedJarFile.clearCache();
		}
	}

	/**
	 * Checks if the given URL is a JAR URL.
	 * @param url the URL to check
	 * @return true if the URL is a JAR URL, false otherwise
	 */
	private boolean isJarUrl(URL url) {
		return "jar".equals(url.getProtocol());
	}

	/**
	 * Closes the JarUrlClassLoader and releases any resources associated with it. This
	 * method overrides the close() method in the superclass and also clears any loaded
	 * jar files.
	 * @throws IOException if an I/O error occurs while closing the JarUrlClassLoader
	 */
	@Override
	public void close() throws IOException {
		super.close();
		clearJarFiles();
	}

	/**
	 * Clears all the Jar files in the JarUrlClassLoader.
	 * @throws IOException if an I/O error occurs while closing the Jar files
	 */
	private void clearJarFiles() throws IOException {
		synchronized (this.jarFiles) {
			for (JarFile jarFile : this.jarFiles.values()) {
				jarFile.close();
			}
			this.jarFiles.clear();
		}
	}

	/**
	 * {@link Enumeration} that uses fast connections.
	 */
	private static class OptimizedEnumeration implements Enumeration<URL> {

		private final Enumeration<URL> delegate;

		/**
		 * Constructs a new OptimizedEnumeration object with the specified delegate.
		 * @param delegate the Enumeration object to be delegated to
		 */
		OptimizedEnumeration(Enumeration<URL> delegate) {
			this.delegate = delegate;
		}

		/**
		 * Returns true if this enumeration contains more elements.
		 * @return true if this enumeration contains more elements, false otherwise
		 */
		@Override
		public boolean hasMoreElements() {
			Optimizations.enable(false);
			try {
				return this.delegate.hasMoreElements();
			}
			finally {
				Optimizations.disable();
			}

		}

		/**
		 * Returns the next element in the enumeration.
		 * @return the next element in the enumeration
		 */
		@Override
		public URL nextElement() {
			Optimizations.enable(false);
			try {
				return this.delegate.nextElement();
			}
			finally {
				Optimizations.disable();
			}
		}

	}

}
