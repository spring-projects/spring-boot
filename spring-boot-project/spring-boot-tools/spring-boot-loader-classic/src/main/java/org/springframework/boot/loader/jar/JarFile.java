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

package org.springframework.boot.loader.jar;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.security.Permission;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Supplier;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;

import org.springframework.boot.loader.data.RandomAccessData;
import org.springframework.boot.loader.data.RandomAccessDataFile;

/**
 * Extended variant of {@link java.util.jar.JarFile} that behaves in the same way but
 * offers the following additional functionality.
 * <ul>
 * <li>A nested {@link JarFile} can be {@link #getNestedJarFile(ZipEntry) obtained} based
 * on any directory entry.</li>
 * <li>A nested {@link JarFile} can be {@link #getNestedJarFile(ZipEntry) obtained} for
 * embedded JAR files (as long as their entry is not compressed).</li>
 * </ul>
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.0.0
 */
public class JarFile extends AbstractJarFile implements Iterable<java.util.jar.JarEntry> {

	private static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";

	private static final String PROTOCOL_HANDLER = "java.protocol.handler.pkgs";

	private static final String HANDLERS_PACKAGE = "org.springframework.boot.loader";

	private static final AsciiBytes META_INF = new AsciiBytes("META-INF/");

	private static final AsciiBytes SIGNATURE_FILE_EXTENSION = new AsciiBytes(".SF");

	private static final String READ_ACTION = "read";

	private final RandomAccessDataFile rootFile;

	private final String pathFromRoot;

	private final RandomAccessData data;

	private final JarFileType type;

	private URL url;

	private String urlString;

	private final JarFileEntries entries;

	private final Supplier<Manifest> manifestSupplier;

	private SoftReference<Manifest> manifest;

	private boolean signed;

	private String comment;

	private volatile boolean closed;

	private volatile JarFileWrapper wrapper;

	/**
	 * Create a new {@link JarFile} backed by the specified file.
	 * @param file the root jar file
	 * @throws IOException if the file cannot be read
	 */
	public JarFile(File file) throws IOException {
		this(new RandomAccessDataFile(file));
	}

	/**
	 * Create a new {@link JarFile} backed by the specified file.
	 * @param file the root jar file
	 * @throws IOException if the file cannot be read
	 */
	JarFile(RandomAccessDataFile file) throws IOException {
		this(file, "", file, JarFileType.DIRECT);
	}

	/**
	 * Private constructor used to create a new {@link JarFile} either directly or from a
	 * nested entry.
	 * @param rootFile the root jar file
	 * @param pathFromRoot the name of this file
	 * @param data the underlying data
	 * @param type the type of the jar file
	 * @throws IOException if the file cannot be read
	 */
	private JarFile(RandomAccessDataFile rootFile, String pathFromRoot, RandomAccessData data, JarFileType type)
			throws IOException {
		this(rootFile, pathFromRoot, data, null, type, null);
	}

	/**
	 * Constructs a new JarFile instance.
	 * @param rootFile the RandomAccessDataFile representing the root file of the JAR
	 * @param pathFromRoot the path from the root file to the JAR file
	 * @param data the RandomAccessData representing the JAR file data
	 * @param filter the JarEntryFilter used to filter the JAR entries
	 * @param type the JarFileType indicating the type of JAR file
	 * @param manifestSupplier the Supplier used to supply the Manifest for the JAR file
	 * @throws IOException if an I/O error occurs while reading the JAR file
	 */
	private JarFile(RandomAccessDataFile rootFile, String pathFromRoot, RandomAccessData data, JarEntryFilter filter,
			JarFileType type, Supplier<Manifest> manifestSupplier) throws IOException {
		super(rootFile.getFile());
		super.close();
		this.rootFile = rootFile;
		this.pathFromRoot = pathFromRoot;
		CentralDirectoryParser parser = new CentralDirectoryParser();
		this.entries = parser.addVisitor(new JarFileEntries(this, filter));
		this.type = type;
		parser.addVisitor(centralDirectoryVisitor());
		try {
			this.data = parser.parse(data, filter == null);
		}
		catch (RuntimeException ex) {
			try {
				this.rootFile.close();
				super.close();
			}
			catch (IOException ioex) {
				// Ignore
			}
			throw ex;
		}
		this.manifestSupplier = (manifestSupplier != null) ? manifestSupplier : () -> {
			try (InputStream inputStream = getInputStream(MANIFEST_NAME)) {
				if (inputStream == null) {
					return null;
				}
				return new Manifest(inputStream);
			}
			catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		};
	}

