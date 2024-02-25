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

package org.springframework.boot.loader.net.protocol.jar;

import java.io.File;
import java.io.IOException;
import java.lang.Runtime.Version;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.springframework.boot.loader.jar.NestedJarFile;

/**
 * {@link NestedJarFile} subclass returned from a {@link JarUrlConnection}.
 *
 * @author Phillip Webb
 */
class UrlNestedJarFile extends NestedJarFile {

	private final UrlJarManifest manifest;

	private final Consumer<JarFile> closeAction;

	/**
     * Constructs a new UrlNestedJarFile with the specified file, nested entry name, version, and close action.
     * 
     * @param file the file representing the nested JAR file
     * @param nestedEntryName the name of the nested entry within the JAR file
     * @param version the version of the nested JAR file
     * @param closeAction the action to be performed when the JAR file is closed
     * @throws IOException if an I/O error occurs while reading the JAR file
     */
    UrlNestedJarFile(File file, String nestedEntryName, Version version, Consumer<JarFile> closeAction)
			throws IOException {
		super(file, nestedEntryName, version);
		this.manifest = new UrlJarManifest(super::getManifest);
		this.closeAction = closeAction;
	}

	/**
     * Returns the manifest of the nested JAR file.
     * 
     * @return the manifest of the nested JAR file
     * @throws IOException if an I/O error occurs while reading the manifest
     */
    @Override
	public Manifest getManifest() throws IOException {
		return this.manifest.get();
	}

	/**
     * Returns a {@code JarEntry} object for the specified entry name.
     * 
     * @param name the name of the entry
     * @return a {@code JarEntry} object for the specified entry name
     */
    @Override
	public JarEntry getEntry(String name) {
		return UrlJarEntry.of(super.getEntry(name), this.manifest);
	}

	/**
     * Closes the UrlNestedJarFile and releases any system resources associated with it.
     * This method overrides the close() method in the superclass and throws an IOException.
     * If a closeAction is set, it will be executed before closing the file.
     *
     * @throws IOException if an I/O error occurs while closing the file
     */
    @Override
	public void close() throws IOException {
		if (this.closeAction != null) {
			this.closeAction.accept(this);
		}
		super.close();
	}

}
