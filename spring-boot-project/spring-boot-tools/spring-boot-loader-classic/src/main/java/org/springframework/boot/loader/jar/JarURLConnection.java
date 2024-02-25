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

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.security.Permission;

/**
 * {@link java.net.JarURLConnection} used to support {@link JarFile#getUrl()}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Rostyslav Dudka
 */
final class JarURLConnection extends java.net.JarURLConnection {

	private static final ThreadLocal<Boolean> useFastExceptions = new ThreadLocal<>();

	private static final FileNotFoundException FILE_NOT_FOUND_EXCEPTION = new FileNotFoundException(
			"Jar file or entry not found");

	private static final IllegalStateException NOT_FOUND_CONNECTION_EXCEPTION = new IllegalStateException(
			FILE_NOT_FOUND_EXCEPTION);

	private static final String SEPARATOR = "!/";

	private static final URL EMPTY_JAR_URL;

	static {
		try {
			EMPTY_JAR_URL = new URL("jar:", null, 0, "file:!/", new URLStreamHandler() {
				/**
     * Opens a connection to the specified URL.
     * 
     * @param u the URL to open a connection to
     * @return the URLConnection object representing the connection to the URL
     * @throws IOException if an I/O error occurs while opening the connection
     */
    @Override
				protected URLConnection openConnection(URL u) throws IOException {
					// Stub URLStreamHandler to prevent the wrong JAR Handler from being
					// Instantiated and cached.
					return null;
				}
			});
		}
		catch (MalformedURLException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static final JarEntryName EMPTY_JAR_ENTRY_NAME = new JarEntryName(new StringSequence(""));

	private static final JarURLConnection NOT_FOUND_CONNECTION = JarURLConnection.notFound();

	private final AbstractJarFile jarFile;

	private Permission permission;

	private URL jarFileUrl;

	private final JarEntryName jarEntryName;

	private java.util.jar.JarEntry jarEntry;

	/**
     * Constructs a new JarURLConnection with the specified URL, AbstractJarFile, and JarEntryName.
     * 
     * @param url the URL of the connection
     * @param jarFile the AbstractJarFile associated with the connection
     * @param jarEntryName the JarEntryName associated with the connection
     * @throws IOException if an I/O error occurs while creating the connection
     */
    private JarURLConnection(URL url, AbstractJarFile jarFile, JarEntryName jarEntryName) throws IOException {
		// What we pass to super is ultimately ignored
		super(EMPTY_JAR_URL);
		this.url = url;
		this.jarFile = jarFile;
		this.jarEntryName = jarEntryName;
	}

	/**
     * Establishes a connection to the JAR file.
     * 
     * @throws IOException if an I/O error occurs while connecting to the JAR file
     * @throws FileNotFoundException if the JAR file is not found
     */
    @Override
	public void connect() throws IOException {
		if (this.jarFile == null) {
			throw FILE_NOT_FOUND_EXCEPTION;
		}
		if (!this.jarEntryName.isEmpty() && this.jarEntry == null) {
			this.jarEntry = this.jarFile.getJarEntry(getEntryName());
			if (this.jarEntry == null) {
				throwFileNotFound(this.jarEntryName, this.jarFile);
			}
		}
		this.connected = true;
	}

	/**
     * Returns the JAR file associated with this JarURLConnection.
     * 
     * @return the JAR file associated with this JarURLConnection
     * @throws IOException if an I/O error occurs while connecting to the JAR file
     */
    @Override
	public java.util.jar.JarFile getJarFile() throws IOException {
		connect();
		return this.jarFile;
	}

	/**
     * Returns the URL of the JAR file associated with this connection.
     * 
     * @return the URL of the JAR file
     * @throws NotFoundException if the JAR file is not found
     */
    @Override
	public URL getJarFileURL() {
		if (this.jarFile == null) {
			throw NOT_FOUND_CONNECTION_EXCEPTION;
		}
		if (this.jarFileUrl == null) {
			this.jarFileUrl = buildJarFileUrl();
		}
		return this.jarFileUrl;
	}

	/**
     * Builds the URL for the JAR file.
     * 
     * @return the URL for the JAR file
     * @throws IllegalStateException if a MalformedURLException occurs
     */
    private URL buildJarFileUrl() {
		try {
			String spec = this.jarFile.getUrl().getFile();
			if (spec.endsWith(SEPARATOR)) {
				spec = spec.substring(0, spec.length() - SEPARATOR.length());
			}
			if (!spec.contains(SEPARATOR)) {
				return new URL(spec);
			}
			return new URL("jar:" + spec);
		}
		catch (MalformedURLException ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
     * Returns the JarEntry associated with this JarURLConnection.
     * 
     * @return the JarEntry associated with this JarURLConnection, or null if the jarEntryName is null or empty
     * @throws IOException if an I/O error occurs while connecting to the JAR file
     */
    @Override
	public java.util.jar.JarEntry getJarEntry() throws IOException {
		if (this.jarEntryName == null || this.jarEntryName.isEmpty()) {
			return null;
		}
		connect();
		return this.jarEntry;
	}

	/**
     * Returns the name of the entry associated with this JarURLConnection.
     * 
     * @return the name of the entry
     * @throws NullPointerException if the jarFile is null
     */
    @Override
	public String getEntryName() {
		if (this.jarFile == null) {
			throw NOT_FOUND_CONNECTION_EXCEPTION;
		}
		return this.jarEntryName.toString();
	}

	/**
     * Returns an input stream for reading the contents of the JAR file entry specified by the entry name.
     * 
     * @return an input stream for reading the contents of the JAR file entry
     * @throws IOException if an I/O error occurs while creating the input stream
     * @throws FileNotFoundException if the JAR file is not found
     * @throws IOException if no entry name is specified or if the entry name is empty
     */
    @Override
	public InputStream getInputStream() throws IOException {
		if (this.jarFile == null) {
			throw FILE_NOT_FOUND_EXCEPTION;
		}
		if (this.jarEntryName.isEmpty() && this.jarFile.getType() == JarFile.JarFileType.DIRECT) {
			throw new IOException("no entry name specified");
		}
		connect();
		InputStream inputStream = (this.jarEntryName.isEmpty() ? this.jarFile.getInputStream()
				: this.jarFile.getInputStream(this.jarEntry));
		if (inputStream == null) {
			throwFileNotFound(this.jarEntryName, this.jarFile);
		}
		return inputStream;
	}

	/**
     * Throws a {@link FileNotFoundException} if the specified JAR entry is not found in the given JAR file.
     * 
     * @param entry the JAR entry to check
     * @param jarFile the JAR file to search in
     * @throws FileNotFoundException if the JAR entry is not found in the JAR file
     */
    private void throwFileNotFound(Object entry, AbstractJarFile jarFile) throws FileNotFoundException {
		if (Boolean.TRUE.equals(useFastExceptions.get())) {
			throw FILE_NOT_FOUND_EXCEPTION;
		}
		throw new FileNotFoundException("JAR entry " + entry + " not found in " + jarFile.getName());
	}

	/**
     * Returns the content length of the resource that this connection's URL references.
     * 
     * @return the content length of the resource, or -1 if the content length is greater than Integer.MAX_VALUE
     */
    @Override
	public int getContentLength() {
		long length = getContentLengthLong();
		if (length > Integer.MAX_VALUE) {
			return -1;
		}
		return (int) length;
	}

	/**
     * Returns the content length of the resource represented by this JarURLConnection.
     * If the JarFile is null, returns -1.
     * If the JarEntryName is empty, returns the size of the JarFile.
     * Otherwise, returns the size of the JarEntry.
     *
     * @return the content length of the resource, or -1 if it cannot be determined
     */
    @Override
	public long getContentLengthLong() {
		if (this.jarFile == null) {
			return -1;
		}
		try {
			if (this.jarEntryName.isEmpty()) {
				return this.jarFile.size();
			}
			java.util.jar.JarEntry entry = getJarEntry();
			return (entry != null) ? (int) entry.getSize() : -1;
		}
		catch (IOException ex) {
			return -1;
		}
	}

	/**
     * Returns the content of the JarURLConnection.
     * 
     * @return the content of the JarURLConnection
     * @throws IOException if an I/O error occurs while connecting
     */
    @Override
	public Object getContent() throws IOException {
		connect();
		return this.jarEntryName.isEmpty() ? this.jarFile : super.getContent();
	}

	/**
     * Returns the content type of the resource pointed to by this JarURLConnection.
     * 
     * @return the content type of the resource, or null if the content type is not available
     */
    @Override
	public String getContentType() {
		return (this.jarEntryName != null) ? this.jarEntryName.getContentType() : null;
	}

	/**
     * Returns the permission associated with this JarURLConnection.
     * 
     * @return the permission associated with this JarURLConnection
     * @throws IOException if an I/O error occurs
     * @throws FileNotFoundException if the jar file is not found
     */
    @Override
	public Permission getPermission() throws IOException {
		if (this.jarFile == null) {
			throw FILE_NOT_FOUND_EXCEPTION;
		}
		if (this.permission == null) {
			this.permission = this.jarFile.getPermission();
		}
		return this.permission;
	}

	/**
     * Returns the last modified timestamp of the JAR file or entry.
     * 
     * @return the last modified timestamp in milliseconds since the epoch, or 0 if the JAR file or entry is not available or an error occurs
     */
    @Override
	public long getLastModified() {
		if (this.jarFile == null || this.jarEntryName.isEmpty()) {
			return 0;
		}
		try {
			java.util.jar.JarEntry entry = getJarEntry();
			return (entry != null) ? entry.getTime() : 0;
		}
		catch (IOException ex) {
			return 0;
		}
	}

	/**
     * Sets the flag indicating whether to use fast exceptions for JarURLConnection.
     * 
     * @param useFastExceptions the flag indicating whether to use fast exceptions
     */
    static void setUseFastExceptions(boolean useFastExceptions) {
		JarURLConnection.useFastExceptions.set(useFastExceptions);
	}

	/**
     * Returns a {@link JarURLConnection} object for the specified URL and {@link JarFile}.
     * 
     * @param url the URL to connect to
     * @param jarFile the {@link JarFile} to use
     * @return a {@link JarURLConnection} object
     * @throws IOException if an I/O error occurs
     */
    static JarURLConnection get(URL url, JarFile jarFile) throws IOException {
		StringSequence spec = new StringSequence(url.getFile());
		int index = indexOfRootSpec(spec, jarFile.getPathFromRoot());
		if (index == -1) {
			return (Boolean.TRUE.equals(useFastExceptions.get()) ? NOT_FOUND_CONNECTION
					: new JarURLConnection(url, null, EMPTY_JAR_ENTRY_NAME));
		}
		int separator;
		while ((separator = spec.indexOf(SEPARATOR, index)) > 0) {
			JarEntryName entryName = JarEntryName.get(spec.subSequence(index, separator));
			JarEntry jarEntry = jarFile.getJarEntry(entryName.toCharSequence());
			if (jarEntry == null) {
				return JarURLConnection.notFound(jarFile, entryName);
			}
			jarFile = jarFile.getNestedJarFile(jarEntry);
			index = separator + SEPARATOR.length();
		}
		JarEntryName jarEntryName = JarEntryName.get(spec, index);
		if (Boolean.TRUE.equals(useFastExceptions.get()) && !jarEntryName.isEmpty()
				&& !jarFile.containsEntry(jarEntryName.toString())) {
			return NOT_FOUND_CONNECTION;
		}
		return new JarURLConnection(url, jarFile.getWrapper(), jarEntryName);
	}

	/**
     * Returns the index of the root specification in the given file path.
     * 
     * @param file The file path to search in.
     * @param pathFromRoot The root specification to search for.
     * @return The index of the root specification in the file path, or -1 if not found.
     */
    private static int indexOfRootSpec(StringSequence file, String pathFromRoot) {
		int separatorIndex = file.indexOf(SEPARATOR);
		if (separatorIndex < 0 || !file.startsWith(pathFromRoot, separatorIndex)) {
			return -1;
		}
		return separatorIndex + SEPARATOR.length() + pathFromRoot.length();
	}

	/**
     * Returns a {@code JarURLConnection} object representing a 404 Not Found response.
     * 
     * @return a {@code JarURLConnection} object representing a 404 Not Found response
     * @throws IllegalStateException if an {@code IOException} occurs
     */
    private static JarURLConnection notFound() {
		try {
			return notFound(null, null);
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
     * Returns a JarURLConnection object representing a not found connection.
     * 
     * @param jarFile the JarFile object associated with the connection
     * @param jarEntryName the JarEntryName object associated with the connection
     * @return a JarURLConnection object representing a not found connection
     * @throws IOException if an I/O error occurs
     */
    private static JarURLConnection notFound(JarFile jarFile, JarEntryName jarEntryName) throws IOException {
		if (Boolean.TRUE.equals(useFastExceptions.get())) {
			return NOT_FOUND_CONNECTION;
		}
		return new JarURLConnection(null, jarFile, jarEntryName);
	}

	/**
	 * A JarEntryName parsed from a URL String.
	 */
	static class JarEntryName {

		private final StringSequence name;

		private String contentType;

		/**
         * Constructs a new JarEntryName object with the specified name.
         * 
         * @param spec the string sequence used to decode the name
         */
        JarEntryName(StringSequence spec) {
			this.name = decode(spec);
		}

		/**
         * Decodes a given StringSequence by replacing any occurrences of '%' with their corresponding characters.
         * If the source StringSequence is empty or does not contain any '%', the original StringSequence is returned.
         * 
         * @param source The StringSequence to be decoded
         * @return The decoded StringSequence
         */
        private StringSequence decode(StringSequence source) {
			if (source.isEmpty() || (source.indexOf('%') < 0)) {
				return source;
			}
			ByteArrayOutputStream bos = new ByteArrayOutputStream(source.length());
			write(source.toString(), bos);
			// AsciiBytes is what is used to store the JarEntries so make it symmetric
			return new StringSequence(AsciiBytes.toString(bos.toByteArray()));
		}

		/**
         * Writes the given source string to the provided output stream.
         * 
         * @param source the source string to be written
         * @param outputStream the output stream to write the source string to
         * @throws IllegalArgumentException if an invalid encoded sequence is encountered
         */
        private void write(String source, ByteArrayOutputStream outputStream) {
			int length = source.length();
			for (int i = 0; i < length; i++) {
				int c = source.charAt(i);
				if (c > 127) {
					String encoded = URLEncoder.encode(String.valueOf((char) c), StandardCharsets.UTF_8);
					write(encoded, outputStream);
				}
				else {
					if (c == '%') {
						if ((i + 2) >= length) {
							throw new IllegalArgumentException(
									"Invalid encoded sequence \"" + source.substring(i) + "\"");
						}
						c = decodeEscapeSequence(source, i);
						i += 2;
					}
					outputStream.write(c);
				}
			}
		}

		/**
         * Decodes an escape sequence in a given source string at a specified index.
         * 
         * @param source the source string containing the escape sequence
         * @param i the index of the escape sequence in the source string
         * @return the decoded character from the escape sequence
         * @throws IllegalArgumentException if the encoded sequence is invalid
         */
        private char decodeEscapeSequence(String source, int i) {
			int hi = Character.digit(source.charAt(i + 1), 16);
			int lo = Character.digit(source.charAt(i + 2), 16);
			if (hi == -1 || lo == -1) {
				throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
			}
			return ((char) ((hi << 4) + lo));
		}

		/**
         * Returns the name of the JarEntryName as a CharSequence.
         *
         * @return the name of the JarEntryName as a CharSequence
         */
        CharSequence toCharSequence() {
			return this.name;
		}

		/**
         * Returns a string representation of the object.
         * 
         * @return the name of the JarEntryName object as a string
         */
        @Override
		public String toString() {
			return this.name.toString();
		}

		/**
         * Returns true if the name of the JarEntryName object is empty, false otherwise.
         * 
         * @return true if the name is empty, false otherwise
         */
        boolean isEmpty() {
			return this.name.isEmpty();
		}

		/**
         * Returns the content type of the JarEntryName.
         * If the content type is null, it deduces the content type and assigns it.
         * 
         * @return the content type of the JarEntryName
         */
        String getContentType() {
			if (this.contentType == null) {
				this.contentType = deduceContentType();
			}
			return this.contentType;
		}

		/**
         * Deduces the content type of the JarEntryName.
         * 
         * @return The deduced content type as a String.
         */
        private String deduceContentType() {
			// Guess the content type, don't bother with streams as mark is not supported
			String type = isEmpty() ? "x-java/jar" : null;
			type = (type != null) ? type : guessContentTypeFromName(toString());
			type = (type != null) ? type : "content/unknown";
			return type;
		}

		/**
         * Retrieves a JarEntryName object based on the given spec.
         * 
         * @param spec the string sequence representing the spec
         * @return the JarEntryName object corresponding to the spec
         */
        static JarEntryName get(StringSequence spec) {
			return get(spec, 0);
		}

		/**
         * Returns a JarEntryName object based on the given spec and beginIndex.
         * 
         * @param spec        the StringSequence representing the spec
         * @param beginIndex  the starting index for the substring of the spec
         * @return            a JarEntryName object based on the spec and beginIndex
         */
        static JarEntryName get(StringSequence spec, int beginIndex) {
			if (spec.length() <= beginIndex) {
				return EMPTY_JAR_ENTRY_NAME;
			}
			return new JarEntryName(spec.subSequence(beginIndex));
		}

	}

}
