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

package org.springframework.boot.devtools.restart.classloader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.Enumeration;

import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile.Kind;
import org.springframework.core.SmartClassLoader;
import org.springframework.util.Assert;

/**
 * Disposable {@link ClassLoader} used to support application restarting. Provides parent
 * last loading for the specified URLs.
 *
 * @author Andy Clement
 * @author Phillip Webb
 * @since 1.3.0
 */
public class RestartClassLoader extends URLClassLoader implements SmartClassLoader {

	private final ClassLoaderFileRepository updatedFiles;

	/**
	 * Create a new {@link RestartClassLoader} instance.
	 * @param parent the parent classloader
	 * @param urls the urls managed by the classloader
	 */
	public RestartClassLoader(ClassLoader parent, URL[] urls) {
		this(parent, urls, ClassLoaderFileRepository.NONE);
	}

	/**
	 * Create a new {@link RestartClassLoader} instance.
	 * @param parent the parent classloader
	 * @param updatedFiles any files that have been updated since the JARs referenced in
	 * URLs were created.
	 * @param urls the urls managed by the classloader
	 */
	public RestartClassLoader(ClassLoader parent, URL[] urls, ClassLoaderFileRepository updatedFiles) {
		super(urls, parent);
		Assert.notNull(parent, "Parent must not be null");
		Assert.notNull(updatedFiles, "UpdatedFiles must not be null");
		this.updatedFiles = updatedFiles;
	}

