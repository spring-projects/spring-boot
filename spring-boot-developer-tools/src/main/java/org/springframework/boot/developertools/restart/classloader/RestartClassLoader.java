/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.developertools.restart.classloader;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

	private final Log logger;

	/**
	 * Create a new {@link RestartClassLoader} instance.
	 * @param parent the parent classloader URLs were created.
	 * @param urls the urls managed by the classloader
	 */
	public RestartClassLoader(ClassLoader parent, URL[] urls) {
		this(parent, urls, LogFactory.getLog(RestartClassLoader.class));
	}

	/**
	 * Create a new {@link RestartClassLoader} instance.
	 * @param parent the parent classloader URLs were created.
	 * @param urls the urls managed by the classloader
	 * @param logger the logger used for messages
	 */
	public RestartClassLoader(ClassLoader parent, URL[] urls, Log logger) {
		super(urls, parent);
		Assert.notNull(parent, "Parent must not be null");
		Assert.notNull(logger, "Logger must not be null");
		this.logger = logger;
		if (logger.isDebugEnabled()) {
			logger.debug("Created RestartClassLoader " + toString());
		}
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		// Use the parent since we're shadowing resource and we don't want duplicates
		return getParent().getResources(name);
	}

	@Override
	public URL getResource(String name) {
		URL resource = findResource(name);
		if (resource != null) {
			return resource;
		}
		return getParent().getResource(name);
	}

	@Override
	public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		Class<?> loadedClass = findLoadedClass(name);
		if (loadedClass == null) {
			try {
				loadedClass = findClass(name);
			}
			catch (ClassNotFoundException ex) {
				loadedClass = getParent().loadClass(name);
			}
		}
		if (resolve) {
			resolveClass(loadedClass);
		}
		return loadedClass;
	}

	@Override
	protected void finalize() throws Throwable {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("Finalized classloader " + toString());
		}
		super.finalize();
	}

	@Override
	public boolean isClassReloadable(Class<?> classType) {
		return (classType.getClassLoader() instanceof RestartClassLoader);
	}

}
