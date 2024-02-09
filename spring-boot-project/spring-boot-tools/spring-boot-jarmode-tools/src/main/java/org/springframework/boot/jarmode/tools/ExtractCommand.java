/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.jarmode.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.boot.jarmode.tools.JarStructure.Entry;
import org.springframework.boot.jarmode.tools.JarStructure.Entry.Type;
import org.springframework.boot.jarmode.tools.Layers.LayersNotEnabledException;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * The {@code 'extract'} tools command.
 *
 * @author Moritz Halbritter
 */
class ExtractCommand extends Command {

	/**
	 * Option to create a launcher.
	 */
	static final Option LAUNCHER_OPTION = Option.of("launcher", null, "Whether to extract the Spring Boot launcher");

	/**
	 * Option to extract layers.
	 */
	static final Option LAYERS_OPTION = Option.of("layers", "string list", "Layers to extract", true);

	/**
	 * Option to specify the destination to write to.
	 */
	static final Option DESTINATION_OPTION = Option.of("destination", "string",
			"Directory to extract files to. Defaults to the current working directory");

	private static final Option LIBRARIES_DIRECTORY_OPTION = Option.of("libraries", "string",
			"Name of the libraries directory. Only applicable when not using --launcher. Defaults to lib/");

	private static final Option RUNNER_FILENAME_OPTION = Option.of("runner-filename", "string",
			"Name of the runner JAR file. Only applicable when not using --launcher. Defaults to runner.jar");

	private final Context context;

	private final Layers layers;

	ExtractCommand(Context context) {
		this(context, null);
	}

	ExtractCommand(Context context, Layers layers) {
		super("extract", "Extract the contents from the jar", Options.of(LAUNCHER_OPTION, LAYERS_OPTION,
				DESTINATION_OPTION, LIBRARIES_DIRECTORY_OPTION, RUNNER_FILENAME_OPTION), Parameters.none());
		this.context = context;
		this.layers = layers;
	}

