/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.filewatch;

import java.io.File;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * A snapshot of a File at a given point in time.
 *
 * @author Phillip Webb
 */
class FileSnapshot {

	private final File file;

	private final boolean exists;

	private final long length;

	private final long lastModified;

	FileSnapshot(File file) {
		Assert.notNull(file, "'file' must not be null");
		Assert.isTrue(file.isFile() || !file.exists(), () -> "'file' [%s] must be a normal file".formatted(file));
		this.file = file;
		this.exists = file.exists();
		this.length = file.length();
		this.lastModified = file.lastModified();
	}

	File getFile() {
		return this.file;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (obj instanceof FileSnapshot other) {
			boolean equals = this.file.equals(other.file);
			equals = equals && this.exists == other.exists;
			equals = equals && this.length == other.length;
			equals = equals && this.lastModified == other.lastModified;
			return equals;
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		int hashCode = this.file.hashCode();
		hashCode = 31 * hashCode + Boolean.hashCode(this.exists);
		hashCode = 31 * hashCode + Long.hashCode(this.length);
		hashCode = 31 * hashCode + Long.hashCode(this.lastModified);
		return hashCode;
	}

	@Override
	public String toString() {
		return this.file.toString();
	}

}
