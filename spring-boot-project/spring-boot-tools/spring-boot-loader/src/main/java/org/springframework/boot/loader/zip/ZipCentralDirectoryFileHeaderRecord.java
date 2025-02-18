/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.loader.zip;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.ValueRange;
import java.util.zip.ZipEntry;

import org.springframework.boot.loader.log.DebugLogger;

/**
 * A ZIP File "Central directory file header record" (CDFH).
 *
 * @author Phillip Webb
 * @param versionMadeBy the version that made the zip
 * @param versionNeededToExtract the version needed to extract the zip
 * @param generalPurposeBitFlag the general purpose bit flag
 * @param compressionMethod the compression method used for this entry
 * @param lastModFileTime the last modified file time
 * @param lastModFileDate the last modified file date
 * @param crc32 the CRC32 checksum
 * @param compressedSize the size of the entry when compressed
 * @param uncompressedSize the size of the entry when uncompressed
 * @param fileNameLength the file name length
 * @param extraFieldLength the extra field length
 * @param fileCommentLength the comment length
 * @param diskNumberStart the disk number where the entry starts
 * @param internalFileAttributes the internal file attributes
 * @param externalFileAttributes the external file attributes
 * @param offsetToLocalHeader the relative offset to the local file header
 * @see <a href="https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT">Chapter
 * 4.3.12 of the Zip File Format Specification</a>
 */
