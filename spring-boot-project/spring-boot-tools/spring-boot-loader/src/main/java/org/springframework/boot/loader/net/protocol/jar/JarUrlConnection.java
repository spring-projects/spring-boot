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

package org.springframework.boot.loader.net.protocol.jar;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.Permission;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.springframework.boot.loader.jar.NestedJarFile;
import org.springframework.boot.loader.net.util.UrlDecoder;

/**
 * {@link java.net.JarURLConnection} alternative to
 * {@code sun.net.www.protocol.jar.JarURLConnection} with optimized support for nested
 * jars.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Rostyslav Dudka
 */
final class JarUrlConnection extends java.net.JarURLConnection {

	static final UrlJarFiles jarFiles = new UrlJarFiles();

	static final InputStream emptyInputStream = new ByteArrayInputStream(new byte[0]);

	static final FileNotFoundException FILE_NOT_FOUND_EXCEPTION = new FileNotFoundException(
			"Jar file or entry not found");

	private static final URL NOT_FOUND_URL;

	static final JarUrlConnection NOT_FOUND_CONNECTION;
	static {
		try {
			NOT_FOUND_URL = new URL("jar:", null, 0, "nested:!/", new EmptyUrlStreamHandler());
			NOT_FOUND_CONNECTION = new JarUrlConnection(() -> FILE_NOT_FOUND_EXCEPTION);
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private final String entryName;

	private final Supplier<FileNotFoundException> notFound;

	private JarFile jarFile;

	private URLConnection jarFileConnection;

	private JarEntry jarEntry;

	private String contentType;

	private JarUrlConnection(URL url) throws IOException {
		super(url);
		this.entryName = getEntryName();
		this.notFound = null;
		this.jarFileConnection = getJarFileURL().openConnection();
		this.jarFileConnection.setUseCaches(this.useCaches);
	}

	private JarUrlConnection(Supplier<FileNotFoundException> notFound) throws IOException {
		super(NOT_FOUND_URL);
		this.entryName = null;
		this.notFound = notFound;
	}

	@Override
	public JarFile getJarFile() throws IOException {
		connect();
		return this.jarFile;
	}

	@Override
	public JarEntry getJarEntry() throws IOException {
		connect();
		return this.jarEntry;
	}

	@Override
	public int getContentLength() {
		long contentLength = getContentLengthLong();
		return (contentLength <= Integer.MAX_VALUE) ? (int) contentLength : -1;
	}

	@Override
	public long getContentLengthLong() {
		try {
			connect();
			return (this.jarEntry != null) ? this.jarEntry.getSize() : this.jarFileConnection.getContentLengthLong();
		}
		catch (IOException ex) {
			return -1;
		}
	}

	@Override
	public String getContentType() {
		if (this.contentType == null) {
			this.contentType = deduceContentType();
		}
		return this.contentType;
	}

	private String deduceContentType() {
		String type = (this.entryName != null) ? null : "x-java/jar";
		type = (type != null) ? type : deduceContentTypeFromStream();
		type = (type != null) ? type : deduceContentTypeFromEntryName();
		return (type != null) ? type : "content/unknown";
	}

	private String deduceContentTypeFromStream() {
		try {
			connect();
			try (InputStream in = this.jarFile.getInputStream(this.jarEntry)) {
				return guessContentTypeFromStream(new BufferedInputStream(in));
			}
		}
		catch (IOException ex) {
			return null;
		}
	}

	private String deduceContentTypeFromEntryName() {
		return guessContentTypeFromName(this.entryName);
	}

	@Override
	public long getLastModified() {
		return (this.jarFileConnection != null) ? this.jarFileConnection.getLastModified() : super.getLastModified();
	}

	@Override
	public String getHeaderField(String name) {
		return (this.jarFileConnection != null) ? this.jarFileConnection.getHeaderField(name) : null;
	}

	@Override
	public Object getContent() throws IOException {
		connect();
		return (this.entryName != null) ? super.getContent() : this.jarFile;
	}

	@Override
	public Permission getPermission() throws IOException {
		return (this.jarFileConnection != null) ? this.jarFileConnection.getPermission() : null;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		if (this.notFound != null) {
			throwFileNotFound();
		}
		URL jarFileURL = getJarFileURL();
		if (this.entryName == null && !UrlJarFileFactory.isNestedUrl(jarFileURL)) {
			throw new IOException("no entry name specified");
		}
		if (!getUseCaches() && Optimizations.isEnabled(false) && this.entryName != null) {
			JarFile cached = jarFiles.getCached(jarFileURL);
			if (cached != null) {
				if (cached.getEntry(this.entryName) != null) {
					return emptyInputStream;
				}
			}
		}
		connect();
		if (this.jarEntry == null) {
			if (this.jarFile instanceof NestedJarFile nestedJarFile) {
				// In order to work with Tomcat's TLD scanning and WarURLConnection we
				// return the raw zip data rather than failing because there is no entry.
				// See gh-38047 for details.
				return nestedJarFile.getRawZipDataInputStream();
			}
			throwFileNotFound();
		}
		return new ConnectionInputStream();
	}

	@Override
	public boolean getAllowUserInteraction() {
		return (this.jarFileConnection != null) && this.jarFileConnection.getAllowUserInteraction();
	}

	@Override
	public void setAllowUserInteraction(boolean allowuserinteraction) {
		if (this.jarFileConnection != null) {
			this.jarFileConnection.setAllowUserInteraction(allowuserinteraction);
		}
	}

	@Override
	public boolean getUseCaches() {
		return (this.jarFileConnection == null) || this.jarFileConnection.getUseCaches();
	}

	@Override
	public void setUseCaches(boolean usecaches) {
		if (this.jarFileConnection != null) {
			this.jarFileConnection.setUseCaches(usecaches);
		}
	}

	@Override
	public boolean getDefaultUseCaches() {
		return (this.jarFileConnection == null) || this.jarFileConnection.getDefaultUseCaches();
	}

	@Override
	public void setDefaultUseCaches(boolean defaultusecaches) {
		if (this.jarFileConnection != null) {
			this.jarFileConnection.setDefaultUseCaches(defaultusecaches);
		}
	}

	@Override
	public void setIfModifiedSince(long ifModifiedSince) {
		if (this.jarFileConnection != null) {
			this.jarFileConnection.setIfModifiedSince(ifModifiedSince);
		}
	}

	@Override
	public String getRequestProperty(String key) {
		return (this.jarFileConnection != null) ? this.jarFileConnection.getRequestProperty(key) : null;
	}

	@Override
	public void setRequestProperty(String key, String value) {
		if (this.jarFileConnection != null) {
			this.jarFileConnection.setRequestProperty(key, value);
		}
	}

	@Override
	public void addRequestProperty(String key, String value) {
		if (this.jarFileConnection != null) {
			this.jarFileConnection.addRequestProperty(key, value);
		}
	}

	@Override
	public Map<String, List<String>> getRequestProperties() {
		return (this.jarFileConnection != null) ? this.jarFileConnection.getRequestProperties()
				: Collections.emptyMap();
	}

	@Override
	public void connect() throws IOException {
		if (this.connected) {
			return;
		}
		if (this.notFound != null) {
			throwFileNotFound();
		}
		boolean useCaches = getUseCaches();
		URL jarFileURL = getJarFileURL();
		if (this.entryName != null && Optimizations.isEnabled()) {
			assertCachedJarFileHasEntry(jarFileURL, this.entryName);
		}
		this.jarFile = jarFiles.getOrCreate(useCaches, jarFileURL);
		this.jarEntry = getJarEntry(jarFileURL);
		boolean addedToCache = jarFiles.cacheIfAbsent(useCaches, jarFileURL, this.jarFile);
		if (addedToCache) {
			this.jarFileConnection = jarFiles.reconnect(this.jarFile, this.jarFileConnection);
		}
		this.connected = true;
	}

	/**
	 * The {@link URLClassLoader} connects often to check if a resource exists, we can
	 * save some object allocations by using the cached copy if we have one.
	 * @param jarFileURL the jar file to check
	 * @param entryName the entry name to check
	 * @throws FileNotFoundException on a missing entry
	 */
	private void assertCachedJarFileHasEntry(URL jarFileURL, String entryName) throws FileNotFoundException {
		JarFile cachedJarFile = jarFiles.getCached(jarFileURL);
		if (cachedJarFile != null && cachedJarFile.getJarEntry(entryName) == null) {
			throw FILE_NOT_FOUND_EXCEPTION;
		}
	}

	private JarEntry getJarEntry(URL jarFileUrl) throws IOException {
		if (this.entryName == null) {
			return null;
		}
		JarEntry jarEntry = this.jarFile.getJarEntry(this.entryName);
		if (jarEntry == null) {
			jarFiles.closeIfNotCached(jarFileUrl, this.jarFile);
			throwFileNotFound();
		}
		return jarEntry;
	}

	private void throwFileNotFound() throws FileNotFoundException {
		if (Optimizations.isEnabled()) {
			throw FILE_NOT_FOUND_EXCEPTION;
		}
		if (this.notFound != null) {
			throw this.notFound.get();
		}
		throw new FileNotFoundException("JAR entry " + this.entryName + " not found in " + this.jarFile.getName());
	}

	static JarUrlConnection open(URL url) throws IOException {
		String spec = url.getFile();
		if (spec.startsWith("nested:")) {
			int separator = spec.indexOf("!/");
			boolean specHasEntry = (separator != -1) && (separator + 2 != spec.length());
			if (specHasEntry) {
				URL jarFileUrl = new URL(spec.substring(0, separator));
				if ("runtime".equals(url.getRef())) {
					jarFileUrl = new URL(jarFileUrl, "#runtime");
				}
				String entryName = UrlDecoder.decode(spec.substring(separator + 2));
				JarFile jarFile = jarFiles.getOrCreate(true, jarFileUrl);
				jarFiles.cacheIfAbsent(true, jarFileUrl, jarFile);
				if (!hasEntry(jarFile, entryName)) {
					return notFoundConnection(jarFile.getName(), entryName);
				}
			}
		}
		return new JarUrlConnection(url);
	}

	private static boolean hasEntry(JarFile jarFile, String name) {
		return (jarFile instanceof NestedJarFile nestedJarFile) ? nestedJarFile.hasEntry(name)
				: jarFile.getEntry(name) != null;
	}

	private static JarUrlConnection notFoundConnection(String jarFileName, String entryName) throws IOException {
		if (Optimizations.isEnabled()) {
			return NOT_FOUND_CONNECTION;
		}
		return new JarUrlConnection(
				() -> new FileNotFoundException("JAR entry " + entryName + " not found in " + jarFileName));
	}

	static void clearCache() {
		jarFiles.clearCache();
	}

	/**
	 * Connection {@link InputStream}. This is not a {@link FilterInputStream} since
	 * {@link URLClassLoader} often creates streams that it doesn't call and we want to be
	 * lazy about getting the underlying {@link InputStream}.
	 */
	class ConnectionInputStream extends LazyDelegatingInputStream {

		@Override
		public void close() throws IOException {
			try {
				super.close();
			}
			finally {
				if (!getUseCaches()) {
					JarUrlConnection.this.jarFile.close();
				}
			}
		}

		@Override
		protected InputStream getDelegateInputStream() throws IOException {
			return JarUrlConnection.this.jarFile.getInputStream(JarUrlConnection.this.jarEntry);
		}

	}

	/**
	 * Empty {@link URLStreamHandler} used to prevent the wrong JAR Handler from being
	 * Instantiated and cached.
	 */
	private static final class EmptyUrlStreamHandler extends URLStreamHandler {

		@Override
		protected URLConnection openConnection(URL url) {
			return null;
		}

	}

}
