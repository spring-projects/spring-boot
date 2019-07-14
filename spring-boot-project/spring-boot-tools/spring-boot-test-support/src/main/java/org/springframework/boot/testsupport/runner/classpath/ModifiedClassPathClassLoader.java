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

package org.springframework.boot.testsupport.runner.classpath;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Custom {@link URLClassLoader} that modifies the class path.
 *
 * @author Andy Wilkinson
 * @author Christoph Dreis
 * @see ModifiedClassPathClassLoaderFactory
 */
final class ModifiedClassPathClassLoader extends URLClassLoader {

	private final ClassLoader junitLoader;

	ModifiedClassPathClassLoader(URL[] urls, ClassLoader parent, ClassLoader junitLoader) {
		super(urls, parent);
		this.junitLoader = junitLoader;
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		if (name.startsWith("org.junit") || name.startsWith("org.hamcrest")) {
			return this.junitLoader.loadClass(name);
		}
		return super.loadClass(name);
	}

}