record ZipCentralDirectoryFileHeaderRecord(short versionMadeBy, short versionNeededToExtract,
		short generalPurposeBitFlag, short compressionMethod, short lastModFileTime, short lastModFileDate, int crc32,
		int compressedSize, int uncompressedSize, short fileNameLength, short extraFieldLength, short fileCommentLength,
		short diskNumberStart, short internalFileAttributes, int externalFileAttributes, int offsetToLocalHeader) {

	private static final DebugLogger debug = DebugLogger.get(ZipCentralDirectoryFileHeaderRecord.class);

	private static final int SIGNATURE = 0x02014b50;

	private static final int MINIMUM_SIZE = 46;

	/**
	 * The offset of the file name relative to the record start position.
	 */
	static final int FILE_NAME_OFFSET = MINIMUM_SIZE;

	/**
	 * Return the size of this record.
	 * @return the record size
	 */
	long size() {
		return MINIMUM_SIZE + fileNameLength() + extraFieldLength() + fileCommentLength();
	}

	/**
	 * Copy values from this block to the given {@link ZipEntry}.
	 * @param dataBlock the source data block
	 * @param pos the position of this {@link ZipCentralDirectoryFileHeaderRecord}
	 * @param zipEntry the destination zip entry
	 * @throws IOException on I/O error
	 */
	void copyTo(DataBlock dataBlock, long pos, ZipEntry zipEntry) throws IOException {
		int fileNameLength = Short.toUnsignedInt(fileNameLength());
		int extraLength = Short.toUnsignedInt(extraFieldLength());
		int commentLength = Short.toUnsignedInt(fileCommentLength());
		zipEntry.setMethod(Short.toUnsignedInt(compressionMethod()));
		zipEntry.setTime(decodeMsDosFormatDateTime(lastModFileDate(), lastModFileTime()));
		zipEntry.setCrc(Integer.toUnsignedLong(crc32()));
		zipEntry.setCompressedSize(Integer.toUnsignedLong(compressedSize()));
		zipEntry.setSize(Integer.toUnsignedLong(uncompressedSize()));
		if (extraLength > 0) {
			long extraPos = pos + MINIMUM_SIZE + fileNameLength;
			ByteBuffer buffer = ByteBuffer.allocate(extraLength);
			dataBlock.readFully(buffer, extraPos);
			zipEntry.setExtra(buffer.array());
		}
		if (commentLength > 0) {
			long commentPos = pos + MINIMUM_SIZE + fileNameLength + extraLength;
			zipEntry.setComment(ZipString.readString(dataBlock, commentPos, commentLength));
		}
	}

	/**
	 * Decode MS-DOS Date Time details. See <a href=
	 * "https://docs.microsoft.com/en-gb/windows/desktop/api/winbase/nf-winbase-dosdatetimetofiletime">
	 * Microsoft's documentation</a> for more details of the format.
	 * @param date the date
	 * @param time the time
	 * @return the date and time as milliseconds since the epoch
	 */
	private long decodeMsDosFormatDateTime(short date, short time) {
		int year = getChronoValue(((date >> 9) & 0x7f) + 1980, ChronoField.YEAR);
		int month = getChronoValue((date >> 5) & 0x0f, ChronoField.MONTH_OF_YEAR);
		int day = getChronoValue(date & 0x1f, ChronoField.DAY_OF_MONTH);
		int hour = getChronoValue((time >> 11) & 0x1f, ChronoField.HOUR_OF_DAY);
		int minute = getChronoValue((time >> 5) & 0x3f, ChronoField.MINUTE_OF_HOUR);
		int second = getChronoValue((time << 1) & 0x3e, ChronoField.SECOND_OF_MINUTE);
		return ZonedDateTime.of(year, month, day, hour, minute, second, 0, ZoneId.systemDefault())
			.toInstant()
			.truncatedTo(ChronoUnit.SECONDS)
			.toEpochMilli();
	}

	private static int getChronoValue(long value, ChronoField field) {
		ValueRange range = field.range();
		return Math.toIntExact(Math.min(Math.max(value, range.getMinimum()), range.getMaximum()));
	}

	/**
	 * Return a new {@link ZipCentralDirectoryFileHeaderRecord} with a new
	 * {@link #fileNameLength()}.
	 * @param fileNameLength the new file name length
	 * @return a new {@link ZipCentralDirectoryFileHeaderRecord} instance
	 */
	ZipCentralDirectoryFileHeaderRecord withFileNameLength(short fileNameLength) {
		return (this.fileNameLength != fileNameLength) ? new ZipCentralDirectoryFileHeaderRecord(this.versionMadeBy,
				this.versionNeededToExtract, this.generalPurposeBitFlag, this.compressionMethod, this.lastModFileTime,
				this.lastModFileDate, this.crc32, this.compressedSize, this.uncompressedSize, fileNameLength,
				this.extraFieldLength, this.fileCommentLength, this.diskNumberStart, this.internalFileAttributes,
				this.externalFileAttributes, this.offsetToLocalHeader) : this;
	}

	/**
	 * Return a new {@link ZipCentralDirectoryFileHeaderRecord} with a new
	 * {@link #offsetToLocalHeader()}.
	 * @param offsetToLocalHeader the new offset to local header
	 * @return a new {@link ZipCentralDirectoryFileHeaderRecord} instance
	 */
	ZipCentralDirectoryFileHeaderRecord withOffsetToLocalHeader(int offsetToLocalHeader) {
		return (this.offsetToLocalHeader != offsetToLocalHeader) ? new ZipCentralDirectoryFileHeaderRecord(
				this.versionMadeBy, this.versionNeededToExtract, this.generalPurposeBitFlag, this.compressionMethod,
				this.lastModFileTime, this.lastModFileDate, this.crc32, this.compressedSize, this.uncompressedSize,
				this.fileNameLength, this.extraFieldLength, this.fileCommentLength, this.diskNumberStart,
				this.internalFileAttributes, this.externalFileAttributes, offsetToLocalHeader) : this;
	}

	/**
	 * Return the contents of this record as a byte array suitable for writing to a zip.
	 * @return the record as a byte array
	 */
	byte[] asByteArray() {
		ByteBuffer buffer = ByteBuffer.allocate(MINIMUM_SIZE);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(SIGNATURE);
		buffer.putShort(this.versionMadeBy);
		buffer.putShort(this.versionNeededToExtract);
		buffer.putShort(this.generalPurposeBitFlag);
		buffer.putShort(this.compressionMethod);
		buffer.putShort(this.lastModFileTime);
		buffer.putShort(this.lastModFileDate);
		buffer.putInt(this.crc32);
		buffer.putInt(this.compressedSize);
		buffer.putInt(this.uncompressedSize);
		buffer.putShort(this.fileNameLength);
		buffer.putShort(this.extraFieldLength);
		buffer.putShort(this.fileCommentLength);
		buffer.putShort(this.diskNumberStart);
		buffer.putShort(this.internalFileAttributes);
		buffer.putInt(this.externalFileAttributes);
		buffer.putInt(this.offsetToLocalHeader);
		return buffer.array();
	}

	/**
	 * Load the {@link ZipCentralDirectoryFileHeaderRecord} from the given data block.
	 * @param dataBlock the source data block
	 * @param pos the position of the record
	 * @return a new {@link ZipCentralDirectoryFileHeaderRecord} instance
	 * @throws IOException on I/O error
	 */
	static ZipCentralDirectoryFileHeaderRecord load(DataBlock dataBlock, long pos) throws IOException {
		debug.log("Loading CentralDirectoryFileHeaderRecord from position %s", pos);
		ByteBuffer buffer = ByteBuffer.allocate(MINIMUM_SIZE);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		dataBlock.readFully(buffer, pos);
		buffer.rewind();
		int signature = buffer.getInt();
		if (signature != SIGNATURE) {
			debug.log("Found incorrect CentralDirectoryFileHeaderRecord signature %s at position %s", signature, pos);
			throw new IOException("Zip 'Central Directory File Header Record' not found at position " + pos);
		}
		return new ZipCentralDirectoryFileHeaderRecord(buffer.getShort(), buffer.getShort(), buffer.getShort(),
				buffer.getShort(), buffer.getShort(), buffer.getShort(), buffer.getInt(), buffer.getInt(),
				buffer.getInt(), buffer.getShort(), buffer.getShort(), buffer.getShort(), buffer.getShort(),
				buffer.getShort(), buffer.getInt(), buffer.getInt());
	}

}
