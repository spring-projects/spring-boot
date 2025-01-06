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

	UrlJarFile(File file, Runtime.Version version, Consumer<JarFile> closeAction) throws IOException {
		super(file, true, ZipFile.OPEN_READ, version);
		// Registered only for test cleanup since parent class is JarFile
		Cleaner.instance.register(this, null);
		this.manifest = new UrlJarManifest(super::getManifest);
		this.closeAction = closeAction;
	}

	@Override
	public ZipEntry getEntry(String name) {
		return UrlJarEntry.of(super.getEntry(name), this.manifest);
	}

	@Override
	public Manifest getManifest() throws IOException {
		return this.manifest.get();
	}

	@Override
	public void close() throws IOException {
		if (this.closeAction != null) {
			this.closeAction.accept(this);
		}
		super.close();
	}

}
