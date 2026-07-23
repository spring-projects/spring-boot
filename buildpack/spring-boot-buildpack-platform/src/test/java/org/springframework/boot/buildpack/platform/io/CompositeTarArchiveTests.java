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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CompositeTarArchive}.
 *
 * @author Vasily Pelikh
 */
class CompositeTarArchiveTests {

	@TempDir
	@SuppressWarnings("NullAway.Init")
	Path tempDir;

	@Test
	void mergesPrimaryAndAdditionalEntries() throws IOException {
		TarArchive primary = TarArchive
			.of((layout) -> layout.file("/BOOT-INF/classes/Main.class", Owner.ROOT, Content.of("classbytes")));
		Path additional = this.tempDir.resolve("aot-cache");
		Files.createDirectories(additional);
		Files.writeString(additional.resolve("application.aot"), "cache-data");
		CompositeTarArchive composite = new CompositeTarArchive(primary, additional, "aot-cache");
		List<TarArchiveEntry> entries = readEntries(composite);
		assertThat(entries).hasSize(2);
		assertThat(entries.get(0).getName()).isEqualTo("/BOOT-INF/classes/Main.class");
		assertThat(entries.get(1).getName()).isEqualTo("aot-cache/application.aot");
	}

	@Test
	void additionalDirectoryNotFoundIsNoOp() throws IOException {
		TarArchive primary = TarArchive
			.of((layout) -> layout.file("/BOOT-INF/classes/Main.class", Owner.ROOT, Content.of("classbytes")));
		Path missing = this.tempDir.resolve("nonexistent");
		CompositeTarArchive composite = new CompositeTarArchive(primary, missing, "aot-cache");
		List<TarArchiveEntry> entries = readEntries(composite);
		assertThat(entries).hasSize(1);
		assertThat(entries.get(0).getName()).isEqualTo("/BOOT-INF/classes/Main.class");
	}

	@Test
	void additionalEntriesUseRootOwner() throws IOException {
		TarArchive primary = TarArchive.of((layout) -> layout.directory("/BOOT-INF", Owner.ROOT));
		Path additional = this.tempDir.resolve("extra");
		Files.createDirectories(additional);
		Files.writeString(additional.resolve("file.txt"), "content");
		CompositeTarArchive composite = new CompositeTarArchive(primary, additional, "extra");
		List<TarArchiveEntry> entries = readEntries(composite);
		assertThat(entries).hasSize(2);
		assertThat(entries.get(1).getName()).isEqualTo("extra/file.txt");
		assertThat(entries.get(1).getLongUserId()).isZero();
		assertThat(entries.get(1).getLongGroupId()).isZero();
	}

	@Test
	void handlesNestedAdditionalDirectories() throws IOException {
		TarArchive primary = TarArchive.of((layout) -> layout.file("/root.txt", Owner.ROOT, Content.of("root")));
		Path additional = this.tempDir.resolve("nested");
		Path subDir = additional.resolve("sub/deep");
		Files.createDirectories(subDir);
		Files.writeString(additional.resolve("top.txt"), "top");
		Files.writeString(subDir.resolve("deep.txt"), "deep");
		CompositeTarArchive composite = new CompositeTarArchive(primary, additional, "cache");
		List<TarArchiveEntry> entries = readEntries(composite);
		assertThat(entries).hasSizeGreaterThanOrEqualTo(3);
		List<String> names = new ArrayList<>();
		for (TarArchiveEntry entry : entries) {
			names.add(entry.getName());
		}
		assertThat(names).contains("cache/top.txt", "cache/sub/deep/deep.txt");
	}

	@Test
	void emptyAdditionalDirectoryMergesOnlyPrimary() throws IOException {
		TarArchive primary = TarArchive.of((layout) -> layout.file("/file.txt", Owner.ROOT, Content.of("data")));
		Path additional = this.tempDir.resolve("empty");
		Files.createDirectories(additional);
		CompositeTarArchive composite = new CompositeTarArchive(primary, additional, "prefix");
		List<TarArchiveEntry> entries = readEntries(composite);
		assertThat(entries).hasSize(1);
		assertThat(entries.get(0).getName()).isEqualTo("/file.txt");
	}

	private List<TarArchiveEntry> readEntries(TarArchive archive) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		archive.writeTo(out);
		List<TarArchiveEntry> entries = new ArrayList<>();
		try (TarArchiveInputStream tarIn = new TarArchiveInputStream(new ByteArrayInputStream(out.toByteArray()))) {
			TarArchiveEntry entry = tarIn.getNextEntry();
			while (entry != null) {
				entries.add(entry);
				entry = tarIn.getNextEntry();
			}
		}
		return entries;
	}

}
