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
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.springframework.boot.loader.ref.Cleaner;

/**
 * A {@link JarFile} subclass returned from a {@link JarUrlConnection}.
 *
 * @author Phillip Webb
 */
class UrlJarFile extends JarFile {

	private final UrlJarManifest manifest;

	private final Consumer<JarFile> closeAction;

	/**
	 * Constructs a new UrlJarFile by opening the specified file for reading.
	 * @param file the file to be opened as a UrlJarFile
	 * @param version the version of the Java runtime environment
	 * @param closeAction the action to be performed when the UrlJarFile is closed
	 * @throws IOException if an I/O error occurs while opening the file
	 */
	UrlJarFile(File file, Runtime.Version version, Consumer<JarFile> closeAction) throws IOException {
		super(file, true, ZipFile.OPEN_READ, version);
		// Registered only for test cleanup since parent class is JarFile
		Cleaner.instance.register(this, null);
		this.manifest = new UrlJarManifest(super::getManifest);
		this.closeAction = closeAction;
	}

	/**
	 * Returns a ZipEntry object for the specified entry name.
	 * @param name the name of the entry
	 * @return a ZipEntry object for the specified entry name
	 */
	@Override
	public ZipEntry getEntry(String name) {
		return UrlJarEntry.of(super.getEntry(name), this.manifest);
	}

	/**
	 * Returns the manifest of the URL jar file.
	 * @return the manifest of the URL jar file
	 * @throws IOException if an I/O error occurs while retrieving the manifest
	 */
	@Override
	public Manifest getManifest() throws IOException {
		return this.manifest.get();
	}

	/**
	 * Closes the UrlJarFile and releases any system resources associated with it. This
	 * method overrides the close() method in the superclass and throws an IOException. If
	 * a close action is set, it will be executed before closing the UrlJarFile.
	 * @throws IOException if an I/O error occurs while closing the UrlJarFile
	 */
	@Override
	public void close() throws IOException {
		if (this.closeAction != null) {
			this.closeAction.accept(this);
		}
		super.close();
	}

}
