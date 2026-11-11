/*
 * Copyright 2012-present the original author or authors.
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
