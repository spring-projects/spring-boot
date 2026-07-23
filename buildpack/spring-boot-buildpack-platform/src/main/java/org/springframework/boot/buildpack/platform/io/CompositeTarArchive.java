/*
 * Copyright 2012-present the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import org.springframework.util.Assert;

/**
 * A {@link TarArchive} that merges entries from a primary archive with entries from an
 * additional content directory. Additional entries are prefixed with a directory path.
 *
 * @author Vasily Pelikh
 * @since 4.2.0
 */
public class CompositeTarArchive implements TarArchive {

	private final TarArchive primary;

	private final Path additionalContent;

	private final String prefix;

	public CompositeTarArchive(TarArchive primary, Path additionalContent, String prefix) {
		Assert.notNull(primary, "'primary' must not be null");
		Assert.notNull(additionalContent, "'additionalContent' must not be null");
		Assert.hasText(prefix, "'prefix' must not be empty");
		this.primary = primary;
		this.additionalContent = additionalContent;
		this.prefix = prefix;
	}

	@Override
	public void writeTo(OutputStream outputStream) throws IOException {
		ByteArrayOutputStream primaryBuffer = new ByteArrayOutputStream();
		this.primary.writeTo(primaryBuffer);
		TarArchive composite = TarArchive.of((layout) -> {
			writePrimaryEntries(layout, primaryBuffer);
			if (Files.isDirectory(this.additionalContent)) {
				collectAdditionalEntries(layout, this.additionalContent, this.prefix);
			}
		});
		composite.writeTo(outputStream);
	}

	private void writePrimaryEntries(Layout layout, ByteArrayOutputStream primaryBuffer) throws IOException {
		try (TarArchiveInputStream tarIn = new TarArchiveInputStream(
				new ByteArrayInputStream(primaryBuffer.toByteArray()))) {
			TarArchiveEntry entry = tarIn.getNextEntry();
			while (entry != null) {
				if (entry.isDirectory()) {
					layout.directory(entry.getName(), Owner.of(entry.getLongUserId(), entry.getLongGroupId()),
							entry.getMode());
				}
				else {
					Content content = Content.of((int) entry.getSize(), () -> tarIn);
					layout.file(entry.getName(), Owner.of(entry.getLongUserId(), entry.getLongGroupId()),
							entry.getMode(), content);
				}
				entry = tarIn.getNextEntry();
			}
		}
	}

	private void collectAdditionalEntries(Layout layout, Path directory, String prefix) throws IOException {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
			for (Path entry : stream) {
				String relativePath = directory.relativize(entry).toString().replace('\\', '/');
				String entryName = prefix + "/" + relativePath;
				if (Files.isRegularFile(entry)) {
					layout.file(entryName, Owner.ROOT, Content.of(entry.toFile()));
				}
				else if (Files.isDirectory(entry)) {
					collectAdditionalEntries(layout, entry, entryName);
				}
			}
		}
	}

}
