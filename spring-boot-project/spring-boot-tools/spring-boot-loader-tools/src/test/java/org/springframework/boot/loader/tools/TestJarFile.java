/*
 * Copyright 2012-2019 the original author or authors.
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

import java.io.BufferedInputStream;
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
import java.util.zip.ZipEntry;

import org.junit.rules.TemporaryFolder;

import org.springframework.util.FileCopyUtils;

/**
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Christoph Dreis
 */
public class TestJarFile {

	private final TemporaryFolder temporaryFolder;

	private final File jarSource;

	private final List<FileEntry> entries = new ArrayList<>();

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
		this.entries.add(new FileEntry(file, filename));
	}

	public void addFile(String filename, File fileToCopy) throws IOException {
		File file = getFilePath(filename);
		file.getParentFile().mkdirs();
		FileCopyUtils.copy(fileToCopy, file);
		this.entries.add(new FileEntry(file, filename));
	}

	public void addManifest(Manifest manifest) throws IOException {
		File manifestFile = new File(this.jarSource, "META-INF/MANIFEST.MF");
		manifestFile.getParentFile().mkdirs();
		try (OutputStream outputStream = new FileOutputStream(manifestFile)) {
			manifest.write(outputStream);
		}
		this.entries.add(new FileEntry(manifestFile, "META-INF/MANIFEST.MF"));
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
			FileCopyUtils.copy(inputStream, outputStream);
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
		writeEntriesToFile(file);
		return file;
	}

	private void writeEntriesToFile(File file) throws IOException {
		try (JarWriter writer = new JarWriter(file)) {
			for (FileEntry entry : this.entries) {
				writeFileEntry(writer, entry);
			}
		}
	}

	private void writeFileEntry(JarWriter writer, FileEntry entry) throws IOException {
		writer.writeEntry(entry.getName(), entry.getInputStream(), entry.getTime());
	}

	private static class FileEntry {

		private final File file;

		private final ZipEntry entry;

		FileEntry(File file, String filename) {
			this.file = file;
			this.entry = new ZipEntry(filename);
			this.entry.setTime(file.lastModified());
		}

		InputStream getInputStream() throws IOException {
			if (this.file.isDirectory()) {
				return null;
			}
			else {
				return new BufferedInputStream(new FileInputStream(this.file));
			}
		}

		String getName() {
			return this.entry.getName();
		}

		long getTime() {
			return this.entry.getTime();
		}

	}

}
