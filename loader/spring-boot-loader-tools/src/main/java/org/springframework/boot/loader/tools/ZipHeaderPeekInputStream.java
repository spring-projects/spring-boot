/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.tools;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.jspecify.annotations.Nullable;

/**
 * {@link InputStream} that can peek ahead at zip header bytes.
 *
 * @author Phillip Webb
 */
class ZipHeaderPeekInputStream extends FilterInputStream {

	private static final byte[] ZIP_HEADER = new byte[] { 0x50, 0x4b, 0x03, 0x04 };

	private final byte[] header;

	private final int headerLength;

	private int position;

	private @Nullable ByteArrayInputStream headerStream;

	protected ZipHeaderPeekInputStream(InputStream in) throws IOException {
		super(in);
		this.header = new byte[4];
		this.headerLength = in.read(this.header);
		this.headerStream = new ByteArrayInputStream(this.header, 0, this.headerLength);
	}

	@Override
	public int read() throws IOException {
		int read = (this.headerStream != null) ? this.headerStream.read() : -1;
		if (read != -1) {
			this.position++;
			if (this.position >= this.headerLength) {
				this.headerStream = null;
			}
			return read;
		}
		return super.read();
	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int read = (this.headerStream != null) ? this.headerStream.read(b, off, len) : -1;
		if (read <= 0) {
			return readRemainder(b, off, len);
		}
		this.position += read;
		if (read < len) {
			int remainderRead = readRemainder(b, off + read, len - read);
			if (remainderRead > 0) {
				read += remainderRead;
			}
		}
		if (this.position >= this.headerLength) {
			this.headerStream = null;
		}
		return read;
	}

	boolean hasZipHeader() {
		return Arrays.equals(this.header, ZIP_HEADER);
	}

	private int readRemainder(byte[] b, int off, int len) throws IOException {
		int read = super.read(b, off, len);
		if (read > 0) {
			this.position += read;
		}
		return read;
	}

}
