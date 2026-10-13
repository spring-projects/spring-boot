/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Supplier to provide an {@link InputStream}.
 *
 * @author Phillip Webb
 */
@FunctionalInterface
interface InputStreamSupplier {

	/**
	 * Returns a new open {@link InputStream} at the beginning of the content.
	 * @return a new {@link InputStream}
	 * @throws IOException on IO error
	 */
	InputStream openStream() throws IOException;

	/**
	 * Factory method to create an {@link InputStreamSupplier} for the given {@link File}.
	 * @param file the source file
	 * @return a new {@link InputStreamSupplier} instance
	 */
	static InputStreamSupplier forFile(File file) {
		return () -> new FileInputStream(file);
	}

}
