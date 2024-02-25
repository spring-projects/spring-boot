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

	/**
     * Constructs a new JarUrlConnection object with the specified URL.
     * 
     * @param url the URL to connect to
     * @throws IOException if an I/O error occurs while opening the connection
     */
    private JarUrlConnection(URL url) throws IOException {
		super(url);
		this.entryName = getEntryName();
		this.notFound = null;
		this.jarFileConnection = getJarFileURL().openConnection();
		this.jarFileConnection.setUseCaches(this.useCaches);
	}

	/**
     * Constructs a new JarUrlConnection with a Supplier for handling FileNotFoundExceptions.
     * 
     * @param notFound a Supplier that provides a FileNotFoundException when called
     * @throws IOException if an I/O error occurs while creating the connection
     */
    private JarUrlConnection(Supplier<FileNotFoundException> notFound) throws IOException {
		super(NOT_FOUND_URL);
		this.entryName = null;
		this.notFound = notFound;
	}

	/**
     * Returns the JarFile associated with this JarUrlConnection.
     * 
     * @return the JarFile associated with this JarUrlConnection
     * @throws IOException if an I/O error occurs while connecting to the Jar URL or retrieving the JarFile
     */
    @Override
	public JarFile getJarFile() throws IOException {
		connect();
		return this.jarFile;
	}

	/**
     * Returns the JarEntry associated with this JarUrlConnection.
     * 
     * @return the JarEntry associated with this JarUrlConnection
     * @throws IOException if an I/O error occurs while connecting
     */
    @Override
	public JarEntry getJarEntry() throws IOException {
		connect();
		return this.jarEntry;
	}

	/**
     * Returns the content length of the resource that this connection is connected to.
     * 
     * @return the content length of the resource, or -1 if the content length is greater than Integer.MAX_VALUE
     */
    @Override
	public int getContentLength() {
		long contentLength = getContentLengthLong();
		return (contentLength <= Integer.MAX_VALUE) ? (int) contentLength : -1;
	}

	/**
     * Returns the content length of the resource represented by this JarUrlConnection object.
     * 
     * @return the content length of the resource, or -1 if the content length is not available or an error occurs
     */
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

	/**
     * Returns the content type of the resource.
     * If the content type is not already set, it will be deduced.
     *
     * @return the content type of the resource
     */
    @Override
	public String getContentType() {
		if (this.contentType == null) {
			this.contentType = deduceContentType();
		}
		return this.contentType;
	}

	/**
     * Deduces the content type of the connection.
     * 
     * @return The content type of the connection. If the content type cannot be deduced, returns "content/unknown".
     */
    private String deduceContentType() {
		String type = (this.entryName != null) ? null : "x-java/jar";
		type = (type != null) ? type : deduceContentTypeFromStream();
		type = (type != null) ? type : deduceContentTypeFromEntryName();
		return (type != null) ? type : "content/unknown";
	}

	/**
     * Deduces the content type from the input stream of the JarUrlConnection.
     * 
     * @return The deduced content type, or null if an IOException occurs.
     */
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

	/**
     * Deduces the content type from the entry name.
     * 
     * @return The content type deduced from the entry name.
     */
    private String deduceContentTypeFromEntryName() {
		return guessContentTypeFromName(this.entryName);
	}

	/**
     * Returns the last modified timestamp of the JAR file connection.
     * 
     * @return the last modified timestamp of the JAR file connection, or the last modified timestamp of the superclass if the JAR file connection is null
     */
    @Override
	public long getLastModified() {
		return (this.jarFileConnection != null) ? this.jarFileConnection.getLastModified() : super.getLastModified();
	}

	/**
     * Returns the value of the specified header field from the JarUrlConnection.
     * 
     * @param name the name of the header field
     * @return the value of the specified header field, or null if the JarUrlConnection is null
     */
    @Override
	public String getHeaderField(String name) {
		return (this.jarFileConnection != null) ? this.jarFileConnection.getHeaderField(name) : null;
	}

	/**
     * Returns the content of the JarUrlConnection.
     * 
     * @return the content of the JarUrlConnection
     * @throws IOException if an I/O error occurs while connecting
     */
    @Override
	public Object getContent() throws IOException {
		connect();
		return (this.entryName != null) ? super.getContent() : this.jarFile;
	}

	/**
     * Returns the permission object for this JarUrlConnection.
     * 
     * @return the permission object for this JarUrlConnection
     * @throws IOException if an I/O error occurs while getting the permission
     */
    @Override
	public Permission getPermission() throws IOException {
		return this.jarFileConnection.getPermission();
	}

	/**
     * Returns an input stream for reading the contents of the URL connection.
     * 
     * @return an input stream for reading the contents of the URL connection
     * @throws IOException if an I/O error occurs while creating the input stream
     */
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

	/**
     * Returns the value of the allowUserInteraction field.
     * 
     * @return true if the allowUserInteraction field is true, false otherwise
     */
    @Override
	public boolean getAllowUserInteraction() {
		return (this.jarFileConnection != null) && this.jarFileConnection.getAllowUserInteraction();
	}

	/**
     * Sets the flag indicating whether this JarUrlConnection allows user interaction.
     * 
     * @param allowuserinteraction true to allow user interaction, false otherwise
     */
    @Override
	public void setAllowUserInteraction(boolean allowuserinteraction) {
		if (this.jarFileConnection != null) {
			this.jarFileConnection.setAllowUserInteraction(allowuserinteraction);
		}
	}

	/**
     * Returns whether this JarUrlConnection uses caches.
     * 
     * @return {@code true} if this JarUrlConnection uses caches, {@code false} otherwise.
     */
    @Override
	public boolean getUseCaches() {
		return (this.jarFileConnection == null) || this.jarFileConnection.getUseCaches();
	}

	/**
     * Sets whether the connection should use caches.
     * 
     * @param usecaches
     *            a boolean value indicating whether to use caches
     */
    @Override
	public void setUseCaches(boolean usecaches) {
		if (this.jarFileConnection != null) {
			this.jarFileConnection.setUseCaches(usecaches);
		}
	}

	/**
     * Returns the default value for the useCaches flag.
     * 
     * @return true if the useCaches flag is set to true by default, false otherwise.
     */
    @Override
	public boolean getDefaultUseCaches() {
		return (this.jarFileConnection == null) || this.jarFileConnection.getDefaultUseCaches();
	}

	/**
     * Sets the default use caches flag for this JarUrlConnection.
     * 
     * @param defaultusecaches the default use caches flag to be set
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
	public void setDefaultUseCaches(boolean defaultusecaches) {
		if (this.jarFileConnection != null) {
			this.jarFileConnection.setDefaultUseCaches(defaultusecaches);
		}
	}

	/**
     * Sets the value of the "If-Modified-Since" header field for this JarUrlConnection.
     * This header field is used to specify the date and time at which the requested resource was last modified.
     * 
     * @param ifModifiedSince the value of the "If-Modified-Since" header field
     * @see JarUrlConnection#getIfModifiedSince()
     * @see JarFileConnection#setIfModifiedSince(long)
     */
    @Override
	public void setIfModifiedSince(long ifModifiedSince) {
		if (this.jarFileConnection != null) {
			this.jarFileConnection.setIfModifiedSince(ifModifiedSince);
		}
	}

	/**
     * Returns the value of the specified request property for this JarUrlConnection.
     * 
     * @param key the key of the request property
     * @return the value of the specified request property, or null if the property is not set
     */
    @Override
	public String getRequestProperty(String key) {
		return (this.jarFileConnection != null) ? this.jarFileConnection.getRequestProperty(key) : null;
	}

	/**
     * Sets a general request property for this connection.
     * 
     * @param key   the key of the request property
     * @param value the value of the request property
     */
    @Override
	public void setRequestProperty(String key, String value) {
		if (this.jarFileConnection != null) {
			this.jarFileConnection.setRequestProperty(key, value);
		}
	}

	/**
     * Adds a request property to the connection.
     * 
     * @param key   the key of the request property
     * @param value the value of the request property
     */
    @Override
	public void addRequestProperty(String key, String value) {
		if (this.jarFileConnection != null) {
			this.jarFileConnection.addRequestProperty(key, value);
		}
	}

	/**
     * Returns a map of the request properties for this JarUrlConnection.
     * If the JarUrlConnection is not initialized, an empty map is returned.
     *
     * @return a map of the request properties for this JarUrlConnection, or an empty map if not initialized
     */
    @Override
	public Map<String, List<String>> getRequestProperties() {
		return (this.jarFileConnection != null) ? this.jarFileConnection.getRequestProperties()
				: Collections.emptyMap();
	}

	/**
     * Establishes a connection to the JAR file specified by the URL.
     * 
     * @throws IOException if an I/O error occurs while connecting to the JAR file
     */
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

	/**
     * Retrieves the JarEntry object for the specified jar file URL.
     * 
     * @param jarFileUrl the URL of the jar file
     * @return the JarEntry object for the specified entry name, or null if the entry name is null
     * @throws IOException if an I/O error occurs while retrieving the JarEntry object
     */
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

	/**
     * Throws a FileNotFoundException if the file is not found.
     * 
     * @throws FileNotFoundException if the file is not found
     */
    private void throwFileNotFound() throws FileNotFoundException {
		if (Optimizations.isEnabled()) {
			throw FILE_NOT_FOUND_EXCEPTION;
		}
		if (this.notFound != null) {
			throw this.notFound.get();
		}
		throw new FileNotFoundException("JAR entry " + this.entryName + " not found in " + this.jarFile.getName());
	}

	/**
     * Opens a connection to the specified URL.
     * 
     * @param url the URL to open a connection to
     * @return a JarUrlConnection object representing the connection to the URL
     * @throws IOException if an I/O error occurs while opening the connection
     */
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

	/**
     * Checks if the given JarFile has an entry with the specified name.
     * 
     * @param jarFile the JarFile to check
     * @param name the name of the entry to check for
     * @return true if the JarFile has an entry with the specified name, false otherwise
     */
    private static boolean hasEntry(JarFile jarFile, String name) {
		return (jarFile instanceof NestedJarFile nestedJarFile) ? nestedJarFile.hasEntry(name)
				: jarFile.getEntry(name) != null;
	}

	/**
     * Returns a JarUrlConnection object representing a connection to a JAR file entry that is not found.
     * 
     * @param jarFileName The name of the JAR file.
     * @param entryName The name of the entry within the JAR file.
     * @return A JarUrlConnection object representing a connection to a JAR file entry that is not found.
     * @throws IOException If an I/O error occurs.
     */
    private static JarUrlConnection notFoundConnection(String jarFileName, String entryName) throws IOException {
		if (Optimizations.isEnabled()) {
			return NOT_FOUND_CONNECTION;
		}
		return new JarUrlConnection(
				() -> new FileNotFoundException("JAR entry " + entryName + " not found in " + jarFileName));
	}

	/**
     * Clears the cache of the JarUrlConnection class.
     */
    static void clearCache() {
		jarFiles.clearCache();
	}

	/**
	 * Connection {@link InputStream}. This is not a {@link FilterInputStream} since
	 * {@link URLClassLoader} often creates streams that it doesn't call and we want to be
	 * lazy about getting the underlying {@link InputStream}.
	 */
	class ConnectionInputStream extends LazyDelegatingInputStream {

		/**
         * Closes the input stream.
         * 
         * @throws IOException if an I/O error occurs while closing the stream
         */
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

		/**
         * Returns the input stream of the delegate connection.
         *
         * @return the input stream of the delegate connection
         * @throws IOException if an I/O error occurs while getting the input stream
         */
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

		/**
         * Opens a connection to the specified URL.
         *
         * @param url the URL to open a connection to
         * @return the URLConnection object representing the connection to the URL, or null if the connection cannot be established
         */
        @Override
		protected URLConnection openConnection(URL url) {
			return null;
		}

	}

}
