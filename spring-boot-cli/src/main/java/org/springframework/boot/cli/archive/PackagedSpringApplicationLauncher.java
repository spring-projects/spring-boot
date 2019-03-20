/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.cli.archive;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.springframework.boot.cli.app.SpringApplicationLauncher;

/**
 * A launcher for a CLI application that has been compiled and packaged as a jar file.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
public final class PackagedSpringApplicationLauncher {

	/**
	 * The entry containing the source class.
	 */
	public static final String SOURCE_ENTRY = "Spring-Application-Source-Classes";

	/**
	 * The entry containing the start class.
	 */
	public static final String START_CLASS_ENTRY = "Start-Class";

	private PackagedSpringApplicationLauncher() {
	}

	private void run(String[] args) throws Exception {
		URLClassLoader classLoader = (URLClassLoader) Thread.currentThread()
				.getContextClassLoader();
		new SpringApplicationLauncher(classLoader).launch(getSources(classLoader), args);
	}

	private Object[] getSources(URLClassLoader classLoader) throws Exception {
		Enumeration<URL> urls = classLoader.getResources("META-INF/MANIFEST.MF");
		while (urls.hasMoreElements()) {
			URL url = urls.nextElement();
			Manifest manifest = new Manifest(url.openStream());
			if (isCliPackaged(manifest)) {
				String sources = manifest.getMainAttributes().getValue(SOURCE_ENTRY);
				return loadClasses(classLoader, sources.split(","));
			}
		}
		throw new IllegalStateException(
				"Cannot locate " + SOURCE_ENTRY + " in MANIFEST.MF");
	}

	private boolean isCliPackaged(Manifest manifest) {
		Attributes attributes = manifest.getMainAttributes();
		String startClass = attributes.getValue(START_CLASS_ENTRY);
		return getClass().getName().equals(startClass);
	}

	private Class<?>[] loadClasses(ClassLoader classLoader, String[] names)
			throws ClassNotFoundException {
		Class<?>[] classes = new Class<?>[names.length];
		for (int i = 0; i < names.length; i++) {
			classes[i] = classLoader.loadClass(names[i]);
		}
		return classes;
	}

	public static void main(String[] args) throws Exception {
		new PackagedSpringApplicationLauncher().run(args);
	}

}