	/**
	 * Returns a CentralDirectoryVisitor object that can be used to visit the central
	 * directory of a JAR file. The visitor provides methods to handle the start of the
	 * visit, file header visit, and end of the visit.
	 * @return a CentralDirectoryVisitor object
	 */
	private CentralDirectoryVisitor centralDirectoryVisitor() {
		return new CentralDirectoryVisitor() {

			@Override
			public void visitStart(CentralDirectoryEndRecord endRecord, RandomAccessData centralDirectoryData) {
				JarFile.this.comment = endRecord.getComment();
			}

			@Override
			public void visitFileHeader(CentralDirectoryFileHeader fileHeader, long dataOffset) {
				AsciiBytes name = fileHeader.getName();
				if (name.startsWith(META_INF) && name.endsWith(SIGNATURE_FILE_EXTENSION)) {
					JarFile.this.signed = true;
				}
			}

			@Override
			public void visitEnd() {
			}

		};
	}

	/**
	 * Returns the JarFileWrapper object associated with this JarFile.
	 * @return the JarFileWrapper object
	 * @throws IOException if an I/O error occurs while creating the JarFileWrapper
	 */
	JarFileWrapper getWrapper() throws IOException {
		JarFileWrapper wrapper = this.wrapper;
		if (wrapper == null) {
			wrapper = new JarFileWrapper(this);
			this.wrapper = wrapper;
		}
		return wrapper;
	}

	/**
	 * Returns the permission required to read the file associated with this JarFile.
	 * @return the FilePermission object representing the permission required to read the
	 * file
	 */
	@Override
	Permission getPermission() {
		return new FilePermission(this.rootFile.getFile().getPath(), READ_ACTION);
	}

	/**
	 * Returns the root jar file associated with this JarFile object.
	 * @return the root jar file
	 */
	protected final RandomAccessDataFile getRootJarFile() {
		return this.rootFile;
	}

	/**
	 * Retrieves the RandomAccessData object associated with this JarFile.
	 * @return the RandomAccessData object
	 */
	RandomAccessData getData() {
		return this.data;
	}

	/**
	 * Returns the manifest of this JarFile.
	 * @return the manifest of this JarFile
	 * @throws IOException if an I/O error occurs while reading the manifest
	 */
	@Override
	public Manifest getManifest() throws IOException {
		Manifest manifest = (this.manifest != null) ? this.manifest.get() : null;
		if (manifest == null) {
			try {
				manifest = this.manifestSupplier.get();
			}
			catch (RuntimeException ex) {
				throw new IOException(ex);
			}
			this.manifest = new SoftReference<>(manifest);
		}
		return manifest;
	}

	/**
	 * Returns an enumeration of the entries in this JarFile.
	 * @return an enumeration of the entries in this JarFile
	 */
	@Override
	public Enumeration<java.util.jar.JarEntry> entries() {
		return new JarEntryEnumeration(this.entries.iterator());
	}

	/**
	 * Returns a stream of JarEntry objects for this JarFile.
	 * @return a stream of JarEntry objects
	 */
	@Override
	public Stream<java.util.jar.JarEntry> stream() {
		Spliterator<java.util.jar.JarEntry> spliterator = Spliterators.spliterator(iterator(), size(),
				Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.IMMUTABLE | Spliterator.NONNULL);
		return StreamSupport.stream(spliterator, false);
	}

