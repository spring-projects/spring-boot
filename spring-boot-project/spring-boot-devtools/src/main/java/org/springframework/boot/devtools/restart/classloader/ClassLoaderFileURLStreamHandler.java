/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.devtools.restart.classloader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * {@link URLStreamHandler} for the contents of a {@link ClassLoaderFile}.
 *
 * @author Phillip Webb
 * @since 1.5.0
 */
public class ClassLoaderFileURLStreamHandler extends URLStreamHandler {

	private ClassLoaderFile file;

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
			return new ByteArrayInputStream(
					ClassLoaderFileURLStreamHandler.this.file.getContents());
		}

		@Override
		public long getLastModified() {
			return ClassLoaderFileURLStreamHandler.this.file.getLastModified();

		}

	}

}
