/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.loader.jar;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.boot.loader.data.RandomAccessData;

/**
 * A {@link ZipInputStream} backed by {@link RandomAccessData}. Parsed entries provide
 * access to the underlying data {@link RandomAccessData#getSubsection(long, long)
 * subsection}.
 * 
 * @author Phillip Webb
 */
public class RandomAccessDataZipInputStream extends ZipInputStream {

	private RandomAccessData data;

	private TrackingInputStream trackingInputStream;

	/**
	 * Create a new {@link RandomAccessData} instance.
	 * @param data the source of the zip stream
	 */
	public RandomAccessDataZipInputStream(RandomAccessData data) {
		this(data, new TrackingInputStream(data.getInputStream()));
	}

	/**
	 * Private constructor used so that we can call the super constructor with a
	 * {@link TrackingInputStream}.
	 * @param data the source of the zip stream
	 * @param trackingInputStream a tracking input stream
	 */
	private RandomAccessDataZipInputStream(RandomAccessData data,
			TrackingInputStream trackingInputStream) {
		super(trackingInputStream);
		this.data = data;
		this.trackingInputStream = trackingInputStream;
	}

	@Override
	public RandomAccessDataZipEntry getNextEntry() throws IOException {
		ZipEntry entry = super.getNextEntry();
		if (entry == null) {
			return null;
		}
		int start = getPosition();
		closeEntry();
		int end = getPosition();
		RandomAccessData entryData = this.data.getSubsection(start, end - start);
		return new RandomAccessDataZipEntry(entry, entryData);
	}

	private int getPosition() throws IOException {
		int pushback = ((PushbackInputStream) this.in).available();
		return this.trackingInputStream.getPosition() - pushback;
	}

	/**
	 * Internal stream that tracks reads to provide a position.
	 */
	private static class TrackingInputStream extends FilterInputStream {

		private int position = 0;

		protected TrackingInputStream(InputStream in) {
			super(in);
		}

		@Override
		public int read() throws IOException {
			return moveOn(super.read(), true);
		}

		@Override
		public int read(byte[] b) throws IOException {
			return moveOn(super.read(b), false);
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			return moveOn(super.read(b, off, len), false);
		}

		private int moveOn(int amount, boolean singleByteRead) {
			this.position += (amount == -1 ? 0 : (singleByteRead ? 1 : amount));
			return amount;
		}

		@Override
		public int available() throws IOException {
			// Always return 0 so that we can accurately use PushbackInputStream.available
			return 0;
		}

		public int getPosition() {
			return this.position;
		}
	}

}
