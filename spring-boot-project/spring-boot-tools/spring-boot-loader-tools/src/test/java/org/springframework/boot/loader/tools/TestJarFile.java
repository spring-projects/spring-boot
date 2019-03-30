/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.loader.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.junit.rules.TemporaryFolder;
import org.zeroturnaround.zip.FileSource;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipUtil;

/**
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class TestJarFile {

	private final byte[] buffer = new byte[4096];

	private final TemporaryFolder temporaryFolder;

	private final File jarSource;

	private final List<ZipEntrySource> entries = new ArrayList<>();

	public TestJarFile(TemporaryFolder temporaryFolder) throws IOException {
		this.temporaryFolder = temporaryFolder;
		this.jarSource = temporaryFolder.newFolder();
	}

	public void addClass(String filename, Class<?> classToCopy) throws IOException {
		addClass(filename, classToCopy, null);
	}

	public void addClass(String filename, Class<?> classToCopy, Long time)
			throws IOException {
		File file = getFilePath(filename);
		file.getParentFile().mkdirs();
		InputStream inputStream = getClass().getResourceAsStream(
				"/" + classToCopy.getName().replace('.', '/') + ".class");
		copyToFile(inputStream, file);
		if (time != null) {
			file.setLastModified(time);
		}
		this.entries.add(new FileSource(filename, file));
	}

	public void addFile(String filename, File fileToCopy) throws IOException {
		File file = getFilePath(filename);
		file.getParentFile().mkdirs();
		try (InputStream inputStream = new FileInputStream(fileToCopy)) {
			copyToFile(inputStream, file);
		}
		this.entries.add(new FileSource(filename, file));
	}

	public void addManifest(Manifest manifest) throws IOException {
		File manifestFile = new File(this.jarSource, "META-INF/MANIFEST.MF");
		manifestFile.getParentFile().mkdirs();
		try (OutputStream outputStream = new FileOutputStream(manifestFile)) {
			manifest.write(outputStream);
		}
		this.entries.add(new FileSource("META-INF/MANIFEST.MF", manifestFile));
	}

	private File getFilePath(String filename) {
		String[] paths = filename.split("\\/");
		File file = this.jarSource;
		for (String path : paths) {
			file = new File(file, path);
		}
		return file;
	}

	private void copyToFile(InputStream inputStream, File file) throws IOException {
		try (OutputStream outputStream = new FileOutputStream(file)) {
			copy(inputStream, outputStream);
		}
	}

	private void copy(InputStream in, OutputStream out) throws IOException {
		int bytesRead;
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
		return getFile("jar");
	}

	public File getFile(String extension) throws IOException {
		File file = this.temporaryFolder.newFile();
		file = new File(file.getParent(), file.getName() + "." + extension);
		ZipUtil.pack(this.entries.toArray(new ZipEntrySource[0]), file);
		return file;
	}

}
