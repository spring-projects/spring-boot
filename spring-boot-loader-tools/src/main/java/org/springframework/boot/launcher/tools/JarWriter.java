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

package org.springframework.boot.launcher.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

/**
 * Writes JAR content, ensuring valid directory entries are always create and duplicate
 * items are ignored.
 * 
 * @author Phillip Webb
 */
class JarWriter {

	private static final String NESTED_LOADER_JAR = "/META-INF/loader/spring-boot-loader.jar";

	private static final int BUFFER_SIZE = 4096;

	private final JarOutputStream jarOutput;

	private final Set<String> writtenEntries = new HashSet<String>();

	/**
	 * Create a new {@link JarWriter} instance.
	 * @param file the file to write
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public JarWriter(File file) throws FileNotFoundException, IOException {
		this.jarOutput = new JarOutputStream(new FileOutputStream(file));
	}

	/**
	 * Write the specified manifest.
	 * @param manifest the manifest to write
	 * @throws IOException
	 */
	public void writeManifest(final Manifest manifest) throws IOException {
		JarEntry entry = new JarEntry("META-INF/MANIFEST.MF");
		writeEntry(entry, new EntryWriter() {
			@Override
			public void write(OutputStream outputStream) throws IOException {
				manifest.write(outputStream);
			}
		});
	}

	/**
	 * Write all entries from the specified jar file.
	 * @param jarFile the source jar file
	 * @throws IOException
	 */
	public void writeEntries(JarFile jarFile) throws IOException {
		Enumeration<JarEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			EntryWriter entryWriter = new InputStreamEntryWriter(
					jarFile.getInputStream(entry), true);
			writeEntry(entry, entryWriter);
		}
	}

	/**
	 * Write a nested library.
	 * @param destination the destination of the library
	 * @param file the library file
	 * @throws IOException
	 */
	public void writeNestedLibrary(String destination, File file) throws IOException {
		JarEntry entry = new JarEntry(destination + file.getName());
		entry.setSize(file.length());
		entry.setCompressedSize(file.length());
		entry.setCrc(getCrc(file));
		entry.setMethod(ZipEntry.STORED);
		writeEntry(entry, new InputStreamEntryWriter(new FileInputStream(file), true));
	}

	private long getCrc(File file) throws IOException {
		FileInputStream inputStream = new FileInputStream(file);
		try {
			byte[] buffer = new byte[BUFFER_SIZE];
			CRC32 crc = new CRC32();
			int bytesRead = -1;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				crc.update(buffer, 0, bytesRead);
			}
			return crc.getValue();
		}
		finally {
			inputStream.close();
		}
	}

	/**
	 * Write the required spring-boot-loader classes to the JAR.
	 * @throws IOException
	 */
	public void writeLoaderClasses() throws IOException {
		JarInputStream inputStream = new JarInputStream(getClass().getResourceAsStream(
				NESTED_LOADER_JAR));
		JarEntry entry;
		while ((entry = inputStream.getNextJarEntry()) != null) {
			if (entry.getName().endsWith(".class")) {
				writeEntry(entry, new InputStreamEntryWriter(inputStream, false));
			}
		}
		inputStream.close();
	}

	/**
	 * Close the writer.
	 * @throws IOException
	 */
	public void close() throws IOException {
		this.jarOutput.close();
	}

	/**
	 * Perform the actual write of a {@link JarEntry}. All other {@code write} method
	 * delegate to this one.
	 * @param entry the entry to write
	 * @param entryWriter the entry writer or {@code null} if there is no content
	 * @throws IOException
	 */
	private void writeEntry(JarEntry entry, EntryWriter entryWriter) throws IOException {
		String parent = entry.getName();
		if (parent.endsWith("/")) {
			parent = parent.substring(0, parent.length() - 1);
		}
		if (parent.lastIndexOf("/") != -1) {
			parent = parent.substring(0, parent.lastIndexOf("/") + 1);
			if (parent.length() > 0) {
				writeEntry(new JarEntry(parent), null);
			}
		}

		if (this.writtenEntries.add(entry.getName())) {
			this.jarOutput.putNextEntry(entry);
			if (entryWriter != null) {
				entryWriter.write(this.jarOutput);
			}
			this.jarOutput.closeEntry();
		}
	}

	/**
	 * Interface used to write jar entry date.
	 */
	private static interface EntryWriter {

		/**
		 * Write entry data to the specified output stream
		 * @param outputStream the destination for the data
		 * @throws IOException
		 */
		void write(OutputStream outputStream) throws IOException;

	}

	/**
	 * {@link EntryWriter} that writes content from an {@link InputStream}.
	 */
	private static class InputStreamEntryWriter implements EntryWriter {

		private final InputStream inputStream;

		private final boolean close;

		public InputStreamEntryWriter(InputStream inputStream, boolean close) {
			this.inputStream = inputStream;
			this.close = close;
		}

		public void write(OutputStream outputStream) throws IOException {
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead = -1;
			while ((bytesRead = this.inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}
			outputStream.flush();
			if (this.close) {
				this.inputStream.close();
			}
		}

	}

}
