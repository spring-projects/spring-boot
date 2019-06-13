/*
 * Copyright 2012-2019 the original author or authors.
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

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;
import java.util.jar.JarFile;

import org.springframework.boot.loader.jar.Handler;

/**
 * {@link ClassLoader} used by the {@link Launcher}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 */
public class LaunchedURLClassLoader extends URLClassLoader {

	static {
		ClassLoader.registerAsParallelCapable();
	}

	/**
	 * Create a new {@link LaunchedURLClassLoader} instance.
	 * @param urls the URLs from which to load classes and resources
	 * @param parent the parent class loader for delegation
	 */
	public LaunchedURLClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
	}

	@Override
	public URL findResource(String name) {
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
		Handler.setUseFastConnectionExceptions(true);
		try {
			try {
				definePackageIfNecessary(name);
			}
			catch (IllegalArgumentException ex) {
				// Tolerate race condition due to being parallel capable
				if (getPackage(name) == null) {
					// This should never happen as the IllegalArgumentException indicates
					// that the package has already been defined and, therefore,
					// getPackage(name) should not return null.
					throw new AssertionError(
							"Package " + name + " has already been " + "defined but it could not be found");
				}
			}
			return super.loadClass(name, resolve);
		}
		finally {
			Handler.setUseFastConnectionExceptions(false);
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
			if (getPackage(packageName) == null) {
				try {
					definePackage(className, packageName);
				}
				catch (IllegalArgumentException ex) {
					// Tolerate race condition due to being parallel capable
					if (getPackage(packageName) == null) {
						// This should never happen as the IllegalArgumentException
						// indicates that the package has already been defined and,
						// therefore, getPackage(name) should not have returned null.
						throw new AssertionError(
								"Package " + packageName + " has already been defined " + "but it could not be found");
					}
				}
			}
		}
	}

	private void definePackage(String className, String packageName) {
		try {
			AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
				String packageEntryName = packageName.replace('.', '/') + "/";
				String classEntryName = className.replace('.', '/') + ".class";
				for (URL url : getURLs()) {
					try {
						URLConnection connection = url.openConnection();
						if (connection instanceof JarURLConnection) {
							JarFile jarFile = ((JarURLConnection) connection).getJarFile();
							if (jarFile.getEntry(classEntryName) != null && jarFile.getEntry(packageEntryName) != null
									&& jarFile.getManifest() != null) {
								definePackage(packageName, jarFile.getManifest(), url);
								return null;
							}
						}
					}
					catch (IOException ex) {
						// Ignore
					}
				}
				return null;
			}, AccessController.getContext());
		}
		catch (java.security.PrivilegedActionException ex) {
			// Ignore
		}
	}

	/**
	 * Clear URL caches.
	 */
	public void clearCache() {
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

}
