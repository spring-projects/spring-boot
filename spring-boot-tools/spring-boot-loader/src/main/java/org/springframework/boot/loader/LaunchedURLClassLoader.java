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

import org.springframework.boot.loader.jar.RandomAccessJarFile;

/**
 * {@link ClassLoader} used by the {@link Launcher}.
 * 
 * @author Phillip Webb
 */
public class LaunchedURLClassLoader extends URLClassLoader {

	/**
	 * Create a new {@link LaunchedURLClassLoader} instance.
	 * @param urls the URLs from which to load classes and resources
	 * @param parent the parent class loader for delegation
	 */
	public LaunchedURLClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
	}

	/**
	 * Attempt to load classes from the URLs before delegating to the parent loader.
	 */
	@Override
	protected Class<?> loadClass(String name, boolean resolve)
			throws ClassNotFoundException {
		synchronized (getClassLoadingLock(name)) {
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
		try {
			return findClass(name);
		}
		catch (ClassNotFoundException e) {
		}
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
				public Object run() throws ClassNotFoundException {
					String path = name.replace('.', '/').concat(".class");
					for (URL url : getURLs()) {
						try {
							if (url.getContent() instanceof RandomAccessJarFile) {
								RandomAccessJarFile jarFile = (RandomAccessJarFile) url
										.getContent();
								if (jarFile.getManifest() != null
										&& jarFile.getJarEntry(path) != null) {
									definePackage(packageName, jarFile.getManifest(), url);
									return null;
								}
							}
						}
						catch (IOException e) {
						}
					}
					return null;
				}
			}, AccessController.getContext());
		}
		catch (java.security.PrivilegedActionException pae) {
		}
	}
}
