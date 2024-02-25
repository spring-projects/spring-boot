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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

/**
 * Adapter class to convert a ZIP file to a {@link TarArchive}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 2.3.0
 */
public class ZipFileTarArchive implements TarArchive {

	static final long NORMALIZED_MOD_TIME = TarArchive.NORMALIZED_TIME.toEpochMilli();

	private final File zip;

	private final Owner owner;

	/**
	 * Creates an archive from the contents of the given {@code zip}. Each entry in the
	 * archive will be owned by the given {@code owner}.
	 * @param zip the zip to use as a source
	 * @param owner the owner of the tar entries
	 */
	public ZipFileTarArchive(File zip, Owner owner) {
		Assert.notNull(zip, "Zip must not be null");
		Assert.notNull(owner, "Owner must not be null");
		assertArchiveHasEntries(zip);
		this.zip = zip;
		this.owner = owner;
	}

	/**
     * Writes the contents of a zip file to a tar archive.
     * 
     * @param outputStream the output stream to write the tar archive to
     * @throws IOException if an I/O error occurs while writing the tar archive
     */
    @Override
	public void writeTo(OutputStream outputStream) throws IOException {
		TarArchiveOutputStream tar = new TarArchiveOutputStream(outputStream);
		tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
		try (ZipFile zipFile = new ZipFile(this.zip)) {
			Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
			while (entries.hasMoreElements()) {
				ZipArchiveEntry zipEntry = entries.nextElement();
				copy(zipEntry, zipFile.getInputStream(zipEntry), tar);
			}
		}
		tar.finish();
	}

	/**
     * Asserts that the given file is a valid archive with entries.
     * 
     * @param file the file to be checked
     * @throws IllegalStateException if the file is not readable
     * @throws IllegalArgumentException if the file is not a valid archive
     */
    private void assertArchiveHasEntries(File file) {
		try (ZipFile zipFile = new ZipFile(file)) {
			Assert.state(zipFile.getEntries().hasMoreElements(), () -> "Archive file '" + file + "' is not valid");
		}
		catch (IOException ex) {
			throw new IllegalStateException("File '" + file + "' is not readable", ex);
		}
	}

	/**
     * Copies a ZipArchiveEntry from a ZipInputStream to a TarArchiveOutputStream.
     * 
     * @param zipEntry The ZipArchiveEntry to be copied.
     * @param zip The InputStream of the Zip file.
     * @param tar The TarArchiveOutputStream to copy the entry to.
     * @throws IOException If an I/O error occurs during the copying process.
     */
    private void copy(ZipArchiveEntry zipEntry, InputStream zip, TarArchiveOutputStream tar) throws IOException {
		TarArchiveEntry tarEntry = convert(zipEntry);
		tar.putArchiveEntry(tarEntry);
		if (tarEntry.isFile()) {
			StreamUtils.copyRange(zip, tar, 0, tarEntry.getSize());
		}
		tar.closeArchiveEntry();
	}

	/**
     * Converts a ZipArchiveEntry to a TarArchiveEntry.
     * 
     * @param zipEntry the ZipArchiveEntry to convert
     * @return the converted TarArchiveEntry
     */
    private TarArchiveEntry convert(ZipArchiveEntry zipEntry) {
		byte linkFlag = (zipEntry.isDirectory()) ? TarConstants.LF_DIR : TarConstants.LF_NORMAL;
		TarArchiveEntry tarEntry = new TarArchiveEntry(zipEntry.getName(), linkFlag, true);
		tarEntry.setUserId(this.owner.getUid());
		tarEntry.setGroupId(this.owner.getGid());
		tarEntry.setModTime(NORMALIZED_MOD_TIME);
		tarEntry.setMode(zipEntry.getUnixMode());
		if (!zipEntry.isDirectory()) {
			tarEntry.setSize(zipEntry.getSize());
		}
		return tarEntry;
	}

}
