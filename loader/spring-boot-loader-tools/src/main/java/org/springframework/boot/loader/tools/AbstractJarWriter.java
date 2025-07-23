/*
 * Copyright 2012-present the original author or authors.
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

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.zip.UnixStat;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;

/**
 * Abstract base class for JAR writers.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @since 2.3.0
 */
public abstract class AbstractJarWriter implements LoaderClassesWriter {

	private static final int BUFFER_SIZE = 32 * 1024;

	private static final int UNIX_FILE_MODE = UnixStat.FILE_FLAG | UnixStat.DEFAULT_FILE_PERM;

	private static final int UNIX_DIR_MODE = UnixStat.DIR_FLAG | UnixStat.DEFAULT_DIR_PERM;

	private final Set<String> writtenEntries = new HashSet<>();

	private Layers layers;

	private LayersIndex layersIndex;

	/**
	 * Update this writer to use specific layers.
	 * @param layers the layers to use
	 * @param layersIndex the layers index to update
	 */
	void useLayers(Layers layers, LayersIndex layersIndex) {
		this.layers = layers;
		this.layersIndex = layersIndex;
	}

	/**
	 * Write the specified manifest.
	 * @param manifest the manifest to write
	 * @throws IOException of the manifest cannot be written
	 */
	public void writeManifest(Manifest manifest) throws IOException {
		JarArchiveEntry entry = new JarArchiveEntry("META-INF/MANIFEST.MF");
		writeEntry(entry, manifest::write);
	}

