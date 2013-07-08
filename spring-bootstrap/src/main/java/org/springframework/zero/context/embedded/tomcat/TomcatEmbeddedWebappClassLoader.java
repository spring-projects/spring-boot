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

package org.springframework.zero.context.embedded.tomcat;

import java.net.URL;
import java.net.URLClassLoader;

import org.apache.catalina.loader.WebappClassLoader;

/**
 * Extension of Tomcat's {@link WebappClassLoader} that does not consider the
 * {@link ClassLoader#getSystemClassLoader() system classloader}. This is required to to
 * ensure that any custom context classloader is always used (as is the case with some
 * executable archives).
 * 
 * @author Phillip Webb
 */
public class TomcatEmbeddedWebappClassLoader extends WebappClassLoader {

	public TomcatEmbeddedWebappClassLoader() {
		super();
		this.system = new EmbeddedSystemClassLoader();
	}

	public TomcatEmbeddedWebappClassLoader(ClassLoader parent) {
		super(parent);
		this.system = new EmbeddedSystemClassLoader();
	}

	private static class EmbeddedSystemClassLoader extends URLClassLoader {

		public EmbeddedSystemClassLoader() {
			super(new URL[] {});
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			throw new ClassNotFoundException(
					"System ClassLoader disabled for embedded context, unable to load "
							+ name);
		}

	}

}
