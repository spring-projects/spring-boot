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

package org.springframework.boot.gradle.tasks.bundling;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

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
import org.gradle.util.GradleVersion;

import org.springframework.boot.gradle.tasks.bundling.ResolvedDependencies.DependencyDescriptor;
import org.springframework.boot.loader.tools.DefaultLaunchScript;
import org.springframework.boot.loader.tools.FileUtils;
import org.springframework.boot.loader.tools.JarModeLibrary;
import org.springframework.boot.loader.tools.Layer;
import org.springframework.boot.loader.tools.LayersIndex;
import org.springframework.boot.loader.tools.LibraryCoordinates;
import org.springframework.boot.loader.tools.LoaderImplementation;
import org.springframework.boot.loader.tools.NativeImageArgFile;
import org.springframework.boot.loader.tools.ReachabilityMetadataProperties;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * A {@link CopyAction} for creating a Spring Boot zip archive (typically a jar or war).
 * Stores jar files without compression as required by Spring Boot's loader.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class BootZipCopyAction implements CopyAction {

	static final long CONSTANT_TIME_FOR_ZIP_ENTRIES = OffsetDateTime.of(1980, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC)
		.toInstant()
		.toEpochMilli();

	private static final Pattern REACHABILITY_METADATA_PROPERTIES_LOCATION_PATTERN = Pattern
		.compile(ReachabilityMetadataProperties.REACHABILITY_METADATA_PROPERTIES_LOCATION_TEMPLATE.formatted(".*", ".*",
				".*"));

	private final File output;

	private final Manifest manifest;

	private final boolean preserveFileTimestamps;

	private final Integer dirMode;

	private final Integer fileMode;

	private final boolean includeDefaultLoader;

	private final String jarmodeToolsLocation;

	private final Spec<FileTreeElement> requiresUnpack;

	private final Spec<FileTreeElement> exclusions;

	private final LaunchScriptConfiguration launchScript;

	private final Spec<FileCopyDetails> librarySpec;

	private final Function<FileCopyDetails, ZipCompression> compressionResolver;

	private final String encoding;

	private final ResolvedDependencies resolvedDependencies;

	private final boolean supportsSignatureFile;

	private final LayerResolver layerResolver;

	private final LoaderImplementation loaderImplementation;

	BootZipCopyAction(File output, Manifest manifest, boolean preserveFileTimestamps, Integer dirMode, Integer fileMode,
			boolean includeDefaultLoader, String jarmodeToolsLocation, Spec<FileTreeElement> requiresUnpack,
			Spec<FileTreeElement> exclusions, LaunchScriptConfiguration launchScript, Spec<FileCopyDetails> librarySpec,
			Function<FileCopyDetails, ZipCompression> compressionResolver, String encoding,
			ResolvedDependencies resolvedDependencies, boolean supportsSignatureFile, LayerResolver layerResolver,
			LoaderImplementation loaderImplementation) {
		this.output = output;
		this.manifest = manifest;
		this.preserveFileTimestamps = preserveFileTimestamps;
		this.dirMode = dirMode;
		this.fileMode = fileMode;
		this.includeDefaultLoader = includeDefaultLoader;
		this.jarmodeToolsLocation = jarmodeToolsLocation;
		this.requiresUnpack = requiresUnpack;
		this.exclusions = exclusions;
		this.launchScript = launchScript;
		this.librarySpec = librarySpec;
		this.compressionResolver = compressionResolver;
		this.encoding = encoding;
		this.resolvedDependencies = resolvedDependencies;
		this.supportsSignatureFile = supportsSignatureFile;
		this.layerResolver = layerResolver;
		this.loaderImplementation = loaderImplementation;
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
		ZipArchiveOutputStream zipOutput = new ZipArchiveOutputStream(output);
		writeLaunchScriptIfNecessary(zipOutput);
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

	private void writeLaunchScriptIfNecessary(ZipArchiveOutputStream outputStream) {
		if (this.launchScript == null) {
			return;
		}
		try {
			File file = this.launchScript.getScript();
			Map<String, String> properties = this.launchScript.getProperties();
			outputStream.writePreamble(new DefaultLaunchScript(file, properties).toByteArray());
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
			// Ignore
		}
	}

	/**
	 * Internal process used to copy {@link FileCopyDetails file details} to the zip file.
	 */
	private class Processor {

		private final ZipArchiveOutputStream out;

		private final LayersIndex layerIndex;

		private LoaderZipEntries.WrittenEntries writtenLoaderEntries;

		private final Set<String> writtenDirectories = new LinkedHashSet<>();

		private final Map<String, FileCopyDetails> writtenLibraries = new LinkedHashMap<>();

		private final Map<String, FileCopyDetails> reachabilityMetadataProperties = new HashMap<>();

		Processor(ZipArchiveOutputStream out) {
			this.out = out;
			this.layerIndex = (BootZipCopyAction.this.layerResolver != null)
					? new LayersIndex(BootZipCopyAction.this.layerResolver.getLayers()) : null;
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
					|| (this.writtenLoaderEntries != null && this.writtenLoaderEntries.isWrittenDirectory(details));
		}

		private void processDirectory(FileCopyDetails details) throws IOException {
			String name = details.getRelativePath().getPathString();
			ZipArchiveEntry entry = new ZipArchiveEntry(name + '/');
			prepareEntry(entry, name, getTime(details), getDirMode(details));
			this.out.putArchiveEntry(entry);
			this.out.closeArchiveEntry();
			this.writtenDirectories.add(name);
		}

		private void processFile(FileCopyDetails details) throws IOException {
			String name = details.getRelativePath().getPathString();
			ZipArchiveEntry entry = new ZipArchiveEntry(name);
			prepareEntry(entry, name, getTime(details), getFileMode(details));
			ZipCompression compression = BootZipCopyAction.this.compressionResolver.apply(details);
			if (compression == ZipCompression.STORED) {
				prepareStoredEntry(details, entry);
			}
			this.out.putArchiveEntry(entry);
			details.copyTo(this.out);
			this.out.closeArchiveEntry();
			if (BootZipCopyAction.this.librarySpec.isSatisfiedBy(details)) {
				this.writtenLibraries.put(name, details);
			}
			if (REACHABILITY_METADATA_PROPERTIES_LOCATION_PATTERN.matcher(name).matches()) {
				this.reachabilityMetadataProperties.put(name, details);
			}
			if (BootZipCopyAction.this.layerResolver != null) {
				Layer layer = BootZipCopyAction.this.layerResolver.getLayer(details);
				this.layerIndex.add(layer, name);
			}
		}

		private void writeParentDirectoriesIfNecessary(String name, Long time) throws IOException {
			String parentDirectory = getParentDirectory(name);
			if (parentDirectory != null && this.writtenDirectories.add(parentDirectory)) {
				ZipArchiveEntry entry = new ZipArchiveEntry(parentDirectory + '/');
				prepareEntry(entry, parentDirectory, time, getDirMode());
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

		void finish() throws IOException {
			writeLoaderEntriesIfNecessary(null);
			writeJarToolsIfNecessary();
			writeSignatureFileIfNecessary();
			writeClassPathIndexIfNecessary();
			writeNativeImageArgFileIfNecessary();
			// We must write the layer index last
			writeLayersIndexIfNecessary();
		}

		private void writeLoaderEntriesIfNecessary(FileCopyDetails details) throws IOException {
			if (!BootZipCopyAction.this.includeDefaultLoader || this.writtenLoaderEntries != null) {
				return;
			}
			if (isInMetaInf(details)) {
				// Always write loader entries after META-INF directory (see gh-16698)
				return;
			}
			LoaderZipEntries loaderEntries = new LoaderZipEntries(getTime(), getDirMode(), getFileMode(),
					BootZipCopyAction.this.loaderImplementation);
			this.writtenLoaderEntries = loaderEntries.writeTo(this.out);
			if (BootZipCopyAction.this.layerResolver != null) {
				for (String name : this.writtenLoaderEntries.getFiles()) {
					Layer layer = BootZipCopyAction.this.layerResolver.getLayer(name);
					this.layerIndex.add(layer, name);
				}
			}
		}

		private boolean isInMetaInf(FileCopyDetails details) {
			if (details == null) {
				return false;
			}
			String[] segments = details.getRelativePath().getSegments();
			return segments.length > 0 && "META-INF".equals(segments[0]);
		}

		private void writeJarToolsIfNecessary() throws IOException {
			if (BootZipCopyAction.this.jarmodeToolsLocation != null) {
				writeJarModeLibrary(BootZipCopyAction.this.jarmodeToolsLocation, JarModeLibrary.TOOLS);
			}
		}

		private void writeJarModeLibrary(String location, JarModeLibrary library) throws IOException {
			String name = location + library.getName();
			writeEntry(name, ZipEntryContentWriter.fromInputStream(library.openStream()), false, (entry) -> {
				try (InputStream in = library.openStream()) {
					prepareStoredEntry(library.openStream(), false, entry);
				}
			});
			if (BootZipCopyAction.this.layerResolver != null) {
				Layer layer = BootZipCopyAction.this.layerResolver.getLayer(library);
				this.layerIndex.add(layer, name);
			}
		}

		private void writeSignatureFileIfNecessary() throws IOException {
			if (BootZipCopyAction.this.supportsSignatureFile && hasSignedLibrary()) {
				writeEntry("META-INF/BOOT.SF", (out) -> {
				}, false);
			}
		}

		private boolean hasSignedLibrary() throws IOException {
			for (FileCopyDetails writtenLibrary : this.writtenLibraries.values()) {
				if (FileUtils.isSignedJarFile(writtenLibrary.getFile())) {
					return true;
				}
			}
			return false;
		}

		private void writeClassPathIndexIfNecessary() throws IOException {
			Attributes manifestAttributes = BootZipCopyAction.this.manifest.getAttributes();
			String classPathIndex = (String) manifestAttributes.get("Spring-Boot-Classpath-Index");
			if (classPathIndex != null) {
				Set<String> libraryNames = this.writtenLibraries.keySet();
				List<String> lines = libraryNames.stream().map((line) -> "- \"" + line + "\"").toList();
				ZipEntryContentWriter writer = ZipEntryContentWriter.fromLines(BootZipCopyAction.this.encoding, lines);
				writeEntry(classPathIndex, writer, true);
			}
		}

		private void writeNativeImageArgFileIfNecessary() throws IOException {
			Set<String> excludes = new LinkedHashSet<>();
			for (Map.Entry<String, FileCopyDetails> entry : this.writtenLibraries.entrySet()) {
				DependencyDescriptor descriptor = BootZipCopyAction.this.resolvedDependencies
					.find(entry.getValue().getFile());
				LibraryCoordinates coordinates = (descriptor != null) ? descriptor.getCoordinates() : null;
				FileCopyDetails propertiesFile = (coordinates != null) ? this.reachabilityMetadataProperties
					.get(ReachabilityMetadataProperties.getLocation(coordinates)) : null;
				if (propertiesFile != null) {
					try (InputStream inputStream = propertiesFile.open()) {
						ReachabilityMetadataProperties properties = ReachabilityMetadataProperties
							.fromInputStream(inputStream);
						if (properties.isOverridden()) {
							excludes.add(entry.getKey());
						}
					}
				}
			}
			NativeImageArgFile argFile = new NativeImageArgFile(excludes);
			argFile.writeIfNecessary((lines) -> {
				ZipEntryContentWriter writer = ZipEntryContentWriter.fromLines(BootZipCopyAction.this.encoding, lines);
				writeEntry(NativeImageArgFile.LOCATION, writer, true);
			});
		}

		private void writeLayersIndexIfNecessary() throws IOException {
			if (BootZipCopyAction.this.layerResolver != null) {
				Attributes manifestAttributes = BootZipCopyAction.this.manifest.getAttributes();
				String name = (String) manifestAttributes.get("Spring-Boot-Layers-Index");
				Assert.state(StringUtils.hasText(name), "Missing layer index manifest attribute");
				Layer layer = BootZipCopyAction.this.layerResolver.getLayer(name);
				this.layerIndex.add(layer, name);
				writeEntry(name, this.layerIndex::writeTo, false);
			}
		}

		private void writeEntry(String name, ZipEntryContentWriter entryWriter, boolean addToLayerIndex)
				throws IOException {
			writeEntry(name, entryWriter, addToLayerIndex, ZipEntryCustomizer.NONE);
		}

		private void writeEntry(String name, ZipEntryContentWriter entryWriter, boolean addToLayerIndex,
				ZipEntryCustomizer entryCustomizer) throws IOException {
			ZipArchiveEntry entry = new ZipArchiveEntry(name);
			prepareEntry(entry, name, getTime(), getFileMode());
			entryCustomizer.customize(entry);
			this.out.putArchiveEntry(entry);
			entryWriter.writeTo(this.out);
			this.out.closeArchiveEntry();
			if (addToLayerIndex && BootZipCopyAction.this.layerResolver != null) {
				Layer layer = BootZipCopyAction.this.layerResolver.getLayer(name);
				this.layerIndex.add(layer, name);
			}
		}

		private void prepareEntry(ZipArchiveEntry entry, String name, Long time, int mode) throws IOException {
			writeParentDirectoriesIfNecessary(name, time);
			entry.setUnixMode(mode);
			if (time != null) {
				entry.setTime(DefaultTimeZoneOffset.INSTANCE.removeFrom(time));
			}
		}

		private void prepareStoredEntry(FileCopyDetails details, ZipArchiveEntry archiveEntry) throws IOException {
			prepareStoredEntry(details.open(), BootZipCopyAction.this.requiresUnpack.isSatisfiedBy(details),
					archiveEntry);
		}

		private void prepareStoredEntry(InputStream input, boolean unpack, ZipArchiveEntry archiveEntry)
				throws IOException {
			new StoredEntryPreparator(input, unpack).prepareStoredEntry(archiveEntry);
		}

		private Long getTime() {
			return getTime(null);
		}

		private Long getTime(FileCopyDetails details) {
			if (!BootZipCopyAction.this.preserveFileTimestamps) {
				return CONSTANT_TIME_FOR_ZIP_ENTRIES;
			}
			if (details != null) {
				return details.getLastModified();
			}
			return null;
		}

		private int getDirMode() {
			return (BootZipCopyAction.this.dirMode != null) ? BootZipCopyAction.this.dirMode
					: UnixStat.DEFAULT_DIR_PERM;
		}

		private int getFileMode() {
			return (BootZipCopyAction.this.fileMode != null) ? BootZipCopyAction.this.fileMode
					: UnixStat.DEFAULT_FILE_PERM;
		}

		private int getDirMode(FileCopyDetails details) {
			return (BootZipCopyAction.this.dirMode != null) ? BootZipCopyAction.this.dirMode : getPermissions(details);
		}

		private int getFileMode(FileCopyDetails details) {
			return (BootZipCopyAction.this.fileMode != null) ? BootZipCopyAction.this.fileMode
					: getPermissions(details);
		}

		private int getPermissions(FileCopyDetails details) {
			return (GradleVersion.current().compareTo(GradleVersion.version("8.3")) >= 0)
					? details.getPermissions().toUnixNumeric() : getMode(details);
		}

		@SuppressWarnings("deprecation")
		private int getMode(FileCopyDetails details) {
			return details.getMode();
		}

	}

	/**
	 * Callback interface used to customize a {@link ZipArchiveEntry}.
	 */
	@FunctionalInterface
	private interface ZipEntryCustomizer {

		ZipEntryCustomizer NONE = (entry) -> {
		};

		/**
		 * Customize the entry.
		 * @param entry the entry to customize
		 * @throws IOException on IO error
		 */
		void customize(ZipArchiveEntry entry) throws IOException;

	}

	/**
	 * Callback used to write a zip entry data.
	 */
	@FunctionalInterface
	private interface ZipEntryContentWriter {

		/**
		 * Write the entry data.
		 * @param out the output stream used to write the data
		 * @throws IOException on IO error
		 */
		void writeTo(ZipArchiveOutputStream out) throws IOException;

		/**
		 * Create a new {@link ZipEntryContentWriter} that will copy content from the
		 * given {@link InputStream}.
		 * @param in the source input stream
		 * @return a new {@link ZipEntryContentWriter} instance
		 */
		static ZipEntryContentWriter fromInputStream(InputStream in) {
			return (out) -> {
				StreamUtils.copy(in, out);
				in.close();
			};
		}

		/**
		 * Create a new {@link ZipEntryContentWriter} that will copy content from the
		 * given lines.
		 * @param encoding the required character encoding
		 * @param lines the lines to write
		 * @return a new {@link ZipEntryContentWriter} instance
		 */
		static ZipEntryContentWriter fromLines(String encoding, Collection<String> lines) {
			return (out) -> {
				OutputStreamWriter writer = new OutputStreamWriter(out, encoding);
				for (String line : lines) {
					writer.append(line).append("\n");
				}
				writer.flush();
			};
		}

	}

	/**
	 * Prepares a {@link ZipEntry#STORED stored} {@link ZipArchiveEntry entry} with CRC
	 * and size information. Also adds an {@code UNPACK} comment, if needed.
	 */
	private static class StoredEntryPreparator {

		private static final int BUFFER_SIZE = 32 * 1024;

		private final MessageDigest messageDigest;

		private final CRC32 crc = new CRC32();

		private long size;

		StoredEntryPreparator(InputStream inputStream, boolean unpack) throws IOException {
			this.messageDigest = (unpack) ? sha1Digest() : null;
			try (inputStream) {
				load(inputStream);
			}
		}

		private static MessageDigest sha1Digest() {
			try {
				return MessageDigest.getInstance("SHA-1");
			}
			catch (NoSuchAlgorithmException ex) {
				throw new IllegalStateException(ex);
			}
		}

		private void load(InputStream inputStream) throws IOException {
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				this.crc.update(buffer, 0, bytesRead);
				if (this.messageDigest != null) {
					this.messageDigest.update(buffer, 0, bytesRead);
				}
				this.size += bytesRead;
			}
		}

		void prepareStoredEntry(ZipArchiveEntry entry) {
			entry.setSize(this.size);
			entry.setCompressedSize(this.size);
			entry.setCrc(this.crc.getValue());
			entry.setMethod(ZipEntry.STORED);
			if (this.messageDigest != null) {
				entry.setComment("UNPACK:" + HexFormat.of().formatHex(this.messageDigest.digest()));
			}
		}

	}

}
