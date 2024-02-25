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

package org.springframework.boot.loader.net.protocol.nested;

import java.io.File;
import java.io.FilePermission;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.ref.Cleaner.Cleanable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.security.Permission;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.boot.loader.ref.Cleaner;

/**
 * {@link URLConnection} to support {@code nested:} URLs. See {@link NestedLocation} for
 * details of the URL format.
 *
 * @author Phillip Webb
 */
class NestedUrlConnection extends URLConnection {

	private static final DateTimeFormatter RFC_1123_DATE_TIME = DateTimeFormatter.RFC_1123_DATE_TIME
		.withZone(ZoneId.of("GMT"));

	private static final String CONTENT_TYPE = "x-java/jar";

	private final NestedUrlConnectionResources resources;

	private final Cleanable cleanup;

	private long lastModified = -1;

	private FilePermission permission;

	private Map<String, List<String>> headerFields;

	/**
	 * Constructs a new NestedUrlConnection with the specified URL and default Cleaner.
	 * @param url the URL to connect to
	 * @throws MalformedURLException if the URL is malformed
	 */
	NestedUrlConnection(URL url) throws MalformedURLException {
		this(url, Cleaner.instance);
	}

	/**
	 * Constructs a new NestedUrlConnection object with the specified URL and Cleaner.
	 * @param url the URL to connect to
	 * @param cleaner the Cleaner object used for resource cleanup
	 * @throws MalformedURLException if the URL is malformed
	 */
	NestedUrlConnection(URL url, Cleaner cleaner) throws MalformedURLException {
		super(url);
		NestedLocation location = parseNestedLocation(url);
		this.resources = new NestedUrlConnectionResources(location);
		this.cleanup = cleaner.register(this, this.resources);
	}

	/**
	 * Parses the nested location from the given URL.
	 * @param url the URL to parse the nested location from
	 * @return the parsed nested location
	 * @throws MalformedURLException if the URL is malformed or the nested location is
	 * invalid
	 */
	private NestedLocation parseNestedLocation(URL url) throws MalformedURLException {
		try {
			return NestedLocation.parse(url.getPath());
		}
		catch (IllegalArgumentException ex) {
			throw new MalformedURLException(ex.getMessage());
		}
	}

	/**
	 * Returns the value of the specified header field.
	 * @param name the name of the header field
	 * @return the value of the specified header field, or null if the header field does
	 * not exist
	 */
	@Override
	public String getHeaderField(String name) {
		List<String> values = getHeaderFields().get(name);
		return (values != null && !values.isEmpty()) ? values.get(0) : null;
	}

	/**
	 * Returns the value of the header field at the specified index.
	 * @param n the index of the header field
	 * @return the value of the header field at the specified index, or null if the index
	 * is out of range or the header field does not exist
	 */
	@Override
	public String getHeaderField(int n) {
		Entry<String, List<String>> entry = getHeaderEntry(n);
		List<String> values = (entry != null) ? entry.getValue() : null;
		return (values != null && !values.isEmpty()) ? values.get(0) : null;
	}

	/**
	 * Returns the key of the header field at the specified index.
	 * @param n the index of the header field
	 * @return the key of the header field at the specified index, or null if the index is
	 * out of range
	 */
	@Override
	public String getHeaderFieldKey(int n) {
		Entry<String, List<String>> entry = getHeaderEntry(n);
		return (entry != null) ? entry.getKey() : null;
	}

	/**
	 * Returns the entry at the specified index in the header fields map.
	 * @param n the index of the entry to retrieve
	 * @return the entry at the specified index, or null if the index is out of bounds
	 */
	private Entry<String, List<String>> getHeaderEntry(int n) {
		Iterator<Entry<String, List<String>>> iterator = getHeaderFields().entrySet().iterator();
		Entry<String, List<String>> entry = null;
		for (int i = 0; i < n; i++) {
			entry = (!iterator.hasNext()) ? null : iterator.next();
		}
		return entry;
	}