	/**
	 * Returns an enumeration of URLs representing the resources with the given name. This
	 * method overrides the getResources() method in the parent class and ensures that
	 * duplicates are not included in the returned enumeration.
	 * @param name the name of the resource
	 * @return an enumeration of URLs representing the resources with the given name
	 * @throws IOException if an I/O error occurs while getting the resources
	 */
	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		// Use the parent since we're shadowing resource and we don't want duplicates
		Enumeration<URL> resources = getParent().getResources(name);
		ClassLoaderFile file = this.updatedFiles.getFile(name);
		if (file != null) {
			// Assume that we're replacing just the first item
			if (resources.hasMoreElements()) {
				resources.nextElement();
			}
			if (file.getKind() != Kind.DELETED) {
				return new CompoundEnumeration<>(createFileUrl(name, file), resources);
			}
		}
		return resources;
	}

	/**
	 * Returns a URL object for reading the specified resource.
	 *
	 * This method first checks if the resource has been marked as deleted in the
	 * updatedFiles map. If it has been marked as deleted, it returns null indicating that
	 * the resource does not exist. Otherwise, it attempts to find the resource using the
	 * findResource() method. If the resource is found, it returns the URL object
	 * representing the resource. If the resource is not found, it delegates the call to
	 * the parent class loader's getResource() method.
	 * @param name the name of the resource
	 * @return a URL object for reading the specified resource, or null if the resource
	 * does not exist
	 */
	@Override
	public URL getResource(String name) {
		ClassLoaderFile file = this.updatedFiles.getFile(name);
		if (file != null && file.getKind() == Kind.DELETED) {
			return null;
		}
		URL resource = findResource(name);
		if (resource != null) {
			return resource;
		}
		return getParent().getResource(name);
	}

	/**
	 * Finds the resource with the specified name.
	 * @param name the name of the resource
	 * @return the URL object representing the resource, or null if the resource is not
	 * found or has been deleted
	 * @see java.lang.ClassLoader#findResource(java.lang.String)
	 */
	@Override
	public URL findResource(String name) {
		final ClassLoaderFile file = this.updatedFiles.getFile(name);
		if (file == null) {
			return super.findResource(name);
		}
		if (file.getKind() == Kind.DELETED) {
			return null;
		}
		return createFileUrl(name, file);
	}

	/**
	 * Loads the class with the specified name.
	 * @param name the fully qualified name of the class to be loaded
	 * @param resolve a boolean indicating whether or not to resolve the class
	 * @return the loaded class
	 * @throws ClassNotFoundException if the class could not be found
	 */
	@Override
	public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		String path = name.replace('.', '/').concat(".class");
		ClassLoaderFile file = this.updatedFiles.getFile(path);
		if (file != null && file.getKind() == Kind.DELETED) {
			throw new ClassNotFoundException(name);
		}
		synchronized (getClassLoadingLock(name)) {
			Class<?> loadedClass = findLoadedClass(name);
			if (loadedClass == null) {
				try {
					loadedClass = findClass(name);
				}
				catch (ClassNotFoundException ex) {
					loadedClass = Class.forName(name, false, getParent());
				}
			}
			if (resolve) {
				resolveClass(loadedClass);
			}
			return loadedClass;
		}
	}

	/**
	 * Finds and loads the class with the specified name.
	 * @param name the fully qualified name of the class to be found
	 * @return the loaded class
	 * @throws ClassNotFoundException if the class with the specified name is not found
	 */
	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		String path = name.replace('.', '/').concat(".class");
		final ClassLoaderFile file = this.updatedFiles.getFile(path);
		if (file == null) {
			return super.findClass(name);
		}
		if (file.getKind() == Kind.DELETED) {
			throw new ClassNotFoundException(name);
		}
		byte[] bytes = file.getContents();
		return defineClass(name, bytes, 0, bytes.length);
	}

	/**
	 * Defines and returns a new class with the specified name, byte code, and protection
	 * domain.
	 * @param name the fully qualified name of the class
	 * @param b the byte code of the class
	 * @param protectionDomain the protection domain of the class
	 * @return the newly defined class
	 */
	@Override
	public Class<?> publicDefineClass(String name, byte[] b, ProtectionDomain protectionDomain) {
		return defineClass(name, b, 0, b.length, protectionDomain);
	}

	/**
	 * Returns the original class loader that was used to load the RestartClassLoader
	 * class.
	 * @return the original class loader
	 */
	@Override
	public ClassLoader getOriginalClassLoader() {
		return getParent();
	}

	/**
	 * Creates a URL for a file with the given name and ClassLoaderFile.
	 * @param name the name of the file
	 * @param file the ClassLoaderFile representing the file
	 * @return the URL for the file
	 * @throws IllegalStateException if the URL is malformed
	 */
	private URL createFileUrl(String name, ClassLoaderFile file) {
		try {
			return new URL("reloaded", null, -1, "/" + name, new ClassLoaderFileURLStreamHandler(file));
		}
		catch (MalformedURLException ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * Checks if the given class is reloadable.
	 * @param classType the class to check
	 * @return true if the class is reloadable, false otherwise
	 */
	@Override
	public boolean isClassReloadable(Class<?> classType) {
		return (classType.getClassLoader() instanceof RestartClassLoader);
	}

	/**
	 * Compound {@link Enumeration} that adds an additional item to the front.
	 */
	private static class CompoundEnumeration<E> implements Enumeration<E> {

		private E firstElement;

		private final Enumeration<E> enumeration;

		/**
		 * Constructs a new CompoundEnumeration object with the specified first element
		 * and enumeration.
		 * @param firstElement the first element of the compound enumeration
		 * @param enumeration the enumeration to be combined with the first element
		 */
		CompoundEnumeration(E firstElement, Enumeration<E> enumeration) {
			this.firstElement = firstElement;
			this.enumeration = enumeration;
		}

		/**
		 * Returns true if this CompoundEnumeration object contains more elements.
		 * @return true if this CompoundEnumeration object contains more elements, false
		 * otherwise
		 */
		@Override
		public boolean hasMoreElements() {
			return (this.firstElement != null || this.enumeration.hasMoreElements());
		}

		/**
		 * Returns the next element in the enumeration. If the first element is not null,
		 * it returns the first element and sets it to null. Otherwise, it returns the
		 * next element from the underlying enumeration.
		 * @return the next element in the enumeration
		 */
		@Override
		public E nextElement() {
			if (this.firstElement == null) {
				return this.enumeration.nextElement();
			}
			E element = this.firstElement;
			this.firstElement = null;
			return element;
		}

	}

}
