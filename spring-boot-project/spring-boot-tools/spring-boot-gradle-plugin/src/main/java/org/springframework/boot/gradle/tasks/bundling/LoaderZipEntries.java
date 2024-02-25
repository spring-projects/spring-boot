/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.gradle.tasks.bundling;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.gradle.api.file.FileTreeElement;

import org.springframework.boot.loader.tools.LoaderImplementation;
import org.springframework.util.StreamUtils;

/**
 * Internal utility used to copy entries from the {@code spring-boot-loader.jar}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class LoaderZipEntries {

	private final LoaderImplementation loaderImplementation;

	private final Long entryTime;

	private final int dirMode;

	private final int fileMode;

	/**
     * Creates a new instance of LoaderZipEntries with the specified parameters.
     * 
     * @param entryTime the entry time to set for the LoaderZipEntries
     * @param dirMode the directory mode to set for the LoaderZipEntries
     * @param fileMode the file mode to set for the LoaderZipEntries
     * @param loaderImplementation the loader implementation to set for the LoaderZipEntries
     */
    LoaderZipEntries(Long entryTime, int dirMode, int fileMode, LoaderImplementation loaderImplementation) {
		this.entryTime = entryTime;
		this.dirMode = dirMode;
		this.fileMode = fileMode;
		this.loaderImplementation = (loaderImplementation != null) ? loaderImplementation
				: LoaderImplementation.DEFAULT;
	}

	/**
     * Writes the entries from the loader jar file to the specified ZipArchiveOutputStream.
     * 
     * @param out the ZipArchiveOutputStream to write the entries to
     * @return a WrittenEntries object containing the directories and files written
     * @throws IOException if an I/O error occurs while writing the entries
     */
    WrittenEntries writeTo(ZipArchiveOutputStream out) throws IOException {
		WrittenEntries written = new WrittenEntries();
		try (ZipInputStream loaderJar = new ZipInputStream(
				getClass().getResourceAsStream("/" + this.loaderImplementation.getJarResourceName()))) {
			java.util.zip.ZipEntry entry = loaderJar.getNextEntry();
			while (entry != null) {
				if (entry.isDirectory() && !entry.getName().equals("META-INF/")) {
					writeDirectory(new ZipArchiveEntry(entry), out);
					written.addDirectory(entry);
				}
				else if (entry.getName().endsWith(".class") || entry.getName().startsWith("META-INF/services/")) {
					writeFile(new ZipArchiveEntry(entry), loaderJar, out);
					written.addFile(entry);
				}
				entry = loaderJar.getNextEntry();
			}
		}
		return written;
	}

	/**
     * Writes a directory entry to the specified ZipArchiveOutputStream.
     * 
     * @param entry The ZipArchiveEntry representing the directory to be written.
     * @param out The ZipArchiveOutputStream to write the directory entry to.
     * @throws IOException If an I/O error occurs while writing the directory entry.
     */
    private void writeDirectory(ZipArchiveEntry entry, ZipArchiveOutputStream out) throws IOException {
		prepareEntry(entry, this.dirMode);
		out.putArchiveEntry(entry);
		out.closeArchiveEntry();
	}

	/**
     * Writes a file to a ZipArchiveOutputStream.
     * 
     * @param entry The ZipArchiveEntry representing the file to be written.
     * @param in The ZipInputStream used to read the file data.
     * @param out The ZipArchiveOutputStream used to write the file data.
     * @throws IOException If an I/O error occurs while writing the file.
     */
    private void writeFile(ZipArchiveEntry entry, ZipInputStream in, ZipArchiveOutputStream out) throws IOException {
		prepareEntry(entry, this.fileMode);
		out.putArchiveEntry(entry);
		copy(in, out);
		out.closeArchiveEntry();
	}

	/**
     * Prepares a ZipArchiveEntry by setting the entry time and Unix mode.
     * 
     * @param entry The ZipArchiveEntry to be prepared.
     * @param unixMode The Unix mode to be set for the entry.
     */
    private void prepareEntry(ZipArchiveEntry entry, int unixMode) {
		if (this.entryTime != null) {
			entry.setTime(DefaultTimeZoneOffset.INSTANCE.removeFrom(this.entryTime));
		}
		entry.setUnixMode(unixMode);
	}

	/**
     * Copies the content from the input stream to the output stream.
     *
     * @param in the input stream to read from
     * @param out the output stream to write to
     * @throws IOException if an I/O error occurs during the copying process
     */
    private void copy(InputStream in, OutputStream out) throws IOException {
		StreamUtils.copy(in, out);
	}

	/**
	 * Tracks entries that have been written.
	 */
	static class WrittenEntries {

		private final Set<String> directories = new LinkedHashSet<>();

		private final Set<String> files = new LinkedHashSet<>();

		/**
         * Adds a directory to the list of directories in the WrittenEntries class.
         * 
         * @param entry the ZipEntry representing the directory to be added
         */
        private void addDirectory(ZipEntry entry) {
			this.directories.add(entry.getName());
		}

		/**
         * Adds a file to the list of files in the WrittenEntries class.
         * 
         * @param entry the ZipEntry object representing the file to be added
         */
        private void addFile(ZipEntry entry) {
			this.files.add(entry.getName());
		}

		/**
         * Checks if the given FileTreeElement represents a written directory.
         * 
         * @param element the FileTreeElement to check
         * @return true if the element is a written directory, false otherwise
         */
        boolean isWrittenDirectory(FileTreeElement element) {
			String path = element.getRelativePath().getPathString();
			if (element.isDirectory() && !path.endsWith(("/"))) {
				path += "/";
			}
			return this.directories.contains(path);
		}

		/**
         * Returns the set of files.
         *
         * @return the set of files
         */
        Set<String> getFiles() {
			return this.files;
		}

	}

}