	/**
	 * Return an iterator for the contained entries.
	 * @since 2.3.0
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Iterator<java.util.jar.JarEntry> iterator() {
		return (Iterator) this.entries.iterator(this::ensureOpen);
	}

	/**
	 * Returns the JarEntry object for the specified entry name.
	 * @param name the name of the entry to retrieve
	 * @return the JarEntry object for the specified entry name, or null if not found
	 */
	public JarEntry getJarEntry(CharSequence name) {
		return this.entries.getEntry(name);
	}

	/**
	 * Returns a JarEntry object for the specified entry name.
	 * @param name the name of the entry
	 * @return the JarEntry object for the specified entry name, or null if not found
	 */
	@Override
	public JarEntry getJarEntry(String name) {
		return (JarEntry) getEntry(name);
	}

	/**
	 * Checks if the JarFile contains an entry with the specified name.
	 * @param name the name of the entry to check
	 * @return true if the JarFile contains an entry with the specified name, false
	 * otherwise
	 */
	public boolean containsEntry(String name) {
		return this.entries.containsEntry(name);
	}

	/**
	 * Returns a ZipEntry object for the specified entry name.
	 * @param name the name of the entry
	 * @return the ZipEntry object for the specified entry name
	 * @throws IllegalStateException if the JarFile is not open
	 */
	@Override
	public ZipEntry getEntry(String name) {
		ensureOpen();
		return this.entries.getEntry(name);
	}

	/**
	 * Returns an input stream for reading the contents of this JarFile entry.
	 * @return an input stream for reading the contents of this JarFile entry
	 * @throws IOException if an I/O error occurs while creating the input stream
	 */
	@Override
	InputStream getInputStream() throws IOException {
		return this.data.getInputStream();
	}

	/**
	 * Returns an input stream for reading the contents of the specified entry from this
	 * JarFile.
	 * @param entry the entry to retrieve the input stream for
	 * @return an input stream for reading the contents of the specified entry
	 * @throws IOException if an I/O error occurs while retrieving the input stream
	 */
	@Override
	public synchronized InputStream getInputStream(ZipEntry entry) throws IOException {
		ensureOpen();
		if (entry instanceof JarEntry jarEntry) {
			return this.entries.getInputStream(jarEntry);
		}
		return getInputStream((entry != null) ? entry.getName() : null);
	}

	/**
	 * Returns an input stream for reading the contents of the specified entry in this JAR
	 * file.
	 * @param name the name of the entry
	 * @return an input stream for reading the contents of the specified entry
	 * @throws IOException if an I/O error occurs while creating the input stream
	 */
	InputStream getInputStream(String name) throws IOException {
		return this.entries.getInputStream(name);
	}

	/**
	 * Return a nested {@link JarFile} loaded from the specified entry.
	 * @param entry the zip entry
	 * @return a {@link JarFile} for the entry
	 * @throws IOException if the nested jar file cannot be read
	 */
	public synchronized JarFile getNestedJarFile(ZipEntry entry) throws IOException {
		return getNestedJarFile((JarEntry) entry);
	}

	/**
	 * Return a nested {@link JarFile} loaded from the specified entry.
	 * @param entry the zip entry
	 * @return a {@link JarFile} for the entry
	 * @throws IOException if the nested jar file cannot be read
	 */
	public synchronized JarFile getNestedJarFile(JarEntry entry) throws IOException {
		try {
			return createJarFileFromEntry(entry);
		}
		catch (Exception ex) {
			throw new IOException("Unable to open nested jar file '" + entry.getName() + "'", ex);
		}
	}

