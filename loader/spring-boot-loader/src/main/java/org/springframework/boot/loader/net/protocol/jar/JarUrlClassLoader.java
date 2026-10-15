/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.net.protocol.jar;

import java.io.EOFException;
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

	private final boolean hasOnlyNestedJarUrls;

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
		this.hasOnlyNestedJarUrls = urls.length > 0 && Arrays.stream(urls).allMatch(this::isNestedJarUrl);
	}

	@Override
	public URL findResource(String name) {
		if (this.hasOnlyNestedJarUrls && isSimpleEntryName(name)) {
			for (URL url : this.urls) {
				if (hasNestedJarEntry(url, name)) {
					return createResourceUrl(url, name);
				}
			}
			return null;
		}
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
		if (this.hasOnlyNestedJarUrls && isSimpleEntryName(name)) {
			List<URL> resources = new ArrayList<>();
			for (URL url : this.urls) {
				if (hasNestedJarEntry(url, name)) {
					URL resource = createResourceUrl(url, name);
					if (resource != null) {
						resources.add(resource);
					}
				}
			}
			return Collections.enumeration(resources);
		}
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
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		if (!this.hasOnlyNestedJarUrls) {
			return super.findClass(name);
		}
		String entryName = name.replace('.', '/').concat(".class");
		for (URL url : this.urls) {
			NestedJarFile jarFile = getNestedJarFile(url);
			JarEntry entry = (jarFile != null) ? jarFile.getJarEntry(entryName) : null;
			if (entry != null) {
				return defineClass(name, jarFile, entry, url);
			}
		}
		throw new ClassNotFoundException(name);
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if (!this.hasJarUrls || this.hasOnlyNestedJarUrls) {
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
		String classEntryName = className.replace('.', '/') + ".class";
		for (URL url : this.urls) {
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

	private NestedJarFile getNestedJarFile(URL url) {
		try {
			return (getJarFile(url) instanceof NestedJarFile nestedJarFile) ? nestedJarFile : null;
		}
		catch (IOException ex) {
			return null;
		}
	}

	private boolean hasNestedJarEntry(URL url, String name) {
		NestedJarFile jarFile = getNestedJarFile(url);
		return jarFile != null && jarFile.hasEntry(name);
	}

	private URL createResourceUrl(URL url, String name) {
		try {
			return new URL(url, name);
		}
		catch (MalformedURLException ex) {
			return null;
		}
	}

	private Class<?> defineClass(String name, NestedJarFile jarFile, JarEntry entry, URL url)
			throws ClassNotFoundException {
		try {
			definePackageForClass(name, jarFile, url);
			byte[] bytes = readEntry(jarFile, entry);
			return defineClass(name, bytes, 0, bytes.length, new CodeSource(url, (CodeSigner[]) null));
		}
		catch (IOException ex) {
			throw new ClassNotFoundException(name, ex);
		}
	}

	private void definePackageForClass(String className, NestedJarFile jarFile, URL url) {
		int lastDot = className.lastIndexOf('.');
		if (lastDot == -1) {
			return;
		}
		String packageName = className.substring(0, lastDot);
		Package definedPackage = getDefinedPackage(packageName);
		if (definedPackage != null) {
			if (definedPackage.isSealed() && !definedPackage.isSealed(url)) {
				throw new SecurityException("sealing violation: package " + packageName + " is sealed");
			}
			return;
		}
		Manifest manifest = getManifest(jarFile);
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

	private Manifest getManifest(NestedJarFile jarFile) {
		try {
			return jarFile.getManifest();
		}
		catch (IOException ex) {
			return null;
		}
	}

	private byte[] readEntry(NestedJarFile jarFile, JarEntry entry) throws IOException {
		try (InputStream inputStream = jarFile.getInputStream(entry)) {
			long size = entry.getSize();
			if (size < 0 || size > Integer.MAX_VALUE) {
				return inputStream.readAllBytes();
			}
			byte[] bytes = new byte[(int) size];
			if (inputStream.readNBytes(bytes, 0, bytes.length) != bytes.length) {
				throw new EOFException("Unexpected end of entry " + entry.getName());
			}
			return bytes;
		}
	}

	private static boolean isSimpleEntryName(String name) {
		if (name.isEmpty() || name.charAt(0) == '/' || name.endsWith(".") || name.contains("./")) {
			return false;
		}
		for (int i = 0; i < name.length(); i++) {
			char ch = name.charAt(i);
			boolean simple = (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9')
					|| ch == '/' || ch == '.' || ch == '-' || ch == '_' || ch == '$';
			if (!simple) {
				return false;
			}
		}
		return true;
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

	private boolean isNestedJarUrl(URL url) {
		String file = url.getFile();
		return isJarUrl(url) && file.startsWith("nested:") && file.endsWith("!/");
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
