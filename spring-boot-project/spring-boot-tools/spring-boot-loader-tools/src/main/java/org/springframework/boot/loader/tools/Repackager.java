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

package org.springframework.boot.loader.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.compress.archivers.jar.JarArchiveEntry;

import org.springframework.boot.loader.tools.JarWriter.EntryTransformer;
import org.springframework.boot.loader.tools.JarWriter.UnpackHandler;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Utility class that can be used to repackage an archive so that it can be executed using
 * '{@literal java -jar}'.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 1.0.0
 */
public class Repackager {

	private static final String MAIN_CLASS_ATTRIBUTE = "Main-Class";

	private static final String START_CLASS_ATTRIBUTE = "Start-Class";

	private static final String BOOT_VERSION_ATTRIBUTE = "Spring-Boot-Version";

	private static final String BOOT_CLASSES_ATTRIBUTE = "Spring-Boot-Classes";

	private static final String BOOT_LIB_ATTRIBUTE = "Spring-Boot-Lib";

	private static final byte[] ZIP_FILE_HEADER = new byte[] { 'P', 'K', 3, 4 };

	private static final long FIND_WARNING_TIMEOUT = TimeUnit.SECONDS.toMillis(10);

	private static final String SPRING_BOOT_APPLICATION_CLASS_NAME = "org.springframework.boot.autoconfigure.SpringBootApplication";

	private List<MainClassTimeoutWarningListener> mainClassTimeoutListeners = new ArrayList<>();

	private String mainClass;

	private boolean backupSource = true;

	private final File source;

	private Layout layout;

	private LayoutFactory layoutFactory;

	public Repackager(File source) {
		this(source, null);
	}

	public Repackager(File source, LayoutFactory layoutFactory) {
		Assert.notNull(source, "Source file must be provided");
		Assert.isTrue(source.exists() && source.isFile(),
				"Source must refer to an existing file, got " + source.getAbsolutePath());
		this.source = source.getAbsoluteFile();
		this.layoutFactory = layoutFactory;
	}

	/**
	 * Add a listener that will be triggered to display a warning if searching for the
	 * main class takes too long.
	 * @param listener the listener to add
	 */
	public void addMainClassTimeoutWarningListener(MainClassTimeoutWarningListener listener) {
		this.mainClassTimeoutListeners.add(listener);
	}

