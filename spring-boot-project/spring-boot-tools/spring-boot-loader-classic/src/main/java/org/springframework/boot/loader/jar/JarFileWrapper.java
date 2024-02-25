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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Permission;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

/**
 * A wrapper used to create a copy of a {@link JarFile} so that it can be safely closed
 * without closing the original.
 *
 * @author Phillip Webb
 */
class JarFileWrapper extends AbstractJarFile {

	private final JarFile parent;

	/**
     * Constructs a new JarFileWrapper object with the specified parent JarFile.
     * 
     * @param parent the parent JarFile to be wrapped
     * @throws IOException if an I/O error occurs while accessing the parent JarFile
     */
    JarFileWrapper(JarFile parent) throws IOException {
		super(parent.getRootJarFile().getFile());
		this.parent = parent;
		super.close();
	}

	/**
     * Returns the URL of the JarFileWrapper.
     *
     * @return the URL of the JarFileWrapper
     * @throws MalformedURLException if the URL is malformed
     */
    @Override
	URL getUrl() throws MalformedURLException {
		return this.parent.getUrl();
	}

	/**
     * Returns the type of the jar file.
     *
     * @return the type of the jar file
     */
    @Override
	JarFileType getType() {
		return this.parent.getType();
	}

	/**
     * Returns the permission of the parent JarFile.
     *
     * @return the permission of the parent JarFile
     */
    @Override
	Permission getPermission() {
		return this.parent.getPermission();
	}

	/**
     * Returns the manifest of the JarFileWrapper.
     * 
     * @return the manifest of the JarFileWrapper
     * @throws IOException if an I/O error occurs while accessing the manifest
     */
    @Override
	public Manifest getManifest() throws IOException {
		return this.parent.getManifest();
	}

	/**
     * Returns an enumeration of the entries in this JarFileWrapper.
     *
     * @return an enumeration of the entries in this JarFileWrapper
     */
    @Override
	public Enumeration<JarEntry> entries() {
		return this.parent.entries();
	}

	/**
     * Returns a stream of JarEntry objects for this JarFileWrapper.
     *
     * @return a stream of JarEntry objects
     */
    @Override
	public Stream<JarEntry> stream() {
		return this.parent.stream();
	}

	/**
     * Retrieves a specific entry from the JAR file.
     * 
     * @param name the name of the entry to retrieve
     * @return the JarEntry object representing the specified entry, or null if not found
     */
    @Override
	public JarEntry getJarEntry(String name) {
		return this.parent.getJarEntry(name);
	}

	/**
     * Returns the ZipEntry object for the specified entry name.
     * 
     * @param name the name of the entry
     * @return the ZipEntry object for the specified entry name
     */
    @Override
	public ZipEntry getEntry(String name) {
		return this.parent.getEntry(name);
	}

	/**
     * Returns an input stream for reading the contents of the parent JarFile.
     *
     * @return an input stream for reading the contents of the parent JarFile
     * @throws IOException if an I/O error occurs while creating the input stream
     */
    @Override
	InputStream getInputStream() throws IOException {
		return this.parent.getInputStream();
	}

	/**
     * Returns an input stream for reading the contents of the specified zip entry.
     * 
     * @param ze the zip entry for which to obtain the input stream
     * @return an input stream for reading the contents of the specified zip entry
     * @throws IOException if an I/O error occurs while obtaining the input stream
     */
    @Override
	public synchronized InputStream getInputStream(ZipEntry ze) throws IOException {
		return this.parent.getInputStream(ze);
	}

	/**
     * Returns the comment of the parent JarFileWrapper object.
     * 
     * @return the comment of the parent JarFileWrapper object
     */
    @Override
	public String getComment() {
		return this.parent.getComment();
	}

	/**
     * Returns the size of the JarFileWrapper object.
     * 
     * @return the size of the JarFileWrapper object
     */
    @Override
	public int size() {
		return this.parent.size();
	}

	/**
     * Returns a string representation of the parent object.
     *
     * @return a string representation of the parent object
     */
    @Override
	public String toString() {
		return this.parent.toString();
	}

	/**
     * Returns the name of the parent directory of the current file.
     * 
     * @return the name of the parent directory
     */
    @Override
	public String getName() {
		return this.parent.getName();
	}

	/**
     * Unwraps a JarFile object from a JarFileWrapper or returns the original JarFile object.
     * 
     * @param jarFile the JarFile object to unwrap
     * @return the unwrapped JarFile object
     * @throws IllegalStateException if the input is not a JarFile or a JarFileWrapper
     */
    static JarFile unwrap(java.util.jar.JarFile jarFile) {
		if (jarFile instanceof JarFile file) {
			return file;
		}
		if (jarFile instanceof JarFileWrapper wrapper) {
			return unwrap(wrapper.parent);
		}
		throw new IllegalStateException("Not a JarFile or Wrapper");
	}

}
