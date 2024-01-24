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

	public InputStream getRawZipDataInputStream() throws IOException {
		RawZipDataInputStream inputStream = new RawZipDataInputStream(
				this.resources.zipContent().openRawZipData().asInputStream());
		this.resources.addInputStream(inputStream);
		return inputStream;
	}

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

	@Override
	public Enumeration<JarEntry> entries() {
		synchronized (this) {
			ensureOpen();
			return new JarEntriesEnumeration(this.resources.zipContent());
		}
	}

	@Override
	public Stream<JarEntry> stream() {
		synchronized (this) {
			ensureOpen();
			return streamContentEntries().map(NestedJarEntry::new);
		}
	}

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

	private Stream<ZipContent.Entry> streamContentEntries() {
		ZipContentEntriesSpliterator spliterator = new ZipContentEntriesSpliterator(this.resources.zipContent());
		return StreamSupport.stream(spliterator, false);
	}

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

	@Override
	public JarEntry getJarEntry(String name) {
		return getNestedJarEntry(name);
	}

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

	private ZipContent.Entry getContentEntry(String namePrefix, String name) {
		synchronized (this) {
			ensureOpen();
			return this.resources.zipContent().getEntry(namePrefix, name);
		}
	}

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

	@Override
	public InputStream getInputStream(ZipEntry entry) throws IOException {
		Objects.requireNonNull(entry, "entry");
		if (entry instanceof NestedJarEntry nestedJarEntry && nestedJarEntry.isOwnedBy(this)) {
			return getInputStream(nestedJarEntry.contentEntry());
		}
		return getInputStream(getNestedJarEntry(entry.getName()).contentEntry());
	}

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

	@Override
	public String getComment() {
		synchronized (this) {
			ensureOpen();
			return this.resources.zipContent().getComment();
		}
	}

	@Override
	public int size() {
		synchronized (this) {
			ensureOpen();
			return this.resources.zipContent().size();
		}
	}

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

	@Override
	public String getName() {
		return this.name;
	}

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

		NestedJarEntry(Entry contentEntry) {
			this(contentEntry, contentEntry.getName());
		}

		NestedJarEntry(ZipContent.Entry contentEntry, String name) {
			super(contentEntry.getName());
			this.contentEntry = contentEntry;
			this.name = name;
		}

		@Override
		public long getTime() {
			populate();
			return super.getTime();
		}

		@Override
		public LocalDateTime getTimeLocal() {
			populate();
			return super.getTimeLocal();
		}

		@Override
		public void setTime(long time) {
			throw CANNOT_BE_MODIFIED_EXCEPTION;
		}

		@Override
		public void setTimeLocal(LocalDateTime time) {
			throw CANNOT_BE_MODIFIED_EXCEPTION;
		}

		@Override
		public FileTime getLastModifiedTime() {
			populate();
			return super.getLastModifiedTime();
		}

		@Override
		public ZipEntry setLastModifiedTime(FileTime time) {
			throw CANNOT_BE_MODIFIED_EXCEPTION;
		}

		@Override
		public FileTime getLastAccessTime() {
			populate();
			return super.getLastAccessTime();
		}

		@Override
		public ZipEntry setLastAccessTime(FileTime time) {
			throw CANNOT_BE_MODIFIED_EXCEPTION;
		}

		@Override
		public FileTime getCreationTime() {
			populate();
			return super.getCreationTime();
		}

		@Override
		public ZipEntry setCreationTime(FileTime time) {
			throw CANNOT_BE_MODIFIED_EXCEPTION;
		}

		@Override
		public long getSize() {
			return this.contentEntry.getUncompressedSize() & 0xFFFFFFFFL;
		}

		@Override
		public void setSize(long size) {
			throw CANNOT_BE_MODIFIED_EXCEPTION;
		}

		@Override
		public long getCompressedSize() {
			populate();
			return super.getCompressedSize();
		}

		@Override
		public void setCompressedSize(long csize) {
			throw CANNOT_BE_MODIFIED_EXCEPTION;
		}

		@Override
		public long getCrc() {
			populate();
			return super.getCrc();
		}

		@Override
		public void setCrc(long crc) {
			throw CANNOT_BE_MODIFIED_EXCEPTION;
		}

		@Override
		public int getMethod() {
			populate();
			return super.getMethod();
		}

		@Override
		public void setMethod(int method) {
			throw CANNOT_BE_MODIFIED_EXCEPTION;
		}

		@Override
		public byte[] getExtra() {
			populate();
			return super.getExtra();
		}

		@Override
		public void setExtra(byte[] extra) {
			throw CANNOT_BE_MODIFIED_EXCEPTION;
		}

		@Override
		public String getComment() {
			populate();
			return super.getComment();
		}

		@Override
		public void setComment(String comment) {
			throw CANNOT_BE_MODIFIED_EXCEPTION;
		}

		boolean isOwnedBy(NestedJarFile nestedJarFile) {
			return NestedJarFile.this == nestedJarFile;
		}

		@Override
		public String getRealName() {
			return super.getName();
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public Attributes getAttributes() throws IOException {
			Manifest manifest = getManifest();
			return (manifest != null) ? manifest.getAttributes(getName()) : null;
		}

		@Override
		public Certificate[] getCertificates() {
			return getSecurityInfo().getCertificates(contentEntry());
		}

		@Override
		public CodeSigner[] getCodeSigners() {
			return getSecurityInfo().getCodeSigners(contentEntry());
		}

		private SecurityInfo getSecurityInfo() {
			return NestedJarFile.this.resources.zipContent().getInfo(SecurityInfo.class, SecurityInfo::get);
		}

		ZipContent.Entry contentEntry() {
			return this.contentEntry;
		}

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

		JarEntriesEnumeration(ZipContent zipContent) {
			this.zipContent = zipContent;
		}

		@Override
		public boolean hasMoreElements() {
			return this.cursor < this.zipContent.size();
		}

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

		ZipContentEntriesSpliterator(ZipContent zipContent) {
			super(zipContent.size(), ADDITIONAL_CHARACTERISTICS);
			this.zipContent = zipContent;
		}

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

		JarEntryInputStream(ZipContent.Entry entry) throws IOException {
			this.uncompressedSize = entry.getUncompressedSize();
			this.content = entry.openContent();
		}

		@Override
		public int read() throws IOException {
			byte[] b = new byte[1];
			return (read(b, 0, 1) == 1) ? b[0] & 0xFF : -1;
		}

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

		private long maxForwardSkip(long n) {
			boolean willCauseOverflow = (this.pos + n) < 0;
			return (willCauseOverflow || n > this.remaining) ? this.remaining : n;
		}

		private long maxBackwardSkip(long n) {
			return Math.max(-this.pos, n);
		}

		@Override
		public int available() {
			return (this.remaining < Integer.MAX_VALUE) ? (int) this.remaining : Integer.MAX_VALUE;
		}

		private void ensureOpen() throws ZipException {
			if (NestedJarFile.this.closed || this.closed) {
				throw new ZipException("ZipFile closed");
			}
		}

		@Override
		public void close() throws IOException {
			if (this.closed) {
				return;
			}
			this.closed = true;
			this.content.close();
			NestedJarFile.this.resources.removeInputStream(this);
		}

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

		JarEntryInflaterInputStream(JarEntryInputStream inputStream, NestedJarFileResources resources) {
			this(inputStream, resources, resources.getOrCreateInflater());
		}

		private JarEntryInflaterInputStream(JarEntryInputStream inputStream, NestedJarFileResources resources,
				Inflater inflater) {
			super(inputStream, inflater, inputStream.getUncompressedSize());
			this.cleanup = NestedJarFile.this.cleaner.register(this, resources.createInflatorCleanupAction(inflater));
		}

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

		RawZipDataInputStream(InputStream in) {
			super(in);
		}

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
