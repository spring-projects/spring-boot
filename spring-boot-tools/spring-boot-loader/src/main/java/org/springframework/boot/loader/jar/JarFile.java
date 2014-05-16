/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.loader.jar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.springframework.boot.loader.data.RandomAccessData;
import org.springframework.boot.loader.data.RandomAccessData.ResourceAccess;
import org.springframework.boot.loader.data.RandomAccessDataFile;
import org.springframework.boot.loader.util.AsciiBytes;

/**
 * Extended variant of {@link java.util.jar.JarFile} that behaves in the same way but
 * offers the following additional functionality.
 * <ul>
 * <li>Jar entries can be {@link JarEntryFilter filtered} during construction and new
 * filtered files can be {@link #getFilteredJarFile(JarEntryFilter...) created} from
 * existing files.</li>
 * <li>A nested {@link JarFile} can be
 * {@link #getNestedJarFile(ZipEntry, JarEntryFilter...) obtained} based on any directory
 * entry.</li>
 * <li>A nested {@link JarFile} can be
 * {@link #getNestedJarFile(ZipEntry, JarEntryFilter...) obtained} for embedded JAR files
 * (as long as their entry is not compressed).</li>
 * <li>Entry data can be accessed as {@link RandomAccessData}.</li>
 * </ul>
 * 
 * @author Phillip Webb
 */
public class JarFile extends java.util.jar.JarFile implements Iterable<JarEntryData> {

	private static final AsciiBytes META_INF = new AsciiBytes("META-INF/");

	private static final AsciiBytes MANIFEST_MF = new AsciiBytes("META-INF/MANIFEST.MF");

	private static final AsciiBytes SIGNATURE_FILE_EXTENSION = new AsciiBytes(".SF");

	private static final String PROTOCOL_HANDLER = "java.protocol.handler.pkgs";

	private static final String HANDLERS_PACKAGE = "org.springframework.boot.loader";

	private final RandomAccessDataFile rootFile;

	private final RandomAccessData data;

	private final String name;

	private final long size;

	private boolean signed;

	private List<JarEntryData> entries;

	private SoftReference<Map<AsciiBytes, JarEntryData>> entriesByName;

	private JarEntryData manifestEntry;

	private SoftReference<Manifest> manifest;

	/**
	 * Create a new {@link JarFile} backed by the specified file.
	 * @param file the root jar file
	 * @param filters an optional set of jar entry filters
	 * @throws IOException
	 */
	public JarFile(File file, JarEntryFilter... filters) throws IOException {
		this(new RandomAccessDataFile(file), filters);
	}

	/**
	 * Create a new {@link JarFile} backed by the specified file.
	 * @param file the root jar file
	 * @param filters an optional set of jar entry filters
	 * @throws IOException
	 */
	JarFile(RandomAccessDataFile file, JarEntryFilter... filters) throws IOException {
		this(file, file.getFile().getAbsolutePath(), file, filters);
	}

	/**
	 * Private constructor used to create a new {@link JarFile} either directly or from a
	 * nested entry.
	 * @param rootFile the root jar file
	 * @param name the name of this file
	 * @param data the underlying data
	 * @param filters an optional set of jar entry filters
	 * @throws IOException
	 */
	private JarFile(RandomAccessDataFile rootFile, String name, RandomAccessData data,
			JarEntryFilter... filters) throws IOException {
		super(rootFile.getFile());
		this.rootFile = rootFile;
		this.name = name;
		this.data = data;
		this.size = data.getSize();
		loadJarEntries(filters);
	}

	private void loadJarEntries(JarEntryFilter[] filters) throws IOException {
		CentralDirectoryEndRecord endRecord = new CentralDirectoryEndRecord(this.data);
		RandomAccessData centralDirectory = endRecord.getCentralDirectory(this.data);
		int numberOfRecords = endRecord.getNumberOfRecords();
		this.entries = new ArrayList<JarEntryData>(numberOfRecords);
		InputStream inputStream = centralDirectory.getInputStream(ResourceAccess.ONCE);
		try {
			JarEntryData entry = JarEntryData.fromInputStream(this, inputStream);
			while (entry != null) {
				addJarEntry(entry, filters);
				entry = JarEntryData.fromInputStream(this, inputStream);
			}
		}
		finally {
			inputStream.close();
		}
	}

