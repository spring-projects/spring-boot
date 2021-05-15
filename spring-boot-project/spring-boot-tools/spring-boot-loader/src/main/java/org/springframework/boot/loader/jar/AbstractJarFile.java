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

package org.springframework.boot.loader.jar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Permission;

/**
 * Base class for extended variants of {@link java.util.jar.JarFile}.
 *
 * @author Phillip Webb
 */
abstract class AbstractJarFile extends java.util.jar.JarFile {

	/**
	 * Create a new {@link AbstractJarFile}.
	 * @param file the root jar file.
	 * @throws IOException on IO error
	 */
	AbstractJarFile(File file) throws IOException {
		super(file);
	}

	/**
	 * Return a URL that can be used to access this JAR file. NOTE: the specified URL
	 * cannot be serialized and or cloned.
	 * @return the URL
	 * @throws MalformedURLException if the URL is malformed
	 */
	abstract URL getUrl() throws MalformedURLException;

	/**
	 * Return the {@link JarFileType} of this instance.
	 * @return the jar file type
	 */
	abstract JarFileType getType();

	/**
	 * Return the security permission for this JAR.
	 * @return the security permission.
	 */
	abstract Permission getPermission();

	/**
	 * Return an {@link InputStream} for the entire jar contents.
	 * @return the contents input stream
	 * @throws IOException on IO error
	 */
	abstract InputStream getInputStream() throws IOException;

	/**
	 * The type of a {@link JarFile}.
	 */
	enum JarFileType {

		DIRECT, NESTED_DIRECTORY, NESTED_JAR

	}

}
