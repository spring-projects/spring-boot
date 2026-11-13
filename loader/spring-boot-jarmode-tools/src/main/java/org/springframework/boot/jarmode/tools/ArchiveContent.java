/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jarmode.tools;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Provides entry contents and metadata from a packed or unpacked archive.
 *
 * @author Xinwei Zhang
 */
final class ArchiveContent implements Closeable {

	private final @Nullable JarFile jarFile;

	private final @Nullable Path directory;

	ArchiveContent(File source) throws IOException {
		this.directory = source.isDirectory() ? source.toPath().toRealPath() : null;
		this.jarFile = (this.directory != null) ? null : new JarFile(source);
	}

	@Nullable Manifest getManifest() throws IOException {
		if (this.jarFile != null) {
			return this.jarFile.getManifest();
		}
		JarEntry entry = getEntry(JarFile.MANIFEST_NAME);
		if (entry == null) {
			return null;
		}
		try (InputStream input = getInputStream(entry)) {
			return new Manifest(input);
		}
	}

	@Nullable JarEntry getEntry(String name) throws IOException {
		if (this.jarFile != null) {
			return this.jarFile.getJarEntry(name);
		}
		Path path = resolve(name);
		if (!Files.exists(path)) {
			return null;
		}
		BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
		String entryName = (attributes.isDirectory() && !name.endsWith("/")) ? name + "/" : name;
		JarEntry entry = new JarEntry(entryName);
		entry.setLastModifiedTime(attributes.lastModifiedTime());
		entry.setLastAccessTime(attributes.lastAccessTime());
		entry.setCreationTime(attributes.creationTime());
		return entry;
	}

	InputStream getInputStream(JarEntry entry) throws IOException {
		if (entry.isDirectory()) {
			return InputStream.nullInputStream();
		}
		return (this.jarFile != null) ? this.jarFile.getInputStream(entry)
				: Files.newInputStream(resolve(entry.getName()));
	}

	void forEach(EntryConsumer consumer) throws IOException {
		if (this.jarFile != null) {
			Enumeration<JarEntry> entries = this.jarFile.entries();
			while (entries.hasMoreElements()) {
				accept(entries.nextElement(), consumer);
			}
			return;
		}
		Assert.state(this.directory != null, "Source directory must not be null");
		try (Stream<Path> paths = Files.walk(this.directory)) {
			Iterator<Path> entries = paths.filter((path) -> !path.equals(this.directory)).sorted().iterator();
			while (entries.hasNext()) {
				String name = this.directory.relativize(entries.next()).toString().replace(File.separatorChar, '/');
				JarEntry entry = getEntry(name);
				Assert.state(entry != null, () -> "Archive entry '" + name + "' no longer exists");
				accept(entry, consumer);
			}
		}
	}

	private void accept(JarEntry entry, EntryConsumer consumer) throws IOException {
		if (!entry.getName().isEmpty()) {
			try (InputStream input = getInputStream(entry)) {
				consumer.accept(input, entry);
			}
		}
	}

	private Path resolve(String name) throws IOException {
		Assert.state(this.directory != null, "Source directory must not be null");
		Path path = this.directory.resolve(name).normalize();
		Assert.state(path.startsWith(this.directory) && !path.equals(this.directory),
				() -> "Entry '" + name + "' is outside the source archive");
		if (Files.exists(path)) {
			Assert.state(path.toRealPath().startsWith(this.directory),
					() -> "Entry '" + name + "' is outside the source archive");
		}
		return path;
	}

	@Override
	public void close() throws IOException {
		if (this.jarFile != null) {
			this.jarFile.close();
		}
	}

	@FunctionalInterface
	interface EntryConsumer {

		void accept(InputStream input, JarEntry entry) throws IOException;

	}

}
