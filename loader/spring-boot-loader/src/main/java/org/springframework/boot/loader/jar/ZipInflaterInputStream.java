/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.jar;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * {@link InflaterInputStream} that supports the writing of an extra "dummy" byte (which
 * is required when using an {@link Inflater} with {@code nowrap}) and returns accurate
 * available() results.
 *
 * @author Phillip Webb
 * @author Jan Mohr
 */
class ZipInflaterInputStream extends InflaterInputStream {

	private int available;

	private boolean extraBytesWritten;

	ZipInflaterInputStream(InputStream inputStream, Inflater inflater, int size) {
		super(inputStream, inflater, getInflaterBufferSize(size));
		this.available = size;
	}

	private static int getInflaterBufferSize(long size) {
		size += 2; // inflater likes some space
		size = (size > 65536) ? 8192 : size;
		size = (size <= 0) ? 4096 : size;
		return (int) size;
	}

	@Override
	public int available() throws IOException {
		return (this.available >= 0) ? this.available : super.available();
	}

	@Override
	public byte[] readAllBytes() throws IOException {
		if (this.available < 0) {
			return super.readAllBytes();
		}
		byte[] bytes = new byte[this.available];
		if (readNBytes(bytes, 0, bytes.length) != bytes.length) {
			throw new EOFException("Unexpected end of ZLIB input stream");
		}
		return bytes;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int result = super.read(b, off, len);
		if (result != -1) {
			this.available -= result;
		}
		return result;
	}

	@Override
	protected void fill() throws IOException {
		try {
			super.fill();
		}
		catch (EOFException ex) {
			if (this.extraBytesWritten) {
				throw ex;
			}
			this.len = 1;
			this.buf[0] = 0x0;
			this.extraBytesWritten = true;
			this.inf.setInput(this.buf, 0, this.len);
		}
	}

}
