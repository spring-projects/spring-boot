/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.gradle.bundling;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.gradle.api.GradleException;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.internal.file.CopyActionProcessingStreamAction;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.internal.file.copy.CopyActionProcessingStream;
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.WorkResult;

import org.springframework.boot.loader.tools.DefaultLaunchScript;
import org.springframework.boot.loader.tools.FileUtils;

/**
 * A {@link CopyAction} for creating a Spring Boot zip archive (typically a jar or war).
 * Stores jar files without compression as required by Spring Boot's loader.
 *
 * @author Andy Wilkinson
 */
class BootZipCopyAction implements CopyAction {

	private final File output;

	private final Spec<FileTreeElement> requiresUnpack;

	private final LaunchScriptConfiguration launchScript;

	private final Set<String> storedPathPrefixes;

	BootZipCopyAction(File output, Spec<FileTreeElement> requiresUnpack,
			LaunchScriptConfiguration launchScript, Set<String> storedPathPrefixes) {
		this.output = output;
		this.requiresUnpack = requiresUnpack;
		this.launchScript = launchScript;
		this.storedPathPrefixes = storedPathPrefixes;
	}

	@Override
	public WorkResult execute(CopyActionProcessingStream stream) {
		ZipOutputStream zipStream;
		try {
			FileOutputStream fileStream = new FileOutputStream(this.output);
			writeLaunchScriptIfNecessary(fileStream);
			zipStream = new ZipOutputStream(fileStream);
			writeLoaderClasses(zipStream);
		}
		catch (IOException ex) {
			throw new GradleException("Failed to create " + this.output, ex);
		}
		try {
			stream.process(new ZipStreamAction(zipStream, this.output,
					this.requiresUnpack, this.storedPathPrefixes));
		}
		finally {
			try {
				zipStream.close();
			}
			catch (IOException ex) {
				// Continue
			}
		}
		return () -> {
			return true;
		};
	}

	private void writeLoaderClasses(ZipOutputStream out) {

		ZipEntry entry;
		try (ZipInputStream in = new ZipInputStream(getClass()
				.getResourceAsStream("/META-INF/loader/spring-boot-loader.jar"))) {
			byte[] buffer = new byte[4096];
			while ((entry = in.getNextEntry()) != null) {
				if (entry.getName().endsWith((".class"))) {
					out.putNextEntry(entry);
					int read;
					while ((read = in.read(buffer)) > 0) {
						out.write(buffer, 0, read);
					}
					out.closeEntry();
				}
			}
		}
		catch (IOException ex) {
			throw new GradleException("Failed to write loader classes", ex);
		}
	}

	private void writeLaunchScriptIfNecessary(FileOutputStream fileStream) {
		try {
			if (this.launchScript.isIncluded()) {
				fileStream.write(new DefaultLaunchScript(this.launchScript.getScript(),
						this.launchScript.getProperties()).toByteArray());
			}
		}
		catch (IOException ex) {
			throw new GradleException("Failed to write launch script to " + this.output,
					ex);
		}
	}

	private static final class ZipStreamAction
			implements CopyActionProcessingStreamAction {

		private final ZipOutputStream zipStream;

		private final File output;

		private final Spec<FileTreeElement> requiresUnpack;

		private final Set<String> storedPathPrefixes;

		private ZipStreamAction(ZipOutputStream zipStream, File output,
				Spec<FileTreeElement> requiresUnpack, Set<String> storedPathPrefixes) {
			this.zipStream = zipStream;
			this.output = output;
			this.requiresUnpack = requiresUnpack;
			this.storedPathPrefixes = storedPathPrefixes;
		}

		@Override
		public void processFile(FileCopyDetailsInternal details) {
			try {
				if (details.isDirectory()) {
					createDirectory(details);
				}
				else {
					createFile(details);
				}
			}
			catch (IOException ex) {
				throw new GradleException(
						"Failed to add " + details + " to " + this.output, ex);
			}
		}

		private void createDirectory(FileCopyDetailsInternal details) throws IOException {
			ZipEntry archiveEntry = new ZipEntry(
					details.getRelativePath().getPathString() + '/');
			archiveEntry.setTime(details.getLastModified());
			this.zipStream.putNextEntry(archiveEntry);
			this.zipStream.closeEntry();
		}

		private void createFile(FileCopyDetailsInternal details) throws IOException {
			String relativePath = details.getRelativePath().getPathString();
			ZipEntry archiveEntry = new ZipEntry(relativePath);
			archiveEntry.setTime(details.getLastModified());
			this.zipStream.putNextEntry(archiveEntry);
			if (isStoredEntry(relativePath)) {
				archiveEntry.setMethod(ZipEntry.STORED);
				archiveEntry.setSize(details.getSize());
				archiveEntry.setCompressedSize(details.getSize());
				Crc32OutputStream crcStream = new Crc32OutputStream(this.zipStream);
				details.copyTo(crcStream);
				archiveEntry.setCrc(crcStream.getCrc());
				if (this.requiresUnpack.isSatisfiedBy(details)) {
					archiveEntry.setComment(
							"UNPACK:" + FileUtils.sha1Hash(details.getFile()));
				}
			}
			else {
				details.copyTo(this.zipStream);
			}
			this.zipStream.closeEntry();
		}

		private boolean isStoredEntry(String relativePath) {
			for (String prefix : this.storedPathPrefixes) {
				if (relativePath.startsWith(prefix)) {
					return true;
				}
			}
			return false;
		}

	}

	/**
	 * A {@code FilterOutputStream} that provides a CRC-32 of the data that is written to
	 * it.
	 */
	private static final class Crc32OutputStream extends FilterOutputStream {

		private final CRC32 crc32 = new CRC32();

		private Crc32OutputStream(OutputStream out) {
			super(out);
		}

		@Override
		public void write(int b) throws IOException {
			this.crc32.update(b);
			this.out.write(b);
		}

		@Override
		public void write(byte[] b) throws IOException {
			this.crc32.update(b);
			this.out.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			this.crc32.update(b, off, len);
			this.out.write(b, off, len);
		}

		private long getCrc() {
			return this.crc32.getValue();
		}

	}

}
