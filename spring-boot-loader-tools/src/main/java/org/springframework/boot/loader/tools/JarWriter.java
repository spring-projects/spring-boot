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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
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

	private static final String NESTED_LOADER_JAR = "META-INF/loader/spring-boot-loader.jar";

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
			ZipHeaderPeekInputStream inputStream = new ZipHeaderPeekInputStream(
					jarFile.getInputStream(entry));
			try {
				if (inputStream.hasZipHeader() && entry.getMethod() != ZipEntry.STORED) {
					new CrcAndSize(inputStream).setupStoredEntry(entry);
					inputStream.close();
					inputStream = new ZipHeaderPeekInputStream(
							jarFile.getInputStream(entry));
				}
				EntryWriter entryWriter = new InputStreamEntryWriter(inputStream, true);
				writeEntry(entry, entryWriter);
			}
			finally {
				inputStream.close();
			}
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
		new CrcAndSize(file).setupStoredEntry(entry);
		writeEntry(entry, new InputStreamEntryWriter(new FileInputStream(file), true));
	}

	/**
	 * Write the required spring-boot-loader classes to the JAR.
	 * @throws IOException
	 */
	public void writeLoaderClasses() throws IOException {
		URL loaderJar = getClass().getClassLoader().getResource(NESTED_LOADER_JAR);
		JarInputStream inputStream = new JarInputStream(new BufferedInputStream(
				loaderJar.openStream()));
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

	/**
	 * {@link InputStream} that can peek ahead at zip header bytes.
	 */
	private static class ZipHeaderPeekInputStream extends FilterInputStream {

		private static final byte[] ZIP_HEADER = new byte[] { 0x50, 0x4b, 0x03, 0x04 };

		private byte[] header;

		private ByteArrayInputStream headerStream;

		protected ZipHeaderPeekInputStream(InputStream in) throws IOException {
			super(in);
			this.header = new byte[4];
			int len = in.read(this.header);
			this.headerStream = new ByteArrayInputStream(this.header, 0, len);
		}

		@Override
		public int read() throws IOException {
			int read = (this.headerStream == null ? -1 : this.headerStream.read());
			if (read != -1) {
				this.headerStream = null;
				return read;
			}
			return super.read();
		}

		@Override
		public int read(byte[] b) throws IOException {
			return read(b, 0, b.length);
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int read = (this.headerStream == null ? -1 : this.headerStream.read(b, off,
					len));
			if (read != -1) {
				this.headerStream = null;
				return read;
			}
			return super.read(b, off, len);
		}

		public boolean hasZipHeader() {
			return Arrays.equals(this.header, ZIP_HEADER);
		}
	}

	/**
	 * Data holder for CRC and Size
	 */
	private static class CrcAndSize {

		private final CRC32 crc = new CRC32();

		private long size;

		public CrcAndSize(File file) throws IOException {
			FileInputStream inputStream = new FileInputStream(file);
			try {
				load(inputStream);
			}
			finally {
				inputStream.close();
			}
		}

		public CrcAndSize(InputStream inputStream) throws IOException {
			load(inputStream);
		}

		private void load(InputStream inputStream) throws IOException {
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead = -1;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				this.crc.update(buffer, 0, bytesRead);
				this.size += bytesRead;
			}
		}

		public void setupStoredEntry(JarEntry entry) {
			entry.setSize(this.size);
			entry.setCompressedSize(this.size);
			entry.setCrc(this.crc.getValue());
			entry.setMethod(ZipEntry.STORED);
		}
	}

}