	/**
	 * Creates a new JarFile object from the given JarEntry.
	 * @param entry the JarEntry to create the JarFile from
	 * @return a new JarFile object created from the given JarEntry
	 * @throws IOException if an I/O error occurs while creating the JarFile
	 */
	private JarFile createJarFileFromEntry(JarEntry entry) throws IOException {
		if (entry.isDirectory()) {
			return createJarFileFromDirectoryEntry(entry);
		}
		return createJarFileFromFileEntry(entry);
	}

	/**
	 * Creates a JarFile object from a directory entry.
	 * @param entry the JarEntry representing the directory entry
	 * @return a JarFile object representing the directory entry
	 * @throws IOException if an I/O error occurs while creating the JarFile
	 */
	private JarFile createJarFileFromDirectoryEntry(JarEntry entry) throws IOException {
		AsciiBytes name = entry.getAsciiBytesName();
		JarEntryFilter filter = (candidate) -> {
			if (candidate.startsWith(name) && !candidate.equals(name)) {
				return candidate.substring(name.length());
			}
			return null;
		};
		return new JarFile(this.rootFile, this.pathFromRoot + "!/" + entry.getName().substring(0, name.length() - 1),
				this.data, filter, JarFileType.NESTED_DIRECTORY, this.manifestSupplier);
	}

	/**
	 * Creates a JarFile object from a given JarEntry.
	 * @param entry the JarEntry to create the JarFile from
	 * @return the created JarFile object
	 * @throws IOException if an I/O error occurs while creating the JarFile
	 * @throws IllegalStateException if the entry is compressed and nested jar files must
	 * be stored without compression
	 */
	private JarFile createJarFileFromFileEntry(JarEntry entry) throws IOException {
		if (entry.getMethod() != ZipEntry.STORED) {
			throw new IllegalStateException(
					"Unable to open nested entry '" + entry.getName() + "'. It has been compressed and nested "
							+ "jar files must be stored without compression. Please check the "
							+ "mechanism used to create your executable jar file");
		}
		RandomAccessData entryData = this.entries.getEntryData(entry.getName());
		return new JarFile(this.rootFile, this.pathFromRoot + "!/" + entry.getName(), entryData,
				JarFileType.NESTED_JAR);
	}

	/**
	 * Returns the comment associated with this JarFile.
	 * @return the comment associated with this JarFile
	 * @throws IllegalStateException if the JarFile is closed
	 */
	@Override
	public String getComment() {
		ensureOpen();
		return this.comment;
	}

	/**
	 * Returns the number of entries in this JarFile.
	 * @return the number of entries in this JarFile
	 * @throws IllegalStateException if the JarFile is closed
	 */
	@Override
	public int size() {
		ensureOpen();
		return this.entries.getSize();
	}

	/**
	 * Closes the JarFile and releases any system resources associated with it. If the
	 * JarFile is already closed, this method has no effect.
	 * @throws IOException if an I/O error occurs while closing the JarFile
	 */
	@Override
	public void close() throws IOException {
		if (this.closed) {
			return;
		}
		super.close();
		if (this.type == JarFileType.DIRECT) {
			this.rootFile.close();
		}
		this.closed = true;
	}

	/**
	 * Ensures that the zip file is open.
	 * @throws IllegalStateException if the zip file is closed
	 */
	private void ensureOpen() {
		if (this.closed) {
			throw new IllegalStateException("zip file closed");
		}
	}

	/**
	 * Returns a boolean value indicating whether the JarFile is closed or not.
	 * @return true if the JarFile is closed, false otherwise
	 */
	boolean isClosed() {
		return this.closed;
	}

	/**
	 * Returns the URL string representation of the current URL.
	 * @return the URL string representation
	 * @throws MalformedURLException if the URL is malformed
	 */
	String getUrlString() throws MalformedURLException {
		if (this.urlString == null) {
			this.urlString = getUrl().toString();
		}
		return this.urlString;
	}

