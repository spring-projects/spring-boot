/*
 * Copyright 2012-present the original author or authors.
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
