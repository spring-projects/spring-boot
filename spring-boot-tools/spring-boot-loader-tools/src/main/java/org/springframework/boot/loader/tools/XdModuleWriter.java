/*
 * Copyright 2014 the original author or authors.
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.jar.JarEntry;

/**
 * @author David Turanski
 */
public class XdModuleWriter extends JarWriter {
	private final String moduleConfigRoot;

	private final static String MODULE_RESOURCE_DESTINATION = "config";

	/**
	 * Create a new {@link org.springframework.boot.loader.tools.JarWriter} instance.
	 *
	 * @param file the file to write
	 * @throws java.io.IOException
	 * @throws java.io.FileNotFoundException
	 */
	public XdModuleWriter(File file, String moduleConfigRoot) throws FileNotFoundException, IOException {
		super(file);
		this.moduleConfigRoot = moduleConfigRoot;
	}

	@Override
	protected JarEntry destinationEntry(JarEntry entry) {
		if (entry.getName().startsWith(moduleConfigRoot)) {
			String destinationName = null;
			String sourceName = entry.getName();
			if (entry.isDirectory()) {

				destinationName = MODULE_RESOURCE_DESTINATION + "/";
			}
			else {
				destinationName = MODULE_RESOURCE_DESTINATION + sourceName.substring(sourceName.lastIndexOf
						("/"));
			}

			return new JarEntry(destinationName);
		}
		return entry;
	}
}

