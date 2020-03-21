/*
 * Copyright 2012-2020 the original author or authors.
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
 * @since 2.3.0
 */
public class ImagePackager extends Packager {

	/**
	 * Create a new {@link ImagePackager} instance.
	 * @param source the source file to package
	 */
	public ImagePackager(File source) {
		super(source, null);
	}

	/**
	 * Create an packaged image.
	 * @param libraries the contained libraries
	 * @param exporter the exporter used to write the image
	 * @throws IOException on IO error
	 */
	public void packageImage(Libraries libraries, BiConsumer<ZipEntry, EntryWriter> exporter) throws IOException {
		packageImage(libraries, new DelegatingJarWriter(exporter));
	}

	private void packageImage(Libraries libraries, AbstractJarWriter writer) throws IOException {
		File source = isAlreadyPackaged() ? getBackupFile() : getSource();
		Assert.state(source.exists() && source.isFile(), () -> "Unable to read jar file " + source);
		Assert.state(!isAlreadyPackaged(source), () -> "Repackaged jar file " + source + " cannot be exported");
		try (JarFile sourceJar = new JarFile(source)) {
			write(sourceJar, libraries, writer);
		}
	}

	/**
	 * {@link AbstractJarWriter} that delegates to a {@link BiConsumer}.
	 */
	private static class DelegatingJarWriter extends AbstractJarWriter {

		private BiConsumer<ZipEntry, EntryWriter> exporter;

		DelegatingJarWriter(BiConsumer<ZipEntry, EntryWriter> exporter) {
			this.exporter = exporter;
		}

		@Override
		protected void writeToArchive(ZipEntry entry, EntryWriter entryWriter) throws IOException {
			this.exporter.accept(entry, entryWriter);
		}

	}

}
