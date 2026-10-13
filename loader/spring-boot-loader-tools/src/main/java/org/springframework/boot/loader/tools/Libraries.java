/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.tools;

import java.io.IOException;

/**
 * Encapsulates information about libraries that may be packed into the archive.
 *
 * @author Phillip Webb
 * @since 1.0.0
 */
@FunctionalInterface
public interface Libraries {

	/**
	 * Represents no libraries.
	 */
	Libraries NONE = (callback) -> {
	};

	/**
	 * Iterate all relevant libraries.
	 * @param callback a callback for each relevant library.
	 * @throws IOException if the operation fails
	 */
	void doWithLibraries(LibraryCallback callback) throws IOException;

}
