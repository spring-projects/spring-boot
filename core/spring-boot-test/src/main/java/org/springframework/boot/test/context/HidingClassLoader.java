/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.test.context;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link URLClassLoader} that hides selected classes and defines another selected set of
 * classes itself. This is useful when testing code that must be loaded by the same class
 * loader that hides an optional dependency.
 *
 * @author Sean Xu
 * @since 4.2.0
 */
public class HidingClassLoader extends URLClassLoader {

	private final Set<String> locallyDefinedClasses;

	private final Set<String> hiddenClasses;

	/**
	 * Create a {@link HidingClassLoader} using the current class path.
	 * @param locallyDefinedClasses the classes that should be defined by this loader
	 * @param hiddenClasses the classes that should be hidden
	 */
	public HidingClassLoader(Collection<String> locallyDefinedClasses, String... hiddenClasses) {
		this(HidingClassLoader.class.getClassLoader(), locallyDefinedClasses, hiddenClasses);
	}

	/**
	 * Create a {@link HidingClassLoader} with the given parent using the current class
	 * path.
	 * @param parent the parent class loader
	 * @param locallyDefinedClasses the classes that should be defined by this loader
	 * @param hiddenClasses the classes that should be hidden
	 */
	public HidingClassLoader(ClassLoader parent, Collection<String> locallyDefinedClasses, String... hiddenClasses) {
		super(classPathUrls(), parent);
		this.locallyDefinedClasses = new HashSet<>(locallyDefinedClasses);
		this.hiddenClasses = Set.copyOf(Arrays.asList(hiddenClasses));
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if (this.hiddenClasses.contains(name)) {
			throw new ClassNotFoundException(name);
		}
		if (isLocallyDefined(name)) {
			synchronized (getClassLoadingLock(name)) {
				Class<?> loaded = findLoadedClass(name);
				if (loaded == null) {
					loaded = findClass(name);
				}
				if (resolve) {
					resolveClass(loaded);
				}
				return loaded;
			}
		}
		return super.loadClass(name, resolve);
	}

	private boolean isLocallyDefined(String name) {
		return this.locallyDefinedClasses.stream()
			.anyMatch((local) -> name.equals(local) || name.startsWith(local + '$'));
	}

	private static URL[] classPathUrls() {
		String classPath = System.getProperty("java.class.path");
		return Arrays.stream(classPath.split(File.pathSeparator))
			.map(Path::of)
			.map(HidingClassLoader::toUrl)
			.toArray(URL[]::new);
	}

	private static URL toUrl(Path path) {
		try {
			return path.toUri().toURL();
		}
		catch (MalformedURLException ex) {
			throw new IllegalStateException("Unable to create a URL for " + path, ex);
		}
	}

}
