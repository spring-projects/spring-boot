/*
 * Copyright 2012-2016 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.springframework.boot.loader.data.RandomAccessData;
import org.springframework.boot.loader.data.RandomAccessData.ResourceAccess;

/**
 * A ZIP File "Central directory file header record" (CDFH).
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @see <a href="http://en.wikipedia.org/wiki/Zip_%28file_format%29">Zip File Format</a>
 */

final class CentralDirectoryFileHeader implements FileHeader {

	private static final AsciiBytes SLASH = new AsciiBytes("/");

	private final byte[] header;

	private AsciiBytes name;

	private final byte[] extra;

	private final AsciiBytes comment;

	private final long localHeaderOffset;

	CentralDirectoryFileHeader(byte[] header, InputStream inputStream)
			throws IOException {
		this.header = header;
		long nameLength = Bytes.littleEndianValue(header, 28, 2);
		long extraLength = Bytes.littleEndianValue(header, 30, 2);
		long commentLength = Bytes.littleEndianValue(header, 32, 2);
		this.name = new AsciiBytes(Bytes.get(inputStream, nameLength));
		this.extra = Bytes.get(inputStream, extraLength);
		this.comment = new AsciiBytes(Bytes.get(inputStream, commentLength));
		this.localHeaderOffset = Bytes.littleEndianValue(header, 42, 4);
	}

	public AsciiBytes getName() {
		return this.name;
	}

	@Override
	public boolean hasName(String name, String suffix) {
		return this.name.equals(new AsciiBytes(suffix == null ? name : name + suffix));
	}

	public boolean isDirectory() {
		return this.name.endsWith(SLASH);
	}

	@Override
	public int getMethod() {
		return (int) Bytes.littleEndianValue(this.header, 10, 2);
	}

	public long getTime() {
		long date = Bytes.littleEndianValue(this.header, 14, 2);
		long time = Bytes.littleEndianValue(this.header, 12, 2);
		return decodeMsDosFormatDateTime(date, time).getTimeInMillis();
	}

	/**
	 * Decode MS-DOS Date Time details. See
	 * <a href="http://mindprod.com/jgloss/zip.html">mindprod.com/jgloss/zip.html</a> for
	 * more details of the format.
	 * @param date the date part
	 * @param time the time part
	 * @return a {@link Calendar} containing the decoded date.
	 */
	private Calendar decodeMsDosFormatDateTime(long date, long time) {
		int year = (int) ((date >> 9) & 0x7F) + 1980;
		int month = (int) ((date >> 5) & 0xF) - 1;
		int day = (int) (date & 0x1F);
		int hours = (int) ((time >> 11) & 0x1F);
		int minutes = (int) ((time >> 5) & 0x3F);
		int seconds = (int) ((time << 1) & 0x3E);
		return new GregorianCalendar(year, month, day, hours, minutes, seconds);
	}

	public long getCrc() {
		return Bytes.littleEndianValue(this.header, 16, 4);
	}

	@Override
	public long getCompressedSize() {
		return Bytes.littleEndianValue(this.header, 20, 4);
	}

	@Override
	public long getSize() {
		return Bytes.littleEndianValue(this.header, 24, 4);
	}

	public byte[] getExtra() {
		return this.extra;
	}

	public AsciiBytes getComment() {
		return this.comment;
	}

	@Override
	public long getLocalHeaderOffset() {
		return this.localHeaderOffset;
	}

	public static CentralDirectoryFileHeader fromRandomAccessData(RandomAccessData data,
			int offset) throws IOException {
		InputStream inputStream = data.getSubsection(offset, data.getSize() - offset)
				.getInputStream(ResourceAccess.ONCE);
		try {
			return fromInputStream(inputStream);
		}
		finally {
			inputStream.close();
		}
	}

	/**
	 * Create a new {@link CentralDirectoryFileHeader} instance from the specified input
	 * stream.
	 * @param inputStream the input stream to load data from
	 * @return a {@link CentralDirectoryFileHeader} or {@code null}
	 * @throws IOException in case of I/O errors
	 */
	static CentralDirectoryFileHeader fromInputStream(InputStream inputStream)
			throws IOException {
		byte[] header = new byte[46];
		if (!Bytes.fill(inputStream, header)) {
			return null;
		}
		return new CentralDirectoryFileHeader(header, inputStream);
	}

}
