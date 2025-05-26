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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.boot.jarmode.tools.JarStructure.Entry;
import org.springframework.boot.jarmode.tools.JarStructure.Entry.Type;
import org.springframework.boot.loader.jarmode.JarModeErrorException;
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
	static final Option LAUNCHER_OPTION = Option.flag("launcher", "Whether to extract the Spring Boot launcher");

	/**
	 * Option to extract layers.
	 */
	static final Option LAYERS_OPTION = Option.of("layers", "string list", "Layers to extract", true);

	/**
	 * Option to specify the destination to write to.
	 */
	static final Option DESTINATION_OPTION = Option.of("destination", "string",
			"Directory to extract files to. Defaults to a directory named after the uber JAR (without the file extension)");

	/**
	 * Option to ignore non-empty directory error.
	 */
	static final Option FORCE_OPTION = Option.flag("force", "Whether to ignore non-empty directories, extract anyway");

	private static final Option LIBRARIES_DIRECTORY_OPTION = Option.of("libraries", "string",
			"Name of the libraries directory. Only applicable when not using --launcher. Defaults to lib/");

	private static final Option APPLICATION_FILENAME_OPTION = Option.of("application-filename", "string",
			"Name of the application JAR file. Only applicable when not using --launcher. Defaults to the uber JAR filename");

	private final Context context;

	private final Layers layers;

	ExtractCommand(Context context) {
		this(context, null);
	}

	ExtractCommand(Context context, Layers layers) {
		super("extract", "Extract the contents from the jar", Options.of(LAUNCHER_OPTION, LAYERS_OPTION,
				DESTINATION_OPTION, LIBRARIES_DIRECTORY_OPTION, APPLICATION_FILENAME_OPTION, FORCE_OPTION),
				Parameters.none());
		this.context = context;
		this.layers = layers;
	}

	@Override
	void run(PrintStream out, Map<Option, String> options, List<String> parameters) {
		try {
			checkJarCompatibility();
			File destination = getDestination(options);
			checkDirectoryIsEmpty(options, destination);
			FileResolver fileResolver = getFileResolver(destination, options);
			fileResolver.createDirectories();
			if (options.containsKey(LAUNCHER_OPTION)) {
				extractArchive(fileResolver);
			}
			else {
				JarStructure jarStructure = getJarStructure();
				extractLibraries(fileResolver, jarStructure, options);
				createApplication(jarStructure, fileResolver, options);
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static void checkDirectoryIsEmpty(Map<Option, String> options, File destination) {
		if (options.containsKey(FORCE_OPTION)) {
			return;
		}
		if (!destination.exists()) {
			return;
		}
		if (!destination.isDirectory()) {
			throw new JarModeErrorException(destination.getAbsoluteFile() + " already exists and is not a directory");
		}
		File[] files = destination.listFiles();
		if (files != null && files.length > 0) {
			throw new JarModeErrorException(destination.getAbsoluteFile() + " already exists and is not empty");
		}
	}

	private void checkJarCompatibility() throws IOException {
		File file = this.context.getArchiveFile();
		try (ZipInputStream stream = new ZipInputStream(new FileInputStream(file))) {
			ZipEntry entry = stream.getNextEntry();
			if (entry == null) {
				throw new JarModeErrorException(
						"File '%s' is not compatible; ensure jar file is valid and launch script is not enabled"
							.formatted(file));
			}
		}
	}

	private void extractLibraries(FileResolver fileResolver, JarStructure jarStructure, Map<Option, String> options)
			throws IOException {
		String librariesDirectory = getLibrariesDirectory(options);
		extractArchive(fileResolver, (jarEntry) -> {
			Entry entry = jarStructure.resolve(jarEntry);
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
		String applicationFilename = getApplicationFilename(options);
		if (!options.containsKey(LAYERS_OPTION)) {
			return new NoLayersFileResolver(destination, applicationFilename);
		}
		Layers layers = getLayers();
		Set<String> layersToExtract = StringUtils.commaDelimitedListToSet(options.get(LAYERS_OPTION));
		return new LayersFileResolver(destination, layers, layersToExtract, applicationFilename);
	}

	private File getDestination(Map<Option, String> options) {
		if (options.containsKey(DESTINATION_OPTION)) {
			File destination = new File(options.get(DESTINATION_OPTION));
			if (destination.isAbsolute()) {
				return destination;
			}
			return new File(this.context.getWorkingDir(), destination.getPath());
		}
		return new File(this.context.getWorkingDir(), stripExtension(this.context.getArchiveFile().getName()));
	}

	private static String stripExtension(String name) {
		if (name.toLowerCase(Locale.ROOT).endsWith(".jar") || name.toLowerCase(Locale.ROOT).endsWith(".war")) {
			return name.substring(0, name.length() - 4);
		}
		return name;
	}

	private JarStructure getJarStructure() {
		IndexedJarStructure jarStructure = IndexedJarStructure.get(this.context.getArchiveFile());
		Assert.state(jarStructure != null, "Couldn't read classpath index");
		return jarStructure;
	}

	private void extractArchive(FileResolver fileResolver) throws IOException {
		extractArchive(fileResolver, JarEntry::getName);
	}

	private void extractArchive(FileResolver fileResolver, EntryNameTransformer entryNameTransformer)
			throws IOException {
		withJarEntries(this.context.getArchiveFile(), (stream, jarEntry) -> {
			if (jarEntry.isDirectory()) {
				return;
			}
			String name = entryNameTransformer.getName(jarEntry);
			if (name == null) {
				return;
			}
			File file = fileResolver.resolve(jarEntry, name);
			if (file != null) {
				extractEntry(stream, jarEntry, file);
			}
		});
	}

	private Layers getLayers() {
		return (this.layers != null) ? this.layers : Layers.get(this.context);
	}

	private void createApplication(JarStructure jarStructure, FileResolver fileResolver, Map<Option, String> options)
			throws IOException {
		File file = fileResolver.resolveApplication();
		if (file == null) {
			return;
		}
		String librariesDirectory = getLibrariesDirectory(options);
		Manifest manifest = jarStructure.createLauncherManifest((library) -> librariesDirectory + library);
		mkdirs(file.getParentFile());
		try (JarOutputStream output = new JarOutputStream(new FileOutputStream(file), manifest)) {
			EnumSet<Type> allowedTypes = EnumSet.of(Type.APPLICATION_CLASS_OR_RESOURCE, Type.META_INF);
			Set<String> writtenEntries = new HashSet<>();
			withJarEntries(this.context.getArchiveFile(), ((stream, jarEntry) -> {
				Entry entry = jarStructure.resolve(jarEntry);
				if (entry != null && allowedTypes.contains(entry.type()) && StringUtils.hasLength(entry.location())) {
					JarEntry newJarEntry = createJarEntry(entry.location(), jarEntry);
					if (writtenEntries.add(newJarEntry.getName())) {
						output.putNextEntry(newJarEntry);
						StreamUtils.copy(stream, output);
						output.closeEntry();
					}
					else {
						if (!newJarEntry.isDirectory()) {
							throw new IllegalStateException("Duplicate jar entry '%s' from original location '%s'"
								.formatted(newJarEntry.getName(), entry.originalLocation()));
						}
					}
				}
			}));
		}
	}

	private String getApplicationFilename(Map<Option, String> options) {
		if (options.containsKey(APPLICATION_FILENAME_OPTION)) {
			return options.get(APPLICATION_FILENAME_OPTION);
		}
		return this.context.getArchiveFile().getName();
	}

	private static boolean isType(Entry entry, Type type) {
		return (entry != null) && entry.type() == type;
	}

	private static void extractEntry(InputStream stream, JarEntry entry, File file) throws IOException {
		mkdirs(file.getParentFile());
		try (OutputStream out = new FileOutputStream(file)) {
			StreamUtils.copy(stream, out);
		}
		try {
			Files.getFileAttributeView(file.toPath(), BasicFileAttributeView.class)
				.setTimes(getLastModifiedTime(entry), getLastAccessTime(entry), getCreationTime(entry));
		}
		catch (IOException ex) {
			// File system does not support setting time attributes. Continue.
		}
	}

	private static FileTime getCreationTime(JarEntry entry) {
		return (entry.getCreationTime() != null) ? entry.getCreationTime() : entry.getLastModifiedTime();
	}

	private static FileTime getLastAccessTime(JarEntry entry) {
		return (entry.getLastAccessTime() != null) ? entry.getLastAccessTime() : getLastModifiedTime(entry);
	}

	private static FileTime getLastModifiedTime(JarEntry entry) {
		return (entry.getLastModifiedTime() != null) ? entry.getLastModifiedTime() : entry.getCreationTime();
	}

	private static void mkdirs(File file) throws IOException {
		if (!file.exists() && !file.mkdirs()) {
			throw new IOException("Unable to create directory " + file);
		}
	}

	private static JarEntry createJarEntry(String location, JarEntry originalEntry) {
		JarEntry jarEntry = new JarEntry(location);
		FileTime lastModifiedTime = getLastModifiedTime(originalEntry);
		if (lastModifiedTime != null) {
			jarEntry.setLastModifiedTime(lastModifiedTime);
		}
		FileTime lastAccessTime = getLastAccessTime(originalEntry);
		if (lastAccessTime != null) {
			jarEntry.setLastAccessTime(lastAccessTime);
		}
		FileTime creationTime = getCreationTime(originalEntry);
		if (creationTime != null) {
			jarEntry.setCreationTime(creationTime);
		}
		return jarEntry;
	}

	private static void withJarEntries(File file, ThrowingConsumer callback) throws IOException {
		try (JarFile jarFile = new JarFile(file)) {
			Enumeration<JarEntry> entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				if (StringUtils.hasLength(entry.getName())) {
					try (InputStream stream = jarFile.getInputStream(entry)) {
						callback.accept(stream, entry);
					}
				}
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

		String getName(JarEntry entry);

	}

	@FunctionalInterface
	private interface ThrowingConsumer {

		void accept(InputStream stream, JarEntry entry) throws IOException;

	}

	private interface FileResolver {

		/**
		 * Creates needed directories.
		 * @throws IOException if something went wrong
		 */
		void createDirectories() throws IOException;

		/**
		 * Resolves the given {@link JarEntry} to a file.
		 * @param entry the jar entry
		 * @param newName the new name of the file
		 * @return file where the contents should be written or {@code null} if this entry
		 * should be skipped
		 * @throws IOException if something went wrong
		 */
		default File resolve(JarEntry entry, String newName) throws IOException {
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
		 * Resolves the file for the application.
		 * @return the file for the application or {@code null} if the application should
		 * be skipped
		 * @throws IOException if something went wrong
		 */
		File resolveApplication() throws IOException;

	}

	private static final class NoLayersFileResolver implements FileResolver {

		private final File directory;

		private final String applicationFilename;

		private NoLayersFileResolver(File directory, String applicationFilename) {
			this.directory = directory;
			this.applicationFilename = applicationFilename;
		}

		@Override
		public void createDirectories() {
		}

		@Override
		public File resolve(String originalName, String newName) throws IOException {
			return assertFileIsContainedInDirectory(this.directory, new File(this.directory, newName), newName);
		}

		@Override
		public File resolveApplication() throws IOException {
			return resolve(this.applicationFilename, this.applicationFilename);
		}

	}

	private static final class LayersFileResolver implements FileResolver {

		private final Layers layers;

		private final Set<String> layersToExtract;

		private final File directory;

		private final String applicationFilename;

		LayersFileResolver(File directory, Layers layers, Set<String> layersToExtract, String applicationFilename) {
			this.layers = layers;
			this.layersToExtract = layersToExtract;
			this.directory = directory;
			this.applicationFilename = applicationFilename;
		}

		@Override
		public void createDirectories() throws IOException {
			for (String layer : this.layers) {
				if (shouldExtractLayer(layer)) {
					mkdirs(getLayerDirectory(layer));
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
		public File resolveApplication() throws IOException {
			String layer = this.layers.getApplicationLayerName();
			if (shouldExtractLayer(layer)) {
				File directory = getLayerDirectory(layer);
				return assertFileIsContainedInDirectory(directory, new File(directory, this.applicationFilename),
						this.applicationFilename);
			}
			return null;
		}

		private File getLayerDirectory(String layer) {
			return new File(this.directory, layer);
		}

		private boolean shouldExtractLayer(String layer) {
			return this.layersToExtract.isEmpty() || this.layersToExtract.contains(layer);
		}

	}

}
