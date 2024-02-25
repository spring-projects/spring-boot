/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.buildpack.platform.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;

import org.springframework.util.StreamUtils;

/**
 * {@link Layout} for writing TAR archive content directly to an {@link OutputStream}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class TarLayoutWriter implements Layout, Closeable {

	static final long NORMALIZED_MOD_TIME = TarArchive.NORMALIZED_TIME.toEpochMilli();

	private final TarArchiveOutputStream outputStream;

	/**
     * Constructs a new TarLayoutWriter object with the specified output stream.
     * 
     * @param outputStream the output stream to write the tar archive to
     */
    TarLayoutWriter(OutputStream outputStream) {
		this.outputStream = new TarArchiveOutputStream(outputStream);
		this.outputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
	}

	/**
     * Creates a directory entry in the archive with the specified name, owner, and mode.
     * 
     * @param name  the name of the directory
     * @param owner the owner of the directory
     * @param mode  the mode of the directory
     * @throws IOException if an I/O error occurs
     */
    @Override
	public void directory(String name, Owner owner, int mode) throws IOException {
		this.outputStream.putArchiveEntry(createDirectoryEntry(name, owner, mode));
		this.outputStream.closeArchiveEntry();
	}

	/**
     * Writes a file to the archive with the specified name, owner, mode, and content.
     * 
     * @param name the name of the file to be written
     * @param owner the owner of the file
     * @param mode the mode of the file
     * @param content the content of the file
     * @throws IOException if an I/O error occurs while writing the file
     */
    @Override
	public void file(String name, Owner owner, int mode, Content content) throws IOException {
		this.outputStream.putArchiveEntry(createFileEntry(name, owner, mode, content.size()));
		content.writeTo(StreamUtils.nonClosing(this.outputStream));
		this.outputStream.closeArchiveEntry();
	}

	/**
     * Creates a TarArchiveEntry for a directory with the specified name, owner, and mode.
     * 
     * @param name The name of the directory.
     * @param owner The owner of the directory.
     * @param mode The mode of the directory.
     * @return The TarArchiveEntry representing the directory.
     */
    private TarArchiveEntry createDirectoryEntry(String name, Owner owner, int mode) {
		return createEntry(name, owner, TarConstants.LF_DIR, mode, 0);
	}

	/**
     * Creates a new TarArchiveEntry for a file with the specified name, owner, mode, and size.
     * 
     * @param name the name of the file
     * @param owner the owner of the file
     * @param mode the mode of the file
     * @param size the size of the file
     * @return the created TarArchiveEntry
     */
    private TarArchiveEntry createFileEntry(String name, Owner owner, int mode, int size) {
		return createEntry(name, owner, TarConstants.LF_NORMAL, mode, size);
	}

	/**
     * Creates a TarArchiveEntry object with the specified parameters.
     * 
     * @param name the name of the entry
     * @param owner the owner of the entry
     * @param linkFlag the link flag of the entry
     * @param mode the mode of the entry
     * @param size the size of the entry
     * @return the created TarArchiveEntry object
     */
    private TarArchiveEntry createEntry(String name, Owner owner, byte linkFlag, int mode, int size) {
		TarArchiveEntry entry = new TarArchiveEntry(name, linkFlag, true);
		entry.setUserId(owner.getUid());
		entry.setGroupId(owner.getGid());
		entry.setMode(mode);
		entry.setModTime(NORMALIZED_MOD_TIME);
		entry.setSize(size);
		return entry;
	}

	/**
     * Finishes writing to the output stream.
     * 
     * @throws IOException if an I/O error occurs
     */
    void finish() throws IOException {
		this.outputStream.finish();
	}

	/**
     * Closes the output stream associated with this TarLayoutWriter.
     * 
     * @throws IOException if an I/O error occurs while closing the output stream
     */
    @Override
	public void close() throws IOException {
		this.outputStream.close();
	}

}
