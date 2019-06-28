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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.function.Function;
import java.util.zip.CRC32;

import org.apache.commons.compress.archivers.zip.UnixStat;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.internal.file.copy.CopyActionProcessingStream;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.WorkResult;

import org.springframework.boot.loader.tools.DefaultLaunchScript;
import org.springframework.boot.loader.tools.FileUtils;

/**
 * A {@link CopyAction} for creating a Spring Boot zip archive (typically a jar or war).
 * Stores jar files without compression as required by Spring Boot's loader.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class BootZipCopyAction implements CopyAction {

	static final long CONSTANT_TIME_FOR_ZIP_ENTRIES = new GregorianCalendar(1980, Calendar.FEBRUARY, 1, 0, 0, 0)
			.getTimeInMillis();

	private final File output;

	private final boolean preserveFileTimestamps;

	private final boolean includeDefaultLoader;

	private final Spec<FileTreeElement> requiresUnpack;

	private final Spec<FileTreeElement> exclusions;

	private final LaunchScriptConfiguration launchScript;

	private final Function<FileCopyDetails, ZipCompression> compressionResolver;

	private final String encoding;

	BootZipCopyAction(File output, boolean preserveFileTimestamps, boolean includeDefaultLoader,
			Spec<FileTreeElement> requiresUnpack, Spec<FileTreeElement> exclusions,
			LaunchScriptConfiguration launchScript, Function<FileCopyDetails, ZipCompression> compressionResolver,
			String encoding) {
		this.output = output;
		this.preserveFileTimestamps = preserveFileTimestamps;
		this.includeDefaultLoader = includeDefaultLoader;
		this.requiresUnpack = requiresUnpack;
		this.exclusions = exclusions;
		this.launchScript = launchScript;
		this.compressionResolver = compressionResolver;
		this.encoding = encoding;
	}

	@Override
	public WorkResult execute(CopyActionProcessingStream stream) {
		try {
			writeArchive(stream);
			return () -> true;
		}
		catch (IOException ex) {
			throw new GradleException("Failed to create " + this.output, ex);
		}
	}

	private void writeArchive(CopyActionProcessingStream stream) throws IOException {
		OutputStream outputStream = new FileOutputStream(this.output);
		try {
			writeLaunchScriptIfNecessary(outputStream);
			ZipArchiveOutputStream zipOutputStream = new ZipArchiveOutputStream(outputStream);
			try {
				if (this.encoding != null) {
					zipOutputStream.setEncoding(this.encoding);
				}
				Processor processor = new Processor(zipOutputStream);
				stream.process(processor::process);
				processor.finish();
			}
			finally {
				closeQuietly(zipOutputStream);
			}
		}
		finally {
			closeQuietly(outputStream);
		}
	}

	private void writeLaunchScriptIfNecessary(OutputStream outputStream) {
		if (this.launchScript == null) {
			return;
		}
		try {
			File file = this.launchScript.getScript();
			Map<String, String> properties = this.launchScript.getProperties();
			outputStream.write(new DefaultLaunchScript(file, properties).toByteArray());
			outputStream.flush();
			this.output.setExecutable(true);
		}
		catch (IOException ex) {
			throw new GradleException("Failed to write launch script to " + this.output, ex);
		}
	}

	private void closeQuietly(OutputStream outputStream) {
		try {
			outputStream.close();
		}
		catch (IOException ex) {
		}
	}

	/**
	 * Internal process used to copy {@link FileCopyDetails file details} to the zip file.
	 */
	private class Processor {

		private ZipArchiveOutputStream outputStream;

		private Spec<FileTreeElement> writtenLoaderEntries;

		Processor(ZipArchiveOutputStream outputStream) {
			this.outputStream = outputStream;
		}

		public void process(FileCopyDetails details) {
			if (BootZipCopyAction.this.exclusions.isSatisfiedBy(details)
					|| (this.writtenLoaderEntries != null && this.writtenLoaderEntries.isSatisfiedBy(details))) {
				return;
			}
			try {
				writeLoaderEntriesIfNecessary(details);
				if (details.isDirectory()) {
					processDirectory(details);
				}
				else {
					processFile(details);
				}
			}
			catch (IOException ex) {
				throw new GradleException("Failed to add " + details + " to " + BootZipCopyAction.this.output, ex);
			}
		}

		public void finish() throws IOException {
			writeLoaderEntriesIfNecessary(null);
		}

		private void writeLoaderEntriesIfNecessary(FileCopyDetails details) throws IOException {
			if (!BootZipCopyAction.this.includeDefaultLoader || this.writtenLoaderEntries != null) {
				return;
			}
			if (isInMetaInf(details)) {
				// Don't write loader entries until after META-INF folder (see gh-16698)
				return;
			}
			LoaderZipEntries loaderEntries = new LoaderZipEntries(
					BootZipCopyAction.this.preserveFileTimestamps ? null : CONSTANT_TIME_FOR_ZIP_ENTRIES);
			this.writtenLoaderEntries = loaderEntries.writeTo(this.outputStream);
		}

		private boolean isInMetaInf(FileCopyDetails details) {
			if (details == null) {
				return false;
			}
			String[] segments = details.getRelativePath().getSegments();
			return segments.length > 0 && "META-INF".equals(segments[0]);
		}

		private void processDirectory(FileCopyDetails details) throws IOException {
			ZipArchiveEntry archiveEntry = new ZipArchiveEntry(details.getRelativePath().getPathString() + '/');
			archiveEntry.setUnixMode(UnixStat.DIR_FLAG | details.getMode());
			archiveEntry.setTime(getTime(details));
			this.outputStream.putArchiveEntry(archiveEntry);
			this.outputStream.closeArchiveEntry();
		}

		private void processFile(FileCopyDetails details) throws IOException {
			String relativePath = details.getRelativePath().getPathString();
			ZipArchiveEntry archiveEntry = new ZipArchiveEntry(relativePath);
			archiveEntry.setUnixMode(UnixStat.FILE_FLAG | details.getMode());
			archiveEntry.setTime(getTime(details));
			ZipCompression compression = BootZipCopyAction.this.compressionResolver.apply(details);
			if (compression == ZipCompression.STORED) {
				prepareStoredEntry(details, archiveEntry);
			}
			this.outputStream.putArchiveEntry(archiveEntry);
			details.copyTo(this.outputStream);
			this.outputStream.closeArchiveEntry();
		}

		private void prepareStoredEntry(FileCopyDetails details, ZipArchiveEntry archiveEntry) throws IOException {
			archiveEntry.setMethod(java.util.zip.ZipEntry.STORED);
			archiveEntry.setSize(details.getSize());
			archiveEntry.setCompressedSize(details.getSize());
			Crc32OutputStream crcStream = new Crc32OutputStream();
			details.copyTo(crcStream);
			archiveEntry.setCrc(crcStream.getCrc());
			if (BootZipCopyAction.this.requiresUnpack.isSatisfiedBy(details)) {
				archiveEntry.setComment("UNPACK:" + FileUtils.sha1Hash(details.getFile()));
			}
		}

		private long getTime(FileCopyDetails details) {
			return BootZipCopyAction.this.preserveFileTimestamps ? details.getLastModified()
					: CONSTANT_TIME_FOR_ZIP_ENTRIES;
		}

	}

	/**
	 * An {@code OutputStream} that provides a CRC-32 of the data that is written to it.
	 */
	private static final class Crc32OutputStream extends OutputStream {

		private final CRC32 crc = new CRC32();

		@Override
		public void write(int b) throws IOException {
			this.crc.update(b);
		}

		@Override
		public void write(byte[] b) throws IOException {
			this.crc.update(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			this.crc.update(b, off, len);
		}

		private long getCrc() {
			return this.crc.getValue();
		}

	}

}
