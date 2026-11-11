/*
 * Copyright 2012-present the original author or authors.
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
