/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.cli.jar;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.Manifest;

/**
 * A launcher for a CLI application that has been compiled and packaged as a jar file.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
public class PackagedSpringApplicationLauncher {

	public static final String SOURCE_MANIFEST_ENTRY = "Spring-Application-Source-Classes";

	public static final String MAIN_CLASS_MANIFEST_ENTRY = "Start-Class";

	private static final String SPRING_APPLICATION_CLASS = "org.springframework.boot.SpringApplication";

	private void run(String[] args) throws Exception {
		URLClassLoader classLoader = (URLClassLoader) Thread.currentThread()
				.getContextClassLoader();
		Class<?> application = classLoader.loadClass(SPRING_APPLICATION_CLASS);
		Method method = application.getMethod("run", Object[].class, String[].class);
		method.invoke(null, getSources(classLoader), args);
	}

	private Object[] getSources(URLClassLoader classLoader) throws Exception {
		for (Enumeration<URL> urls = classLoader.findResources("META-INF/MANIFEST.MF"); urls
				.hasMoreElements();) {
			URL url = urls.nextElement();
			Manifest manifest = new Manifest(url.openStream());
			if (getClass().getName().equals(
					manifest.getMainAttributes().getValue(MAIN_CLASS_MANIFEST_ENTRY))) {
				String attribute = manifest.getMainAttributes().getValue(
						SOURCE_MANIFEST_ENTRY);
				return loadClasses(classLoader, attribute.split(","));
			}
		}
		throw new IllegalStateException("Cannot locate " + SOURCE_MANIFEST_ENTRY
				+ " in MANIFEST.MF");
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
