/*
 * Copyright 2012-2021 the original author or authors.
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
import java.util.stream.Collectors;

import org.apache.commons.compress.archivers.jar.JarArchiveEntry;

import org.springframework.boot.loader.tools.AbstractJarWriter.EntryTransformer;
import org.springframework.boot.loader.tools.AbstractJarWriter.UnpackHandler;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Abstract base class for packagers.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @author Scott Frederick
 * @since 2.3.0
 */
public abstract class Packager {

	private static final String MAIN_CLASS_ATTRIBUTE = "Main-Class";

	private static final String START_CLASS_ATTRIBUTE = "Start-Class";

	private static final String BOOT_VERSION_ATTRIBUTE = "Spring-Boot-Version";

	private static final String BOOT_CLASSES_ATTRIBUTE = "Spring-Boot-Classes";

	private static final String BOOT_LIB_ATTRIBUTE = "Spring-Boot-Lib";

	private static final String BOOT_CLASSPATH_INDEX_ATTRIBUTE = "Spring-Boot-Classpath-Index";

	private static final String BOOT_LAYERS_INDEX_ATTRIBUTE = "Spring-Boot-Layers-Index";

	private static final byte[] ZIP_FILE_HEADER = new byte[] { 'P', 'K', 3, 4 };

	private static final long FIND_WARNING_TIMEOUT = TimeUnit.SECONDS.toMillis(10);

	private static final String SPRING_BOOT_APPLICATION_CLASS_NAME = "org.springframework.boot.autoconfigure.SpringBootApplication";

	private final List<MainClassTimeoutWarningListener> mainClassTimeoutListeners = new ArrayList<>();

	private String mainClass;

	private final File source;

	private File backupFile;

	private Layout layout;

	private LayoutFactory layoutFactory;

	private Layers layers;

	private LayersIndex layersIndex;

	private boolean includeRelevantJarModeJars = true;

	/**
	 * Create a new {@link Packager} instance.
	 * @param source the source archive file to package
	 */
	protected Packager(File source) {
		this(source, null);
	}

