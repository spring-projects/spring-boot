/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * {@link URLStreamHandler} for Spring Boot loader {@link JarFile}s.
 * 
 * @author Phillip Webb
 * @see JarFile#registerUrlProtocolHandler()
 */
public class Handler extends URLStreamHandler {

	// NOTE: in order to be found as a URL protocol hander, this class must be public,
	// must be named Handler and must be in a package ending '.jar'

	private static final String FILE_PROTOCOL = "file:";

	private static final String SEPARATOR = JarURLConnection.SEPARATOR;

	private final JarFile jarFile;

	public Handler() {
		this(null);
	}

	public Handler(JarFile jarFile) {
		this.jarFile = jarFile;
	}

	@Override
	protected URLConnection openConnection(URL url) throws IOException {
		JarFile jarFile = (this.jarFile != null ? this.jarFile : getJarFileFromUrl(url));
		return new JarURLConnection(url, jarFile);
	}

	public JarFile getJarFileFromUrl(URL url) throws IOException {

		String spec = url.getFile();

		int separatorIndex = spec.indexOf(SEPARATOR);
		if (separatorIndex == -1) {
			throw new MalformedURLException("Jar URL does not contain !/ separator");
		}

		JarFile jar = null;
		while (separatorIndex != -1) {
			String name = spec.substring(0, separatorIndex);
			jar = (jar == null ? getRootJarFile(name) : getNestedJarFile(jar, name));
			spec = spec.substring(separatorIndex + SEPARATOR.length());
			separatorIndex = spec.indexOf(SEPARATOR);
		}

		return jar;
	}

	private JarFile getRootJarFile(String name) throws IOException {
		try {
			if (!name.startsWith(FILE_PROTOCOL)) {
				throw new IllegalStateException("Not a file URL");
			}
			String path = name.substring(FILE_PROTOCOL.length());
			return new JarFile(new File(path));
		}
		catch (Exception ex) {
			throw new IOException("Unable to open root Jar file '" + name + "'", ex);
		}
	}

	private JarFile getNestedJarFile(JarFile jarFile, String name) throws IOException {
		JarEntry jarEntry = jarFile.getJarEntry(name);
		if (jarEntry == null) {
			throw new IOException("Unable to find nested jar '" + name + "' from '"
					+ jarFile + "'");
		}
		return jarFile.getNestedJarFile(jarEntry);
	}
}
