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

package org.springframework.boot.devtools.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ApplicationLauncher} that launches a remote application with its classes in a
 * jar file.
 *
 * @author Andy Wilkinson
 */
public class JarFileRemoteApplicationLauncher extends RemoteApplicationLauncher {

	public JarFileRemoteApplicationLauncher(Directories directories) {
		super(directories);
	}

	@Override
	protected String createApplicationClassPath() throws Exception {
		File appDirectory = getDirectories().getAppDirectory();
		copyApplicationTo(appDirectory);
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		File appJar = new File(appDirectory, "app.jar");
		JarOutputStream output = new JarOutputStream(new FileOutputStream(appJar), manifest);
		addToJar(output, appDirectory, appDirectory);
		output.close();
		List<String> entries = new ArrayList<>();
		entries.add(appJar.getAbsolutePath());
		entries.addAll(getDependencyJarPaths());
		String classpath = StringUtils.collectionToDelimitedString(entries, File.pathSeparator);
		return classpath;
	}

	private void addToJar(JarOutputStream output, File root, File current) throws IOException {
		for (File file : current.listFiles()) {
			if (file.isDirectory()) {
				addToJar(output, root, file);
			}
			output.putNextEntry(new ZipEntry(
					file.getAbsolutePath().substring(root.getAbsolutePath().length() + 1).replace("\\", "/")
							+ (file.isDirectory() ? "/" : "")));
			if (file.isFile()) {
				try (FileInputStream input = new FileInputStream(file)) {
					StreamUtils.copy(input, output);
				}
			}
			output.closeEntry();
		}
	}

	@Override
	public String toString() {
		return "jar file remote";
	}

}