	/**
	 * Returns the URL of the Jar file.
	 * @return the URL of the Jar file
	 * @throws MalformedURLException if the URL is malformed
	 */
	@Override
	public URL getUrl() throws MalformedURLException {
		if (this.url == null) {
			String file = this.rootFile.getFile().toURI() + this.pathFromRoot + "!/";
			file = file.replace("file:////", "file://"); // Fix UNC paths
			this.url = new URL("jar", "", -1, file, new Handler(this));
		}
		return this.url;
	}

	/**
	 * Returns a string representation of the JarFile object.
	 * @return the name of the JarFile object
	 */
	@Override
	public String toString() {
		return getName();
	}

	/**
	 * Returns the name of the file represented by this JarFile object. The name is
	 * obtained by concatenating the root file name and the path from the root.
	 * @return the name of the file represented by this JarFile object
	 */
	@Override
	public String getName() {
		return this.rootFile.getFile() + this.pathFromRoot;
	}

	/**
	 * Returns a boolean value indicating whether the JarFile is signed.
	 * @return true if the JarFile is signed, false otherwise
	 */
	boolean isSigned() {
		return this.signed;
	}

	/**
	 * Retrieves the certification for the specified JarEntry.
	 * @param entry the JarEntry for which to retrieve the certification
	 * @return the certification for the specified JarEntry
	 * @throws IllegalStateException if an IOException occurs while retrieving the
	 * certification
	 */
	JarEntryCertification getCertification(JarEntry entry) {
		try {
			return this.entries.getCertification(entry);
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * Clears the cache of the JarFile. This method removes all entries from the cache.
	 */
	public void clearCache() {
		this.entries.clearCache();
	}

	/**
	 * Returns the path from the root directory of the JAR file.
	 * @return the path from the root directory of the JAR file
	 */
	protected String getPathFromRoot() {
		return this.pathFromRoot;
	}

	/**
	 * Returns the type of the Jar file.
	 * @return the type of the Jar file
	 */
	@Override
	JarFileType getType() {
		return this.type;
	}

	/**
	 * Register a {@literal 'java.protocol.handler.pkgs'} property so that a
	 * {@link URLStreamHandler} will be located to deal with jar URLs.
	 */
	public static void registerUrlProtocolHandler() {
		Handler.captureJarContextUrl();
		String handlers = System.getProperty(PROTOCOL_HANDLER, "");
		System.setProperty(PROTOCOL_HANDLER,
				((handlers == null || handlers.isEmpty()) ? HANDLERS_PACKAGE : handlers + "|" + HANDLERS_PACKAGE));
		resetCachedUrlHandlers();
	}

	/**
	 * Reset any cached handlers just in case a jar protocol has already been used. We
	 * reset the handler by trying to set a null {@link URLStreamHandlerFactory} which
	 * should have no effect other than clearing the handlers cache.
	 */
	private static void resetCachedUrlHandlers() {
		try {
			URL.setURLStreamHandlerFactory(null);
		}
		catch (Error ex) {
			// Ignore
		}
	}

	/**
	 * An {@link Enumeration} on {@linkplain java.util.jar.JarEntry jar entries}.
	 */
	private static class JarEntryEnumeration implements Enumeration<java.util.jar.JarEntry> {

		private final Iterator<JarEntry> iterator;

		/**
		 * Constructs a new JarEntryEnumeration object with the specified iterator.
		 * @param iterator the iterator to be used for iterating over JarEntry objects
		 */
		JarEntryEnumeration(Iterator<JarEntry> iterator) {
			this.iterator = iterator;
		}

		/**
		 * Returns true if this enumeration contains more elements.
		 * @return true if there are more elements, false otherwise
		 */
		@Override
		public boolean hasMoreElements() {
			return this.iterator.hasNext();
		}

		/**
		 * Returns the next element in the enumeration.
		 * @return the next element in the enumeration
		 * @throws NoSuchElementException if there are no more elements in the enumeration
		 */
		@Override
		public java.util.jar.JarEntry nextElement() {
			return this.iterator.next();
		}

	}

}
