/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.launcher;

import java.io.IOException;
import java.util.List;
import java.util.jar.JarEntry;

import org.springframework.launcher.jar.JarEntryFilter;
import org.springframework.launcher.jar.RandomAccessJarFile;

/**
 * {@link Launcher} for WAR based archives. This launcher for standard WAR archives.
 * Supports dependencies in {@code WEB-INF/lib} as well as {@code WEB-INF/lib-provided},
 * classes are loaded from {@code WEB-INF/classes}.
 * 
 * @author Phillip Webb
 */
public class WarLauncher extends Launcher {

	@Override
	protected boolean isNestedJarFile(JarEntry jarEntry) {
		if (jarEntry.isDirectory()) {
			return jarEntry.getName().equals("WEB-INF/classes/");
		}
		else {
			return jarEntry.getName().startsWith("WEB-INF/lib/")
					|| jarEntry.getName().startsWith("WEB-INF/lib-provided/");
		}
	}

	@Override
	protected void postProcessLib(RandomAccessJarFile jarFile,
			List<RandomAccessJarFile> lib) throws Exception {
		lib.add(0, filterJarFile(jarFile));
	}

	/**
	 * Filter the specified WAR file to exclude elements that should not appear on the
	 * classpath.
	 * @param jarFile the source file
	 * @return the filtered file
	 * @throws IOException on error
	 */
	protected RandomAccessJarFile filterJarFile(RandomAccessJarFile jarFile)
			throws IOException {
		return jarFile.getFilteredJarFile(new JarEntryFilter() {

			@Override
			public String apply(String entryName, JarEntry entry) {
				if (entryName.startsWith("META-INF/") || entryName.startsWith("WEB-INF/")) {
					return null;
				}
				return entryName;
			}
		});
	}

	public static void main(String[] args) {
		new WarLauncher().launch(args);
	}

}