	/**
	 * Create a new {@link Packager} instance.
	 * @param source the source archive file to package
	 * @param layoutFactory the layout factory to use or {@code null}
	 * @deprecated since 2.3.10 for removal in 2.5 in favor of {@link #Packager(File)} and
	 * {@link #setLayoutFactory(LayoutFactory)}
	 */
	@Deprecated
	protected Packager(File source, LayoutFactory layoutFactory) {
		Assert.notNull(source, "Source file must not be null");
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
	 * Sets the layers that should be used in the jar.
	 * @param layers the jar layers
	 */
	public void setLayers(Layers layers) {
		Assert.notNull(layers, "Layers must not be null");
		this.layers = layers;
		this.layersIndex = new LayersIndex(layers);
	}

	/**
	 * Sets the {@link File} to use to backup the original source.
	 * @param backupFile the file to use to backup the original source
	 */
	protected void setBackupFile(File backupFile) {
		this.backupFile = backupFile;
	}

	/**
	 * Sets if jarmode jars relevant for the packaging should be automatically included.
	 * @param includeRelevantJarModeJars if relevant jars are included
	 */
	public void setIncludeRelevantJarModeJars(boolean includeRelevantJarModeJars) {
		this.includeRelevantJarModeJars = includeRelevantJarModeJars;
	}

	protected final boolean isAlreadyPackaged() {
		return isAlreadyPackaged(this.source);
	}

	protected final boolean isAlreadyPackaged(File file) {
		try (JarFile jarFile = new JarFile(file)) {
			Manifest manifest = jarFile.getManifest();
			return (manifest != null && manifest.getMainAttributes().getValue(BOOT_VERSION_ATTRIBUTE) != null);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Error reading archive file", ex);
		}
	}

	protected final void write(JarFile sourceJar, Libraries libraries, AbstractJarWriter writer) throws IOException {
		Assert.notNull(libraries, "Libraries must not be null");
		WritableLibraries writeableLibraries = new WritableLibraries(libraries);
		writer.useLayers(this.layers, this.layersIndex);
		writer.writeManifest(buildManifest(sourceJar));
		writeLoaderClasses(writer);
		writer.writeEntries(sourceJar, getEntityTransformer(), writeableLibraries);
		writeableLibraries.write(writer);
		if (this.layers != null) {
			writeLayerIndex(writer);
		}
	}

	private void writeLoaderClasses(AbstractJarWriter writer) throws IOException {
		Layout layout = getLayout();
		if (layout instanceof CustomLoaderLayout) {
			((CustomLoaderLayout) getLayout()).writeLoadedClasses(writer);
		}
		else if (layout.isExecutable()) {
			writer.writeLoaderClasses();
		}
	}

	private void writeLayerIndex(AbstractJarWriter writer) throws IOException {
		String name = ((RepackagingLayout) this.layout).getLayersIndexFileLocation();
		if (StringUtils.hasLength(name)) {
			Layer layer = this.layers.getLayer(name);
			this.layersIndex.add(layer, name);
			writer.writeEntry(name, this.layersIndex::writeTo);
		}
	}

	private EntryTransformer getEntityTransformer() {
		if (getLayout() instanceof RepackagingLayout) {
			return new RepackagingEntryTransformer((RepackagingLayout) getLayout());
		}
		return EntryTransformer.NONE;
	}

	private boolean isZip(InputStreamSupplier supplier) {
		try {
			try (InputStream inputStream = supplier.openStream()) {
				return isZip(inputStream);
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
		String launcherClass = getLayout().getLauncherClassName();
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
		return MainClassFinder.findSingleMainClass(source, getLayout().getClassesLocation(),
				SPRING_BOOT_APPLICATION_CLASS_NAME);
	}

	/**
	 * Return the {@link File} to use to backup the original source.
	 * @return the file to use to backup the original source
	 */
	public final File getBackupFile() {
		if (this.backupFile != null) {
			return this.backupFile;
		}
		return new File(this.source.getParentFile(), this.source.getName() + ".original");
	}

	protected final File getSource() {
		return this.source;
	}

	protected final Layout getLayout() {
		if (this.layout == null) {
			Layout createdLayout = getLayoutFactory().getLayout(this.source);
			Assert.state(createdLayout != null, "Unable to detect layout");
			this.layout = createdLayout;
		}
		return this.layout;
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

	private void addBootAttributes(Attributes attributes) {
		attributes.putValue(BOOT_VERSION_ATTRIBUTE, getClass().getPackage().getImplementationVersion());
		Layout layout = getLayout();
		if (layout instanceof RepackagingLayout) {
			addBootBootAttributesForRepackagingLayout(attributes, (RepackagingLayout) layout);
		}
		else {
			addBootBootAttributesForPlainLayout(attributes);
		}
	}

	private void addBootBootAttributesForRepackagingLayout(Attributes attributes, RepackagingLayout layout) {
		attributes.putValue(BOOT_CLASSES_ATTRIBUTE, layout.getRepackagedClassesLocation());
		putIfHasLength(attributes, BOOT_LIB_ATTRIBUTE, getLayout().getLibraryLocation("", LibraryScope.COMPILE));
		putIfHasLength(attributes, BOOT_CLASSPATH_INDEX_ATTRIBUTE, layout.getClasspathIndexFileLocation());
		if (this.layers != null) {
			putIfHasLength(attributes, BOOT_LAYERS_INDEX_ATTRIBUTE, layout.getLayersIndexFileLocation());
		}
	}

	private void addBootBootAttributesForPlainLayout(Attributes attributes) {
		attributes.putValue(BOOT_CLASSES_ATTRIBUTE, getLayout().getClassesLocation());
		putIfHasLength(attributes, BOOT_LIB_ATTRIBUTE, getLayout().getLibraryLocation("", LibraryScope.COMPILE));
	}

	private void putIfHasLength(Attributes attributes, String name, String value) {
		if (StringUtils.hasLength(value)) {
			attributes.putValue(name, value);
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
			String transformedName = transformName(entry.getName());
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

		private String transformName(String name) {
			return this.layout.getRepackagedClassesLocation() + name;
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

		private final Map<String, Library> libraries = new LinkedHashMap<>();

		WritableLibraries(Libraries libraries) throws IOException {
			libraries.doWithLibraries((library) -> {
				if (isZip(library::openStream)) {
					addLibrary(library);
				}
			});
			if (Packager.this.layers != null && Packager.this.includeRelevantJarModeJars) {
				addLibrary(JarModeLibrary.LAYER_TOOLS);
			}
		}

		private void addLibrary(Library library) {
			String location = getLayout().getLibraryLocation(library.getName(), library.getScope());
			if (location != null) {
				String path = location + library.getName();
				Library existing = this.libraries.putIfAbsent(path, library);
				Assert.state(existing == null, () -> "Duplicate library " + library.getName());
			}
		}

		@Override
		public boolean requiresUnpack(String name) {
			Library library = this.libraries.get(name);
			return library != null && library.isUnpackRequired();
		}

		@Override
		public String sha1Hash(String name) throws IOException {
			Library library = this.libraries.get(name);
			Assert.notNull(library, () -> "No library found for entry name '" + name + "'");
			return Digest.sha1(library::openStream);
		}

		private void write(AbstractJarWriter writer) throws IOException {
			for (Entry<String, Library> entry : this.libraries.entrySet()) {
				String path = entry.getKey();
				Library library = entry.getValue();
				String location = path.substring(0, path.lastIndexOf('/') + 1);
				writer.writeNestedLibrary(location, library);
			}
			if (getLayout() instanceof RepackagingLayout) {
				writeClasspathIndex((RepackagingLayout) getLayout(), writer);
			}
		}

		private void writeClasspathIndex(RepackagingLayout layout, AbstractJarWriter writer) throws IOException {
			List<String> names = this.libraries.keySet().stream().map((path) -> "- \"" + path + "\"")
					.collect(Collectors.toList());
			writer.writeIndexFile(layout.getClasspathIndexFileLocation(), names);
		}

	}

}
