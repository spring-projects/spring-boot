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

package org.springframework.boot.loader.net.protocol.jar;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.Arrays;
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

	static {
		ClassLoader.registerAsParallelCapable();
	}

	private final URL[] urls;

	private final boolean hasJarUrls;

	private final Map<URL, JarFile> jarFiles = new ConcurrentHashMap<>();

	private final Set<String> undefinablePackages = ConcurrentHashMap.newKeySet();

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

	private boolean hasEntry(JarFile jarFile, String name) {
		return (jarFile instanceof NestedJarFile nestedJarFile) ? nestedJarFile.hasEntry(name)
				: jarFile.getEntry(name) != null;
	}

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

	private void clearCache(JarURLConnection connection) throws IOException {
		JarFile jarFile = connection.getJarFile();
		if (jarFile instanceof NestedJarFile nestedJarFile) {
			nestedJarFile.clearCache();
		}
	}

	private boolean isJarUrl(URL url) {
		return "jar".equals(url.getProtocol());
	}

	@Override
	public void close() throws IOException {
		super.close();
		clearJarFiles();
	}

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

		OptimizedEnumeration(Enumeration<URL> delegate) {
			this.delegate = delegate;
		}

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
