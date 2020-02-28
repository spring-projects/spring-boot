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

package org.springframework.boot.loader.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;

/**
 * Tests for {@link ImagePackager}
 *
 * @author Phillip Webb
 */
class ImagePackagerTests extends AbstractPackagerTests<ImagePackager> {

	private Map<ZipArchiveEntry, byte[]> entries;

	@Override
	protected ImagePackager createPackager(File source) {
		return new ImagePackager(source);
	}

	@Override
	protected void execute(ImagePackager packager, Libraries libraries) throws IOException {
		this.entries = new LinkedHashMap<>();
		packager.packageImage(libraries, this::save);
	}

	private void save(ZipEntry entry, EntryWriter writer) {
		try {
			this.entries.put((ZipArchiveEntry) entry, getContent(writer));
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private byte[] getContent(EntryWriter writer) throws IOException {
		if (writer == null) {
			return null;
		}
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		writer.write(outputStream);
		return outputStream.toByteArray();
	}

	@Override
	protected Collection<ZipArchiveEntry> getAllPackagedEntries() throws IOException {
		return this.entries.keySet();
	}

	@Override
	protected Manifest getPackagedManifest() throws IOException {
		byte[] bytes = getEntryBytes("META-INF/MANIFEST.MF");
		return (bytes != null) ? new Manifest(new ByteArrayInputStream(bytes)) : null;
	}

	@Override
	protected String getPackagedEntryContent(String name) throws IOException {
		byte[] bytes = getEntryBytes(name);
		return (bytes != null) ? new String(bytes, StandardCharsets.UTF_8) : null;
	}

	private byte[] getEntryBytes(String name) throws IOException {
		ZipEntry entry = getPackagedEntry(name);
		return (entry != null) ? this.entries.get(entry) : null;
	}

}
