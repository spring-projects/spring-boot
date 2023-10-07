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

	UrlNestedJarFile(File file, String nestedEntryName, Version version, Consumer<JarFile> closeAction)
			throws IOException {
		super(file, nestedEntryName, version);
		this.manifest = new UrlJarManifest(super::getManifest);
		this.closeAction = closeAction;
	}

	@Override
	public Manifest getManifest() throws IOException {
		return this.manifest.get();
	}

	@Override
	public JarEntry getEntry(String name) {
		return UrlJarEntry.of(super.getEntry(name), this.manifest);
	}

	@Override
	public void close() throws IOException {
		if (this.closeAction != null) {
			this.closeAction.accept(this);
		}
		super.close();
	}

}
