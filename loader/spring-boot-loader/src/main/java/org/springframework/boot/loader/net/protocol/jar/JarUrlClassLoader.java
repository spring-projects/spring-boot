/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.net.protocol.jar;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.springframework.boot.loader.jar.NestedJarFile;
import org.springframework.boot.loader.launch.LaunchedClassLoader;
import org.springframework.boot.loader.net.util.UrlEncoder;

/**
 * {@link URLClassLoader} with optimized support for Jar URLs.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Jan Mohr
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

	private final NestedOnlyOptimizedLoader nestedOnlyOptimizedLoader;

	/**
	 * Create a new {@link LaunchedClassLoader} instance.
	 * @param urls the URLs from which to load classes and resources
	 * @param parent the parent class loader for delegation
	 */
	public JarUrlClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
		this.urls = urls;
		this.hasJarUrls = (urls.length != 0) && Arrays.stream(urls).anyMatch(this::isJarUrl);
		this.nestedOnlyOptimizedLoader = (this.hasJarUrls && Arrays.stream(urls).allMatch(this::isNestedJarUrl))
				? new NestedOnlyOptimizedLoader() : null;
	}

	@Override
	public URL findResource(String name) {
		if (this.nestedOnlyOptimizedLoader != null) {
			return this.nestedOnlyOptimizedLoader.findResource(name);
		}
		if (this.hasJarUrls) {
			Optimizations.enable(false);
			try {
				return super.findResource(name);
			}
			finally {
				Optimizations.disable();
			}
		}
		return super.findResource(name);
	}

	@Override
	public Enumeration<URL> findResources(String name) throws IOException {
		if (this.nestedOnlyOptimizedLoader != null) {
			return this.nestedOnlyOptimizedLoader.findResources(name);
		}
		if (this.hasJarUrls) {
			Optimizations.enable(false);
			try {
				return new OptimizedEnumeration(super.findResources(name));
			}
			finally {
				Optimizations.disable();
			}
		}
		return super.findResources(name);
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		if (!LoadableNames.isValidClassName(name)) {
			throw new ClassNotFoundException(name);
		}
		if (this.nestedOnlyOptimizedLoader != null) {
			return this.nestedOnlyOptimizedLoader.findClass(name);
		}
		return super.findClass(name);
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if (!LoadableNames.isValidClassName(name)) {
			throw new ClassNotFoundException(name);
		}
		if (this.nestedOnlyOptimizedLoader != null) {
			// No need to define package here since super.loadClass will end up in the
			// optimized findClass method.
			return super.loadClass(name, resolve);
		}
		if (this.hasJarUrls) {
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
		return super.loadClass(name, resolve);
	}

	final Class<?> defineClass(String name, byte[] bytes, URL url) {
		return defineClass(name, bytes, 0, bytes.length, new CodeSource(url, (CodeSigner[]) null));
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
		String classEntryName = className.replace('.', '/') + ".class";
		for (URL url : urls()) {
			try {
				JarFile jarFile = getJarFile(url);
				if (jarFile != null) {
					if (hasEntry(jarFile, classEntryName) && jarFile.getManifest() != null) {
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

	final void definePackageWithOptionalManifest(String packageName, Manifest manifest, URL url) throws AssertionError {
		try {
			if (manifest != null) {
				definePackage(packageName, manifest, url);
			}
			else {
				definePackage(packageName, null, null, null, null, null, null, null);
			}
		}
		catch (IllegalArgumentException ex) {
			tolerateRaceConditionDueToBeingParallelCapable(ex, packageName);
		}
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
		for (URL url : urls()) {
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

	private boolean isNestedJarUrl(URL url) {
		return isJarUrl(url) && isNestedJarUrlFilePart(url.getFile());
	}

	private boolean isNestedJarUrlFilePart(String file) {
		return file.startsWith("nested:") && file.endsWith("!/");
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

	final URL[] urls() {
		return this.urls;
	}

	/**
	 * Optimzations that can be used when the classloader contains only
	 * {@code 'jar:nested'} URLs.
	 */
	private final class NestedOnlyOptimizedLoader {

		Class<?> findClass(String name) throws ClassNotFoundException {
			String entryName = name.replace('.', '/').concat(".class");
			for (URL url : urls()) {
				NestedJarFile jarFile = getNestedJarFile(url);
				JarEntry entry = (jarFile != null) ? jarFile.getJarEntry(entryName) : null;
				if (entry != null) {
					return defineClass(jarFile, entry, url, name);
				}
			}
			throw new ClassNotFoundException(name);
		}

		URL findResource(String name) {
			for (URL url : urls()) {
				NestedJarFile jarFile = getNestedJarFile(url);
				URL resource = createResource(jarFile, url, name);
				if (resource != null) {
					return resource;
				}
			}
			return null;
		}

		Enumeration<URL> findResources(String name) {
			List<URL> resources = new ArrayList<>();
			for (URL url : urls()) {
				NestedJarFile jarFile = getNestedJarFile(url);
				URL resource = createResource(jarFile, url, name);
				if (resource != null) {
					resources.add(resource);
				}
			}
			return Collections.enumeration(resources);
		}

		private URL createResource(NestedJarFile jarFile, URL url, String name) {
			try {
				if (jarFile != null && jarFile.hasEntry(name)) {
					return new URL(url, UrlEncoder.encode(name));
				}
			}
			catch (MalformedURLException ex) {
			}
			return null;
		}

		private NestedJarFile getNestedJarFile(URL url) {
			try {
				JarFile jarFile = getJarFile(url);
				if (jarFile instanceof NestedJarFile nestedJarFile) {
					return nestedJarFile;
				}
			}
			catch (IOException ex) {
			}
			return null;
		}

		private Class<?> defineClass(NestedJarFile jarFile, JarEntry entry, URL url, String name)
				throws ClassNotFoundException {
			try {
				definePackageIfNecessary(jarFile, url, name);
				try (InputStream inputStream = jarFile.getInputStream(entry)) {
					return JarUrlClassLoader.this.defineClass(name, inputStream.readAllBytes(), url);
				}
			}
			catch (IOException ex) {
				throw new ClassNotFoundException(name, ex);
			}
		}

		private void definePackageIfNecessary(NestedJarFile jarFile, URL url, String className) {
			int lastDot = className.lastIndexOf('.');
			if (lastDot >= 0) {
				String packageName = className.substring(0, lastDot);
				definePackage(jarFile, url, packageName);
			}
		}

		private void definePackage(NestedJarFile jarFile, URL url, String packageName) throws AssertionError {
			Package definedPackage = getDefinedPackage(packageName);
			if (definedPackage != null) {
				if (definedPackage.isSealed() && !definedPackage.isSealed(url)) {
					throw new SecurityException("sealing violation: package " + packageName + " is sealed");
				}
				return;
			}
			definePackageWithOptionalManifest(packageName, getManifest(jarFile), url);
		}

		private Manifest getManifest(NestedJarFile jarFile) {
			try {
				return jarFile.getManifest();
			}
			catch (IOException ex) {
				return null;
			}
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
