/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.restart.classloader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.springframework.util.Assert;

/**
 * {@link URLStreamHandler} for the contents of a {@link ClassLoaderFile}.
 *
 * @author Phillip Webb
 * @since 1.5.0
 */
public class ClassLoaderFileURLStreamHandler extends URLStreamHandler {

	private final ClassLoaderFile file;

	public ClassLoaderFileURLStreamHandler(ClassLoaderFile file) {
		this.file = file;
	}

	@Override
	protected URLConnection openConnection(URL url) throws IOException {
		return new Connection(url);
	}

	private class Connection extends URLConnection {

		Connection(URL url) {
			super(url);
		}

		@Override
		public void connect() throws IOException {
		}

		@Override
		public InputStream getInputStream() throws IOException {
			byte[] contents = ClassLoaderFileURLStreamHandler.this.file.getContents();
			Assert.state(contents != null, "'contents' must not be null");
			return new ByteArrayInputStream(contents);
		}

		@Override
		public long getLastModified() {
			return ClassLoaderFileURLStreamHandler.this.file.getLastModified();
		}

	}

}
