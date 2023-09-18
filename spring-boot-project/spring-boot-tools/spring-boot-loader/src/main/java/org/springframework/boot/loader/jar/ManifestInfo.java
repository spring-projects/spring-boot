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

import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import org.springframework.boot.loader.zip.ZipContent;

/**
 * Info obtained from a {@link ZipContent} instance relating to the {@link Manifest}.
 *
 * @author Phillip Webb
 */
class ManifestInfo {

	private static final Name MULTI_RELEASE = new Name("Multi-Release");

	static final ManifestInfo NONE = new ManifestInfo(null, false);

	private final Manifest manifest;

	private volatile Boolean multiRelease;

	/**
	 * Create a new {@link ManifestInfo} instance.
	 * @param manifest the jar manifest
	 */
	ManifestInfo(Manifest manifest) {
		this(manifest, null);
	}

	private ManifestInfo(Manifest manifest, Boolean multiRelease) {
		this.manifest = manifest;
		this.multiRelease = multiRelease;
	}

	/**
	 * Return the manifest, if any.
	 * @return the manifest or {@code null}
	 */
	Manifest getManifest() {
		return this.manifest;
	}

	/**
	 * Return if this is a multi-release jar.
	 * @return if the jar is multi-release
	 */
	boolean isMultiRelease() {
		if (this.manifest == null) {
			return false;
		}
		Boolean multiRelease = this.multiRelease;
		if (multiRelease != null) {
			return multiRelease;
		}
		Attributes attributes = this.manifest.getMainAttributes();
		multiRelease = attributes.containsKey(MULTI_RELEASE);
		this.multiRelease = multiRelease;
		return multiRelease;
	}

}
