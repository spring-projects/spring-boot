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

package org.springframework.boot.gradle.tasks.bundling;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.zip.UnixStat;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.specs.Spec;

/**
 * Internal utility used to copy entries from the {@code spring-boot-loader.jar}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class LoaderZipEntries {

	private Long entryTime;

	LoaderZipEntries(Long entryTime) {
		this.entryTime = entryTime;
	}

	Spec<FileTreeElement> writeTo(ZipArchiveOutputStream zipOutputStream) throws IOException {
		WrittenDirectoriesSpec writtenDirectoriesSpec = new WrittenDirectoriesSpec();
		try (ZipInputStream loaderJar = new ZipInputStream(
				getClass().getResourceAsStream("/META-INF/loader/spring-boot-loader.jar"))) {
			java.util.zip.ZipEntry entry = loaderJar.getNextEntry();
			while (entry != null) {
				if (entry.isDirectory() && !entry.getName().equals("META-INF/")) {
					writeDirectory(new ZipArchiveEntry(entry), zipOutputStream);
					writtenDirectoriesSpec.add(entry);
				}
				else if (entry.getName().endsWith(".class")) {
					writeClass(new ZipArchiveEntry(entry), loaderJar, zipOutputStream);
				}
				entry = loaderJar.getNextEntry();
			}
		}
		return writtenDirectoriesSpec;
	}

	private void writeDirectory(ZipArchiveEntry entry, ZipArchiveOutputStream out) throws IOException {
		prepareEntry(entry, UnixStat.DIR_FLAG | UnixStat.DEFAULT_DIR_PERM);
		out.putArchiveEntry(entry);
		out.closeArchiveEntry();
	}

	private void writeClass(ZipArchiveEntry entry, ZipInputStream in, ZipArchiveOutputStream out) throws IOException {
		prepareEntry(entry, UnixStat.FILE_FLAG | UnixStat.DEFAULT_FILE_PERM);
		out.putArchiveEntry(entry);
		copy(in, out);
		out.closeArchiveEntry();
	}

	private void prepareEntry(ZipArchiveEntry entry, int unixMode) {
		if (this.entryTime != null) {
			entry.setTime(this.entryTime);
		}
		entry.setUnixMode(unixMode);
	}

	private void copy(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[4096];
		int bytesRead = -1;
		while ((bytesRead = in.read(buffer)) != -1) {
			out.write(buffer, 0, bytesRead);
		}
	}

	/**
	 * Spec to track directories that have been written.
	 */
	private static class WrittenDirectoriesSpec implements Spec<FileTreeElement> {

		private final Set<String> entries = new HashSet<>();

		@Override
		public boolean isSatisfiedBy(FileTreeElement element) {
			String path = element.getRelativePath().getPathString();
			if (element.isDirectory() && !path.endsWith(("/"))) {
				path += "/";
			}
			return this.entries.contains(path);
		}

		void add(ZipEntry entry) {
			this.entries.add(entry.getName());
		}

	}

}