	/**
	 * Sets the main class that should be run. If not specified the value from the
	 * MANIFEST will be used, or if no manifest entry is found the archive will be
	 * searched for a suitable class.
	 * @param mainClass the main class name
	 */
	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}

	/**
	 * Sets if source files should be backed up when they would be overwritten.
	 * @param backupSource if source files should be backed up
	 */
	public void setBackupSource(boolean backupSource) {
		this.backupSource = backupSource;
	}

	/**
	 * Sets the layout to use for the jar. Defaults to {@link Layouts#forFile(File)}.
	 * @param layout the layout
	 */
	public void setLayout(Layout layout) {
		Assert.notNull(layout, "Layout must not be null");
		this.layout = layout;
	}

	/**
	 * Sets the layout factory for the jar. The factory can be used when no specific
	 * layout is specified.
	 * @param layoutFactory the layout factory to set
	 */
	public void setLayoutFactory(LayoutFactory layoutFactory) {
		this.layoutFactory = layoutFactory;
	}

	/**
	 * Repackage the source file so that it can be run using '{@literal java -jar}'.
	 * @param libraries the libraries required to run the archive
	 * @throws IOException if the file cannot be repackaged
	 */
	public void repackage(Libraries libraries) throws IOException {
		repackage(this.source, libraries);
	}

	/**
	 * Repackage to the given destination so that it can be launched using '
	 * {@literal java -jar}'.
	 * @param destination the destination file (may be the same as the source)
	 * @param libraries the libraries required to run the archive
	 * @throws IOException if the file cannot be repackaged
	 */
	public void repackage(File destination, Libraries libraries) throws IOException {
		repackage(destination, libraries, null);
	}

	/**
	 * Repackage to the given destination so that it can be launched using '
	 * {@literal java -jar}'.
	 * @param destination the destination file (may be the same as the source)
	 * @param libraries the libraries required to run the archive
	 * @param launchScript an optional launch script prepended to the front of the jar
	 * @throws IOException if the file cannot be repackaged
	 * @since 1.3.0
	 */
	public void repackage(File destination, Libraries libraries, LaunchScript launchScript) throws IOException {
		Assert.isTrue(destination != null && !destination.isDirectory(), "Invalid destination");
		Assert.notNull(libraries, "Libraries must not be null");
		if (this.layout == null) {
			this.layout = getLayoutFactory().getLayout(this.source);
		}
		destination = destination.getAbsoluteFile();
		File workingSource = this.source;
		if (alreadyRepackaged() && this.source.equals(destination)) {
			return;
		}
		if (this.source.equals(destination)) {
			workingSource = getBackupFile();
			workingSource.delete();
			renameFile(this.source, workingSource);
		}
		destination.delete();
		try {
			try (JarFile jarFileSource = new JarFile(workingSource)) {
				repackage(jarFileSource, destination, libraries, launchScript);
			}
		}
		finally {
			if (!this.backupSource && !this.source.equals(workingSource)) {
				deleteFile(workingSource);
			}
		}
	}

	private LayoutFactory getLayoutFactory() {
		if (this.layoutFactory != null) {
			return this.layoutFactory;
		}
		List<LayoutFactory> factories = SpringFactoriesLoader.loadFactories(LayoutFactory.class, null);
		if (factories.isEmpty()) {
			return new DefaultLayoutFactory();
		}
		Assert.state(factories.size() == 1, "No unique LayoutFactory found");
		return factories.get(0);
	}

	/**
	 * Return the {@link File} to use to backup the original source.
	 * @return the file to use to backup the original source
	 */
	public final File getBackupFile() {
		return new File(this.source.getParentFile(), this.source.getName() + ".original");
	}

	private boolean alreadyRepackaged() throws IOException {
		try (JarFile jarFile = new JarFile(this.source)) {
			Manifest manifest = jarFile.getManifest();
			return (manifest != null && manifest.getMainAttributes().getValue(BOOT_VERSION_ATTRIBUTE) != null);
		}
	}

	private void repackage(JarFile sourceJar, File destination, Libraries libraries, LaunchScript launchScript)
			throws IOException {
		WritableLibraries writeableLibraries = new WritableLibraries(libraries);
		try (JarWriter writer = new JarWriter(destination, launchScript)) {
			writer.writeManifest(buildManifest(sourceJar));
			writeLoaderClasses(writer);
			writer.writeEntries(sourceJar, getEntityTransformer(), writeableLibraries);
			writeableLibraries.write(writer);
		}
	}

	private void writeLoaderClasses(JarWriter writer) throws IOException {
		if (this.layout instanceof CustomLoaderLayout) {
			((CustomLoaderLayout) this.layout).writeLoadedClasses(writer);
		}
		else if (this.layout.isExecutable()) {
			writer.writeLoaderClasses();
		}
	}

	private EntryTransformer getEntityTransformer() {
		if (this.layout instanceof RepackagingLayout) {
			return new RepackagingEntryTransformer((RepackagingLayout) this.layout);
		}
		return EntryTransformer.NONE;
	}

	private boolean isZip(File file) {
		try {
			try (FileInputStream fileInputStream = new FileInputStream(file)) {
				return isZip(fileInputStream);
			}
		}
		catch (IOException ex) {
			return false;
		}
	}

	private boolean isZip(InputStream inputStream) throws IOException {
		for (byte magicByte : ZIP_FILE_HEADER) {
			if (inputStream.read() != magicByte) {
				return false;
			}
		}
		return true;
	}

	private Manifest buildManifest(JarFile source) throws IOException {
		Manifest manifest = createInitialManifest(source);
		addMainAndStartAttributes(source, manifest);
		addBootAttributes(manifest.getMainAttributes());
		return manifest;
	}

	private Manifest createInitialManifest(JarFile source) throws IOException {
		if (source.getManifest() != null) {
			return new Manifest(source.getManifest());
		}
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
		return manifest;
	}

	private void addMainAndStartAttributes(JarFile source, Manifest manifest) throws IOException {
		String mainClass = getMainClass(source, manifest);
		String launcherClass = this.layout.getLauncherClassName();
		if (launcherClass != null) {
			Assert.state(mainClass != null, "Unable to find main class");
			manifest.getMainAttributes().putValue(MAIN_CLASS_ATTRIBUTE, launcherClass);
			manifest.getMainAttributes().putValue(START_CLASS_ATTRIBUTE, mainClass);
		}
		else if (mainClass != null) {
			manifest.getMainAttributes().putValue(MAIN_CLASS_ATTRIBUTE, mainClass);
		}
	}

	private String getMainClass(JarFile source, Manifest manifest) throws IOException {
		if (this.mainClass != null) {
			return this.mainClass;
		}
		String attributeValue = manifest.getMainAttributes().getValue(MAIN_CLASS_ATTRIBUTE);
		if (attributeValue != null) {
			return attributeValue;
		}
		return findMainMethodWithTimeoutWarning(source);
	}

	private String findMainMethodWithTimeoutWarning(JarFile source) throws IOException {
		long startTime = System.currentTimeMillis();
		String mainMethod = findMainMethod(source);
		long duration = System.currentTimeMillis() - startTime;
		if (duration > FIND_WARNING_TIMEOUT) {
			for (MainClassTimeoutWarningListener listener : this.mainClassTimeoutListeners) {
				listener.handleTimeoutWarning(duration, mainMethod);
			}
		}
		return mainMethod;
	}

	protected String findMainMethod(JarFile source) throws IOException {
		return MainClassFinder.findSingleMainClass(source, this.layout.getClassesLocation(),
				SPRING_BOOT_APPLICATION_CLASS_NAME);
	}

	private void addBootAttributes(Attributes attributes) {
		attributes.putValue(BOOT_VERSION_ATTRIBUTE, getClass().getPackage().getImplementationVersion());
		if (this.layout instanceof RepackagingLayout) {
			addBootBootAttributesForRepackagingLayout(attributes, (RepackagingLayout) this.layout);
		}
		else {
			addBootBootAttributesForPlainLayout(attributes, this.layout);
		}
	}

	private void addBootBootAttributesForRepackagingLayout(Attributes attributes, RepackagingLayout layout) {
		attributes.putValue(BOOT_CLASSES_ATTRIBUTE, layout.getRepackagedClassesLocation());
		putIfHasLength(attributes, BOOT_LIB_ATTRIBUTE, this.layout.getLibraryLocation("", LibraryScope.COMPILE));
	}

	private void addBootBootAttributesForPlainLayout(Attributes attributes, Layout layout) {
		attributes.putValue(BOOT_CLASSES_ATTRIBUTE, this.layout.getClassesLocation());
		putIfHasLength(attributes, BOOT_LIB_ATTRIBUTE, this.layout.getLibraryLocation("", LibraryScope.COMPILE));
	}

	private void putIfHasLength(Attributes attributes, String name, String value) {
		if (StringUtils.hasLength(value)) {
			attributes.putValue(name, value);
		}
	}

	private void renameFile(File file, File dest) {
		if (!file.renameTo(dest)) {
			throw new IllegalStateException("Unable to rename '" + file + "' to '" + dest + "'");
		}
	}

	private void deleteFile(File file) {
		if (!file.delete()) {
			throw new IllegalStateException("Unable to delete '" + file + "'");
		}
	}

	/**
	 * Callback interface used to present a warning when finding the main class takes too
	 * long.
	 */
	@FunctionalInterface
	public interface MainClassTimeoutWarningListener {

		/**
		 * Handle a timeout warning.
		 * @param duration the amount of time it took to find the main method
		 * @param mainMethod the main method that was actually found
		 */
		void handleTimeoutWarning(long duration, String mainMethod);

	}

	/**
	 * An {@code EntryTransformer} that renames entries by applying a prefix.
	 */
	private static final class RepackagingEntryTransformer implements EntryTransformer {

		private final RepackagingLayout layout;

		private RepackagingEntryTransformer(RepackagingLayout layout) {
			this.layout = layout;
		}

		@Override
		public JarArchiveEntry transform(JarArchiveEntry entry) {
			if (entry.getName().equals("META-INF/INDEX.LIST")) {
				return null;
			}
			if (!isTransformable(entry)) {
				return entry;
			}
			String transformedName = this.layout.getRepackagedClassesLocation() + entry.getName();
			JarArchiveEntry transformedEntry = new JarArchiveEntry(transformedName);
			transformedEntry.setTime(entry.getTime());
			transformedEntry.setSize(entry.getSize());
			transformedEntry.setMethod(entry.getMethod());
			if (entry.getComment() != null) {
				transformedEntry.setComment(entry.getComment());
			}
			transformedEntry.setCompressedSize(entry.getCompressedSize());
			transformedEntry.setCrc(entry.getCrc());
			if (entry.getCreationTime() != null) {
				transformedEntry.setCreationTime(entry.getCreationTime());
			}
			if (entry.getExtra() != null) {
				transformedEntry.setExtra(entry.getExtra());
			}
			if (entry.getLastAccessTime() != null) {
				transformedEntry.setLastAccessTime(entry.getLastAccessTime());
			}
			if (entry.getLastModifiedTime() != null) {
				transformedEntry.setLastModifiedTime(entry.getLastModifiedTime());
			}
			return transformedEntry;
		}

		private boolean isTransformable(JarArchiveEntry entry) {
			String name = entry.getName();
			if (name.startsWith("META-INF/")) {
				return name.equals("META-INF/aop.xml") || name.endsWith(".kotlin_module");
			}
			return !name.startsWith("BOOT-INF/") && !name.equals("module-info.class");
		}

	}

	/**
	 * An {@link UnpackHandler} that determines that an entry needs to be unpacked if a
	 * library that requires unpacking has a matching entry name.
	 */
	private final class WritableLibraries implements UnpackHandler {

		private final Map<String, Library> libraryEntryNames = new LinkedHashMap<>();

		private WritableLibraries(Libraries libraries) throws IOException {
			libraries.doWithLibraries((library) -> {
				if (isZip(library.getFile())) {
					String location = getLocation(library);
					if (location != null) {
						Library existing = this.libraryEntryNames.putIfAbsent(location + library.getName(), library);
						Assert.state(existing == null, "Duplicate library " + library.getName());
					}
				}
			});
		}

		private String getLocation(Library library) {
			return Repackager.this.layout.getLibraryLocation(library.getName(), library.getScope());
		}

		@Override
		public boolean requiresUnpack(String name) {
			Library library = this.libraryEntryNames.get(name);
			return library != null && library.isUnpackRequired();
		}

		@Override
		public String sha1Hash(String name) throws IOException {
			Library library = this.libraryEntryNames.get(name);
			Assert.notNull(library, "No library found for entry name '" + name + "'");
			return FileUtils.sha1Hash(library.getFile());
		}

		private void write(JarWriter writer) throws IOException {
			for (Entry<String, Library> entry : this.libraryEntryNames.entrySet()) {
				writer.writeNestedLibrary(entry.getKey().substring(0, entry.getKey().lastIndexOf('/') + 1),
						entry.getValue());
			}
		}

	}

}