	private void addJarEntry(JarEntryData entry, JarEntryFilter[] filters) {
		AsciiBytes name = entry.getName();
		for (JarEntryFilter filter : filters) {
			name = (filter == null || name == null ? name : filter.apply(name, entry));
		}
		if (name != null) {
			entry.setName(name);
			this.entries.add(entry);
			if (name.startsWith(META_INF)) {
				processMetaInfEntry(name, entry);
			}
		}
	}

	private void processMetaInfEntry(AsciiBytes name, JarEntryData entry) {
		if (name.equals(MANIFEST_MF)) {
			this.manifestEntry = entry;
		}
		if (name.endsWith(SIGNATURE_FILE_EXTENSION)) {
			this.signed = true;
		}
	}

	protected final RandomAccessDataFile getRootJarFile() {
		return this.rootFile;
	}

	RandomAccessData getData() {
		return this.data;
	}

	@Override
	public Manifest getManifest() throws IOException {
		if (this.manifestEntry == null) {
			return null;
		}
		Manifest manifest = (this.manifest == null ? null : this.manifest.get());
		if (manifest == null) {
			InputStream inputStream = this.manifestEntry.getInputStream();
			try {
				manifest = new Manifest(inputStream);
			}
			finally {
				inputStream.close();
			}
			this.manifest = new SoftReference<Manifest>(manifest);
		}
		return manifest;
	}

	@Override
	public Enumeration<java.util.jar.JarEntry> entries() {
		final Iterator<JarEntryData> iterator = iterator();
		return new Enumeration<java.util.jar.JarEntry>() {

			@Override
			public boolean hasMoreElements() {
				return iterator.hasNext();
			}

			@Override
			public java.util.jar.JarEntry nextElement() {
				return iterator.next().asJarEntry();
			}
		};
	}

	@Override
	public Iterator<JarEntryData> iterator() {
		return this.entries.iterator();
	}

	@Override
	public JarEntry getJarEntry(String name) {
		return (JarEntry) getEntry(name);
	}

	@Override
	public ZipEntry getEntry(String name) {
		JarEntryData jarEntryData = getJarEntryData(name);
		return (jarEntryData == null ? null : jarEntryData.asJarEntry());
	}

	public JarEntryData getJarEntryData(String name) {
		if (name == null) {
			return null;
		}
		Map<AsciiBytes, JarEntryData> entriesByName = (this.entriesByName == null ? null
				: this.entriesByName.get());
		if (entriesByName == null) {
			entriesByName = new HashMap<AsciiBytes, JarEntryData>();
			for (JarEntryData entry : this.entries) {
				entriesByName.put(entry.getName(), entry);
			}
			this.entriesByName = new SoftReference<Map<AsciiBytes, JarEntryData>>(
					entriesByName);
		}

		JarEntryData entryData = entriesByName.get(new AsciiBytes(name));
		if (entryData == null && !name.endsWith("/")) {
			entryData = entriesByName.get(new AsciiBytes(name + "/"));
		}
		return entryData;
	}

	boolean isSigned() {
		return this.signed;
	}