	/**
	 * Returns the header fields of the connection.
	 * @return a map containing the header fields, or an empty map if an IOException
	 * occurs during connection
	 * @throws IOException if an error occurs while connecting
	 */
	@Override
	public Map<String, List<String>> getHeaderFields() {
		try {
			connect();
		}
		catch (IOException ex) {
			return Collections.emptyMap();
		}
		Map<String, List<String>> headerFields = this.headerFields;
		if (headerFields == null) {
			headerFields = new LinkedHashMap<>();
			long contentLength = getContentLengthLong();
			long lastModified = getLastModified();
			if (contentLength > 0) {
				headerFields.put("content-length", List.of(String.valueOf(contentLength)));
			}
			if (getLastModified() > 0) {
				headerFields.put("last-modified",
						List.of(RFC_1123_DATE_TIME.format(Instant.ofEpochMilli(lastModified))));
			}
			headerFields = Collections.unmodifiableMap(headerFields);
			this.headerFields = headerFields;
		}
		return headerFields;
	}

	/**
	 * Returns the content length of the resource that this connection's URL references.
	 * @return the content length of the resource, or -1 if the content length is greater
	 * than Integer.MAX_VALUE
	 */
	@Override
	public int getContentLength() {
		long contentLength = getContentLengthLong();
		return (contentLength <= Integer.MAX_VALUE) ? (int) contentLength : -1;
	}

	/**
	 * Returns the content length of the resource as a long value.
	 * @return the content length of the resource, or -1 if the content length cannot be
	 * determined
	 */
	@Override
	public long getContentLengthLong() {
		try {
			connect();
			return this.resources.getContentLength();
		}
		catch (IOException ex) {
			return -1;
		}
	}

	/**
	 * Returns the content type of the response.
	 * @return the content type of the response
	 */
	@Override
	public String getContentType() {
		return CONTENT_TYPE;
	}

	/**
	 * Returns the last modified timestamp of the resource.
	 * @return the last modified timestamp in milliseconds
	 */
	@Override
	public long getLastModified() {
		if (this.lastModified == -1) {
			try {
				this.lastModified = Files.getLastModifiedTime(this.resources.getLocation().path()).toMillis();
			}
			catch (IOException ex) {
				this.lastModified = 0;
			}
		}
		return this.lastModified;
	}

	/**
	 * Retrieves the permission required to read the file associated with this
	 * NestedUrlConnection.
	 * @return the permission object representing the read access to the file
	 * @throws IOException if an I/O error occurs while retrieving the permission
	 */
	@Override
	public Permission getPermission() throws IOException {
		if (this.permission == null) {
			File file = this.resources.getLocation().path().toFile();
			this.permission = new FilePermission(file.getCanonicalPath(), "read");
		}
		return this.permission;
	}

	/**
	 * Returns an input stream that reads from this connection.
	 * @return an input stream that reads from this connection
	 * @throws IOException if an I/O error occurs while creating the input stream
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		connect();
		return new ConnectionInputStream(this.resources.getInputStream());
	}

	/**
	 * Establishes a connection to the specified URL.
	 * @throws IOException if an I/O error occurs while connecting
	 */
	@Override
	public void connect() throws IOException {
		if (this.connected) {
			return;
		}
		this.resources.connect();
		this.connected = true;
	}

	/**
	 * Connection {@link InputStream}.
	 */
	class ConnectionInputStream extends FilterInputStream {

		private volatile boolean closing;

		/**
		 * Constructs a new ConnectionInputStream object with the specified InputStream.
		 * @param in the InputStream to be wrapped by this ConnectionInputStream
		 */
		ConnectionInputStream(InputStream in) {
			super(in);
		}

		/**
		 * Closes the input stream.
		 * @throws IOException if an I/O error occurs while closing the stream
		 */
		@Override
		public void close() throws IOException {
			if (this.closing) {
				return;
			}
			this.closing = true;
			try {
				super.close();
			}
			finally {
				try {
					NestedUrlConnection.this.cleanup.clean();
				}
				catch (UncheckedIOException ex) {
					throw ex.getCause();
				}
			}
		}

	}

}
