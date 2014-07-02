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

package org.springframework.boot.loader.tools;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Java Virtual Machine Utils.
 *
 * @author Phillip Webb
 */
abstract class JvmUtils {

	/**
	 * Various search locations for tools, including the odd Java 6 OSX jar
	 */
	private static final String[] TOOLS_LOCATIONS = { "lib/tools.jar",
			"../lib/tools.jar", "../Classes/classes.jar" };

	public static ClassLoader getToolsClassLoader() {
		ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
		return new URLClassLoader(new URL[] { getToolsJarUrl() }, systemClassLoader);
	}

	public static URL getToolsJarUrl() {
		String javaHome = getJavaHome();
		for (String location : TOOLS_LOCATIONS) {
			try {
				URL url = new URL(javaHome + "/" + location);
				if (new File(url.toURI()).exists()) {
					return url;
				}
			}
			catch (Exception ex) {
				// Ignore and try the next location
			}
		}
		throw new IllegalStateException("Unable to locate tools.jar");
	}

	private static String getJavaHome() {
		try {
			return new File(System.getProperty("java.home")).toURI().toURL()
					.toExternalForm();
		}
		catch (MalformedURLException e) {
			throw new IllegalStateException("Cannot locate java.home", e);
		}
	}

}
