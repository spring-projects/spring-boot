/*
 * Copyright 2012-2019 the original author or authors.
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
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.loader.data.RandomAccessData;

/**
 * Parses the central directory from a JAR file.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @see CentralDirectoryVisitor
 */
class CentralDirectoryParser {

	private static final int CENTRAL_DIRECTORY_HEADER_BASE_SIZE = 46;

	private final List<CentralDirectoryVisitor> visitors = new ArrayList<>();

	<T extends CentralDirectoryVisitor> T addVisitor(T visitor) {
		this.visitors.add(visitor);
		return visitor;
	}

	/**
	 * Parse the source data, triggering {@link CentralDirectoryVisitor visitors}.
	 * @param data the source data
	 * @param skipPrefixBytes if prefix bytes should be skipped
	 * @return the actual archive data without any prefix bytes
	 * @throws IOException on error
	 */
	RandomAccessData parse(RandomAccessData data, boolean skipPrefixBytes) throws IOException {
		CentralDirectoryEndRecord endRecord = new CentralDirectoryEndRecord(data);
		if (skipPrefixBytes) {
			data = getArchiveData(endRecord, data);
		}
		RandomAccessData centralDirectoryData = endRecord.getCentralDirectory(data);
		visitStart(endRecord, centralDirectoryData);
		parseEntries(endRecord, centralDirectoryData);
		visitEnd();
		return data;
	}

	private void parseEntries(CentralDirectoryEndRecord endRecord, RandomAccessData centralDirectoryData)
			throws IOException {
		byte[] bytes = centralDirectoryData.read(0, centralDirectoryData.getSize());
		CentralDirectoryFileHeader fileHeader = new CentralDirectoryFileHeader();
		int dataOffset = 0;
		for (int i = 0; i < endRecord.getNumberOfRecords(); i++) {
			fileHeader.load(bytes, dataOffset, null, 0, null);
			visitFileHeader(dataOffset, fileHeader);
			dataOffset += CENTRAL_DIRECTORY_HEADER_BASE_SIZE + fileHeader.getName().length()
					+ fileHeader.getComment().length() + fileHeader.getExtra().length;
		}
	}

	private RandomAccessData getArchiveData(CentralDirectoryEndRecord endRecord, RandomAccessData data) {
		long offset = endRecord.getStartOfArchive(data);
		if (offset == 0) {
			return data;
		}
		return data.getSubsection(offset, data.getSize() - offset);
	}

	private void visitStart(CentralDirectoryEndRecord endRecord, RandomAccessData centralDirectoryData) {
		for (CentralDirectoryVisitor visitor : this.visitors) {
			visitor.visitStart(endRecord, centralDirectoryData);
		}
	}

	private void visitFileHeader(int dataOffset, CentralDirectoryFileHeader fileHeader) {
		for (CentralDirectoryVisitor visitor : this.visitors) {
			visitor.visitFileHeader(fileHeader, dataOffset);
		}
	}

	private void visitEnd() {
		for (CentralDirectoryVisitor visitor : this.visitors) {
			visitor.visitEnd();
		}
	}

}
