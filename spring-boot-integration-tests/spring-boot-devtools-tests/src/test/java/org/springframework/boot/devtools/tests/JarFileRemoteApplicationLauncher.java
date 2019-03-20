/*
 * Copyright 2012-2017 the original author or authors.
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

import org.springframework.util.FileSystemUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ApplicationLauncher} that launches a remote application with its classes in a
 * jar file.
 *
 * @author Andy Wilkinson
 */
public class JarFileRemoteApplicationLauncher extends RemoteApplicationLauncher {

	@Override
	protected String createApplicationClassPath() throws Exception {
		File appDirectory = new File("target/app");
		FileSystemUtils.deleteRecursively(appDirectory);
		appDirectory.mkdirs();
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		JarOutputStream output = new JarOutputStream(
				new FileOutputStream(new File(appDirectory, "app.jar")), manifest);
		FileSystemUtils.copyRecursively(new File("target/test-classes/com"),
				new File("target/app/com"));
		addToJar(output, new File("target/app/"), new File("target/app/"));
		output.close();
		List<String> entries = new ArrayList<String>();
		entries.add("target/app/app.jar");
		for (File jar : new File("target/dependencies").listFiles()) {
			entries.add(jar.getAbsolutePath());
		}
		String classpath = StringUtils.collectionToDelimitedString(entries,
				File.pathSeparator);
		return classpath;
	}

	private void addToJar(JarOutputStream output, File root, File current)
			throws IOException {
		for (File file : current.listFiles()) {
			if (file.isDirectory()) {
				addToJar(output, root, file);
			}
			output.putNextEntry(new ZipEntry(
					file.getAbsolutePath().substring(root.getAbsolutePath().length() + 1)
							.replace("\\", "/") + (file.isDirectory() ? "/" : "")));
			if (file.isFile()) {
				FileInputStream input = new FileInputStream(file);
				try {
					StreamUtils.copy(input, output);
				}
				finally {
					input.close();
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
