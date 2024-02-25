/*
 * Copyright 2012-2023 the original author or authors.
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
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
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

	private final String layerToolsLocation;

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

	/**
	 * Creates a new instance of BootZipCopyAction.
	 * @param output the output file for the boot zip
	 * @param manifest the manifest for the boot zip
	 * @param preserveFileTimestamps flag indicating whether to preserve file timestamps
	 * during the copy action
	 * @param dirMode the directory mode for the boot zip
	 * @param fileMode the file mode for the boot zip
	 * @param includeDefaultLoader flag indicating whether to include the default loader
	 * in the boot zip
	 * @param layerToolsLocation the location of the layer tools in the boot zip
	 * @param requiresUnpack the specification for files that require unpacking in the
	 * boot zip
	 * @param exclusions the specification for files to be excluded from the boot zip
	 * @param launchScript the configuration for the launch script in the boot zip
	 * @param librarySpec the specification for libraries to be included in the boot zip
	 * @param compressionResolver the resolver for zip compression settings
	 * @param encoding the encoding for the boot zip
	 * @param resolvedDependencies the resolved dependencies for the boot zip
	 * @param supportsSignatureFile flag indicating whether the boot zip supports a
	 * signature file
	 * @param layerResolver the resolver for layers in the boot zip
	 * @param loaderImplementation the implementation of the loader in the boot zip
	 */
	BootZipCopyAction(File output, Manifest manifest, boolean preserveFileTimestamps, Integer dirMode, Integer fileMode,
			boolean includeDefaultLoader, String layerToolsLocation, Spec<FileTreeElement> requiresUnpack,
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
		this.layerToolsLocation = layerToolsLocation;
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

	/**
	 * Executes the copy action processing stream by writing the archive.
	 * @param copyActions the copy action processing stream
	 * @return the result of the work execution
	 * @throws GradleException if failed to create the output archive
	 */
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

	/**
	 * Writes the archive using the provided CopyActionProcessingStream.
	 * @param copyActions the CopyActionProcessingStream containing the copy actions to be
	 * written
	 * @throws IOException if an I/O error occurs while writing the archive
	 */
	private void writeArchive(CopyActionProcessingStream copyActions) throws IOException {
		OutputStream output = new FileOutputStream(this.output);
		try {
			writeArchive(copyActions, output);
		}
		finally {
			closeQuietly(output);
		}
	}

	/**
	 * Writes the archive by performing the copy actions.
	 * @param copyActions the copy actions to be performed
	 * @param output the output stream to write the archive to
	 * @throws IOException if an I/O error occurs while writing the archive
	 */
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

	/**
	 * Writes the launch script to the given output stream if necessary.
	 * @param outputStream the output stream to write the launch script to
	 */
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

	/**
	 * Sets the encoding for the given ZipArchiveOutputStream if necessary.
	 * @param zipOutputStream the ZipArchiveOutputStream to set the encoding for
	 */
	private void setEncodingIfNecessary(ZipArchiveOutputStream zipOutputStream) {
		if (this.encoding != null) {
			zipOutputStream.setEncoding(this.encoding);
		}
	}

	/**
	 * Closes the given OutputStream quietly, without throwing an exception.
	 * @param outputStream the OutputStream to be closed
	 */
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

		/**
		 * Constructs a new Processor object with the specified ZipArchiveOutputStream.
		 * @param out the ZipArchiveOutputStream to be used by the Processor
		 */
		Processor(ZipArchiveOutputStream out) {
			this.out = out;
			this.layerIndex = (BootZipCopyAction.this.layerResolver != null)
					? new LayersIndex(BootZipCopyAction.this.layerResolver.getLayers()) : null;
		}

		/**
		 * Processes the given file copy details.
		 * @param details the file copy details to be processed
		 * @throws GradleException if an error occurs during processing
		 */
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

		/**
		 * Determines whether to skip processing the given file copy details.
		 * @param details the file copy details to be processed
		 * @return {@code true} if the file copy details should be skipped, {@code false}
		 * otherwise
		 */
		private boolean skipProcessing(FileCopyDetails details) {
			return BootZipCopyAction.this.exclusions.isSatisfiedBy(details)
					|| (this.writtenLoaderEntries != null && this.writtenLoaderEntries.isWrittenDirectory(details));
		}

		/**
		 * Processes a directory by creating a zip archive entry for it.
		 * @param details the FileCopyDetails object containing information about the
		 * directory
		 * @throws IOException if an I/O error occurs while processing the directory
		 */
		private void processDirectory(FileCopyDetails details) throws IOException {
			String name = details.getRelativePath().getPathString();
			ZipArchiveEntry entry = new ZipArchiveEntry(name + '/');
			prepareEntry(entry, name, getTime(details), getFileMode(details));
			this.out.putArchiveEntry(entry);
			this.out.closeArchiveEntry();
			this.writtenDirectories.add(name);
		}

		/**
		 * Processes a file for copying to a zip archive.
		 * @param details the file copy details
		 * @throws IOException if an I/O error occurs
		 */
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

		/**
		 * Writes the parent directories if necessary.
		 * @param name the name of the directory
		 * @param time the time of the directory
		 * @throws IOException if an I/O error occurs
		 */
		private void writeParentDirectoriesIfNecessary(String name, Long time) throws IOException {
			String parentDirectory = getParentDirectory(name);
			if (parentDirectory != null && this.writtenDirectories.add(parentDirectory)) {
				ZipArchiveEntry entry = new ZipArchiveEntry(parentDirectory + '/');
				prepareEntry(entry, parentDirectory, time, getDirMode());
				this.out.putArchiveEntry(entry);
				this.out.closeArchiveEntry();
			}
		}

		/**
		 * Returns the parent directory of the given file or directory name.
		 * @param name the name of the file or directory
		 * @return the parent directory of the given name, or null if the name does not
		 * contain a directory path
		 */
		private String getParentDirectory(String name) {
			int lastSlash = name.lastIndexOf('/');
			if (lastSlash == -1) {
				return null;
			}
			return name.substring(0, lastSlash);
		}

		/**
		 * Finishes the processing and writes necessary files.
		 * @throws IOException if an I/O error occurs while writing the files
		 */
		void finish() throws IOException {
			writeLoaderEntriesIfNecessary(null);
			writeJarToolsIfNecessary();
			writeSignatureFileIfNecessary();
			writeClassPathIndexIfNecessary();
			writeNativeImageArgFileIfNecessary();
			// We must write the layer index last
			writeLayersIndexIfNecessary();
		}

		/**
		 * Writes loader entries to the output file if necessary.
		 * @param details the file copy details
		 * @throws IOException if an I/O error occurs
		 */
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

		/**
		 * Checks if the given FileCopyDetails object is located in the META-INF
		 * directory.
		 * @param details the FileCopyDetails object to check
		 * @return true if the FileCopyDetails object is located in the META-INF
		 * directory, false otherwise
		 */
		private boolean isInMetaInf(FileCopyDetails details) {
			if (details == null) {
				return false;
			}
			String[] segments = details.getRelativePath().getSegments();
			return segments.length > 0 && "META-INF".equals(segments[0]);
		}

		/**
		 * Writes the Jar tools if necessary.
		 * @throws IOException if an I/O error occurs
		 */
		private void writeJarToolsIfNecessary() throws IOException {
			if (BootZipCopyAction.this.layerToolsLocation != null) {
				writeJarModeLibrary(BootZipCopyAction.this.layerToolsLocation, JarModeLibrary.LAYER_TOOLS);
			}
		}

		/**
		 * Writes a JarModeLibrary to the specified location.
		 * @param location the location where the library should be written
		 * @param library the JarModeLibrary to be written
		 * @throws IOException if an I/O error occurs while writing the library
		 */
		private void writeJarModeLibrary(String location, JarModeLibrary library) throws IOException {
			String name = location + library.getName();
			writeEntry(name, ZipEntryContentWriter.fromInputStream(library.openStream()), false,
					(entry) -> prepareStoredEntry(library.openStream(), entry));
			if (BootZipCopyAction.this.layerResolver != null) {
				Layer layer = BootZipCopyAction.this.layerResolver.getLayer(library);
				this.layerIndex.add(layer, name);
			}
		}

		/**
		 * Writes the signature file if necessary.
		 * @throws IOException if an I/O error occurs while writing the signature file
		 */
		private void writeSignatureFileIfNecessary() throws IOException {
			if (BootZipCopyAction.this.supportsSignatureFile && hasSignedLibrary()) {
				writeEntry("META-INF/BOOT.SF", (out) -> {
				}, false);
			}
		}

		/**
		 * Checks if any of the written libraries have been signed.
		 * @return true if at least one written library is signed, false otherwise
		 * @throws IOException if an I/O error occurs while checking the libraries
		 */
		private boolean hasSignedLibrary() throws IOException {
			for (FileCopyDetails writtenLibrary : this.writtenLibraries.values()) {
				if (FileUtils.isSignedJarFile(writtenLibrary.getFile())) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Writes the class path index to the manifest file if necessary.
		 * @throws IOException if an I/O error occurs while writing the class path index
		 */
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

		/**
		 * Writes a native image argument file if necessary.
		 * @throws IOException if an I/O error occurs while writing the file
		 */
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

		/**
		 * Writes the layers index if necessary.
		 * @throws IOException if an I/O error occurs
		 */
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

		/**
		 * Writes an entry to the zip file with the specified name, using the provided
		 * entry writer.
		 * @param name The name of the entry to be written.
		 * @param entryWriter The writer responsible for writing the content of the entry.
		 * @param addToLayerIndex Specifies whether the entry should be added to the layer
		 * index.
		 * @throws IOException If an I/O error occurs while writing the entry.
		 */
		private void writeEntry(String name, ZipEntryContentWriter entryWriter, boolean addToLayerIndex)
				throws IOException {
			writeEntry(name, entryWriter, addToLayerIndex, ZipEntryCustomizer.NONE);
		}

		/**
		 * Writes an entry to the zip archive.
		 * @param name the name of the entry
		 * @param entryWriter the writer for the entry content
		 * @param addToLayerIndex flag indicating whether to add the entry to the layer
		 * index
		 * @param entryCustomizer the customizer for the entry
		 * @throws IOException if an I/O error occurs
		 */
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

		/**
		 * Prepares a ZipArchiveEntry with the given name, time, and mode.
		 * @param entry the ZipArchiveEntry to be prepared
		 * @param name the name of the entry
		 * @param time the time of the entry (can be null)
		 * @param mode the mode of the entry
		 * @throws IOException if an I/O error occurs
		 */
		private void prepareEntry(ZipArchiveEntry entry, String name, Long time, int mode) throws IOException {
			writeParentDirectoriesIfNecessary(name, time);
			entry.setUnixMode(mode);
			if (time != null) {
				entry.setTime(DefaultTimeZoneOffset.INSTANCE.removeFrom(time));
			}
		}

		/**
		 * Prepares a stored entry in the zip archive.
		 * @param details the file copy details
		 * @param archiveEntry the zip archive entry
		 * @throws IOException if an I/O error occurs
		 */
		private void prepareStoredEntry(FileCopyDetails details, ZipArchiveEntry archiveEntry) throws IOException {
			prepareStoredEntry(details.open(), archiveEntry);
			if (BootZipCopyAction.this.requiresUnpack.isSatisfiedBy(details)) {
				archiveEntry.setComment("UNPACK:" + FileUtils.sha1Hash(details.getFile()));
			}
		}

		/**
		 * Prepares a stored entry in a ZIP archive by setting up the CRC and size.
		 * @param input the input stream containing the data for the entry
		 * @param archiveEntry the ZIP archive entry to be prepared
		 * @throws IOException if an I/O error occurs while setting up the entry
		 */
		private void prepareStoredEntry(InputStream input, ZipArchiveEntry archiveEntry) throws IOException {
			new CrcAndSize(input).setUpStoredEntry(archiveEntry);
		}

		/**
		 * Returns the current time in milliseconds.
		 * @return the current time in milliseconds
		 */
		private Long getTime() {
			return getTime(null);
		}

		/**
		 * Returns the time of the file copy details.
		 * @param details the file copy details
		 * @return the time of the file copy details, or null if the details are null
		 * @since version 1.0
		 */
		private Long getTime(FileCopyDetails details) {
			if (!BootZipCopyAction.this.preserveFileTimestamps) {
				return CONSTANT_TIME_FOR_ZIP_ENTRIES;
			}
			if (details != null) {
				return details.getLastModified();
			}
			return null;
		}

		/**
		 * Returns the directory mode.
		 * @return the directory mode. If the directory mode is not set, it returns the
		 * default directory mode.
		 */
		private int getDirMode() {
			return (BootZipCopyAction.this.dirMode != null) ? BootZipCopyAction.this.dirMode
					: UnixStat.DIR_FLAG | UnixStat.DEFAULT_DIR_PERM;
		}

		/**
		 * Returns the file mode for the current BootZipCopyAction. If the file mode is
		 * not null, it returns the file mode. Otherwise, it returns the default file mode
		 * for UnixStat.
		 * @return the file mode for the current BootZipCopyAction
		 */
		private int getFileMode() {
			return (BootZipCopyAction.this.fileMode != null) ? BootZipCopyAction.this.fileMode
					: UnixStat.FILE_FLAG | UnixStat.DEFAULT_FILE_PERM;
		}

		/**
		 * Returns the file mode for the given FileCopyDetails. If the file mode is not
		 * null, it is returned. Otherwise, the UnixStat.FILE_FLAG is combined with the
		 * permissions obtained from the FileCopyDetails and returned.
		 * @param details the FileCopyDetails for which the file mode is to be determined
		 * @return the file mode for the given FileCopyDetails
		 */
		private int getFileMode(FileCopyDetails details) {
			return (BootZipCopyAction.this.fileMode != null) ? BootZipCopyAction.this.fileMode
					: UnixStat.FILE_FLAG | getPermissions(details);
		}

		/**
		 * Returns the permissions of the given file copy details.
		 * @param details the file copy details
		 * @return the permissions of the file copy details
		 * @throws GradleException if failed to get permissions
		 */
		private int getPermissions(FileCopyDetails details) {
			if (GradleVersion.current().compareTo(GradleVersion.version("8.3")) >= 0) {
				try {
					Method getPermissionsMethod = details.getClass().getMethod("getPermissions");
					getPermissionsMethod.setAccessible(true);
					Object permissions = getPermissionsMethod.invoke(details);
					return (int) permissions.getClass().getMethod("toUnixNumeric").invoke(permissions);
				}
				catch (Exception ex) {
					throw new GradleException("Failed to get permissions", ex);
				}
			}
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
	 * Data holder for CRC and Size.
	 */
	private static class CrcAndSize {

		private static final int BUFFER_SIZE = 32 * 1024;

		private final CRC32 crc = new CRC32();

		private long size;

		/**
		 * Calculates the CRC and size of the given input stream.
		 * @param inputStream the input stream to calculate CRC and size for
		 * @throws IOException if an I/O error occurs while reading the input stream
		 */
		CrcAndSize(InputStream inputStream) throws IOException {
			try (inputStream) {
				load(inputStream);
			}
		}

		/**
		 * Loads data from the given input stream and updates the CRC and size.
		 * @param inputStream the input stream to read data from
		 * @throws IOException if an I/O error occurs while reading from the input stream
		 */
		private void load(InputStream inputStream) throws IOException {
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				this.crc.update(buffer, 0, bytesRead);
				this.size += bytesRead;
			}
		}

		/**
		 * Sets up the stored entry for the given ZipArchiveEntry.
		 * @param entry the ZipArchiveEntry to set up the stored entry for
		 */
		void setUpStoredEntry(ZipArchiveEntry entry) {
			entry.setSize(this.size);
			entry.setCompressedSize(this.size);
			entry.setCrc(this.crc.getValue());
			entry.setMethod(ZipEntry.STORED);
		}

	}

}
