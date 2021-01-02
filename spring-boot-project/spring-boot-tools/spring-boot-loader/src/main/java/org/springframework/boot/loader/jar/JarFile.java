/*
 * Copyright 2012-2019 the original author or authors.
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
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.function.Supplier;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.springframework.boot.loader.data.RandomAccessData;
import org.springframework.boot.loader.data.RandomAccessDataFile;

/**
 * {@link java.util.jar.JarFile} 的变体，提供了一下额外的功能：
 * <ul>
 *     <li>1、可以根据任何目录条目获得嵌套的 JarFile。</li>
 *     <li>2、可以为嵌入的 JAR 文件获得嵌套的 JAR 文件(只要它们的条目没有被压缩)。</li>
 * </ul>
 * <p> Extended variant of {@link java.util.jar.JarFile} that behaves in the same way but
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
public class JarFile extends java.util.jar.JarFile {

	private static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";

	private static final String PROTOCOL_HANDLER = "java.protocol.handler.pkgs";

	private static final String HANDLERS_PACKAGE = "org.springframework.boot.loader";

	private static final AsciiBytes META_INF = new AsciiBytes("META-INF/");

	private static final AsciiBytes SIGNATURE_FILE_EXTENSION = new AsciiBytes(".SF");

	private final RandomAccessDataFile rootFile;

	private final String pathFromRoot;

	private final RandomAccessData data;

	private final JarFileType type;

	private URL url;

	private String urlString;

	private JarFileEntries entries;

	private Supplier<Manifest> manifestSupplier;

	private SoftReference<Manifest> manifest;

	private boolean signed;

	private String comment;

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

	private JarFile(RandomAccessDataFile rootFile, String pathFromRoot, RandomAccessData data, JarEntryFilter filter,
			JarFileType type, Supplier<Manifest> manifestSupplier) throws IOException {
		super(rootFile.getFile());
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
			close();
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

	private CentralDirectoryVisitor centralDirectoryVisitor() {
		return new CentralDirectoryVisitor() {

			@Override
			public void visitStart(CentralDirectoryEndRecord endRecord, RandomAccessData centralDirectoryData) {
				JarFile.this.comment = endRecord.getComment();
			}

			@Override
			public void visitFileHeader(CentralDirectoryFileHeader fileHeader, int dataOffset) {
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

	protected final RandomAccessDataFile getRootJarFile() {
		return this.rootFile;
	}

	RandomAccessData getData() {
		return this.data;
	}

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

	@Override
	public Enumeration<java.util.jar.JarEntry> entries() {
		final Iterator<JarEntry> iterator = this.entries.iterator();
		return new Enumeration<java.util.jar.JarEntry>() {

			@Override
			public boolean hasMoreElements() {
				return iterator.hasNext();
			}

			@Override
			public java.util.jar.JarEntry nextElement() {
				return iterator.next();
			}

		};
	}

	public JarEntry getJarEntry(CharSequence name) {
		return this.entries.getEntry(name);
	}

	@Override
	public JarEntry getJarEntry(String name) {
		return (JarEntry) getEntry(name);
	}

	public boolean containsEntry(String name) {
		return this.entries.containsEntry(name);
	}

	@Override
	public ZipEntry getEntry(String name) {
		return this.entries.getEntry(name);
	}

	@Override
	public synchronized InputStream getInputStream(ZipEntry entry) throws IOException {
		if (entry instanceof JarEntry) {
			return this.entries.getInputStream((JarEntry) entry);
		}
		return getInputStream((entry != null) ? entry.getName() : null);
	}

	InputStream getInputStream(String name) throws IOException {
		return this.entries.getInputStream(name);
	}

	/**	获取内嵌的 jar 包
	 * <p> Return a nested {@link JarFile} loaded from the specified entry.
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

	private JarFile createJarFileFromEntry(JarEntry entry) throws IOException {
		if (entry.isDirectory()) {
			return createJarFileFromDirectoryEntry(entry);
		}
		return createJarFileFromFileEntry(entry);
	}

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

	@Override
	public String getComment() {
		return this.comment;
	}

	@Override
	public int size() {
		return this.entries.getSize();
	}

	@Override
	public void close() throws IOException {
		super.close();
		if (this.type == JarFileType.DIRECT) {
			this.rootFile.close();
		}
	}

	String getUrlString() throws MalformedURLException {
		if (this.urlString == null) {
			this.urlString = getUrl().toString();
		}
		return this.urlString;
	}

	/**
	 * Return a URL that can be used to access this JAR file. NOTE: the specified URL
	 * cannot be serialized and or cloned.
	 * @return the URL
	 * @throws MalformedURLException if the URL is malformed
	 */
	public URL getUrl() throws MalformedURLException {
		if (this.url == null) {
			Handler handler = new Handler(this);
			String file = this.rootFile.getFile().toURI() + this.pathFromRoot + "!/";
			file = file.replace("file:////", "file://"); // Fix UNC paths
			this.url = new URL("jar", "", -1, file, handler);
		}
		return this.url;
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public String getName() {
		return this.rootFile.getFile() + this.pathFromRoot;
	}

	boolean isSigned() {
		return this.signed;
	}

	void setupEntryCertificates(JarEntry entry) {
		// Fallback to JarInputStream to obtain certificates, not fast but hopefully not
		// happening that often.
		try {
			try (JarInputStream inputStream = new JarInputStream(getData().getInputStream())) {
				java.util.jar.JarEntry certEntry = inputStream.getNextJarEntry();
				while (certEntry != null) {
					inputStream.closeEntry();
					if (entry.getName().equals(certEntry.getName())) {
						setCertificates(entry, certEntry);
					}
					setCertificates(getJarEntry(certEntry.getName()), certEntry);
					certEntry = inputStream.getNextJarEntry();
				}
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private void setCertificates(JarEntry entry, java.util.jar.JarEntry certEntry) {
		if (entry != null) {
			entry.setCertificates(certEntry);
		}
	}

	public void clearCache() {
		this.entries.clearCache();
	}

	protected String getPathFromRoot() {
		return this.pathFromRoot;
	}

	JarFileType getType() {
		return this.type;
	}

	/** 注册一个 {@literal 'java.protocol.handler.pkgs'} 的属性，注册 Spring Boot
	 * 自定义的 {@link URLStreamHandler} 实现类，用于 jar 包的加载读取
	 * <h3>目的</h3>
	 * 通过将 {@literal 'org.springframework.boot.loader'} 包设置到 {@literal 'java.protocol.handler.pkgs'}
	 * 环境变量，从而使用到自定义的 {@link URLStreamHandler} 实现类 {@link Handler}，处理 jar: 协议的 URL。
	 * <p> https://www.iteye.com/blog/mercyblitz-735529
	 * <p> Register a {@literal 'java.protocol.handler.pkgs'} property so that a
	 * {@link URLStreamHandler} will be located to deal with jar URLs.
	 */
	public static void registerUrlProtocolHandler() {
		/**
		 * 获取 {@link URLStreamHandler} 的路径
		 */
		// java.protocol.handler.pkgs -> org.springframework.boot.loader
		String handlers = System.getProperty(PROTOCOL_HANDLER, "");
		/**
		 * 将 Spring Boot 自定义的 HANDLERS_PACKAGE(org.springframework.boot.loader) 补充上去
		 */
		// java.protocol.handler.pkgs -> org.springframework.boot.loader|org.springframework.boot.loader
		System.setProperty(PROTOCOL_HANDLER,
				("".equals(handlers) ? HANDLERS_PACKAGE : handlers + "|" + HANDLERS_PACKAGE));
		/**
		 * 重置已缓存的 {@link URLStreamHandler} 处理器们
		 */
		resetCachedUrlHandlers();
	}

	/**重置 URL 中的 URLStreamHandler 的缓存，防止 `jar://` 协议对应的 {@link URLStreamHandler}
	 * 已经创建我们通过设置 {@link URLStreamHandlerFactory} 为 null 的方式，清空 URL 中的该缓存。
	 * <p> Reset any cached handlers just in case a jar protocol has already been used. We
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
	 * The type of a {@link JarFile}.
	 */
	enum JarFileType {

		DIRECT, NESTED_DIRECTORY, NESTED_JAR

	}

}
