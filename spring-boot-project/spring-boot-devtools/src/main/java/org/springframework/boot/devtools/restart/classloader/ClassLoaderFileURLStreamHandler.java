/*
 * Copyright 2012-2020 the original author or authors.
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

	private final ClassLoaderFile file;

	/**
     * Constructs a new ClassLoaderFileURLStreamHandler with the specified ClassLoaderFile.
     * 
     * @param file the ClassLoaderFile to be used by this ClassLoaderFileURLStreamHandler
     */
    public ClassLoaderFileURLStreamHandler(ClassLoaderFile file) {
		this.file = file;
	}

	/**
     * Opens a connection to the specified URL.
     *
     * @param url the URL to open a connection to
     * @return a URLConnection object representing the connection to the specified URL
     * @throws IOException if an I/O error occurs while opening the connection
     */
    @Override
	protected URLConnection openConnection(URL url) throws IOException {
		return new Connection(url);
	}

	/**
     * Connection class.
     */
    private class Connection extends URLConnection {

		/**
         * Constructs a new Connection object with the specified URL.
         *
         * @param url the URL to establish the connection with
         */
        Connection(URL url) {
			super(url);
		}

		/**
         * Connects to the server.
         *
         * @throws IOException if an I/O error occurs while connecting.
         */
        @Override
		public void connect() throws IOException {
		}

		/**
         * Returns an input stream for reading the contents of the file associated with this connection.
         * 
         * @return an input stream for reading the contents of the file
         * @throws IOException if an I/O error occurs while creating the input stream
         */
        @Override
		public InputStream getInputStream() throws IOException {
			return new ByteArrayInputStream(ClassLoaderFileURLStreamHandler.this.file.getContents());
		}

		/**
         * Returns the last modified timestamp of the file associated with this Connection.
         *
         * @return the last modified timestamp of the file
         */
        @Override
		public long getLastModified() {
			return ClassLoaderFileURLStreamHandler.this.file.getLastModified();
		}

	}

}
