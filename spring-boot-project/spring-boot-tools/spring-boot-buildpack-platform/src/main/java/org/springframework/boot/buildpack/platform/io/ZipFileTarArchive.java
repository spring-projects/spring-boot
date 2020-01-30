/*
 * Copyright 2012-2020 the original author or authors.
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
		this.zip = zip;
		this.owner = owner;
	}

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

	private void copy(ZipArchiveEntry zipEntry, InputStream zip, TarArchiveOutputStream tar) throws IOException {
		TarArchiveEntry tarEntry = convert(zipEntry);
		tar.putArchiveEntry(tarEntry);
		if (tarEntry.isFile()) {
			StreamUtils.copyRange(zip, tar, 0, tarEntry.getSize());
		}
		tar.closeArchiveEntry();
	}

	private TarArchiveEntry convert(ZipArchiveEntry zipEntry) {
		byte linkFlag = (zipEntry.isDirectory()) ? TarConstants.LF_DIR : TarConstants.LF_NORMAL;
		TarArchiveEntry tarEntry = new TarArchiveEntry(zipEntry.getName(), linkFlag, true);
		tarEntry.setUserId(this.owner.getUid());
		tarEntry.setGroupId(this.owner.getGid());
		tarEntry.setModTime(NORMALIZED_MOD_TIME);
		if (!zipEntry.isDirectory()) {
			tarEntry.setSize(zipEntry.getSize());
		}
		return tarEntry;
	}

}
