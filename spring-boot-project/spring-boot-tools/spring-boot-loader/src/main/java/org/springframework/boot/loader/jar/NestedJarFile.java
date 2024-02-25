/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.loader.jar;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.ref.Cleaner.Cleanable;
import java.nio.ByteBuffer;
import java.nio.file.attribute.FileTime;
import java.security.CodeSigner;
import java.security.cert.Certificate;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import org.springframework.boot.loader.log.DebugLogger;
import org.springframework.boot.loader.ref.Cleaner;
import org.springframework.boot.loader.zip.CloseableDataBlock;
import org.springframework.boot.loader.zip.ZipContent;
import org.springframework.boot.loader.zip.ZipContent.Entry;

/**
 * Extended variant of {@link JarFile} that behaves in the same way but can open nested
 * jars.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 3.2.0
 */
public class NestedJarFile extends JarFile {

	private static final int DECIMAL = 10;

	private static final String META_INF = "META-INF/";

	static final String META_INF_VERSIONS = META_INF + "versions/";

	static final int BASE_VERSION = baseVersion().feature();

	private static final DebugLogger debug = DebugLogger.get(NestedJarFile.class);

	private final Cleaner cleaner;

	private final NestedJarFileResources resources;

	private final Cleanable cleanup;

	private final String name;

	private final int version;

	private volatile NestedJarEntry lastEntry;

	private volatile boolean closed;

	private volatile ManifestInfo manifestInfo;

	private volatile MetaInfVersionsInfo metaInfVersionsInfo;

	/**
	 * Creates a new {@link NestedJarFile} instance to read from the specific
	 * {@code File}.
	 * @param file the jar file to be opened for reading
	 * @throws IOException on I/O error
	 */
	NestedJarFile(File file) throws IOException {
		this(file, null, null, false, Cleaner.instance);
	}

	/**
	 * Creates a new {@link NestedJarFile} instance to read from the specific
	 * {@code File}.
	 * @param file the jar file to be opened for reading
	 * @param nestedEntryName the nested entry name to open
	 * @throws IOException on I/O error
	 * @throws IllegalArgumentException if {@code nestedEntryName} is {@code null} or
	 * empty
	 */
	public NestedJarFile(File file, String nestedEntryName) throws IOException {
		this(file, nestedEntryName, null, true, Cleaner.instance);
	}

	/**
	 * Creates a new {@link NestedJarFile} instance to read from the specific
	 * {@code File}.
	 * @param file the jar file to be opened for reading
	 * @param nestedEntryName the nested entry name to open
	 * @param version the release version to use when opening a multi-release jar
	 * @throws IOException on I/O error
	 * @throws IllegalArgumentException if {@code nestedEntryName} is {@code null} or
	 * empty
	 */
	public NestedJarFile(File file, String nestedEntryName, Runtime.Version version) throws IOException {
		this(file, nestedEntryName, version, true, Cleaner.instance);
	}

	/**
	 * Creates a new {@link NestedJarFile} instance to read from the specific
	 * {@code File}.
	 * @param file the jar file to be opened for reading
	 * @param nestedEntryName the nested entry name to open
	 * @param version the release version to use when opening a multi-release jar
	 * @param onlyNestedJars if <em>only</em> nested jars should be opened
	 * @param cleaner the cleaner used to release resources
	 * @throws IOException on I/O error
	 * @throws IllegalArgumentException if {@code nestedEntryName} is {@code null} or
	 * empty
	 */
	NestedJarFile(File file, String nestedEntryName, Runtime.Version version, boolean onlyNestedJars, Cleaner cleaner)
			throws IOException {
		super(file);
		if (onlyNestedJars && (nestedEntryName == null || nestedEntryName.isEmpty())) {
			throw new IllegalArgumentException("nestedEntryName must not be empty");
		}
		debug.log("Created nested jar file (%s, %s, %s)", file, nestedEntryName, version);
		this.cleaner = cleaner;
		this.resources = new NestedJarFileResources(file, nestedEntryName);
		this.cleanup = cleaner.register(this, this.resources);
		this.name = file.getPath() + ((nestedEntryName != null) ? "!/" + nestedEntryName : "");
		this.version = (version != null) ? version.feature() : baseVersion().feature();
	}

	/**
     * Returns an InputStream for the raw zip data.
     * 
     * @return the InputStream for the raw zip data
     * @throws IOException if an I/O error occurs while opening the raw zip data
     */
    public InputStream getRawZipDataInputStream() throws IOException {
		RawZipDataInputStream inputStream = new RawZipDataInputStream(
				this.resources.zipContent().openRawZipData().asInputStream());
		this.resources.addInputStream(inputStream);
		return inputStream;
	}

