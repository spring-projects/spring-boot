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

package org.springframework.boot.loader.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.junit.rules.TemporaryFolder;
import org.zeroturnaround.zip.ZipUtil;

/**
 * @author Phillip Webb
 */
public class TestJarFile {

	private final byte[] buffer = new byte[4096];

	private final TemporaryFolder temporaryFolder;

	private final File jarSource;

	public TestJarFile(TemporaryFolder temporaryFolder) throws IOException {
		this.temporaryFolder = temporaryFolder;
		this.jarSource = temporaryFolder.newFolder();
	}

	public void addClass(String filename, Class<?> classToCopy) throws IOException {
		File file = getFilePath(filename);
		file.getParentFile().mkdirs();
		InputStream inputStream = getClass().getResourceAsStream(
				"/" + classToCopy.getName().replace(".", "/") + ".class");
		copyToFile(inputStream, file);
	}

	public void addFile(String filename, File fileToCopy) throws IOException {
		File file = getFilePath(filename);
		file.getParentFile().mkdirs();
		InputStream inputStream = new FileInputStream(fileToCopy);
		try {
			copyToFile(inputStream, file);
		}
		finally {
			inputStream.close();
		}
	}

	public void addManifest(Manifest manifest) throws IOException {
		File manifestFile = new File(this.jarSource, "META-INF/MANIFEST.MF");
		manifestFile.getParentFile().mkdirs();
		OutputStream outputStream = new FileOutputStream(manifestFile);
		try {
			manifest.write(outputStream);
		}
		finally {
			outputStream.close();
		}
	}

	private File getFilePath(String filename) {
		String[] paths = filename.split("\\/");
		File file = this.jarSource;
		for (String path : paths) {
			file = new File(file, path);
		}
		return file;
	}

	private void copyToFile(InputStream inputStream, File file)
			throws FileNotFoundException, IOException {
		OutputStream outputStream = new FileOutputStream(file);
		try {
			copy(inputStream, outputStream);
		}
		finally {
			outputStream.close();
		}
	}

	private void copy(InputStream in, OutputStream out) throws IOException {
		int bytesRead = -1;
		while ((bytesRead = in.read(this.buffer)) != -1) {
			out.write(this.buffer, 0, bytesRead);
		}
	}

	public JarFile getJarFile() throws IOException {
		return new JarFile(getFile());
	}

	public File getJarSource() {
		return this.jarSource;
	}

	public File getFile() throws IOException {
		File file = this.temporaryFolder.newFile();
		file = new File(file.getParent(), file.getName() + ".jar");
		ZipUtil.pack(this.jarSource, file);
		return file;
	}

}
