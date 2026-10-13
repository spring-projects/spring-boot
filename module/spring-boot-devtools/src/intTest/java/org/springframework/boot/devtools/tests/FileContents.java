/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.tests;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.util.FileCopyUtils;

/**
 * Provides access to the contents of a file.
 *
 * @author Andy Wilkinson
 */
class FileContents {

	private final File file;

	FileContents(File file) {
		this.file = file;
	}

	@Nullable String get() {
		return get(Function.identity());
	}

	<T> @Nullable T get(Function<String, T> transformer) {
		if ((!this.file.exists()) || this.file.length() == 0) {
			return null;
		}
		try {
			return transformer.apply(FileCopyUtils.copyToString(new FileReader(this.file)));
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public @Nullable String toString() {
		return get();
	}

}