	/**
     * Retrieves the manifest of the nested JAR file.
     * 
     * @return The manifest of the nested JAR file.
     * @throws IOException If an I/O error occurs while retrieving the manifest.
     */
    @Override
	public Manifest getManifest() throws IOException {
		try {
			return this.resources.zipContentForManifest()
				.getInfo(ManifestInfo.class, this::getManifestInfo)
				.getManifest();
		}
		catch (UncheckedIOException ex) {
			throw ex.getCause();
		}
	}

	/**
     * Returns an enumeration of the entries in this nested JAR file.
     * 
     * @return an enumeration of the entries in this nested JAR file
     * @throws IllegalStateException if the nested JAR file is not open
     */
    @Override
	public Enumeration<JarEntry> entries() {
		synchronized (this) {
			ensureOpen();
			return new JarEntriesEnumeration(this.resources.zipContent());
		}
	}

	/**
     * Returns a stream of JarEntry objects representing the entries in this NestedJarFile.
     * 
     * <p>
     * The stream is obtained by first ensuring that the NestedJarFile is open, and then
     * streaming the content entries of the NestedJarFile. Each content entry is then
     * wrapped in a NestedJarEntry object before being returned in the stream.
     * </p>
     * 
     * <p>
     * This method is synchronized to ensure thread safety when accessing the NestedJarFile.
     * </p>
     * 
     * @return a stream of JarEntry objects representing the entries in this NestedJarFile
     * 
     * @throws IllegalStateException if the NestedJarFile is not open
     */
    @Override
	public Stream<JarEntry> stream() {
		synchronized (this) {
			ensureOpen();
			return streamContentEntries().map(NestedJarEntry::new);
		}
	}

	/**
     * Returns a stream of versioned JarEntries.
     * 
     * <p>
     * This method returns a stream of JarEntries that represent the versioned content entries in the NestedJarFile.
     * The stream is obtained by first obtaining a stream of all content entries in the NestedJarFile using the
     * {@link #streamContentEntries()} method. Then, the base name of each content entry is extracted using the
     * {@link #getBaseName(JarEntry)} method. The base names are filtered to remove any null values using the
     * {@link Objects#nonNull(Object)} method. The distinct base names are then mapped to their corresponding JarEntries
     * using the {@link #getJarEntry(String)} method. Finally, any null JarEntries are filtered out using the
     * {@link Objects#nonNull(Object)} method.
     * </p>
     * 
     * <p>
     * This method is synchronized to ensure thread safety when accessing the NestedJarFile. It first checks if the
     * NestedJarFile is open using the {@link #ensureOpen()} method. If it is open, the stream of versioned JarEntries is
     * returned. Otherwise, an IllegalStateException is thrown.
     * </p>
     * 
     * @return a stream of versioned JarEntries
     * @throws IllegalStateException if the NestedJarFile is not open
     */
    @Override
	public Stream<JarEntry> versionedStream() {
		synchronized (this) {
			ensureOpen();
			return streamContentEntries().map(this::getBaseName)
				.filter(Objects::nonNull)
				.distinct()
				.map(this::getJarEntry)
				.filter(Objects::nonNull);
		}
	}

	/**
     * Returns a stream of ZipContent.Entry objects representing the entries in the nested jar file.
     *
     * @return a stream of ZipContent.Entry objects
     */
    private Stream<ZipContent.Entry> streamContentEntries() {
		ZipContentEntriesSpliterator spliterator = new ZipContentEntriesSpliterator(this.resources.zipContent());
		return StreamSupport.stream(spliterator, false);
	}

	/**
     * Returns the base name of the given ZipContent.Entry.
     * 
     * @param contentEntry the ZipContent.Entry to get the base name from
     * @return the base name of the ZipContent.Entry, or null if the entry is not valid
     */
    private String getBaseName(ZipContent.Entry contentEntry) {
		String name = contentEntry.getName();
		if (!name.startsWith(META_INF_VERSIONS)) {
			return name;
		}
		int versionNumberStartIndex = META_INF_VERSIONS.length();
		int versionNumberEndIndex = (versionNumberStartIndex != -1) ? name.indexOf('/', versionNumberStartIndex) : -1;
		if (versionNumberEndIndex == -1 || versionNumberEndIndex == (name.length() - 1)) {
			return null;
		}
		try {
			int versionNumber = Integer.parseInt(name, versionNumberStartIndex, versionNumberEndIndex, DECIMAL);
			if (versionNumber > this.version) {
				return null;
			}
		}
		catch (NumberFormatException ex) {
			return null;
		}
		return name.substring(versionNumberEndIndex + 1);
	}

	/**
     * Retrieves the JarEntry with the specified name from the nested jar file.
     * 
     * @param name the name of the JarEntry to retrieve
     * @return the JarEntry with the specified name, or null if not found
     */
    @Override
	public JarEntry getJarEntry(String name) {
		return getNestedJarEntry(name);
	}

