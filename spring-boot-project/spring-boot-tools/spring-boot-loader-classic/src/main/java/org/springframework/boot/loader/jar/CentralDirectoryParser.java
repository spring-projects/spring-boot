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

	/**
     * Adds a visitor to the list of visitors for the CentralDirectoryParser.
     * 
     * @param <T> the type of visitor to add
     * @param visitor the visitor to add
     * @return the added visitor
     */
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

	/**
     * Parses the entries in the central directory.
     * 
     * @param endRecord The central directory end record.
     * @param centralDirectoryData The random access data containing the central directory.
     * @throws IOException If an I/O error occurs.
     */
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

	/**
     * Retrieves the archive data from the given RandomAccessData based on the provided CentralDirectoryEndRecord.
     * If the start offset of the archive is 0, the entire data is considered as the archive data.
     * Otherwise, a subsection of the data starting from the offset is returned.
     *
     * @param endRecord The CentralDirectoryEndRecord containing the start offset of the archive.
     * @param data The RandomAccessData containing the archive data.
     * @return The archive data as a RandomAccessData object.
     */
    private RandomAccessData getArchiveData(CentralDirectoryEndRecord endRecord, RandomAccessData data) {
		long offset = endRecord.getStartOfArchive(data);
		if (offset == 0) {
			return data;
		}
		return data.getSubsection(offset, data.getSize() - offset);
	}

	/**
     * Visits the start of the central directory.
     * 
     * @param endRecord the central directory end record
     * @param centralDirectoryData the random access data of the central directory
     */
    private void visitStart(CentralDirectoryEndRecord endRecord, RandomAccessData centralDirectoryData) {
		for (CentralDirectoryVisitor visitor : this.visitors) {
			visitor.visitStart(endRecord, centralDirectoryData);
		}
	}

	/**
     * Visits the file header of a central directory entry.
     * 
     * @param dataOffset the offset of the file data within the archive
     * @param fileHeader the central directory file header to visit
     */
    private void visitFileHeader(long dataOffset, CentralDirectoryFileHeader fileHeader) {
		for (CentralDirectoryVisitor visitor : this.visitors) {
			visitor.visitFileHeader(fileHeader, dataOffset);
		}
	}

	/**
     * Visits the end of the central directory.
     * 
     * This method calls the visitEnd() method of each visitor in the list of visitors.
     * 
     * @see CentralDirectoryVisitor#visitEnd()
     */
    private void visitEnd() {
		for (CentralDirectoryVisitor visitor : this.visitors) {
			visitor.visitEnd();
		}
	}

}
