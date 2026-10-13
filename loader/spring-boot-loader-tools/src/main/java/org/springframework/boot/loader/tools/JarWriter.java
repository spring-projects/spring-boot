/*
 * Copyright 2012-present the original author or authors.
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
import org.jspecify.annotations.Nullable;

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

	private final @Nullable FileTime lastModifiedTime;

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
	 * @param lastModifiedTime an optional last modified time to apply to the written
	 * entries
	 * @throws IOException if the file cannot be opened
	 * @throws FileNotFoundException if the file cannot be found
	 * @since 4.0.0
	 */
	public JarWriter(File file, @Nullable FileTime lastModifiedTime) throws FileNotFoundException, IOException {
		this.jarOutputStream = new JarArchiveOutputStream(new FileOutputStream(file));
		this.jarOutputStream.setEncoding("UTF-8");
		this.lastModifiedTime = lastModifiedTime;
	}

	@Override
	protected void writeToArchive(ZipEntry entry, @Nullable EntryWriter entryWriter) throws IOException {
		JarArchiveEntry jarEntry = asJarArchiveEntry(entry);
		if (this.lastModifiedTime != null) {
			jarEntry.setTime(DefaultTimeZoneOffset.INSTANCE.removeFrom(this.lastModifiedTime).toMillis());
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
