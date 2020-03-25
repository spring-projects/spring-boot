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

package org.springframework.boot.gradle.tasks.bundling;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
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
import org.gradle.api.java.archives.Attributes;
import org.gradle.api.java.archives.Manifest;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.WorkResult;
import org.gradle.api.tasks.WorkResults;

import org.springframework.boot.loader.tools.DefaultLaunchScript;
import org.springframework.boot.loader.tools.FileUtils;
import org.springframework.boot.loader.tools.JarModeLibrary;
import org.springframework.util.StreamUtils;

/**
 * A {@link CopyAction} for creating a Spring Boot zip archive (typically a jar or war).
 * Stores jar files without compression as required by Spring Boot's loader.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class BootZipCopyAction implements CopyAction {

	static final long CONSTANT_TIME_FOR_ZIP_ENTRIES = OffsetDateTime.of(1980, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC)
			.toInstant().toEpochMilli();

	private final File output;

	private final Manifest manifest;

	private final boolean preserveFileTimestamps;

	private final boolean includeDefaultLoader;

	private final boolean includeLayerTools;

	private final Spec<FileTreeElement> requiresUnpack;

	private final Spec<FileTreeElement> exclusions;

	private final LaunchScriptConfiguration launchScript;

	private final Spec<FileCopyDetails> librarySpec;

	private final Function<FileCopyDetails, ZipCompression> compressionResolver;

	private final String encoding;

	private final LayerResolver layerResolver;

	BootZipCopyAction(File output, Manifest manifest, boolean preserveFileTimestamps, boolean includeDefaultLoader,
			boolean includeLayerTools, Spec<FileTreeElement> requiresUnpack, Spec<FileTreeElement> exclusions,
			LaunchScriptConfiguration launchScript, Spec<FileCopyDetails> librarySpec,
			Function<FileCopyDetails, ZipCompression> compressionResolver, String encoding,
			LayerResolver layerResolver) {
		this.output = output;
		this.manifest = manifest;
		this.preserveFileTimestamps = preserveFileTimestamps;
		this.includeDefaultLoader = includeDefaultLoader;
		this.includeLayerTools = includeLayerTools;
		this.requiresUnpack = requiresUnpack;
		this.exclusions = exclusions;
		this.launchScript = launchScript;
		this.librarySpec = librarySpec;
		this.compressionResolver = compressionResolver;
		this.encoding = encoding;
		this.layerResolver = layerResolver;
	}

	@Override
	public WorkResult execute(CopyActionProcessingStream copyActions) {
		try {
			writeArchive(copyActions);
			return WorkResults.didWork(true);
		}
		catch (IOException ex) {
			throw new GradleException("Failed to create " + this.output, ex);
		}
	}

	private void writeArchive(CopyActionProcessingStream copyActions) throws IOException {
		OutputStream output = new FileOutputStream(this.output);
		try {
			writeArchive(copyActions, output);
		}
		finally {
			closeQuietly(output);
		}
	}

	private void writeArchive(CopyActionProcessingStream copyActions, OutputStream output) throws IOException {
		writeLaunchScriptIfNecessary(output);
		ZipArchiveOutputStream zipOutput = new ZipArchiveOutputStream(output);
		try {
			setEncodingIfNecessary(zipOutput);
			Processor processor = new Processor(zipOutput);
			copyActions.process(processor::process);
			processor.finish();
		}
		finally {
			closeQuietly(zipOutput);
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

	private void setEncodingIfNecessary(ZipArchiveOutputStream zipOutputStream) {
		if (this.encoding != null) {
			zipOutputStream.setEncoding(this.encoding);
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

		private ZipArchiveOutputStream out;

		private Spec<FileTreeElement> writtenLoaderEntries;

		private Set<String> writtenDirectories = new LinkedHashSet<>();

		private Set<String> writtenLibraries = new LinkedHashSet<>();

		Processor(ZipArchiveOutputStream out) {
			this.out = out;
		}

		void process(FileCopyDetails details) {
			if (skipProcessing(details)) {
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

		private boolean skipProcessing(FileCopyDetails details) {
			return BootZipCopyAction.this.exclusions.isSatisfiedBy(details)
					|| (this.writtenLoaderEntries != null && this.writtenLoaderEntries.isSatisfiedBy(details));
		}

		private void processDirectory(FileCopyDetails details) throws IOException {
			String name = getEntryName(details);
			long time = getTime(details);
			writeParentDirectoriesIfNecessary(name, time);
			ZipArchiveEntry entry = new ZipArchiveEntry(name + '/');
			entry.setUnixMode(UnixStat.DIR_FLAG | details.getMode());
			entry.setTime(time);
			this.out.putArchiveEntry(entry);
			this.out.closeArchiveEntry();
			this.writtenDirectories.add(name);
		}

		private void processFile(FileCopyDetails details) throws IOException {
			String name = getEntryName(details);
			long time = getTime(details);
			writeParentDirectoriesIfNecessary(name, time);
			ZipArchiveEntry entry = new ZipArchiveEntry(name);
			entry.setUnixMode(UnixStat.FILE_FLAG | details.getMode());
			entry.setTime(time);
			ZipCompression compression = BootZipCopyAction.this.compressionResolver.apply(details);
			if (compression == ZipCompression.STORED) {
				prepareStoredEntry(details, entry);
			}
			this.out.putArchiveEntry(entry);
			details.copyTo(this.out);
			this.out.closeArchiveEntry();
			if (BootZipCopyAction.this.librarySpec.isSatisfiedBy(details)) {
				this.writtenLibraries.add(name.substring(name.lastIndexOf('/') + 1));
			}
		}

		private void writeParentDirectoriesIfNecessary(String name, long time) throws IOException {
			String parentDirectory = getParentDirectory(name);
			if (parentDirectory != null && this.writtenDirectories.add(parentDirectory)) {
				writeParentDirectoriesIfNecessary(parentDirectory, time);
				ZipArchiveEntry entry = new ZipArchiveEntry(parentDirectory + '/');
				entry.setUnixMode(UnixStat.DIR_FLAG);
				entry.setTime(time);
				this.out.putArchiveEntry(entry);
				this.out.closeArchiveEntry();
			}
		}

		private String getParentDirectory(String name) {
			int lastSlash = name.lastIndexOf('/');
			if (lastSlash == -1) {
				return null;
			}
			return name.substring(0, lastSlash);
		}

		private String getEntryName(FileCopyDetails details) {
			if (BootZipCopyAction.this.layerResolver == null) {
				return details.getRelativePath().getPathString();
			}
			return BootZipCopyAction.this.layerResolver.getPath(details);
		}

		private void prepareStoredEntry(FileCopyDetails details, ZipArchiveEntry archiveEntry) throws IOException {
			archiveEntry.setMethod(java.util.zip.ZipEntry.STORED);
			archiveEntry.setSize(details.getSize());
			archiveEntry.setCompressedSize(details.getSize());
			archiveEntry.setCrc(getCrc(details));
			if (BootZipCopyAction.this.requiresUnpack.isSatisfiedBy(details)) {
				archiveEntry.setComment("UNPACK:" + FileUtils.sha1Hash(details.getFile()));
			}
		}

		private long getCrc(FileCopyDetails details) {
			Crc32OutputStream crcStream = new Crc32OutputStream();
			details.copyTo(crcStream);
			return crcStream.getCrc();
		}

		void finish() throws IOException {
			writeLoaderEntriesIfNecessary(null);
			writeJarToolsIfNecessary();
			writeLayersIndexIfNecessary();
			writeClassPathIndexIfNecessary();
		}

		private void writeLoaderEntriesIfNecessary(FileCopyDetails details) throws IOException {
			if (!BootZipCopyAction.this.includeDefaultLoader || this.writtenLoaderEntries != null) {
				return;
			}
			if (isInMetaInf(details)) {
				// Don't write loader entries until after META-INF folder (see gh-16698)
				return;
			}
			LoaderZipEntries entries = new LoaderZipEntries(
					BootZipCopyAction.this.preserveFileTimestamps ? null : CONSTANT_TIME_FOR_ZIP_ENTRIES);
			this.writtenLoaderEntries = entries.writeTo(this.out);
		}

		private boolean isInMetaInf(FileCopyDetails details) {
			if (details == null) {
				return false;
			}
			String[] segments = details.getRelativePath().getSegments();
			return segments.length > 0 && "META-INF".equals(segments[0]);
		}

		private void writeJarToolsIfNecessary() throws IOException {
			if (BootZipCopyAction.this.layerResolver == null || !BootZipCopyAction.this.includeLayerTools) {
				return;
			}
			writeJarModeLibrary(JarModeLibrary.LAYER_TOOLS);
		}

		private void writeJarModeLibrary(JarModeLibrary jarModeLibrary) throws IOException {
			String name = BootZipCopyAction.this.layerResolver.getPath(jarModeLibrary);
			writeFile(name, ZipEntryWriter.fromInputStream(jarModeLibrary.openStream()));
		}

		private void writeLayersIndexIfNecessary() throws IOException {
			Attributes manifestAttributes = BootZipCopyAction.this.manifest.getAttributes();
			String layersIndex = (String) manifestAttributes.get("Spring-Boot-Layers-Index");
			if (layersIndex != null && BootZipCopyAction.this.layerResolver != null) {
				writeFile(layersIndex, ZipEntryWriter.fromLines(BootZipCopyAction.this.encoding,
						BootZipCopyAction.this.layerResolver.getLayerNames()));
			}
		}

		private void writeClassPathIndexIfNecessary() throws IOException {
			Attributes manifestAttributes = BootZipCopyAction.this.manifest.getAttributes();
			String classPathIndex = (String) manifestAttributes.get("Spring-Boot-Classpath-Index");
			if (classPathIndex != null) {
				writeFile(classPathIndex,
						ZipEntryWriter.fromLines(BootZipCopyAction.this.encoding, this.writtenLibraries));
			}
		}

		private void writeFile(String name, ZipEntryWriter entryWriter) throws IOException {
			writeParentDirectoriesIfNecessary(name, CONSTANT_TIME_FOR_ZIP_ENTRIES);
			ZipArchiveEntry entry = new ZipArchiveEntry(name);
			entry.setUnixMode(UnixStat.FILE_FLAG);
			entry.setTime(CONSTANT_TIME_FOR_ZIP_ENTRIES);
			this.out.putArchiveEntry(entry);
			entryWriter.writeTo(entry, this.out);
			this.out.closeArchiveEntry();

		}

		private long getTime(FileCopyDetails details) {
			return BootZipCopyAction.this.preserveFileTimestamps ? details.getLastModified()
					: CONSTANT_TIME_FOR_ZIP_ENTRIES;
		}

	}

	/**
	 * Callback used to write a zip entry data.
	 */
	@FunctionalInterface
	private interface ZipEntryWriter {

		/**
		 * Write the entry data.
		 * @param entry the entry being written
		 * @param out the output stream used to write the data
		 * @throws IOException on IO error
		 */
		void writeTo(ZipArchiveEntry entry, ZipArchiveOutputStream out) throws IOException;

		/**
		 * Create a new {@link ZipEntryWriter} that will copy content from the given
		 * {@link InputStream}.
		 * @param in the source input stream
		 * @return a new {@link ZipEntryWriter} instance
		 */
		static ZipEntryWriter fromInputStream(InputStream in) {
			return (entry, out) -> {
				StreamUtils.copy(in, out);
				in.close();
			};
		}

		/**
		 * Create a new {@link ZipEntryWriter} that will copy content from the given
		 * lines.
		 * @param encoding the required character encoding
		 * @param lines the lines to write
		 * @return a new {@link ZipEntryWriter} instance
		 */
		static ZipEntryWriter fromLines(String encoding, Collection<String> lines) {
			return (entry, out) -> {
				OutputStreamWriter writer = new OutputStreamWriter(out, encoding);
				for (String line : lines) {
					writer.append(line + "\n");
				}
				writer.flush();
			};
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