	/**
     * Returns the JarEntry object for the specified entry name.
     * This method is overridden from the parent class and delegates the task to the getNestedJarEntry method.
     * 
     * @param name the name of the entry to retrieve
     * @return the JarEntry object for the specified entry name
     */
    @Override
	public JarEntry getEntry(String name) {
		return getNestedJarEntry(name);
	}

	/**
	 * Return if an entry with the given name exists.
	 * @param name the name to check
	 * @return if the entry exists
	 */
	public boolean hasEntry(String name) {
		NestedJarEntry lastEntry = this.lastEntry;
		if (lastEntry != null && name.equals(lastEntry.getName())) {
			return true;
		}
		ZipContent.Entry entry = getVersionedContentEntry(name);
		if (entry != null) {
			return true;
		}
		synchronized (this) {
			ensureOpen();
			return this.resources.zipContent().hasEntry(null, name);
		}
	}

	/**
     * Retrieves the NestedJarEntry with the specified name.
     * 
     * @param name the name of the NestedJarEntry to retrieve
     * @return the NestedJarEntry with the specified name, or null if not found
     * @throws NullPointerException if the name is null
     */
    private NestedJarEntry getNestedJarEntry(String name) {
		Objects.requireNonNull(name, "name");
		NestedJarEntry lastEntry = this.lastEntry;
		if (lastEntry != null && name.equals(lastEntry.getName())) {
			return lastEntry;
		}
		ZipContent.Entry entry = getVersionedContentEntry(name);
		entry = (entry != null) ? entry : getContentEntry(null, name);
		if (entry == null) {
			return null;
		}
		NestedJarEntry nestedJarEntry = new NestedJarEntry(entry, name);
		this.lastEntry = nestedJarEntry;
		return nestedJarEntry;
	}

	/**
     * Returns the versioned content entry with the given name.
     * 
     * @param name the name of the entry
     * @return the versioned content entry, or null if not found
     */
    private ZipContent.Entry getVersionedContentEntry(String name) {
		// NOTE: we can't call isMultiRelease() directly because it's a final method and
		// it inspects the container jar. We use ManifestInfo instead.
		if (BASE_VERSION >= this.version || name.startsWith(META_INF) || !getManifestInfo().isMultiRelease()) {
			return null;
		}
		MetaInfVersionsInfo metaInfVersionsInfo = getMetaInfVersionsInfo();
		int[] versions = metaInfVersionsInfo.versions();
		String[] directories = metaInfVersionsInfo.directories();
		for (int i = versions.length - 1; i >= 0; i--) {
			if (versions[i] <= this.version) {
				ZipContent.Entry entry = getContentEntry(directories[i], name);
				if (entry != null) {
					return entry;
				}
			}
		}
		return null;
	}

	/**
     * Retrieves the content entry with the specified name prefix and name from the nested JAR file.
     * 
     * @param namePrefix the prefix of the entry name
     * @param name the name of the entry
     * @return the content entry with the specified name prefix and name
     * @throws IllegalStateException if the nested JAR file is not open
     */
    private ZipContent.Entry getContentEntry(String namePrefix, String name) {
		synchronized (this) {
			ensureOpen();
			return this.resources.zipContent().getEntry(namePrefix, name);
		}
	}

	/**
     * Retrieves the manifest information for this NestedJarFile.
     * 
     * @return The ManifestInfo object containing the manifest information.
     */
    private ManifestInfo getManifestInfo() {
		ManifestInfo manifestInfo = this.manifestInfo;
		if (manifestInfo != null) {
			return manifestInfo;
		}
		synchronized (this) {
			ensureOpen();
			manifestInfo = this.resources.zipContent().getInfo(ManifestInfo.class, this::getManifestInfo);
		}
		this.manifestInfo = manifestInfo;
		return manifestInfo;
	}

