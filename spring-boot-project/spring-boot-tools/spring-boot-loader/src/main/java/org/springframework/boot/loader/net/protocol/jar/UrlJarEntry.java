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

import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;

/**
 * A {@link JarEntry} returned from a {@link UrlJarFile} or {@link UrlNestedJarFile}.
 *
 * @author Phillip Webb
 */
final class UrlJarEntry extends JarEntry {

	private final UrlJarManifest manifest;

	private UrlJarEntry(JarEntry entry, UrlJarManifest manifest) {
		super(entry);
		this.manifest = manifest;
	}

	@Override
	public Attributes getAttributes() throws IOException {
		return this.manifest.getEntryAttributes(this);
	}

	static UrlJarEntry of(ZipEntry entry, UrlJarManifest manifest) {
		return (entry != null) ? new UrlJarEntry((JarEntry) entry, manifest) : null;
	}

}
