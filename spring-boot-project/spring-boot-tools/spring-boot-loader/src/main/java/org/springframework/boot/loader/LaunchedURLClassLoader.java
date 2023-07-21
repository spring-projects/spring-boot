/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.loader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.jar.Handler;

/**
 * {@link ClassLoader} used by the {@link Launcher}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @since 1.0.0
 */
public class LaunchedURLClassLoader extends URLClassLoader {

	private static final int BUFFER_SIZE = 4096;

	static {
		ClassLoader.registerAsParallelCapable();
	}

	private final boolean exploded;

	private final Archive rootArchive;

	private final Object packageLock = new Object();

	private volatile DefinePackageCallType definePackageCallType;

	/**
	 * Create a new {@link LaunchedURLClassLoader} instance.
	 * @param urls the URLs from which to load classes and resources
	 * @param parent the parent class loader for delegation
	 */
	public LaunchedURLClassLoader(URL[] urls, ClassLoader parent) {
		this(false, urls, parent);
	}

	/**
	 * Create a new {@link LaunchedURLClassLoader} instance.
	 * @param exploded if the underlying archive is exploded
	 * @param urls the URLs from which to load classes and resources
	 * @param parent the parent class loader for delegation
	 */
	public LaunchedURLClassLoader(boolean exploded, URL[] urls, ClassLoader parent) {
		this(exploded, null, urls, parent);
	}

	/**
	 * Create a new {@link LaunchedURLClassLoader} instance.
	 * @param exploded if the underlying archive is exploded
	 * @param rootArchive the root archive or {@code null}
	 * @param urls the URLs from which to load classes and resources
	 * @param parent the parent class loader for delegation
	 * @since 2.3.1
	 */
	public LaunchedURLClassLoader(boolean exploded, Archive rootArchive, URL[] urls, ClassLoader parent) {
		super(urls, parent);
		this.exploded = exploded;
		this.rootArchive = rootArchive;
	}

	@Override
	public URL findResource(String name) {
		if (this.exploded) {
			return super.findResource(name);
		}
		Handler.setUseFastConnectionExceptions(true);
		try {
			return super.findResource(name);
		}
		finally {
			Handler.setUseFastConnectionExceptions(false);
		}
	}

	@Override
	public Enumeration<URL> findResources(String name) throws IOException {
		if (this.exploded) {
			return super.findResources(name);
		}
		Handler.setUseFastConnectionExceptions(true);
		try {
			return new UseFastConnectionExceptionsEnumeration(super.findResources(name));
		}
		finally {
			Handler.setUseFastConnectionExceptions(false);
		}
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if (name.startsWith("org.springframework.boot.loader.jarmode.")) {
			try {
				Class<?> result = loadClassInLaunchedClassLoader(name);
				if (resolve) {
					resolveClass(result);
				}
				return result;
			}
			catch (ClassNotFoundException ex) {
			}
		}
		if (this.exploded) {
			return super.loadClass(name, resolve);
		}
		Handler.setUseFastConnectionExceptions(true);
		try {
			try {
				definePackageIfNecessary(name);
			}
			catch (IllegalArgumentException ex) {
				// Tolerate race condition due to being parallel capable
				if (getDefinedPackage(name) == null) {
					// This should never happen as the IllegalArgumentException indicates
					// that the package has already been defined and, therefore,
					// getDefinedPackage(name) should not return null.
					throw new AssertionError("Package " + name + " has already been defined but it could not be found");
				}
			}
			return super.loadClass(name, resolve);
		}
		finally {
			Handler.setUseFastConnectionExceptions(false);
		}
	}

	private Class<?> loadClassInLaunchedClassLoader(String name) throws ClassNotFoundException {
		String internalName = name.replace('.', '/') + ".class";
		InputStream inputStream = getParent().getResourceAsStream(internalName);
		if (inputStream == null) {
			throw new ClassNotFoundException(name);
		}
		try {
			try {
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				byte[] buffer = new byte[BUFFER_SIZE];
				int bytesRead = -1;
				while ((bytesRead = inputStream.read(buffer)) != -1) {
					outputStream.write(buffer, 0, bytesRead);
				}
				inputStream.close();
				byte[] bytes = outputStream.toByteArray();
				Class<?> definedClass = defineClass(name, bytes, 0, bytes.length);
				definePackageIfNecessary(name);
				return definedClass;
			}
			finally {
				inputStream.close();
			}
		}
		catch (IOException ex) {
			throw new ClassNotFoundException("Cannot load resource for class [" + name + "]", ex);
		}
	}

