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

	/**
	 * Constructs a new UrlJarManifest object with the specified ManifestSupplier.
	 * @param supplier the supplier used to obtain the manifest for the URL JAR
	 */
	UrlJarManifest(ManifestSupplier supplier) {
		this.supplier = supplier;
	}

	/**
	 * Retrieves the manifest from the URL jar file.
	 * @return the manifest object if found, null otherwise
	 * @throws IOException if an I/O error occurs while retrieving the manifest
	 */
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

	/**
	 * Retrieves the attributes of a given JarEntry from the manifest.
	 * @param entry The JarEntry for which to retrieve the attributes.
	 * @return The attributes of the given JarEntry, or null if the manifest is not
	 * available.
	 * @throws IOException If an I/O error occurs while retrieving the manifest.
	 */
	Attributes getEntryAttributes(JarEntry entry) throws IOException {
		Manifest manifest = supply();
		if (manifest == null) {
			return null;
		}
		Attributes attributes = manifest.getEntries().get(entry.getName());
		return cloneAttributes(attributes);
	}

	/**
	 * Clones the given Attributes object.
	 * @param attributes the Attributes object to be cloned
	 * @return a cloned copy of the Attributes object, or null if the input is null
	 */
	private Attributes cloneAttributes(Attributes attributes) {
		return (attributes != null) ? (Attributes) attributes.clone() : null;
	}

	/**
	 * Retrieves the manifest of the URL jar.
	 * @return The manifest of the URL jar, or null if no manifest is available.
	 * @throws IOException If an I/O error occurs while retrieving the manifest.
	 */
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
