/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.nio.file;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;

import org.springframework.boot.loader.net.protocol.nested.NestedLocation;

/**
 * {@link FileStore} implementation for {@link NestedLocation nested} jar files.
 *
 * @author Phillip Webb
 * @see NestedFileSystemProvider
 */
class NestedFileStore extends FileStore {

	private final NestedFileSystem fileSystem;

	NestedFileStore(NestedFileSystem fileSystem) {
		this.fileSystem = fileSystem;
	}

	@Override
	public String name() {
		return this.fileSystem.toString();
	}

	@Override
	public String type() {
		return "nestedfs";
	}

	@Override
	public boolean isReadOnly() {
		return this.fileSystem.isReadOnly();
	}

	@Override
	public long getTotalSpace() throws IOException {
		return 0;
	}

	@Override
	public long getUsableSpace() throws IOException {
		return 0;
	}

	@Override
	public long getUnallocatedSpace() throws IOException {
		return 0;
	}

	@Override
	public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
		return getJarPathFileStore().supportsFileAttributeView(type);
	}

	@Override
	public boolean supportsFileAttributeView(String name) {
		return getJarPathFileStore().supportsFileAttributeView(name);
	}

	@Override
	public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
		return getJarPathFileStore().getFileStoreAttributeView(type);
	}

	@Override
	public Object getAttribute(String attribute) throws IOException {
		try {
			return getJarPathFileStore().getAttribute(attribute);
		}
		catch (UncheckedIOException ex) {
			throw ex.getCause();
		}
	}

	protected FileStore getJarPathFileStore() {
		try {
			return Files.getFileStore(this.fileSystem.getJarPath());
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
