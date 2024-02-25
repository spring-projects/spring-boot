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
import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.springframework.util.Assert;

/**
 * Utility class that can be used to export a fully packaged archive to an OCI image.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 2.3.0
 */
public class ImagePackager extends Packager {

	/**
	 * Create a new {@link ImagePackager} instance.
	 * @param source the source file to package
	 * @param backupFile the backup of the source file to package
	 */
	public ImagePackager(File source, File backupFile) {
		super(source);
		setBackupFile(backupFile);
		if (isAlreadyPackaged()) {
			Assert.isTrue(getBackupFile().exists() && getBackupFile().isFile(),
					"Original source '" + getBackupFile() + "' is required for building an image");
			Assert.state(!isAlreadyPackaged(getBackupFile()),
					() -> "Repackaged archive file " + source + " cannot be used to build an image");
		}
	}

	/**
	 * Create a packaged image.
	 * @param libraries the contained libraries
	 * @param exporter the exporter used to write the image
	 * @throws IOException on IO error
	 */
	public void packageImage(Libraries libraries, BiConsumer<ZipEntry, EntryWriter> exporter) throws IOException {
		packageImage(libraries, new DelegatingJarWriter(exporter));
	}

	/**
     * Packages the image by creating a JAR file with the specified libraries and using the provided writer.
     * If the image is already packaged, a backup file is used instead of the original source file.
     * 
     * @param libraries The libraries to include in the JAR file.
     * @param writer The writer to use for creating the JAR file.
     * @throws IOException If an I/O error occurs during the packaging process.
     */
    private void packageImage(Libraries libraries, AbstractJarWriter writer) throws IOException {
		File source = isAlreadyPackaged() ? getBackupFile() : getSource();
		try (JarFile sourceJar = new JarFile(source)) {
			write(sourceJar, libraries, writer);
		}
	}

	/**
	 * {@link AbstractJarWriter} that delegates to a {@link BiConsumer}.
	 */
	private static class DelegatingJarWriter extends AbstractJarWriter {

		private final BiConsumer<ZipEntry, EntryWriter> exporter;

		/**
         * Constructs a new DelegatingJarWriter with the specified exporter.
         *
         * @param exporter the exporter to be used for writing entries to the JAR file
         */
        DelegatingJarWriter(BiConsumer<ZipEntry, EntryWriter> exporter) {
			this.exporter = exporter;
		}

		/**
         * Writes the given ZipEntry to the archive using the provided EntryWriter.
         * 
         * @param entry The ZipEntry to be written to the archive.
         * @param entryWriter The EntryWriter used to write the ZipEntry.
         * @throws IOException If an I/O error occurs while writing the ZipEntry.
         */
        @Override
		protected void writeToArchive(ZipEntry entry, EntryWriter entryWriter) throws IOException {
			this.exporter.accept(entry, entryWriter);
		}

	}

}
