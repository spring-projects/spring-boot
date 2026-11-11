/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.zip;

import org.springframework.boot.loader.zip.FileDataBlock.FileAccess;

/**
 * Test access to {@link FileAccess} details.
 *
 * @author Phillip Webb
 */
public final class FileChannelDataBlockManagedFileChannel {

	private FileChannelDataBlockManagedFileChannel() {
	}

	public static int BUFFER_SIZE = FileDataBlock.FileAccess.BUFFER_SIZE;

}
