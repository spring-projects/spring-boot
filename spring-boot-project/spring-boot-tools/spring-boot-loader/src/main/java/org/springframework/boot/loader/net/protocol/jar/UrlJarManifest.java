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
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;

/**
 * Provides access {@link Manifest} content that can be safely returned from
 * {@link UrlJarFile} or {@link UrlNestedJarFile}.
 *
 * @author Phillip Webb
 */
class UrlJarManifest {

	private static final Object NONE = new Object();

	private final ManifestSupplier supplier;

	private volatile Object supplied;

	UrlJarManifest(ManifestSupplier supplier) {
		this.supplier = supplier;
	}

	Manifest get() throws IOException {
		Manifest manifest = supply();
		if (manifest == null) {
			return null;
		}
		Manifest copy = new Manifest();
		copy.getMainAttributes().putAll((Map<?, ?>) manifest.getMainAttributes().clone());
		manifest.getEntries().forEach((key, value) -> copy.getEntries().put(key, cloneAttributes(value)));
		return copy;
	}

	Attributes getEntryAttributes(JarEntry entry) throws IOException {
		Manifest manifest = supply();
		if (manifest == null) {
			return null;
		}
		Attributes attributes = manifest.getEntries().get(entry.getName());
		return cloneAttributes(attributes);
	}

	private Attributes cloneAttributes(Attributes attributes) {
		return (attributes != null) ? (Attributes) attributes.clone() : null;
	}

	private Manifest supply() throws IOException {
		Object supplied = this.supplied;
		if (supplied == null) {
			supplied = this.supplier.getManifest();
			this.supplied = (supplied != null) ? supplied : NONE;
		}
		return (supplied != NONE) ? (Manifest) supplied : null;
	}

	/**
	 * Interface used to supply the actual manifest.
	 */
	@FunctionalInterface
	interface ManifestSupplier {

		Manifest getManifest() throws IOException;

	}

}
