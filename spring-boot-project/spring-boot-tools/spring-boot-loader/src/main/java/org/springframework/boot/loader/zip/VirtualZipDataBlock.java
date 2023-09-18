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

package org.springframework.boot.loader.zip;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileSystem;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link DataBlock} that creates a virtual zip. This class allows us to create virtual
 * zip files that can be parsed by regular JDK classes such as the zip {@link FileSystem}.
 *
 * @author Phillip Webb
 */
class VirtualZipDataBlock extends VirtualDataBlock implements CloseableDataBlock {

	private final FileChannelDataBlock data;

	/**
	 * Create a new {@link VirtualZipDataBlock} for the given entries.
	 * @param data the source zip data
	 * @param nameOffsetLookups the name offsets to apply
	 * @param centralRecords the records that should be copied to the virtual zip
	 * @param centralRecordPositions the record positions in the data block.
	 * @throws IOException on I/O error
	 */
	VirtualZipDataBlock(FileChannelDataBlock data, NameOffsetLookups nameOffsetLookups,
			ZipCentralDirectoryFileHeaderRecord[] centralRecords, long[] centralRecordPositions) throws IOException {
		this.data = data;
		List<DataBlock> parts = new ArrayList<>();
		List<DataBlock> centralParts = new ArrayList<>();
		long offset = 0;
		long sizeOfCentralDirectory = 0;
		for (int i = 0; i < centralRecords.length; i++) {
			ZipCentralDirectoryFileHeaderRecord centralRecord = centralRecords[i];
			int nameOffset = nameOffsetLookups.get(i);
			long centralRecordPos = centralRecordPositions[i];
			DataBlock name = new DataPart(
					centralRecordPos + ZipCentralDirectoryFileHeaderRecord.FILE_NAME_OFFSET + nameOffset,
					(centralRecord.fileNameLength() & 0xFFFF) - nameOffset);
			ZipLocalFileHeaderRecord localRecord = ZipLocalFileHeaderRecord.load(this.data,
					centralRecord.offsetToLocalHeader());
			DataBlock content = new DataPart(centralRecord.offsetToLocalHeader() + localRecord.size(),
					centralRecord.compressedSize());
			sizeOfCentralDirectory += addToCentral(centralParts, centralRecord, centralRecordPos, name, (int) offset);
			offset += addToLocal(parts, localRecord, name, content);
		}
		parts.addAll(centralParts);
		ZipEndOfCentralDirectoryRecord eocd = new ZipEndOfCentralDirectoryRecord((short) centralRecords.length,
				(int) sizeOfCentralDirectory, (int) offset);
		parts.add(new ByteArrayDataBlock(eocd.asByteArray()));
		setParts(parts);
	}

	private long addToCentral(List<DataBlock> parts, ZipCentralDirectoryFileHeaderRecord originalRecord,
			long originalRecordPos, DataBlock name, int offsetToLocalHeader) throws IOException {
		ZipCentralDirectoryFileHeaderRecord record = originalRecord.withFileNameLength((short) (name.size() & 0xFFFF))
			.withOffsetToLocalHeader(offsetToLocalHeader);
		int originalExtraFieldLength = originalRecord.extraFieldLength() & 0xFFFF;
		int originalFileCommentLength = originalRecord.fileCommentLength() & 0xFFFF;
		DataBlock extraFieldAndComment = new DataPart(
				originalRecordPos + originalRecord.size() - originalExtraFieldLength - originalFileCommentLength,
				originalExtraFieldLength + originalFileCommentLength);
		parts.add(new ByteArrayDataBlock(record.asByteArray()));
		parts.add(name);
		parts.add(extraFieldAndComment);
		return record.size();
	}

	private long addToLocal(List<DataBlock> parts, ZipLocalFileHeaderRecord originalRecord, DataBlock name,
			DataBlock content) throws IOException {
		ZipLocalFileHeaderRecord record = originalRecord.withExtraFieldLength((short) 0)
			.withFileNameLength((short) (name.size() & 0xFFFF));
		parts.add(new ByteArrayDataBlock(record.asByteArray()));
		parts.add(name);
		parts.add(content);
		return record.size() + content.size();
	}

	@Override
	public void close() throws IOException {
		this.data.close();
	}

	/**
	 * {@link DataBlock} that points to part of the original data block.
	 */
	final class DataPart implements DataBlock {

		private final long offset;

		private final long size;

		DataPart(long offset, long size) {
			this.offset = offset;
			this.size = size;
		}

		@Override
		public long size() throws IOException {
			return this.size;
		}

		@Override
		public int read(ByteBuffer dst, long pos) throws IOException {
			int remaining = (int) (this.size - pos);
			if (remaining <= 0) {
				return -1;
			}
			int originalLimit = -1;
			if (dst.remaining() > remaining) {
				originalLimit = dst.limit();
				dst.limit(dst.position() + remaining);
			}
			int result = VirtualZipDataBlock.this.data.read(dst, this.offset + pos);
			if (originalLimit != -1) {
				dst.limit(originalLimit);
			}
			return result;
		}

	}

}
