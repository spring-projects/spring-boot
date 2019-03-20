/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.loader.jar;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.springframework.boot.loader.data.RandomAccessData;

/**
 * A ZIP File "Central directory file header record" (CDFH).
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @see <a href="http://en.wikipedia.org/wiki/Zip_%28file_format%29">Zip File Format</a>
 */

final class CentralDirectoryFileHeader implements FileHeader {

	private static final AsciiBytes SLASH = new AsciiBytes("/");

	private static final byte[] NO_EXTRA = {};

	private static final AsciiBytes NO_COMMENT = new AsciiBytes("");

	private byte[] header;

	private int headerOffset;

	private AsciiBytes name;

	private byte[] extra;

	private AsciiBytes comment;

	private long localHeaderOffset;

	CentralDirectoryFileHeader() {
	}

	CentralDirectoryFileHeader(byte[] header, int headerOffset, AsciiBytes name,
			byte[] extra, AsciiBytes comment, long localHeaderOffset) {
		super();
		this.header = header;
		this.headerOffset = headerOffset;
		this.name = name;
		this.extra = extra;
		this.comment = comment;
		this.localHeaderOffset = localHeaderOffset;
	}

	void load(byte[] data, int dataOffset, RandomAccessData variableData,
			int variableOffset, JarEntryFilter filter) throws IOException {
		// Load fixed part
		this.header = data;
		this.headerOffset = dataOffset;
		long nameLength = Bytes.littleEndianValue(data, dataOffset + 28, 2);
		long extraLength = Bytes.littleEndianValue(data, dataOffset + 30, 2);
		long commentLength = Bytes.littleEndianValue(data, dataOffset + 32, 2);
		this.localHeaderOffset = Bytes.littleEndianValue(data, dataOffset + 42, 4);
		// Load variable part
		dataOffset += 46;
		if (variableData != null) {
			data = Bytes.get(variableData.getSubsection(variableOffset + 46,
					nameLength + extraLength + commentLength));
			dataOffset = 0;
		}
		this.name = new AsciiBytes(data, dataOffset, (int) nameLength);
		if (filter != null) {
			this.name = filter.apply(this.name);
		}
		this.extra = NO_EXTRA;
		this.comment = NO_COMMENT;
		if (extraLength > 0) {
			this.extra = new byte[(int) extraLength];
			System.arraycopy(data, (int) (dataOffset + nameLength), this.extra, 0,
					this.extra.length);
		}
		if (commentLength > 0) {
			this.comment = new AsciiBytes(data,
					(int) (dataOffset + nameLength + extraLength), (int) commentLength);
		}
	}

	public AsciiBytes getName() {
		return this.name;
	}

	@Override
	public boolean hasName(String name, String suffix) {
		return this.name.equals(new AsciiBytes((suffix != null) ? name + suffix : name));
	}

	public boolean isDirectory() {
		return this.name.endsWith(SLASH);
	}

	@Override
	public int getMethod() {
		return (int) Bytes.littleEndianValue(this.header, this.headerOffset + 10, 2);
	}

	public long getTime() {
		long date = Bytes.littleEndianValue(this.header, this.headerOffset + 14, 2);
		long time = Bytes.littleEndianValue(this.header, this.headerOffset + 12, 2);
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
		return Bytes.littleEndianValue(this.header, this.headerOffset + 16, 4);
	}

	@Override
	public long getCompressedSize() {
		return Bytes.littleEndianValue(this.header, this.headerOffset + 20, 4);
	}

	@Override
	public long getSize() {
		return Bytes.littleEndianValue(this.header, this.headerOffset + 24, 4);
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

	@Override
	public CentralDirectoryFileHeader clone() {
		byte[] header = new byte[46];
		System.arraycopy(this.header, this.headerOffset, header, 0, header.length);
		return new CentralDirectoryFileHeader(header, 0, this.name, header, this.comment,
				this.localHeaderOffset);
	}

	public static CentralDirectoryFileHeader fromRandomAccessData(RandomAccessData data,
			int offset, JarEntryFilter filter) throws IOException {
		CentralDirectoryFileHeader fileHeader = new CentralDirectoryFileHeader();
		byte[] bytes = Bytes.get(data.getSubsection(offset, 46));
		fileHeader.load(bytes, 0, data, offset, filter);
		return fileHeader;
	}

}
