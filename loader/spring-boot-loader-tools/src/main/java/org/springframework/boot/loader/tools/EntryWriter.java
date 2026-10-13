/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.tools;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Interface used to write jar entry data.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
@FunctionalInterface
public interface EntryWriter {

	/**
	 * Write entry data to the specified output stream.
	 * @param outputStream the destination for the data
	 * @throws IOException in case of I/O errors
	 */
	void write(OutputStream outputStream) throws IOException;

	/**
	 * Return the size of the content that will be written, or {@code -1} if the size is
	 * not known.
	 * @return the size of the content
	 */
	default int size() {
		return -1;
	}

}
