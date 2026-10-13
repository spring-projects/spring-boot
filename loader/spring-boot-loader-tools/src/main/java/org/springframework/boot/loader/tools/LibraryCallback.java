/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.tools;

import java.io.File;
import java.io.IOException;

/**
 * Callback interface used to iterate {@link Libraries}.
 *
 * @author Phillip Webb
 * @since 1.0.0
 */
@FunctionalInterface
public interface LibraryCallback {

	/**
	 * Callback for a single library backed by a {@link File}.
	 * @param library the library
	 * @throws IOException if the operation fails
	 */
	void library(Library library) throws IOException;

}
