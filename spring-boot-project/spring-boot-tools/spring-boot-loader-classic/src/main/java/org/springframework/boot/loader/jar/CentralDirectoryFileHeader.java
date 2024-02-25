/*
 * Copyright 2012-2023 the original author or authors.
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.ValueRange;

import org.springframework.boot.loader.data.RandomAccessData;

/**
 * A ZIP File "Central directory file header record" (CDFH).
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Dmytro Nosan
 * @see <a href="https://en.wikipedia.org/wiki/Zip_%28file_format%29">Zip File Format</a>
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

	/**
	 * Creates a new instance of the CentralDirectoryFileHeader class.
	 */
	CentralDirectoryFileHeader() {
	}

	/**
	 * Constructs a new CentralDirectoryFileHeader object with the specified parameters.
	 * @param header the byte array containing the header data
	 * @param headerOffset the offset in the byte array where the header starts
	 * @param name the name of the file
	 * @param extra the extra data associated with the file
	 * @param comment the comment associated with the file
	 * @param localHeaderOffset the offset in the archive where the local file header
	 * starts
	 */
	CentralDirectoryFileHeader(byte[] header, int headerOffset, AsciiBytes name, byte[] extra, AsciiBytes comment,
			long localHeaderOffset) {
		this.header = header;
		this.headerOffset = headerOffset;
		this.name = name;
		this.extra = extra;
		this.comment = comment;
		this.localHeaderOffset = localHeaderOffset;
	}

	/**
	 * Loads the data for a CentralDirectoryFileHeader object.
	 * @param data The byte array containing the data to load.
	 * @param dataOffset The offset in the byte array where the data starts.
	 * @param variableData The RandomAccessData object containing the variable part of the
	 * data.
	 * @param variableOffset The offset in the RandomAccessData object where the variable
	 * part starts.
	 * @param filter The JarEntryFilter object used to filter the name of the file.
	 * @throws IOException If an I/O error occurs while loading the data.
	 */
	void load(byte[] data, int dataOffset, RandomAccessData variableData, long variableOffset, JarEntryFilter filter)
			throws IOException {
		// Load fixed part
		this.header = data;
		this.headerOffset = dataOffset;
		long compressedSize = Bytes.littleEndianValue(data, dataOffset + 20, 4);
		long uncompressedSize = Bytes.littleEndianValue(data, dataOffset + 24, 4);
		long nameLength = Bytes.littleEndianValue(data, dataOffset + 28, 2);
		long extraLength = Bytes.littleEndianValue(data, dataOffset + 30, 2);
		long commentLength = Bytes.littleEndianValue(data, dataOffset + 32, 2);
		long localHeaderOffset = Bytes.littleEndianValue(data, dataOffset + 42, 4);
		// Load variable part
		dataOffset += 46;
		if (variableData != null) {
			data = variableData.read(variableOffset + 46, nameLength + extraLength + commentLength);
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
			System.arraycopy(data, (int) (dataOffset + nameLength), this.extra, 0, this.extra.length);
		}
		this.localHeaderOffset = getLocalHeaderOffset(compressedSize, uncompressedSize, localHeaderOffset, this.extra);
		if (commentLength > 0) {
			this.comment = new AsciiBytes(data, (int) (dataOffset + nameLength + extraLength), (int) commentLength);
		}
	}

	/**
	 * Returns the offset of the local file header for this file entry in the ZIP file.
	 * @param compressedSize the compressed size of the file entry
	 * @param uncompressedSize the uncompressed size of the file entry
	 * @param localHeaderOffset the offset of the local file header in the ZIP file
	 * @param extra the extra field data of the file entry
	 * @return the offset of the local file header
	 * @throws IOException if the Zip64 Extended Information Extra Field is not found
	 */
	private long getLocalHeaderOffset(long compressedSize, long uncompressedSize, long localHeaderOffset, byte[] extra)
			throws IOException {
		if (localHeaderOffset != 0xFFFFFFFFL) {
			return localHeaderOffset;
		}
		int extraOffset = 0;
		while (extraOffset < extra.length - 2) {
			int id = (int) Bytes.littleEndianValue(extra, extraOffset, 2);
			int length = (int) Bytes.littleEndianValue(extra, extraOffset, 2);
			extraOffset += 4;
			if (id == 1) {
				int localHeaderExtraOffset = 0;
				if (compressedSize == 0xFFFFFFFFL) {
					localHeaderExtraOffset += 4;
				}
				if (uncompressedSize == 0xFFFFFFFFL) {
					localHeaderExtraOffset += 4;
				}
				return Bytes.littleEndianValue(extra, extraOffset + localHeaderExtraOffset, 8);
			}
			extraOffset += length;
		}
		throw new IOException("Zip64 Extended Information Extra Field not found");
	}

	/**
	 * Returns the name of the CentralDirectoryFileHeader.
	 * @return the name of the CentralDirectoryFileHeader
	 */
	AsciiBytes getName() {
		return this.name;
	}

	/**
	 * Checks if the CentralDirectoryFileHeader has the specified name and suffix.
	 * @param name the name to check against
	 * @param suffix the suffix to check against
	 * @return true if the CentralDirectoryFileHeader has the specified name and suffix,
	 * false otherwise
	 */
	@Override
	public boolean hasName(CharSequence name, char suffix) {
		return this.name.matches(name, suffix);
	}

	/**
	 * Returns a boolean value indicating whether the current file is a directory.
	 * @return true if the current file is a directory, false otherwise.
	 */
	boolean isDirectory() {
		return this.name.endsWith(SLASH);
	}

	/**
	 * Returns the method value from the Central Directory File Header.
	 * @return The method value as an integer.
	 */
	@Override
	public int getMethod() {
		return (int) Bytes.littleEndianValue(this.header, this.headerOffset + 10, 2);
	}

	/**
	 * Returns the time in milliseconds when the file was last modified.
	 * @return the time in milliseconds
	 */
	long getTime() {
		long datetime = Bytes.littleEndianValue(this.header, this.headerOffset + 12, 4);
		return decodeMsDosFormatDateTime(datetime);
	}

	/**
	 * Decode MS-DOS Date Time details. See <a href=
	 * "https://docs.microsoft.com/en-gb/windows/desktop/api/winbase/nf-winbase-dosdatetimetofiletime">
	 * Microsoft's documentation</a> for more details of the format.
	 * @param datetime the date and time
	 * @return the date and time as milliseconds since the epoch
	 */
	private long decodeMsDosFormatDateTime(long datetime) {
		int year = getChronoValue(((datetime >> 25) & 0x7f) + 1980, ChronoField.YEAR);
		int month = getChronoValue((datetime >> 21) & 0x0f, ChronoField.MONTH_OF_YEAR);
		int day = getChronoValue((datetime >> 16) & 0x1f, ChronoField.DAY_OF_MONTH);
		int hour = getChronoValue((datetime >> 11) & 0x1f, ChronoField.HOUR_OF_DAY);
		int minute = getChronoValue((datetime >> 5) & 0x3f, ChronoField.MINUTE_OF_HOUR);
		int second = getChronoValue((datetime << 1) & 0x3e, ChronoField.SECOND_OF_MINUTE);
		return ZonedDateTime.of(year, month, day, hour, minute, second, 0, ZoneId.systemDefault())
			.toInstant()
			.truncatedTo(ChronoUnit.SECONDS)
			.toEpochMilli();
	}

	/**
	 * Returns the CRC value of the Central Directory File Header.
	 * @return the CRC value as a long
	 */
	long getCrc() {
		return Bytes.littleEndianValue(this.header, this.headerOffset + 16, 4);
	}

	/**
	 * Returns the compressed size of the file.
	 * @return The compressed size of the file.
	 */
	@Override
	public long getCompressedSize() {
		return Bytes.littleEndianValue(this.header, this.headerOffset + 20, 4);
	}

	/**
	 * Returns the size of the file.
	 * @return The size of the file in bytes.
	 */
	@Override
	public long getSize() {
		return Bytes.littleEndianValue(this.header, this.headerOffset + 24, 4);
	}

	/**
	 * Returns the extra data of the CentralDirectoryFileHeader.
	 * @return the extra data as a byte array
	 */
	byte[] getExtra() {
		return this.extra;
	}

	/**
	 * Checks if the CentralDirectoryFileHeader has any extra data.
	 * @return true if the CentralDirectoryFileHeader has extra data, false otherwise.
	 */
	boolean hasExtra() {
		return this.extra.length > 0;
	}

	/**
	 * Returns the comment associated with this CentralDirectoryFileHeader.
	 * @return the comment associated with this CentralDirectoryFileHeader
	 */
	AsciiBytes getComment() {
		return this.comment;
	}

	/**
	 * Returns the offset of the local header for this central directory file header.
	 * @return the offset of the local header
	 */
	@Override
	public long getLocalHeaderOffset() {
		return this.localHeaderOffset;
	}

	/**
	 * Creates and returns a deep copy of the CentralDirectoryFileHeader object.
	 * @return a new CentralDirectoryFileHeader object that is a clone of the current
	 * object
	 */
	@Override
	public CentralDirectoryFileHeader clone() {
		byte[] header = new byte[46];
		System.arraycopy(this.header, this.headerOffset, header, 0, header.length);
		return new CentralDirectoryFileHeader(header, 0, this.name, header, this.comment, this.localHeaderOffset);
	}

	/**
	 * Creates a CentralDirectoryFileHeader object from the given RandomAccessData,
	 * offset, and JarEntryFilter.
	 * @param data the RandomAccessData containing the file header information
	 * @param offset the offset in the RandomAccessData where the file header starts
	 * @param filter the JarEntryFilter used to filter the file header
	 * @return the CentralDirectoryFileHeader object created from the given data and
	 * offset
	 * @throws IOException if an I/O error occurs while reading the data
	 */
	static CentralDirectoryFileHeader fromRandomAccessData(RandomAccessData data, long offset, JarEntryFilter filter)
			throws IOException {
		CentralDirectoryFileHeader fileHeader = new CentralDirectoryFileHeader();
		byte[] bytes = data.read(offset, 46);
		fileHeader.load(bytes, 0, data, offset, filter);
		return fileHeader;
	}

	/**
	 * Returns the value of the specified ChronoField within the given range.
	 * @param value the value to be checked
	 * @param field the ChronoField to retrieve the value from
	 * @return the value of the ChronoField within the specified range
	 * @throws ArithmeticException if the value is out of the range of the ChronoField
	 */
	private static int getChronoValue(long value, ChronoField field) {
		ValueRange range = field.range();
		return Math.toIntExact(Math.min(Math.max(value, range.getMinimum()), range.getMaximum()));
	}

}
