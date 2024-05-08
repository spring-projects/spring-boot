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

	NestedUrlConnection(URL url) throws MalformedURLException {
		this(url, Cleaner.instance);
	}

	NestedUrlConnection(URL url, Cleaner cleaner) throws MalformedURLException {
		super(url);
		NestedLocation location = parseNestedLocation(url);
		this.resources = new NestedUrlConnectionResources(location);
		this.cleanup = cleaner.register(this, this.resources);
	}

	private NestedLocation parseNestedLocation(URL url) throws MalformedURLException {
		try {
			return NestedLocation.fromUrl(url);
		}
		catch (IllegalArgumentException ex) {
			throw new MalformedURLException(ex.getMessage());
		}
	}

	@Override
	public String getHeaderField(String name) {
		List<String> values = getHeaderFields().get(name);
		return (values != null && !values.isEmpty()) ? values.get(0) : null;
	}

	@Override
	public String getHeaderField(int n) {
		Entry<String, List<String>> entry = getHeaderEntry(n);
		List<String> values = (entry != null) ? entry.getValue() : null;
		return (values != null && !values.isEmpty()) ? values.get(0) : null;
	}

	@Override
	public String getHeaderFieldKey(int n) {
		Entry<String, List<String>> entry = getHeaderEntry(n);
		return (entry != null) ? entry.getKey() : null;
	}

	private Entry<String, List<String>> getHeaderEntry(int n) {
		Iterator<Entry<String, List<String>>> iterator = getHeaderFields().entrySet().iterator();
		Entry<String, List<String>> entry = null;
		for (int i = 0; i < n; i++) {
			entry = (!iterator.hasNext()) ? null : iterator.next();
		}
		return entry;
	}

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

	@Override
	public int getContentLength() {
		long contentLength = getContentLengthLong();
		return (contentLength <= Integer.MAX_VALUE) ? (int) contentLength : -1;
	}

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

	@Override
	public String getContentType() {
		return CONTENT_TYPE;
	}

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

	@Override
	public Permission getPermission() throws IOException {
		if (this.permission == null) {
			File file = this.resources.getLocation().path().toFile();
			this.permission = new FilePermission(file.getCanonicalPath(), "read");
		}
		return this.permission;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		connect();
		return new ConnectionInputStream(this.resources.getInputStream());
	}

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

		ConnectionInputStream(InputStream in) {
			super(in);
		}

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
