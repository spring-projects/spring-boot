/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.zip;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * {@link DataBlock} backed by a byte array .
 *
 * @author Phillip Webb
 */
class ByteArrayDataBlock implements CloseableDataBlock {

	private final byte[] bytes;

	private final int maxReadSize;

	/**
	 * Create a new {@link ByteArrayDataBlock} backed by the given bytes.
	 * @param bytes the bytes to use
	 */
	ByteArrayDataBlock(byte... bytes) {
		this(bytes, -1);
	}

	ByteArrayDataBlock(byte[] bytes, int maxReadSize) {
		this.bytes = bytes;
		this.maxReadSize = maxReadSize;
	}

	@Override
	public long size() throws IOException {
		return this.bytes.length;
	}

	@Override
	public int read(ByteBuffer dst, long pos) throws IOException {
		return read(dst, (int) pos);
	}

	private int read(ByteBuffer dst, int pos) {
		int remaining = dst.remaining();
		int length = Math.min(this.bytes.length - pos, remaining);
		if (this.maxReadSize > 0 && length > this.maxReadSize) {
			length = this.maxReadSize;
		}
		dst.put(this.bytes, pos, length);
		return length;
	}

	@Override
	public void close() throws IOException {
	}

}