	@Override
	void run(PrintStream out, Map<Option, String> options, List<String> parameters) {
		try {
			checkJarCompatibility();
			File destination = getWorkingDirectory(options);
			FileResolver fileResolver = getFileResolver(destination, options);
			fileResolver.createDirectories();
			if (options.containsKey(LAUNCHER_OPTION)) {
				extractArchive(fileResolver);
			}
			else {
				JarStructure jarStructure = getJarStructure();
				extractLibraries(fileResolver, jarStructure, options);
				createRunner(jarStructure, fileResolver, options);
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		catch (LayersNotEnabledException ex) {
			printError(out, "Layers are not enabled");
		}
	}

	private void checkJarCompatibility() throws IOException {
		File file = this.context.getArchiveFile();
		try (ZipInputStream stream = new ZipInputStream(new FileInputStream(file))) {
			ZipEntry entry = stream.getNextEntry();
			Assert.state(entry != null,
					() -> "File '%s' is not compatible; ensure jar file is valid and launch script is not enabled"
						.formatted(file));
		}
	}

	private void printError(PrintStream out, String message) {
		out.println("Error: " + message);
		out.println();
	}

	private void extractLibraries(FileResolver fileResolver, JarStructure jarStructure, Map<Option, String> options)
			throws IOException {
		String librariesDirectory = getLibrariesDirectory(options);
		extractArchive(fileResolver, (zipEntry) -> {
			Entry entry = jarStructure.resolve(zipEntry);
			if (isType(entry, Type.LIBRARY)) {
				return librariesDirectory + entry.location();
			}
			return null;
		});
	}

	private static String getLibrariesDirectory(Map<Option, String> options) {
		if (options.containsKey(LIBRARIES_DIRECTORY_OPTION)) {
			String value = options.get(LIBRARIES_DIRECTORY_OPTION);
			if (value.endsWith("/")) {
				return value;
			}
			return value + "/";
		}
		return "lib/";
	}

	private FileResolver getFileResolver(File destination, Map<Option, String> options) {
		String runnerFilename = getRunnerFilename(options);
		if (!options.containsKey(LAYERS_OPTION)) {
			return new NoLayersFileResolver(destination, runnerFilename);
		}
		Layers layers = getLayers();
		Set<String> layersToExtract = StringUtils.commaDelimitedListToSet(options.get(LAYERS_OPTION));
		return new LayersFileResolver(destination, layers, layersToExtract, runnerFilename);
	}

	private File getWorkingDirectory(Map<Option, String> options) {
		if (options.containsKey(DESTINATION_OPTION)) {
			return new File(options.get(DESTINATION_OPTION));
		}
		return this.context.getWorkingDir();
	}

	private JarStructure getJarStructure() {
		IndexedJarStructure jarStructure = IndexedJarStructure.get(this.context.getArchiveFile());
		Assert.state(jarStructure != null, "Couldn't read classpath index");
		return jarStructure;
	}

	private void extractArchive(FileResolver fileResolver) throws IOException {
		extractArchive(fileResolver, ZipEntry::getName);
	}

	private void extractArchive(FileResolver fileResolver, EntryNameTransformer entryNameTransformer)
			throws IOException {
		withZipEntries(this.context.getArchiveFile(), (stream, zipEntry) -> {
			if (zipEntry.isDirectory()) {
				return;
			}
			String name = entryNameTransformer.getName(zipEntry);
			if (name == null) {
				return;
			}
			File file = fileResolver.resolve(zipEntry, name);
			if (file != null) {
				extractEntry(stream, zipEntry, file);
			}
		});
	}

	private Layers getLayers() {
		if (this.layers != null) {
			return this.layers;
		}
		return Layers.get(this.context);
	}

	private void createRunner(JarStructure jarStructure, FileResolver fileResolver, Map<Option, String> options)
			throws IOException {
		File file = fileResolver.resolveRunner();
		if (file == null) {
			return;
		}
		String librariesDirectory = getLibrariesDirectory(options);
		Manifest manifest = jarStructure.createLauncherManifest((library) -> librariesDirectory + library);
		mkDirs(file.getParentFile());
		try (JarOutputStream output = new JarOutputStream(new FileOutputStream(file), manifest)) {
			withZipEntries(this.context.getArchiveFile(), ((stream, zipEntry) -> {
				Entry entry = jarStructure.resolve(zipEntry);
				if (isType(entry, Type.APPLICATION_CLASS_OR_RESOURCE) && StringUtils.hasLength(entry.location())) {
					JarEntry jarEntry = createJarEntry(entry.location(), zipEntry);
					output.putNextEntry(jarEntry);
					StreamUtils.copy(stream, output);
					output.closeEntry();
				}
			}));
		}
	}

	private String getRunnerFilename(Map<Option, String> options) {
		if (options.containsKey(RUNNER_FILENAME_OPTION)) {
			return options.get(RUNNER_FILENAME_OPTION);
		}
		return "runner.jar";
	}

	private static boolean isType(Entry entry, Type type) {
		if (entry == null) {
			return false;
		}
		return entry.type() == type;
	}

	private static void extractEntry(ZipInputStream zip, ZipEntry entry, File file) throws IOException {
		mkDirs(file.getParentFile());
		try (OutputStream out = new FileOutputStream(file)) {
			StreamUtils.copy(zip, out);
		}
		try {
			Files.getFileAttributeView(file.toPath(), BasicFileAttributeView.class)
				.setTimes(entry.getLastModifiedTime(), entry.getLastAccessTime(), entry.getCreationTime());
		}
		catch (IOException ex) {
			// File system does not support setting time attributes. Continue.
		}
	}

	private static void mkDirs(File file) throws IOException {
		if (!file.exists() && !file.mkdirs()) {
			throw new IOException("Unable to create directory " + file);
		}
	}

	private static JarEntry createJarEntry(String location, ZipEntry originalEntry) {
		JarEntry jarEntry = new JarEntry(location);
		FileTime lastModifiedTime = originalEntry.getLastModifiedTime();
		if (lastModifiedTime != null) {
			jarEntry.setLastModifiedTime(lastModifiedTime);
		}
		FileTime lastAccessTime = originalEntry.getLastAccessTime();
		if (lastAccessTime != null) {
			jarEntry.setLastAccessTime(lastAccessTime);
		}
		FileTime creationTime = originalEntry.getCreationTime();
		if (creationTime != null) {
			jarEntry.setCreationTime(creationTime);
		}
		return jarEntry;
	}

	private static void withZipEntries(File file, ThrowingConsumer callback) throws IOException {
		try (ZipInputStream stream = new ZipInputStream(new FileInputStream(file))) {
			ZipEntry entry = stream.getNextEntry();
			while (entry != null) {
				if (StringUtils.hasLength(entry.getName())) {
					callback.accept(stream, entry);
				}
				entry = stream.getNextEntry();
			}
		}
	}

	private static File assertFileIsContainedInDirectory(File directory, File file, String name) throws IOException {
		String canonicalOutputPath = directory.getCanonicalPath() + File.separator;
		String canonicalEntryPath = file.getCanonicalPath();
		Assert.state(canonicalEntryPath.startsWith(canonicalOutputPath),
				() -> "Entry '%s' would be written to '%s'. This is outside the output location of '%s'. Verify the contents of your archive."
					.formatted(name, canonicalEntryPath, canonicalOutputPath));
		return file;
	}

	@FunctionalInterface
	private interface EntryNameTransformer {

		String getName(ZipEntry entry);

	}

	@FunctionalInterface
	private interface ThrowingConsumer {

		void accept(ZipInputStream stream, ZipEntry entry) throws IOException;

	}

	private interface FileResolver {

		/**
		 * Creates needed directories.
		 * @throws IOException if something went wrong
		 */
		void createDirectories() throws IOException;

		/**
		 * Resolves the given {@link ZipEntry} to a file.
		 * @param entry the zip entry
		 * @param newName the new name of the file
		 * @return file where the contents should be written or {@code null} if this entry
		 * should be skipped
		 * @throws IOException if something went wrong
		 */
		default File resolve(ZipEntry entry, String newName) throws IOException {
			return resolve(entry.getName(), newName);
		}

		/**
		 * Resolves the given name to a file.
		 * @param originalName the original name of the file
		 * @param newName the new name of the file
		 * @return file where the contents should be written or {@code null} if this name
		 * should be skipped
		 * @throws IOException if something went wrong
		 */
		File resolve(String originalName, String newName) throws IOException;

		/**
		 * Resolves the file for the runner.
		 * @return the file for the runner or {@code null} if the runner should be skipped
		 * @throws IOException if something went wrong
		 */
		File resolveRunner() throws IOException;

	}

	private static final class NoLayersFileResolver implements FileResolver {

		private final File directory;

		private final String runnerFilename;

		private NoLayersFileResolver(File directory, String runnerFilename) {
			this.directory = directory;
			this.runnerFilename = runnerFilename;
		}

		@Override
		public void createDirectories() {
		}

		@Override
		public File resolve(String originalName, String newName) throws IOException {
			return assertFileIsContainedInDirectory(this.directory, new File(this.directory, newName), newName);
		}

		@Override
		public File resolveRunner() throws IOException {
			return resolve(this.runnerFilename, this.runnerFilename);
		}

	}

	private static final class LayersFileResolver implements FileResolver {

		private final Layers layers;

		private final Set<String> layersToExtract;

		private final File directory;

		private final String runnerFilename;

		LayersFileResolver(File directory, Layers layers, Set<String> layersToExtract, String runnerFilename) {
			this.layers = layers;
			this.layersToExtract = layersToExtract;
			this.directory = directory;
			this.runnerFilename = runnerFilename;
		}

		@Override
		public void createDirectories() throws IOException {
			for (String layer : this.layers) {
				if (shouldExtractLayer(layer)) {
					mkDirs(getLayerDirectory(layer));
				}
			}
		}

		@Override
		public File resolve(String originalName, String newName) throws IOException {
			String layer = this.layers.getLayer(originalName);
			if (shouldExtractLayer(layer)) {
				File directory = getLayerDirectory(layer);
				return assertFileIsContainedInDirectory(directory, new File(directory, newName), newName);
			}
			return null;
		}

		@Override
		public File resolveRunner() throws IOException {
			String layer = this.layers.getApplicationLayerName();
			if (shouldExtractLayer(layer)) {
				File directory = getLayerDirectory(layer);
				return assertFileIsContainedInDirectory(directory, new File(directory, this.runnerFilename),
						this.runnerFilename);
			}
			return null;
		}

		private File getLayerDirectory(String layer) {
			return new File(this.directory, layer);
		}

		private boolean shouldExtractLayer(String layer) {
			if (this.layersToExtract.isEmpty()) {
				return true;
			}
			return this.layersToExtract.contains(layer);
		}

	}

}
