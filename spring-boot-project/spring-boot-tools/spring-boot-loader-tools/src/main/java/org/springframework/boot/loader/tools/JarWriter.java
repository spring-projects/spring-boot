/*
 * Copyright 2012-2022 the original author or authors.
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;

/**
 * Writes JAR content, ensuring valid directory entries are always created and duplicate
 * items are ignored.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Scott Frederick
 * @since 1.0.0
 */
public class JarWriter extends AbstractJarWriter implements AutoCloseable {

	private final JarArchiveOutputStream jarOutputStream;

	private final FileTime lastModifiedTime;

	/**
	 * Create a new {@link JarWriter} instance.
	 * @param file the file to write
	 * @throws IOException if the file cannot be opened
	 * @throws FileNotFoundException if the file cannot be found
	 */
	public JarWriter(File file) throws FileNotFoundException, IOException {
		this(file, null);
	}

	/**
	 * Create a new {@link JarWriter} instance.
	 * @param file the file to write
	 * @param launchScript an optional launch script to prepend to the front of the jar
	 * @throws IOException if the file cannot be opened
	 * @throws FileNotFoundException if the file cannot be found
	 */
	public JarWriter(File file, LaunchScript launchScript) throws FileNotFoundException, IOException {
		this(file, launchScript, null);
	}

	/**
	 * Create a new {@link JarWriter} instance.
	 * @param file the file to write
	 * @param launchScript an optional launch script to prepend to the front of the jar
	 * @param lastModifiedTime an optional last modified time to apply to the written
	 * entries
	 * @throws IOException if the file cannot be opened
	 * @throws FileNotFoundException if the file cannot be found
	 * @since 2.3.0
	 */
	public JarWriter(File file, LaunchScript launchScript, FileTime lastModifiedTime)
			throws FileNotFoundException, IOException {
		this.jarOutputStream = new JarArchiveOutputStream(new FileOutputStream(file));
		if (launchScript != null) {
			this.jarOutputStream.writePreamble(launchScript.toByteArray());
			file.setExecutable(true);
		}
		this.jarOutputStream.setEncoding("UTF-8");
		this.lastModifiedTime = lastModifiedTime;
	}

	@Override
	protected void writeToArchive(ZipEntry entry, EntryWriter entryWriter) throws IOException {
		JarArchiveEntry jarEntry = asJarArchiveEntry(entry);
		if (this.lastModifiedTime != null) {
			jarEntry.setLastModifiedTime(this.lastModifiedTime);
		}
		this.jarOutputStream.putArchiveEntry(jarEntry);
		if (entryWriter != null) {
			entryWriter.write(this.jarOutputStream);
		}
		this.jarOutputStream.closeArchiveEntry();
	}

	private JarArchiveEntry asJarArchiveEntry(ZipEntry entry) throws ZipException {
		if (entry instanceof JarArchiveEntry jarArchiveEntry) {
			return jarArchiveEntry;
		}
		return new JarArchiveEntry(entry);
	}

	/**
	 * Close the writer.
	 * @throws IOException if the file cannot be closed
	 */
	@Override
	public void close() throws IOException {
		this.jarOutputStream.close();
	}

}