	/**
     * Retrieves the manifest information from the given ZipContent.
     * 
     * @param zipContent The ZipContent from which to retrieve the manifest information.
     * @return The ManifestInfo object containing the manifest information, or ManifestInfo.NONE if the manifest is not found.
     * @throws UncheckedIOException If an IOException occurs while reading the manifest.
     */
    private ManifestInfo getManifestInfo(ZipContent zipContent) {
		ZipContent.Entry contentEntry = zipContent.getEntry(MANIFEST_NAME);
		if (contentEntry == null) {
			return ManifestInfo.NONE;
		}
		try {
			try (InputStream inputStream = getInputStream(contentEntry)) {
				Manifest manifest = new Manifest(inputStream);
				return new ManifestInfo(manifest);
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/**
     * Retrieves the MetaInfVersionsInfo object for this NestedJarFile.
     * 
     * @return The MetaInfVersionsInfo object.
     */
    private MetaInfVersionsInfo getMetaInfVersionsInfo() {
		MetaInfVersionsInfo metaInfVersionsInfo = this.metaInfVersionsInfo;
		if (metaInfVersionsInfo != null) {
			return metaInfVersionsInfo;
		}
		synchronized (this) {
			ensureOpen();
			metaInfVersionsInfo = this.resources.zipContent()
				.getInfo(MetaInfVersionsInfo.class, MetaInfVersionsInfo::get);
		}
		this.metaInfVersionsInfo = metaInfVersionsInfo;
		return metaInfVersionsInfo;
	}

	/**
     * Returns an input stream for reading the contents of the specified zip entry.
     * 
     * @param entry the zip entry to get the input stream for
     * @return an input stream for reading the contents of the specified zip entry
     * @throws IOException if an I/O error occurs while creating the input stream
     * @throws NullPointerException if the entry is null
     */
    @Override
	public InputStream getInputStream(ZipEntry entry) throws IOException {
		Objects.requireNonNull(entry, "entry");
		if (entry instanceof NestedJarEntry nestedJarEntry && nestedJarEntry.isOwnedBy(this)) {
			return getInputStream(nestedJarEntry.contentEntry());
		}
		return getInputStream(getNestedJarEntry(entry.getName()).contentEntry());
	}

	/**
     * Retrieves an input stream for the specified content entry in the nested JAR file.
     * 
     * @param contentEntry the content entry for which to retrieve the input stream
     * @return the input stream for the specified content entry
     * @throws IOException if an I/O error occurs while retrieving the input stream
     * @throws ZipException if the compression method of the content entry is invalid
     */
    private InputStream getInputStream(ZipContent.Entry contentEntry) throws IOException {
		int compression = contentEntry.getCompressionMethod();
		if (compression != ZipEntry.STORED && compression != ZipEntry.DEFLATED) {
			throw new ZipException("invalid compression method");
		}
		synchronized (this) {
			ensureOpen();
			InputStream inputStream = new JarEntryInputStream(contentEntry);
			try {
				if (compression == ZipEntry.DEFLATED) {
					inputStream = new JarEntryInflaterInputStream((JarEntryInputStream) inputStream, this.resources);
				}
				this.resources.addInputStream(inputStream);
				return inputStream;
			}
			catch (RuntimeException ex) {
				inputStream.close();
				throw ex;
			}
		}
	}

	/**
     * Returns the comment associated with the nested JAR file.
     * 
     * @return the comment of the nested JAR file
     * @throws IllegalStateException if the nested JAR file is not open
     */
    @Override
	public String getComment() {
		synchronized (this) {
			ensureOpen();
			return this.resources.zipContent().getComment();
		}
	}

	/**
     * Returns the size of the NestedJarFile.
     * 
     * @return the size of the NestedJarFile
     * @throws IllegalStateException if the NestedJarFile is not open
     */
    @Override
	public int size() {
		synchronized (this) {
			ensureOpen();
			return this.resources.zipContent().size();
		}
	}

	/**
     * Closes the NestedJarFile and releases any system resources associated with it.
     * This method also performs cleanup operations before closing the file.
     * 
     * @throws IOException if an I/O error occurs while closing the file
     */
    @Override
	public void close() throws IOException {
		super.close();
		if (this.closed) {
			return;
		}
		this.closed = true;
		synchronized (this) {
			try {
				this.cleanup.clean();
			}
			catch (UncheckedIOException ex) {
				throw ex.getCause();
			}
		}
	}

	/**
     * Returns the name of the NestedJarFile.
     *
     * @return the name of the NestedJarFile
     */
    @Override
	public String getName() {
		return this.name;
	}

	/**
     * Ensures that the NestedJarFile is open and initialized.
     * 
     * @throws IllegalStateException if the NestedJarFile is closed or not initialized
     */
    private void ensureOpen() {
		if (this.closed) {
			throw new IllegalStateException("Zip file closed");
		}
		if (this.resources.zipContent() == null) {
			throw new IllegalStateException("The object is not initialized.");
		}
	}

	/**
	 * Clear any internal caches.
	 */
	public void clearCache() {
		synchronized (this) {
			this.lastEntry = null;
		}
	}

	/**
	 * An individual entry from a {@link NestedJarFile}.
	 */
	private class NestedJarEntry extends java.util.jar.JarEntry {

		private static final IllegalStateException CANNOT_BE_MODIFIED_EXCEPTION = new IllegalStateException(
				"Neste jar entries cannot be modified");

		private final ZipContent.Entry contentEntry;

		private final String name;

		private volatile boolean populated;

		/**
         * Constructs a new NestedJarEntry object with the specified content entry and name.
         * 
         * @param contentEntry the entry representing the content of the nested jar
         */
        NestedJarEntry(Entry contentEntry) {
			this(contentEntry, contentEntry.getName());
		}

		/**
         * Constructs a new NestedJarEntry with the specified content entry and name.
         * 
         * @param contentEntry the content entry of the nested jar
         * @param name the name of the nested jar entry
         */
        NestedJarEntry(ZipContent.Entry contentEntry, String name) {
			super(contentEntry.getName());
			this.contentEntry = contentEntry;
			this.name = name;
		}

		/**
         * Returns the time at which this NestedJarEntry was last modified.
         * 
         * @return the time at which this NestedJarEntry was last modified, measured in milliseconds since the epoch (00:00:00 GMT, January 1, 1970)
         */
        @Override
		public long getTime() {
			populate();
			return super.getTime();
		}

		/**
         * Returns the local date and time of the NestedJarEntry.
         * 
         * @return the local date and time of the NestedJarEntry
         */
        @Override
		public LocalDateTime getTimeLocal() {
			populate();
			return super.getTimeLocal();
		}

		/**
         * Sets the time of the nested jar entry.
         * 
         * @param time the time to set for the nested jar entry
         * @throws UnsupportedOperationException if the nested jar entry cannot be modified
         */
        @Override
		public void setTime(long time) {
			throw CANNOT_BE_MODIFIED_EXCEPTION;
		}

		/**
         * Sets the local time of the NestedJarEntry.
         * 
         * @param time the local date and time to be set
         * @throws UnsupportedOperationException if the local time cannot be modified
         */
        @Override
		public void setTimeLocal(LocalDateTime time) {
			throw CANNOT_BE_MODIFIED_EXCEPTION;
		}

		/**
         * Returns the last modified time of the nested JAR entry.
         * <p>
         * This method first populates the necessary information about the nested JAR entry
         * and then returns the last modified time of the entry.
         * </p>
         *
         * @return the last modified time of the nested JAR entry
         */
        @Override
		public FileTime getLastModifiedTime() {
			populate();
			return super.getLastModifiedTime();
		}

		/**
         * Sets the last modified time of this ZipEntry to the specified FileTime.
         *
         * @param time the FileTime representing the new last modified time
         * @return the updated ZipEntry with the new last modified time
         * @throws UnsupportedOperationException if the last modified time cannot be modified
         */
        @Override
		public ZipEntry setLastModifiedTime(FileTime time) {
			throw CANNOT_BE_MODIFIED_EXCEPTION;
		}

		/**
         * Returns the last access time of the nested JAR entry.
         * 
         * @return the last access time of the nested JAR entry
         */
        @Override
		public FileTime getLastAccessTime() {
			populate();
			return super.getLastAccessTime();
		}

		/**
         * Sets the last access time of the nested jar entry.
         *
         * @param time the new last access time
         * @return the updated ZipEntry object
         * @throws UnsupportedOperationException if the nested jar entry cannot be modified
         */
        @Override
		public ZipEntry setLastAccessTime(FileTime time) {
			throw CANNOT_BE_MODIFIED_EXCEPTION;
		}

		/**
         * Returns the creation time of the nested JAR entry.
         * 
         * @return the creation time of the nested JAR entry
         */
        @Override
		public FileTime getCreationTime() {
			populate();
			return super.getCreationTime();
		}

		/**
         * Sets the creation time of this ZipEntry to the specified FileTime.
         * 
         * @param time the new creation time for this ZipEntry
         * @return the updated ZipEntry with the new creation time
         * @throws UnsupportedOperationException if the creation time cannot be modified
         */
        @Override
		public ZipEntry setCreationTime(FileTime time) {
			throw CANNOT_BE_MODIFIED_EXCEPTION;
		}

		/**
         * Returns the size of the NestedJarEntry.
         * 
         * @return the size of the NestedJarEntry
         */
        @Override
		public long getSize() {
			return this.contentEntry.getUncompressedSize() & 0xFFFFFFFFL;
		}

		/**
         * Sets the size of the nested jar entry.
         * 
         * @param size the size of the nested jar entry
         * @throws UnsupportedOperationException if the size of the nested jar entry cannot be modified
         */
        @Override
		public void setSize(long size) {
			throw CANNOT_BE_MODIFIED_EXCEPTION;
		}

		/**
         * Returns the compressed size of this nested JAR entry.
         * <p>
         * The compressed size is the size of the entry's data in its compressed form.
         * </p>
         * <p>
         * This method first populates the data of the nested JAR entry if it has not been done already,
         * and then returns the compressed size by calling the superclass's {@code getCompressedSize()} method.
         * </p>
         *
         * @return the compressed size of this nested JAR entry
         */
        @Override
		public long getCompressedSize() {
			populate();
			return super.getCompressedSize();
		}

		/**
         * Sets the compressed size of the nested jar entry.
         * 
         * @param csize the compressed size to be set
         * @throws UnsupportedOperationException if the compressed size cannot be modified
         */
        @Override
		public void setCompressedSize(long csize) {
			throw CANNOT_BE_MODIFIED_EXCEPTION;
		}

		/**
         * Returns the CRC (Cyclic Redundancy Check) value of the nested JAR entry.
         * <p>
         * This method populates the nested JAR entry before returning the CRC value.
         * </p>
         *
         * @return the CRC value of the nested JAR entry
         */
        @Override
		public long getCrc() {
			populate();
			return super.getCrc();
		}

		/**
         * Sets the CRC (Cyclic Redundancy Check) value for this NestedJarEntry.
         * 
         * @param crc the CRC value to be set
         * @throws UnsupportedOperationException if the CRC value cannot be modified
         */
        @Override
		public void setCrc(long crc) {
			throw CANNOT_BE_MODIFIED_EXCEPTION;
		}

		/**
         * Retrieves the method value.
         * 
         * @return The method value.
         */
        @Override
		public int getMethod() {
			populate();
			return super.getMethod();
		}

		/**
         * Sets the method for the NestedJarEntry.
         * 
         * @param method the method to be set
         * @throws CannotBeModifiedException if the method cannot be modified
         */
        @Override
		public void setMethod(int method) {
			throw CANNOT_BE_MODIFIED_EXCEPTION;
		}

		/**
         * Retrieves the extra data associated with this NestedJarEntry.
         * 
         * @return the extra data as a byte array
         */
        @Override
		public byte[] getExtra() {
			populate();
			return super.getExtra();
		}

		/**
         * Sets the extra data for this NestedJarEntry.
         * 
         * @param extra the extra data to be set
         * @throws UnsupportedOperationException if the extra data cannot be modified
         */
        @Override
		public void setExtra(byte[] extra) {
			throw CANNOT_BE_MODIFIED_EXCEPTION;
		}

		/**
         * Retrieves the comment associated with this NestedJarEntry.
         * 
         * @return the comment of this NestedJarEntry
         */
        @Override
		public String getComment() {
			populate();
			return super.getComment();
		}

		/**
         * Sets the comment for this NestedJarEntry.
         * 
         * @param comment the comment to be set
         * @throws UnsupportedOperationException if the comment cannot be modified
         */
        @Override
		public void setComment(String comment) {
			throw CANNOT_BE_MODIFIED_EXCEPTION;
		}

		/**
         * Checks if the NestedJarEntry is owned by the specified NestedJarFile.
         * 
         * @param nestedJarFile the NestedJarFile to compare with
         * @return true if the NestedJarEntry is owned by the specified NestedJarFile, false otherwise
         */
        boolean isOwnedBy(NestedJarFile nestedJarFile) {
			return NestedJarFile.this == nestedJarFile;
		}

		/**
         * Returns the real name of the NestedJarEntry.
         * 
         * @return the real name of the NestedJarEntry
         */
        @Override
		public String getRealName() {
			return super.getName();
		}

		/**
         * Returns the name of the NestedJarEntry.
         *
         * @return the name of the NestedJarEntry
         */
        @Override
		public String getName() {
			return this.name;
		}

		/**
         * Returns the attributes of the nested JAR entry.
         * 
         * @return the attributes of the nested JAR entry, or {@code null} if the manifest is not available
         * @throws IOException if an I/O error occurs while retrieving the manifest
         */
        @Override
		public Attributes getAttributes() throws IOException {
			Manifest manifest = getManifest();
			return (manifest != null) ? manifest.getAttributes(getName()) : null;
		}

		/**
         * Returns an array of certificates associated with this NestedJarEntry.
         * 
         * @return an array of certificates associated with this NestedJarEntry
         */
        @Override
		public Certificate[] getCertificates() {
			return getSecurityInfo().getCertificates(contentEntry());
		}

		/**
         * Returns an array of CodeSigner objects representing the signers of the nested jar entry.
         * 
         * @return an array of CodeSigner objects representing the signers of the nested jar entry
         */
        @Override
		public CodeSigner[] getCodeSigners() {
			return getSecurityInfo().getCodeSigners(contentEntry());
		}

		/**
         * Retrieves the security information of the nested jar file.
         *
         * @return the security information of the nested jar file
         */
        private SecurityInfo getSecurityInfo() {
			return NestedJarFile.this.resources.zipContent().getInfo(SecurityInfo.class, SecurityInfo::get);
		}

		/**
         * Returns the content entry of the nested jar entry.
         * 
         * @return the content entry of the nested jar entry
         */
        ZipContent.Entry contentEntry() {
			return this.contentEntry;
		}

		/**
         * Populates the NestedJarEntry object with the necessary information from the underlying ZipEntry.
         * If the object has already been populated, the method does nothing.
         * 
         * @throws IllegalStateException if the underlying ZipEntry is null
         */
        private void populate() {
			boolean populated = this.populated;
			if (!populated) {
				ZipEntry entry = this.contentEntry.as(ZipEntry::new);
				super.setMethod(entry.getMethod());
				super.setTime(entry.getTime());
				super.setCrc(entry.getCrc());
				super.setCompressedSize(entry.getCompressedSize());
				super.setSize(entry.getSize());
				super.setExtra(entry.getExtra());
				super.setComment(entry.getComment());
				this.populated = true;
			}
		}

	}

	/**
	 * {@link Enumeration} of {@link NestedJarEntry} instances.
	 */
	private class JarEntriesEnumeration implements Enumeration<JarEntry> {

		private final ZipContent zipContent;

		private int cursor;

		/**
         * Constructs a new JarEntriesEnumeration object with the specified ZipContent.
         * 
         * @param zipContent the ZipContent object to be used for enumeration
         */
        JarEntriesEnumeration(ZipContent zipContent) {
			this.zipContent = zipContent;
		}

		/**
         * Returns true if there are more elements in the enumeration, false otherwise.
         *
         * @return true if there are more elements, false otherwise
         */
        @Override
		public boolean hasMoreElements() {
			return this.cursor < this.zipContent.size();
		}

		/**
         * Returns the next element in the enumeration.
         * 
         * @return the next element in the enumeration
         * @throws NoSuchElementException if there are no more elements in the enumeration
         */
        @Override
		public NestedJarEntry nextElement() {
			if (!hasMoreElements()) {
				throw new NoSuchElementException();
			}
			synchronized (NestedJarFile.this) {
				ensureOpen();
				return new NestedJarEntry(this.zipContent.getEntry(this.cursor++));
			}
		}

	}

	/**
	 * {@link Spliterator} for {@link ZipContent.Entry} instances.
	 */
	private class ZipContentEntriesSpliterator extends AbstractSpliterator<ZipContent.Entry> {

		private static final int ADDITIONAL_CHARACTERISTICS = Spliterator.ORDERED | Spliterator.DISTINCT
				| Spliterator.IMMUTABLE | Spliterator.NONNULL;

		private final ZipContent zipContent;

		private int cursor;

		/**
         * Constructs a new ZipContentEntriesSpliterator with the specified ZipContent.
         * 
         * @param zipContent the ZipContent object to be used by the spliterator
         */
        ZipContentEntriesSpliterator(ZipContent zipContent) {
			super(zipContent.size(), ADDITIONAL_CHARACTERISTICS);
			this.zipContent = zipContent;
		}

		/**
         * Attempts to advance the spliterator to the next element and performs the given action on it.
         * 
         * @param action the action to be performed on the next element
         * @return {@code true} if there is a next element and the action was performed, {@code false} otherwise
         * @throws IllegalStateException if the spliterator is closed
         * @throws NullPointerException if the action is null
         */
        @Override
		public boolean tryAdvance(Consumer<? super ZipContent.Entry> action) {
			if (this.cursor < this.zipContent.size()) {
				synchronized (NestedJarFile.this) {
					ensureOpen();
					action.accept(this.zipContent.getEntry(this.cursor++));
				}
				return true;
			}
			return false;
		}

	}

	/**
	 * {@link InputStream} to read jar entry content.
	 */
	private class JarEntryInputStream extends InputStream {

		private final int uncompressedSize;

		private final CloseableDataBlock content;

		private long pos;

		private long remaining;

		private volatile boolean closed;

		/**
         * Constructs a new JarEntryInputStream object with the given ZipContent.Entry.
         * 
         * @param entry the ZipContent.Entry object representing the entry in the JAR file
         * @throws IOException if an I/O error occurs while opening the content of the entry
         */
        JarEntryInputStream(ZipContent.Entry entry) throws IOException {
			this.uncompressedSize = entry.getUncompressedSize();
			this.content = entry.openContent();
		}

		/**
         * Reads a single byte of data from the input stream.
         * 
         * @return the byte read, or -1 if the end of the stream is reached
         * @throws IOException if an I/O error occurs
         */
        @Override
		public int read() throws IOException {
			byte[] b = new byte[1];
			return (read(b, 0, 1) == 1) ? b[0] & 0xFF : -1;
		}

		/**
         * Reads up to len bytes of data from this input stream into an array of bytes.
         * 
         * @param b   the buffer into which the data is read.
         * @param off the start offset in the buffer at which the data is written.
         * @param len the maximum number of bytes to read.
         * @return the total number of bytes read into the buffer, or -1 if there is no more data because the end of the stream has been reached.
         * @throws IOException if an I/O error occurs.
         */
        @Override
		public int read(byte[] b, int off, int len) throws IOException {
			int result;
			synchronized (NestedJarFile.this) {
				ensureOpen();
				ByteBuffer dst = ByteBuffer.wrap(b, off, len);
				int count = this.content.read(dst, this.pos);
				if (count > 0) {
					this.pos += count;
					this.remaining -= count;
				}
				result = count;
			}
			if (this.remaining == 0) {
				close();
			}
			return result;
		}

		/**
         * Skips over and discards a specified number of bytes from the input stream.
         * 
         * @param n the number of bytes to be skipped
         * @return the actual number of bytes skipped
         * @throws IOException if an I/O error occurs
         */
        @Override
		public long skip(long n) throws IOException {
			long result;
			synchronized (NestedJarFile.this) {
				result = (n > 0) ? maxForwardSkip(n) : maxBackwardSkip(n);
				this.pos += result;
				this.remaining -= result;
			}
			if (this.remaining == 0) {
				close();
			}
			return result;
		}

		/**
         * Returns the maximum number of bytes that can be skipped forward in the input stream.
         * 
         * @param n the number of bytes to skip forward
         * @return the maximum number of bytes that can be skipped forward without causing an overflow or exceeding the remaining bytes in the input stream
         */
        private long maxForwardSkip(long n) {
			boolean willCauseOverflow = (this.pos + n) < 0;
			return (willCauseOverflow || n > this.remaining) ? this.remaining : n;
		}

		/**
         * Returns the maximum number of bytes that can be skipped backwards from the current position in the input stream.
         * 
         * @param n the number of bytes to skip backwards
         * @return the maximum number of bytes that can be skipped backwards
         */
        private long maxBackwardSkip(long n) {
			return Math.max(-this.pos, n);
		}

		/**
         * Returns the number of bytes that can be read from this input stream without blocking.
         * 
         * @return the number of bytes that can be read from this input stream without blocking,
         *         or {@link Integer#MAX_VALUE} if the remaining bytes is greater than {@link Integer#MAX_VALUE}.
         */
        @Override
		public int available() {
			return (this.remaining < Integer.MAX_VALUE) ? (int) this.remaining : Integer.MAX_VALUE;
		}

		/**
         * Ensures that the JarEntryInputStream is open.
         * 
         * @throws ZipException if the JarEntryInputStream or the enclosing NestedJarFile is closed
         */
        private void ensureOpen() throws ZipException {
			if (NestedJarFile.this.closed || this.closed) {
				throw new ZipException("ZipFile closed");
			}
		}

		/**
         * Closes the input stream.
         * 
         * @throws IOException if an I/O error occurs while closing the stream
         */
        @Override
		public void close() throws IOException {
			if (this.closed) {
				return;
			}
			this.closed = true;
			this.content.close();
			NestedJarFile.this.resources.removeInputStream(this);
		}

		/**
         * Returns the uncompressed size of the current entry in the JAR file.
         *
         * @return the uncompressed size of the current entry
         */
        int getUncompressedSize() {
			return this.uncompressedSize;
		}

	}

	/**
	 * {@link ZipInflaterInputStream} to read and inflate jar entry content.
	 */
	private class JarEntryInflaterInputStream extends ZipInflaterInputStream {

		private final Cleanable cleanup;

		private volatile boolean closed;

		/**
         * Constructs a new JarEntryInflaterInputStream with the specified JarEntryInputStream, NestedJarFileResources, and Inflater.
         * 
         * @param inputStream the JarEntryInputStream to read from
         * @param resources the NestedJarFileResources to use for accessing nested jar files
         * @param inflater the Inflater to use for decompressing data
         */
        JarEntryInflaterInputStream(JarEntryInputStream inputStream, NestedJarFileResources resources) {
			this(inputStream, resources, resources.getOrCreateInflater());
		}

		/**
         * Constructs a new JarEntryInflaterInputStream with the specified parameters.
         * 
         * @param inputStream the JarEntryInputStream to read from
         * @param resources the NestedJarFileResources associated with the input stream
         * @param inflater the Inflater to use for decompression
         */
        private JarEntryInflaterInputStream(JarEntryInputStream inputStream, NestedJarFileResources resources,
				Inflater inflater) {
			super(inputStream, inflater, inputStream.getUncompressedSize());
			this.cleanup = NestedJarFile.this.cleaner.register(this, resources.createInflatorCleanupAction(inflater));
		}

		/**
         * Closes the input stream.
         * 
         * @throws IOException if an I/O error occurs
         */
        @Override
		public void close() throws IOException {
			if (this.closed) {
				return;
			}
			this.closed = true;
			super.close();
			NestedJarFile.this.resources.removeInputStream(this);
			this.cleanup.clean();
		}

	}

	/**
	 * {@link InputStream} for raw zip data.
	 */
	private class RawZipDataInputStream extends FilterInputStream {

		private volatile boolean closed;

		/**
         * Constructs a new RawZipDataInputStream object with the specified input stream.
         * 
         * @param in the input stream to be read from
         */
        RawZipDataInputStream(InputStream in) {
			super(in);
		}

		/**
         * Closes the input stream.
         * 
         * @throws IOException if an I/O error occurs
         */
        @Override
		public void close() throws IOException {
			if (this.closed) {
				return;
			}
			this.closed = true;
			super.close();
			NestedJarFile.this.resources.removeInputStream(this);
		}

	}

}