	final void writeEntries(JarFile jarFile, EntryTransformer entryTransformer, UnpackHandler unpackHandler,
			Function<JarEntry, Library> libraryLookup) throws IOException {
		Enumeration<JarEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			Library library = libraryLookup.apply(entry);
			if (library == null || library.isIncluded()) {
				writeEntry(jarFile, entryTransformer, unpackHandler, new JarArchiveEntry(entry), library);
			}
		}
	}

	private void writeEntry(JarFile jarFile, EntryTransformer entryTransformer, UnpackHandler unpackHandler,
			JarArchiveEntry entry, Library library) throws IOException {
		setUpEntry(jarFile, entry, unpackHandler);
		try (ZipHeaderPeekInputStream inputStream = new ZipHeaderPeekInputStream(jarFile.getInputStream(entry))) {
			EntryWriter entryWriter = new InputStreamEntryWriter(inputStream);
			JarArchiveEntry transformedEntry = entryTransformer.transform(entry);
			if (transformedEntry != null) {
				writeEntry(transformedEntry, library, entryWriter);
			}
		}
	}

	private void setUpEntry(JarFile jarFile, JarArchiveEntry entry, UnpackHandler unpackHandler) throws IOException {
		try (ZipHeaderPeekInputStream inputStream = new ZipHeaderPeekInputStream(jarFile.getInputStream(entry))) {
			if (inputStream.hasZipHeader() && entry.getMethod() != ZipEntry.STORED) {
				new StoredEntryPreparator(inputStream, unpackHandler.requiresUnpack(entry.getName()))
					.prepareStoredEntry(entry);
			}
			else {
				entry.setCompressedSize(-1);
			}
		}
	}

	/**
	 * Writes an entry. The {@code inputStream} is closed once the entry has been written
	 * @param entryName the name of the entry
	 * @param inputStream the stream from which the entry's data can be read
	 * @throws IOException if the write fails
	 */
	@Override
	public void writeEntry(String entryName, InputStream inputStream) throws IOException {
		try (inputStream) {
			writeEntry(entryName, new InputStreamEntryWriter(inputStream));
		}
	}

	/**
	 * Writes an entry. The {@code inputStream} is closed once the entry has been written
	 * @param entryName the name of the entry
	 * @param entryWriter the entry writer
	 * @throws IOException if the write fails
	 */
	public void writeEntry(String entryName, EntryWriter entryWriter) throws IOException {
		JarArchiveEntry entry = new JarArchiveEntry(entryName);
		writeEntry(entry, entryWriter);
	}

	/**
	 * Write a nested library.
	 * @param location the destination of the library
	 * @param library the library
	 * @throws IOException if the write fails
	 */
	public void writeNestedLibrary(String location, Library library) throws IOException {
		JarArchiveEntry entry = new JarArchiveEntry(location + library.getName());
		entry.setTime(getNestedLibraryTime(library));
		new StoredEntryPreparator(library.openStream(), new LibraryUnpackHandler(library).requiresUnpack(location))
			.prepareStoredEntry(entry);
		try (InputStream inputStream = library.openStream()) {
			writeEntry(entry, library, new InputStreamEntryWriter(inputStream));
		}
	}

	/**
	 * Write a simple index file containing the specified UTF-8 lines.
	 * @param location the location of the index file
	 * @param lines the lines to write
	 * @throws IOException if the write fails
	 * @since 2.3.0
	 */
	public void writeIndexFile(String location, Collection<String> lines) throws IOException {
		if (location != null) {
			JarArchiveEntry entry = new JarArchiveEntry(location);
			writeEntry(entry, (outputStream) -> {
				BufferedWriter writer = new BufferedWriter(
						new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
				for (String line : lines) {
					writer.write(line);
					writer.write("\n");
				}
				writer.flush();
			});
		}
	}

	private long getNestedLibraryTime(Library library) {
		try {
			try (JarInputStream jarStream = new JarInputStream(library.openStream())) {
				JarEntry entry = jarStream.getNextJarEntry();
				while (entry != null) {
					if (!entry.isDirectory()) {
						return entry.getTime();
					}
					entry = jarStream.getNextJarEntry();
				}
			}
		}
		catch (Exception ex) {
			// Ignore and just use the library timestamp
		}
		return library.getLastModified();
	}

	@Override
	public void writeLoaderClasses() throws IOException {
		writeLoaderClasses(LoaderImplementation.DEFAULT);
	}

	@Override
	public void writeLoaderClasses(LoaderImplementation loaderImplementation) throws IOException {
		writeLoaderClasses((loaderImplementation != null) ? loaderImplementation.getJarResourceName()
				: LoaderImplementation.DEFAULT.getJarResourceName());
	}

	/**
	 * Write the required spring-boot-loader classes to the JAR.
	 * @param loaderJarResourceName the name of the resource containing the loader classes
	 * to be written
	 * @throws IOException if the classes cannot be written
	 */
	@Override
	public void writeLoaderClasses(String loaderJarResourceName) throws IOException {
		URL loaderJar = getClass().getClassLoader().getResource(loaderJarResourceName);
		try (JarInputStream inputStream = new JarInputStream(new BufferedInputStream(loaderJar.openStream()))) {
			JarEntry entry;
			while ((entry = inputStream.getNextJarEntry()) != null) {
				if (isDirectoryEntry(entry) || isClassEntry(entry) || isServicesEntry(entry)) {
					writeEntry(new JarArchiveEntry(entry), new InputStreamEntryWriter(inputStream));
				}
			}
		}
	}

	private boolean isDirectoryEntry(JarEntry entry) {
		return entry.isDirectory() && !entry.getName().equals("META-INF/");
	}

	private boolean isClassEntry(JarEntry entry) {
		return entry.getName().endsWith(".class");
	}

	private boolean isServicesEntry(JarEntry entry) {
		return !entry.isDirectory() && entry.getName().startsWith("META-INF/services/");
	}

	private void writeEntry(JarArchiveEntry entry, EntryWriter entryWriter) throws IOException {
		writeEntry(entry, null, entryWriter);
	}

	/**
	 * Perform the actual write of a {@link JarEntry}. All other write methods delegate to
	 * this one.
	 * @param entry the entry to write
	 * @param library the library for the entry or {@code null}
	 * @param entryWriter the entry writer or {@code null} if there is no content
	 * @throws IOException in case of I/O errors
	 */
	private void writeEntry(JarArchiveEntry entry, Library library, EntryWriter entryWriter) throws IOException {
		String name = entry.getName();
		if (this.writtenEntries.add(name)) {
			writeParentDirectoryEntries(name);
			entry.setUnixMode(name.endsWith("/") ? UNIX_DIR_MODE : UNIX_FILE_MODE);
			entry.getGeneralPurposeBit().useUTF8ForNames(true);
			if (!entry.isDirectory() && entry.getSize() == -1) {
				entryWriter = SizeCalculatingEntryWriter.get(entryWriter);
				entry.setSize(entryWriter.size());
			}
			updateLayerIndex(entry, library);
			writeToArchive(entry, entryWriter);
		}
	}

	private void updateLayerIndex(JarArchiveEntry entry, Library library) {
		if (this.layers != null && !entry.getName().endsWith("/")) {
			Layer layer = (library != null) ? this.layers.getLayer(library) : this.layers.getLayer(entry.getName());
			this.layersIndex.add(layer, entry.getName());
		}
	}

	protected abstract void writeToArchive(ZipEntry entry, EntryWriter entryWriter) throws IOException;

	private void writeParentDirectoryEntries(String name) throws IOException {
		String parent = name.endsWith("/") ? name.substring(0, name.length() - 1) : name;
		while (parent.lastIndexOf('/') != -1) {
			parent = parent.substring(0, parent.lastIndexOf('/'));
			if (!parent.isEmpty()) {
				writeEntry(new JarArchiveEntry(parent + "/"), null, null);
			}
		}
	}

	/**
	 * {@link EntryWriter} that writes content from an {@link InputStream}.
	 */
	private static class InputStreamEntryWriter implements EntryWriter {

		private final InputStream inputStream;

		InputStreamEntryWriter(InputStream inputStream) {
			this.inputStream = inputStream;
		}

		@Override
		public void write(OutputStream outputStream) throws IOException {
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead;
			while ((bytesRead = this.inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}
			outputStream.flush();
		}

	}

	/**
	 * Prepares a {@link ZipEntry#STORED stored} {@link ZipArchiveEntry entry} with CRC
	 * and size information. Also adds an {@code UNPACK} comment, if needed.
	 */
	private static class StoredEntryPreparator {

		private static final int BUFFER_SIZE = 32 * 1024;

		private final boolean unpack;

		private final CRC32 crc = new CRC32();

		private long size;

		StoredEntryPreparator(InputStream inputStream, boolean unpack) throws IOException {
			this.unpack = unpack;
			try (inputStream) {
				load(inputStream);
			}
		}

		private void load(InputStream inputStream) throws IOException {
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				this.crc.update(buffer, 0, bytesRead);
				this.size += bytesRead;
			}
		}

		void prepareStoredEntry(ZipArchiveEntry entry) {
			entry.setSize(this.size);
			entry.setCompressedSize(this.size);
			entry.setCrc(this.crc.getValue());
			entry.setMethod(ZipEntry.STORED);
			if (this.unpack) {
				entry.setComment("UNPACK:");
			}
		}

	}

	/**
	 * An {@code EntryTransformer} enables the transformation of {@link JarEntry jar
	 * entries} during the writing process.
	 */
	@FunctionalInterface
	interface EntryTransformer {

		/**
		 * No-op entity transformer.
		 */
		EntryTransformer NONE = (jarEntry) -> jarEntry;

		JarArchiveEntry transform(JarArchiveEntry jarEntry);

	}

	/**
	 * An {@code UnpackHandler} determines whether unpacking is required and provides a
	 * SHA-1 hash if required.
	 */
	interface UnpackHandler {

		UnpackHandler NEVER = (name) -> false;

		boolean requiresUnpack(String name);

	}

	/**
	 * {@link UnpackHandler} backed by a {@link Library}.
	 */
	private static final class LibraryUnpackHandler implements UnpackHandler {

		private final Library library;

		private LibraryUnpackHandler(Library library) {
			this.library = library;
		}

		@Override
		public boolean requiresUnpack(String name) {
			return this.library.isUnpackRequired();
		}

	}

}