	/**
	 * Define a package before a {@code findClass} call is made. This is necessary to
	 * ensure that the appropriate manifest for nested JARs is associated with the
	 * package.
	 * @param className the class name being found
	 */
	private void definePackageIfNecessary(String className) {
		int lastDot = className.lastIndexOf('.');
		if (lastDot >= 0) {
			String packageName = className.substring(0, lastDot);
			if (getDefinedPackage(packageName) == null) {
				try {
					definePackage(className, packageName);
				}
				catch (IllegalArgumentException ex) {
					// Tolerate race condition due to being parallel capable
					if (getDefinedPackage(packageName) == null) {
						// This should never happen as the IllegalArgumentException
						// indicates that the package has already been defined and,
						// therefore, getDefinedPackage(name) should not have returned
						// null.
						throw new AssertionError(
								"Package " + packageName + " has already been defined but it could not be found");
					}
				}
			}
		}
	}

	private void definePackage(String className, String packageName) {
		String packageEntryName = packageName.replace('.', '/') + "/";
		String classEntryName = className.replace('.', '/') + ".class";
		for (URL url : getURLs()) {
			try {
				URLConnection connection = url.openConnection();
				if (connection instanceof JarURLConnection jarURLConnection) {
					JarFile jarFile = jarURLConnection.getJarFile();
					if (jarFile.getEntry(classEntryName) != null && jarFile.getEntry(packageEntryName) != null
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
	}

	@Override
	protected Package definePackage(String name, Manifest man, URL url) throws IllegalArgumentException {
		if (!this.exploded) {
			return super.definePackage(name, man, url);
		}
		synchronized (this.packageLock) {
			return doDefinePackage(DefinePackageCallType.MANIFEST, () -> super.definePackage(name, man, url));
		}
	}

	@Override
	protected Package definePackage(String name, String specTitle, String specVersion, String specVendor,
			String implTitle, String implVersion, String implVendor, URL sealBase) throws IllegalArgumentException {
		if (!this.exploded) {
			return super.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor,
					sealBase);
		}
		synchronized (this.packageLock) {
			if (this.definePackageCallType == null) {
				// We're not part of a call chain which means that the URLClassLoader
				// is trying to define a package for our exploded JAR. We use the
				// manifest version to ensure package attributes are set
				Manifest manifest = getManifest(this.rootArchive);
				if (manifest != null) {
					return definePackage(name, manifest, sealBase);
				}
			}
			return doDefinePackage(DefinePackageCallType.ATTRIBUTES, () -> super.definePackage(name, specTitle,
					specVersion, specVendor, implTitle, implVersion, implVendor, sealBase));
		}
	}

	private Manifest getManifest(Archive archive) {
		try {
			return (archive != null) ? archive.getManifest() : null;
		}
		catch (IOException ex) {
			return null;
		}
	}

	private <T> T doDefinePackage(DefinePackageCallType type, Supplier<T> call) {
		DefinePackageCallType existingType = this.definePackageCallType;
		try {
			this.definePackageCallType = type;
			return call.get();
		}
		finally {
			this.definePackageCallType = existingType;
		}
	}

	/**
	 * Clear URL caches.
	 */
	public void clearCache() {
		if (this.exploded) {
			return;
		}
		for (URL url : getURLs()) {
			try {
				URLConnection connection = url.openConnection();
				if (connection instanceof JarURLConnection) {
					clearCache(connection);
				}
			}
			catch (IOException ex) {
				// Ignore
			}
		}

	}

	private void clearCache(URLConnection connection) throws IOException {
		Object jarFile = ((JarURLConnection) connection).getJarFile();
		if (jarFile instanceof org.springframework.boot.loader.jar.JarFile) {
			((org.springframework.boot.loader.jar.JarFile) jarFile).clearCache();
		}
	}

	private static class UseFastConnectionExceptionsEnumeration implements Enumeration<URL> {

		private final Enumeration<URL> delegate;

		UseFastConnectionExceptionsEnumeration(Enumeration<URL> delegate) {
			this.delegate = delegate;
		}

		@Override
		public boolean hasMoreElements() {
			Handler.setUseFastConnectionExceptions(true);
			try {
				return this.delegate.hasMoreElements();
			}
			finally {
				Handler.setUseFastConnectionExceptions(false);
			}

		}

		@Override
		public URL nextElement() {
			Handler.setUseFastConnectionExceptions(true);
			try {
				return this.delegate.nextElement();
			}
			finally {
				Handler.setUseFastConnectionExceptions(false);
			}
		}

	}

	/**
	 * The different types of call made to define a package. We track these for exploded
	 * jars so that we can detect packages that should have manifest attributes applied.
	 */
	private enum DefinePackageCallType {

		/**
		 * A define package call from a resource that has a manifest.
		 */
		MANIFEST,

		/**
		 * A define package call with a direct set of attributes.
		 */
		ATTRIBUTES

	}

}
