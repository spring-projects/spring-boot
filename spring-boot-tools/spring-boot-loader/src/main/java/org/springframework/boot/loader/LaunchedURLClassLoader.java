/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.loader;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;

import org.springframework.boot.loader.jar.JarFile;

/**
 * {@link ClassLoader} used by the {@link Launcher}.
 * 
 * @author Phillip Webb
 */
public class LaunchedURLClassLoader extends URLClassLoader {

	private final ClassLoader rootClassLoader;

	/**
	 * Create a new {@link LaunchedURLClassLoader} instance.
	 * @param urls the URLs from which to load classes and resources
	 * @param parent the parent class loader for delegation
	 */
	public LaunchedURLClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
		this.rootClassLoader = findRootClassLoader(parent);
	}

	private ClassLoader findRootClassLoader(ClassLoader classLoader) {
		while (classLoader != null) {
			if (classLoader.getParent() == null) {
				return classLoader;
			}
			classLoader = classLoader.getParent();
		}
		return null;
	}

	@Override
	public URL getResource(String name) {
		URL url = null;
		if (this.rootClassLoader != null) {
			url = this.rootClassLoader.getResource(name);
		}
		return (url == null ? findResource(name) : url);
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {

		if (this.rootClassLoader == null) {
			return findResources(name);
		}

		final Enumeration<URL> rootResources = this.rootClassLoader.getResources(name);
		final Enumeration<URL> localResources = findResources(name);

		return new Enumeration<URL>() {

			@Override
			public boolean hasMoreElements() {
				return rootResources.hasMoreElements()
						|| localResources.hasMoreElements();
			}

			@Override
			public URL nextElement() {
				if (rootResources.hasMoreElements()) {
					return rootResources.nextElement();
				}
				return localResources.nextElement();
			}
		};
	}

	/**
	 * Attempt to load classes from the URLs before delegating to the parent loader.
	 */
	@Override
	protected Class<?> loadClass(String name, boolean resolve)
			throws ClassNotFoundException {
		synchronized (this) {
			Class<?> loadedClass = findLoadedClass(name);
			if (loadedClass == null) {
				loadedClass = doLoadClass(name);
			}
			if (resolve) {
				resolveClass(loadedClass);
			}
			return loadedClass;
		}
	}

	private Class<?> doLoadClass(String name) throws ClassNotFoundException {

		// 1) Try the root class loader
		try {
			if (this.rootClassLoader != null) {
				return this.rootClassLoader.loadClass(name);
			}
		}
		catch (Exception ex) {
		}

		// 2) Try to find locally
		try {
			return findClass(name);
		}
		catch (Exception ex) {
		}

		// 3) Use standard loading
		return super.loadClass(name, false);
	}

	@Override
	protected Class<?> findClass(final String name) throws ClassNotFoundException {
		int lastDot = name.lastIndexOf('.');
		if (lastDot != -1) {
			String packageName = name.substring(0, lastDot);
			if (getPackage(packageName) == null) {
				try {
					definePackageForFindClass(name, packageName);
				}
				catch (Exception ex) {
					// Swallow and continue
				}
			}
		}
		return super.findClass(name);
	}

	/**
	 * Define a package before a {@code findClass} call is made. This is necessary to
	 * ensure that the appropriate manifest for nested JARs associated with the package.
	 * @param name the class name being found
	 * @param packageName the pacakge
	 */
	private void definePackageForFindClass(final String name, final String packageName) {
		try {
			AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
				@Override
				public Object run() throws ClassNotFoundException {
					String path = name.replace('.', '/').concat(".class");
					for (URL url : getURLs()) {
						try {
							if (url.getContent() instanceof JarFile) {
								JarFile jarFile = (JarFile) url.getContent();
								// Check the jar entry data before needlessly creating the
								// manifest
								if (jarFile.getJarEntryData(path) != null
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
				}
			}, AccessController.getContext());
		}
		catch (java.security.PrivilegedActionException ex) {
			// Ignore
		}
	}
}
