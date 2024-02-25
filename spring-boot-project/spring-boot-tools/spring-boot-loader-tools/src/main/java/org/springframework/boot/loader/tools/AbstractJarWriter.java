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

package org.springframework.boot.loader.tools;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

	/**
     * Writes the entries of a JarFile to a destination, applying transformations and handling unpacking.
     * 
     * @param jarFile the JarFile to write entries from
     * @param entryTransformer the transformer to apply to each entry
     * @param unpackHandler the handler for unpacking entries
     * @param libraryLookup the function to lookup the library for each entry
     * @throws IOException if an I/O error occurs during the writing process
     */
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

	/**
     * Writes an entry to a JAR file.
     * 
     * @param jarFile          the JAR file to write the entry to
     * @param entryTransformer the transformer to apply to the entry before writing
     * @param unpackHandler    the handler for unpacking the entry
     * @param entry            the entry to write
     * @param library          the library associated with the entry
     * @throws IOException if an I/O error occurs while writing the entry
     */
    private void writeEntry(JarFile jarFile, EntryTransformer entryTransformer, UnpackHandler unpackHandler,
			JarArchiveEntry entry, Library library) throws IOException {
		setUpEntry(jarFile, entry);
		try (ZipHeaderPeekInputStream inputStream = new ZipHeaderPeekInputStream(jarFile.getInputStream(entry))) {
			EntryWriter entryWriter = new InputStreamEntryWriter(inputStream);
			JarArchiveEntry transformedEntry = entryTransformer.transform(entry);
			if (transformedEntry != null) {
				writeEntry(transformedEntry, library, entryWriter, unpackHandler);
			}
		}
	}

	/**
     * Sets up the given JarArchiveEntry by checking if it has a valid zip header and if its compression method is not stored.
     * If the entry has a valid zip header and is not stored, it sets up the entry's compressed size and CRC.
     * If the entry does not have a valid zip header or is stored, it sets the entry's compressed size to -1.
     *
     * @param jarFile The JarFile containing the entry.
     * @param entry   The JarArchiveEntry to be set up.
     * @throws IOException If an I/O error occurs while reading the entry's input stream.
     */
    private void setUpEntry(JarFile jarFile, JarArchiveEntry entry) throws IOException {
		try (ZipHeaderPeekInputStream inputStream = new ZipHeaderPeekInputStream(jarFile.getInputStream(entry))) {
			if (inputStream.hasZipHeader() && entry.getMethod() != ZipEntry.STORED) {
				new CrcAndSize(inputStream).setupStoredEntry(entry);
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
		new CrcAndSize(library::openStream).setupStoredEntry(entry);
		try (InputStream inputStream = library.openStream()) {
			writeEntry(entry, library, new InputStreamEntryWriter(inputStream), new LibraryUnpackHandler(library));
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

	/**
     * Retrieves the timestamp of the first non-directory entry in the nested library.
     * 
     * @param library the library to retrieve the timestamp from
     * @return the timestamp of the first non-directory entry in the nested library, or the last modified timestamp of the library if an exception occurs
     */
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

	/**
     * Writes the loader classes using the default loader implementation.
     * 
     * @throws IOException if an I/O error occurs while writing the loader classes
     */
    @Override
	public void writeLoaderClasses() throws IOException {
		writeLoaderClasses(LoaderImplementation.DEFAULT);
	}

	/**
     * Writes the loader classes for the given loader implementation.
     * 
     * @param loaderImplementation the loader implementation to write the classes for
     * @throws IOException if an I/O error occurs while writing the classes
     */
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

	/**
     * Checks if the given JarEntry is a directory entry.
     * 
     * @param entry the JarEntry to check
     * @return true if the entry is a directory entry and not the "META-INF/" directory, false otherwise
     */
    private boolean isDirectoryEntry(JarEntry entry) {
		return entry.isDirectory() && !entry.getName().equals("META-INF/");
	}

	/**
     * Checks if the given JarEntry is a class entry.
     * 
     * @param entry the JarEntry to be checked
     * @return true if the JarEntry is a class entry, false otherwise
     */
    private boolean isClassEntry(JarEntry entry) {
		return entry.getName().endsWith(".class");
	}

	/**
     * Checks if the given JarEntry is a services entry.
     * 
     * @param entry the JarEntry to be checked
     * @return true if the entry is a services entry, false otherwise
     */
    private boolean isServicesEntry(JarEntry entry) {
		return !entry.isDirectory() && entry.getName().startsWith("META-INF/services/");
	}

	/**
     * Writes a JarArchiveEntry to the Jar file using the specified EntryWriter.
     * 
     * @param entry the JarArchiveEntry to be written
     * @param entryWriter the EntryWriter to use for writing the entry
     * @throws IOException if an I/O error occurs while writing the entry
     */
    private void writeEntry(JarArchiveEntry entry, EntryWriter entryWriter) throws IOException {
		writeEntry(entry, null, entryWriter, UnpackHandler.NEVER);
	}

	/**
	 * Perform the actual write of a {@link JarEntry}. All other write methods delegate to
	 * this one.
	 * @param entry the entry to write
	 * @param library the library for the entry or {@code null}
	 * @param entryWriter the entry writer or {@code null} if there is no content
	 * @param unpackHandler handles possible unpacking for the entry
	 * @throws IOException in case of I/O errors
	 */
	private void writeEntry(JarArchiveEntry entry, Library library, EntryWriter entryWriter,
			UnpackHandler unpackHandler) throws IOException {
		String name = entry.getName();
		if (this.writtenEntries.add(name)) {
			writeParentDirectoryEntries(name);
			entry.setUnixMode(name.endsWith("/") ? UNIX_DIR_MODE : UNIX_FILE_MODE);
			entry.getGeneralPurposeBit().useUTF8ForNames(true);
			if (!entry.isDirectory() && entry.getSize() == -1) {
				entryWriter = SizeCalculatingEntryWriter.get(entryWriter);
				entry.setSize(entryWriter.size());
			}
			entryWriter = addUnpackCommentIfNecessary(entry, entryWriter, unpackHandler);
			updateLayerIndex(entry, library);
			writeToArchive(entry, entryWriter);
		}
	}

	/**
     * Updates the layer index for the given JarArchiveEntry and Library.
     * 
     * @param entry   the JarArchiveEntry to update the layer index for
     * @param library the Library associated with the entry (can be null)
     */
    private void updateLayerIndex(JarArchiveEntry entry, Library library) {
		if (this.layers != null && !entry.getName().endsWith("/")) {
			Layer layer = (library != null) ? this.layers.getLayer(library) : this.layers.getLayer(entry.getName());
			this.layersIndex.add(layer, entry.getName());
		}
	}

	/**
     * Writes the specified entry to the archive using the provided entry writer.
     *
     * @param entry the zip entry to be written to the archive
     * @param entryWriter the entry writer used to write the entry
     * @throws IOException if an I/O error occurs while writing the entry
     */
    protected abstract void writeToArchive(ZipEntry entry, EntryWriter entryWriter) throws IOException;

	/**
     * Writes the parent directory entries for the given name.
     * 
     * @param name the name of the directory
     * @throws IOException if an I/O error occurs
     */
    private void writeParentDirectoryEntries(String name) throws IOException {
		String parent = name.endsWith("/") ? name.substring(0, name.length() - 1) : name;
		while (parent.lastIndexOf('/') != -1) {
			parent = parent.substring(0, parent.lastIndexOf('/'));
			if (!parent.isEmpty()) {
				writeEntry(new JarArchiveEntry(parent + "/"), null, null, UnpackHandler.NEVER);
			}
		}
	}

	/**
     * Adds an unpack comment to the given entry if necessary.
     * 
     * @param entry         the JarArchiveEntry to add the comment to
     * @param entryWriter   the EntryWriter for the entry
     * @param unpackHandler the UnpackHandler to check if unpacking is required
     * @return the updated EntryWriter with the unpack comment added, or the original EntryWriter if no unpacking is required
     * @throws IOException if an I/O error occurs while writing the entry
     */
    private EntryWriter addUnpackCommentIfNecessary(JarArchiveEntry entry, EntryWriter entryWriter,
			UnpackHandler unpackHandler) throws IOException {
		if (entryWriter == null || !unpackHandler.requiresUnpack(entry.getName())) {
			return entryWriter;
		}
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		entryWriter.write(output);
		entry.setComment("UNPACK:" + unpackHandler.sha1Hash(entry.getName()));
		return new InputStreamEntryWriter(new ByteArrayInputStream(output.toByteArray()));
	}

	/**
	 * {@link EntryWriter} that writes content from an {@link InputStream}.
	 */
	private static class InputStreamEntryWriter implements EntryWriter {

		private final InputStream inputStream;

		/**
         * Constructs a new InputStreamEntryWriter with the specified input stream.
         *
         * @param inputStream the input stream to be written
         */
        InputStreamEntryWriter(InputStream inputStream) {
			this.inputStream = inputStream;
		}

		/**
         * Writes the contents of the input stream to the specified output stream.
         *
         * @param outputStream the output stream to write the contents to
         * @throws IOException if an I/O error occurs while reading from the input stream or writing to the output stream
         */
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
	 * Data holder for CRC and Size.
	 */
	private static class CrcAndSize {

		private final CRC32 crc = new CRC32();

		private long size;

		/**
         * Calculates the CRC and size of the input stream.
         * 
         * @param supplier the supplier of the input stream
         * @throws IOException if an I/O error occurs while reading the input stream
         */
        CrcAndSize(InputStreamSupplier supplier) throws IOException {
			try (InputStream inputStream = supplier.openStream()) {
				load(inputStream);
			}
		}

		/**
         * Calculates the CRC (Cyclic Redundancy Check) and size of the given input stream.
         * 
         * @param inputStream the input stream to calculate CRC and size for
         * @throws IOException if an I/O error occurs while reading the input stream
         */
        CrcAndSize(InputStream inputStream) throws IOException {
			load(inputStream);
		}

		/**
         * Loads data from the given input stream and updates the CRC and size.
         * 
         * @param inputStream the input stream to read data from
         * @throws IOException if an I/O error occurs while reading from the input stream
         */
        private void load(InputStream inputStream) throws IOException {
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				this.crc.update(buffer, 0, bytesRead);
				this.size += bytesRead;
			}
		}

		/**
         * Sets up the stored entry for the given JarArchiveEntry.
         * 
         * @param entry the JarArchiveEntry to set up
         */
        void setupStoredEntry(JarArchiveEntry entry) {
			entry.setSize(this.size);
			entry.setCompressedSize(this.size);
			entry.setCrc(this.crc.getValue());
			entry.setMethod(ZipEntry.STORED);
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

		UnpackHandler NEVER = new UnpackHandler() {

			/**
     * Determines if the given file name requires unpacking.
     * 
     * @param name the name of the file
     * @return true if the file requires unpacking, false otherwise
     */
    @Override
			public boolean requiresUnpack(String name) {
				return false;
			}

			/**
     * Calculates the SHA1 hash of the given name.
     *
     * @param name the name to calculate the SHA1 hash for
     * @return the SHA1 hash of the given name
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedOperationException if the operation is not supported
     */
    @Override
			public String sha1Hash(String name) throws IOException {
				throw new UnsupportedOperationException();
			}

		};

		boolean requiresUnpack(String name);

		String sha1Hash(String name) throws IOException;

	}

	/**
	 * {@link UnpackHandler} backed by a {@link Library}.
	 */
	private static final class LibraryUnpackHandler implements UnpackHandler {

		private final Library library;

		/**
         * Constructs a new LibraryUnpackHandler with the specified Library.
         * 
         * @param library the Library to be used by the handler
         */
        private LibraryUnpackHandler(Library library) {
			this.library = library;
		}

		/**
         * Determines if the given name requires unpacking.
         * 
         * @param name the name of the file or resource
         * @return true if unpacking is required, false otherwise
         */
        @Override
		public boolean requiresUnpack(String name) {
			return this.library.isUnpackRequired();
		}

		/**
         * Calculates the SHA1 hash of a given file.
         * 
         * @param name the name of the file to calculate the hash for
         * @return the SHA1 hash of the file
         * @throws IOException if an I/O error occurs while reading the file
         */
        @Override
		public String sha1Hash(String name) throws IOException {
			return Digest.sha1(this.library::openStream);
		}

	}

}