	void setupEntryCertificates() {
		// Fallback to JarInputStream to obtain certificates, not fast but hopefully not
		// happening that often.
		try {
			JarInputStream inputStream = new JarInputStream(getData().getInputStream(
					ResourceAccess.ONCE));
			try {
				java.util.jar.JarEntry entry = inputStream.getNextJarEntry();
				while (entry != null) {
					inputStream.closeEntry();
					JarEntry jarEntry = getJarEntry(entry.getName());
					if (jarEntry != null) {
						jarEntry.setupCertificates(entry);
					}
					entry = inputStream.getNextJarEntry();
				}
			}
			finally {
				inputStream.close();
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Override
	public synchronized InputStream getInputStream(ZipEntry ze) throws IOException {
		return getContainedEntry(ze).getSource().getInputStream();
	}

	/**
	 * Return a nested {@link JarFile} loaded from the specified entry.
	 * @param ze the zip entry
	 * @param filters an optional set of jar entry filters to be applied
	 * @return a {@link JarFile} for the entry
	 * @throws IOException
	 */
	public synchronized JarFile getNestedJarFile(final ZipEntry ze,
			JarEntryFilter... filters) throws IOException {
		return getNestedJarFile(getContainedEntry(ze).getSource());
	}

	/**
	 * Return a nested {@link JarFile} loaded from the specified entry.
	 * @param sourceEntry the zip entry
	 * @param filters an optional set of jar entry filters to be applied
	 * @return a {@link JarFile} for the entry
	 * @throws IOException
	 */
	public synchronized JarFile getNestedJarFile(final JarEntryData sourceEntry,
			JarEntryFilter... filters) throws IOException {
		try {
			if (sourceEntry.isDirectory()) {
				return getNestedJarFileFromDirectoryEntry(sourceEntry, filters);
			}
			return getNestedJarFileFromFileEntry(sourceEntry, filters);
		}
		catch (IOException ex) {
			throw new IOException("Unable to open nested jar file '"
					+ sourceEntry.getName() + "'", ex);
		}
	}

	private JarFile getNestedJarFileFromDirectoryEntry(JarEntryData sourceEntry,
			JarEntryFilter... filters) throws IOException {
		final AsciiBytes sourceName = sourceEntry.getName();
		JarEntryFilter[] filtersToUse = new JarEntryFilter[filters.length + 1];
		System.arraycopy(filters, 0, filtersToUse, 1, filters.length);
		filtersToUse[0] = new JarEntryFilter() {
			@Override
			public AsciiBytes apply(AsciiBytes name, JarEntryData entryData) {
				if (name.startsWith(sourceName) && !name.equals(sourceName)) {
					return name.substring(sourceName.length());
				}
				return null;
			}
		};
		return new JarFile(this.rootFile, getName() + "!/"
				+ sourceEntry.getName().substring(0, sourceName.length() - 1), this.data,
				filtersToUse);
	}

	private JarFile getNestedJarFileFromFileEntry(JarEntryData sourceEntry,
			JarEntryFilter... filters) throws IOException {
		if (sourceEntry.getMethod() != ZipEntry.STORED) {
			throw new IllegalStateException("Unable to open nested compressed entry "
					+ sourceEntry.getName());
		}
		return new JarFile(this.rootFile, getName() + "!/" + sourceEntry.getName(),
				sourceEntry.getData(), filters);
	}

	/**
	 * Return a new jar based on the filtered contents of this file.
	 * @param filters the set of jar entry filters to be applied
	 * @return a filtered {@link JarFile}
	 * @throws IOException
	 */
	public synchronized JarFile getFilteredJarFile(JarEntryFilter... filters)
			throws IOException {
		return new JarFile(this.rootFile, getName(), this.data, filters);
	}

	private JarEntry getContainedEntry(ZipEntry zipEntry) throws IOException {
		if (zipEntry instanceof JarEntry
				&& ((JarEntry) zipEntry).getSource().getSource() == this) {
			return (JarEntry) zipEntry;
		}
		throw new IllegalArgumentException("ZipEntry must be contained in this file");
	}

	@Override
	public int size() {
		return (int) this.size;
	}

	@Override
	public void close() throws IOException {
		this.rootFile.close();
	}

	/**
	 * Return a URL that can be used to access this JAR file. NOTE: the specified URL
	 * cannot be serialized and or cloned.
	 * @return the URL
	 * @throws MalformedURLException
	 */
	public URL getUrl() throws MalformedURLException {
		Handler handler = new Handler(this);
		String file = "file:" + getName(PathForm.SYSTEM_INDEPENDENT) + "!/";
		return new URL("jar", "", -1, file, handler);
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public String getName() {
		return getName(PathForm.SYSTEM_DEPENDENT);
	}

	private String getName(PathForm pathForm) {
		if (pathForm == PathForm.SYSTEM_INDEPENDENT && File.separatorChar != '/') {
			return this.name.replace(File.separatorChar, '/');
		}
		return this.name;

	}

	/**
	 * Register a {@literal 'java.protocol.handler.pkgs'} property so that a
	 * {@link URLStreamHandler} will be located to deal with jar URLs.
	 */
	public static void registerUrlProtocolHandler() {
		String handlers = System.getProperty(PROTOCOL_HANDLER);
		System.setProperty(PROTOCOL_HANDLER, ("".equals(handlers) ? HANDLERS_PACKAGE
				: handlers + "|" + HANDLERS_PACKAGE));
		resetCachedUrlHandlers();
	}

	/**
	 * Reset any cached handers just in case a jar protocol has already been used. We
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
	 * Different forms that paths can be returned.
	 */
	private static enum PathForm {

		/**
		 * Use system dependent paths (i.e. include backslashes on Windows)
		 */
		SYSTEM_DEPENDENT,

		/**
		 * Use system independent paths (i.e. replace backslashes on Windows)
		 */
		SYSTEM_INDEPENDENT
	}
}
