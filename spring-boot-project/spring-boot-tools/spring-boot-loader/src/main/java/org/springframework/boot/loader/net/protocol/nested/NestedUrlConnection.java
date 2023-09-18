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

import java.io.FilePermission;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.ref.Cleaner.Cleanable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;

import org.springframework.boot.loader.ref.Cleaner;

/**
 * {@link URLConnection} to support {@code nested:} URLs. See {@link NestedLocation} for
 * details of the URL format.
 *
 * @author Phillip Webb
 */
class NestedUrlConnection extends URLConnection {

	private static final String CONTENT_TYPE = "x-java/jar";

	private final NestedUrlConnectionResources resources;

	private final Cleanable cleanup;

	private long lastModified;

	private FilePermission permission;

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
			return NestedLocation.parse(url.getPath());
		}
		catch (IllegalArgumentException ex) {
			throw new MalformedURLException(ex.getMessage());
		}
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
		if (this.lastModified == 0) {
			this.lastModified = this.resources.getLocation().file().lastModified();
		}
		return this.lastModified;
	}

	@Override
	public Permission getPermission() throws IOException {
		if (this.permission == null) {
			this.permission = new FilePermission(this.resources.getLocation().file().getCanonicalPath(), "read");
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
