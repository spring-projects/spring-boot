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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;

import org.gradle.api.GradleException;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.file.RelativePath;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.internal.file.copy.CopyActionProcessingStream;
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal;
import org.gradle.api.java.archives.Attributes;
import org.gradle.api.java.archives.Manifest;
import org.gradle.api.provider.Property;
import org.gradle.api.specs.Spec;
import org.gradle.api.specs.Specs;
import org.gradle.api.tasks.WorkResult;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.util.PatternSet;
import org.gradle.util.GradleVersion;

import org.springframework.boot.loader.tools.LoaderImplementation;

/**
 * Support class for implementations of {@link BootArchive}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 * @see BootJar
 * @see BootWar
 */
class BootArchiveSupport {

	private static final byte[] ZIP_FILE_HEADER = new byte[] { 'P', 'K', 3, 4 };

	private static final String UNSPECIFIED_VERSION = "unspecified";

	private static final Set<String> DEFAULT_LAUNCHER_CLASSES;

	static {
		Set<String> defaultLauncherClasses = new HashSet<>();
		defaultLauncherClasses.add("org.springframework.boot.loader.launch.JarLauncher");
		defaultLauncherClasses.add("org.springframework.boot.loader.launch.PropertiesLauncher");
		defaultLauncherClasses.add("org.springframework.boot.loader.launch.WarLauncher");
		DEFAULT_LAUNCHER_CLASSES = Collections.unmodifiableSet(defaultLauncherClasses);
	}

	private final PatternSet requiresUnpack = new PatternSet();

	private final PatternSet exclusions = new PatternSet();

	private final String loaderMainClass;

	private final Spec<FileCopyDetails> librarySpec;

	private final Function<FileCopyDetails, ZipCompression> compressionResolver;

	private LaunchScriptConfiguration launchScript;

	/**
     * Creates a new instance of BootArchiveSupport with the specified parameters.
     * 
     * @param loaderMainClass The main class to be used as the entry point for the boot archive.
     * @param librarySpec The specification for copying library files to the boot archive.
     * @param compressionResolver The function used to resolve the compression method for each file in the boot archive.
     */
    BootArchiveSupport(String loaderMainClass, Spec<FileCopyDetails> librarySpec,
			Function<FileCopyDetails, ZipCompression> compressionResolver) {
		this.loaderMainClass = loaderMainClass;
		this.librarySpec = librarySpec;
		this.compressionResolver = compressionResolver;
		this.requiresUnpack.include(Specs.satisfyNone());
	}

	/**
     * Configures the manifest file with the provided parameters.
     * 
     * @param manifest              the manifest object to be configured
     * @param mainClass             the main class of the application
     * @param classes               the classes directory of the application
     * @param lib                   the lib directory of the application
     * @param classPathIndex        the classpath index of the application
     * @param layersIndex           the layers index of the application
     * @param jdkVersion            the JDK version used for building the application
     * @param implementationTitle   the title of the implementation
     * @param implementationVersion the version of the implementation
     */
    void configureManifest(Manifest manifest, String mainClass, String classes, String lib, String classPathIndex,
			String layersIndex, String jdkVersion, String implementationTitle, Object implementationVersion) {
		Attributes attributes = manifest.getAttributes();
		attributes.putIfAbsent("Main-Class", this.loaderMainClass);
		attributes.putIfAbsent("Start-Class", mainClass);
		attributes.computeIfAbsent("Spring-Boot-Version", (name) -> determineSpringBootVersion());
		attributes.putIfAbsent("Spring-Boot-Classes", classes);
		attributes.putIfAbsent("Spring-Boot-Lib", lib);
		if (classPathIndex != null) {
			attributes.putIfAbsent("Spring-Boot-Classpath-Index", classPathIndex);
		}
		if (layersIndex != null) {
			attributes.putIfAbsent("Spring-Boot-Layers-Index", layersIndex);
		}
		attributes.putIfAbsent("Build-Jdk-Spec", jdkVersion);
		attributes.putIfAbsent("Implementation-Title", implementationTitle);
		if (implementationVersion != null) {
			String versionString = implementationVersion.toString();
			if (!UNSPECIFIED_VERSION.equals(versionString)) {
				attributes.putIfAbsent("Implementation-Version", versionString);
			}
		}
	}

	/**
     * Determines the version of Spring Boot.
     * 
     * @return The version of Spring Boot if available, otherwise "unknown".
     */
    private String determineSpringBootVersion() {
		String version = getClass().getPackage().getImplementationVersion();
		return (version != null) ? version : "unknown";
	}

	/**
     * Creates a copy action for a given JAR file with resolved dependencies, loader implementation,
     * and support for signature files.
     * 
     * @param jar the JAR file to create a copy action for
     * @param resolvedDependencies the resolved dependencies for the JAR file
     * @param loaderImplementation the loader implementation to use for the copy action
     * @param supportsSignatureFile flag indicating whether the copy action should support signature files
     * @return the created copy action
     */
    CopyAction createCopyAction(Jar jar, ResolvedDependencies resolvedDependencies,
			LoaderImplementation loaderImplementation, boolean supportsSignatureFile) {
		return createCopyAction(jar, resolvedDependencies, loaderImplementation, supportsSignatureFile, null, null);
	}

	/**
     * Creates a copy action for a given JAR file.
     * 
     * @param jar The JAR file to create a copy action for.
     * @param resolvedDependencies The resolved dependencies for the JAR file.
     * @param loaderImplementation The loader implementation for the JAR file.
     * @param supportsSignatureFile A flag indicating if the JAR file supports signature files.
     * @param layerResolver The layer resolver for the JAR file.
     * @param layerToolsLocation The location of the layer tools for the JAR file.
     * @return The copy action for the JAR file.
     */
    CopyAction createCopyAction(Jar jar, ResolvedDependencies resolvedDependencies,
			LoaderImplementation loaderImplementation, boolean supportsSignatureFile, LayerResolver layerResolver,
			String layerToolsLocation) {
		File output = jar.getArchiveFile().get().getAsFile();
		Manifest manifest = jar.getManifest();
		boolean preserveFileTimestamps = jar.isPreserveFileTimestamps();
		Integer dirMode = getDirMode(jar);
		Integer fileMode = getFileMode(jar);
		boolean includeDefaultLoader = isUsingDefaultLoader(jar);
		Spec<FileTreeElement> requiresUnpack = this.requiresUnpack.getAsSpec();
		Spec<FileTreeElement> exclusions = this.exclusions.getAsExcludeSpec();
		LaunchScriptConfiguration launchScript = this.launchScript;
		Spec<FileCopyDetails> librarySpec = this.librarySpec;
		Function<FileCopyDetails, ZipCompression> compressionResolver = this.compressionResolver;
		String encoding = jar.getMetadataCharset();
		CopyAction action = new BootZipCopyAction(output, manifest, preserveFileTimestamps, dirMode, fileMode,
				includeDefaultLoader, layerToolsLocation, requiresUnpack, exclusions, launchScript, librarySpec,
				compressionResolver, encoding, resolvedDependencies, supportsSignatureFile, layerResolver,
				loaderImplementation);
		return jar.isReproducibleFileOrder() ? new ReproducibleOrderingCopyAction(action) : action;
	}

	/**
     * Returns the directory mode for the given CopySpec.
     * 
     * @param copySpec the CopySpec to get the directory mode from
     * @return the directory mode as an Integer
     */
    private Integer getDirMode(CopySpec copySpec) {
		return getMode(copySpec, "getDirPermissions", copySpec::getDirMode);
	}

	/**
     * Returns the file mode for the given CopySpec.
     * 
     * @param copySpec the CopySpec to get the file mode from
     * @return the file mode as an Integer
     */
    private Integer getFileMode(CopySpec copySpec) {
		return getMode(copySpec, "getFilePermissions", copySpec::getFileMode);
	}

	/**
     * Retrieves the mode of the specified copySpec using the given methodName and fallback supplier.
     * 
     * @param copySpec the copySpec to retrieve the mode from
     * @param methodName the name of the method to invoke on the copySpec
     * @param fallback the fallback supplier to use if the Gradle version is less than 8.3
     * @return the mode of the copySpec, or the fallback value if the Gradle version is less than 8.3
     * @throws GradleException if there is an error retrieving the permissions
     */
    @SuppressWarnings("unchecked")
	private Integer getMode(CopySpec copySpec, String methodName, Supplier<Integer> fallback) {
		if (GradleVersion.current().compareTo(GradleVersion.version("8.3")) >= 0) {
			try {
				Object filePermissions = ((Property<Object>) copySpec.getClass().getMethod(methodName).invoke(copySpec))
					.getOrNull();
				return (filePermissions != null)
						? (int) filePermissions.getClass().getMethod("toUnixNumeric").invoke(filePermissions) : null;
			}
			catch (Exception ex) {
				throw new GradleException("Failed to get permissions", ex);
			}
		}
		return fallback.get();
	}

	/**
     * Checks if the given Jar is using the default loader.
     * 
     * @param jar the Jar to check
     * @return true if the Jar is using the default loader, false otherwise
     */
    private boolean isUsingDefaultLoader(Jar jar) {
		return DEFAULT_LAUNCHER_CLASSES.contains(jar.getManifest().getAttributes().get("Main-Class"));
	}

	/**
     * Returns the launch script configuration for the BootArchiveSupport class.
     *
     * @return the launch script configuration
     */
    LaunchScriptConfiguration getLaunchScript() {
		return this.launchScript;
	}

	/**
     * Sets the launch script configuration for the boot archive.
     * 
     * @param launchScript the launch script configuration to be set
     */
    void setLaunchScript(LaunchScriptConfiguration launchScript) {
		this.launchScript = launchScript;
	}

	/**
     * Adds the specified patterns to the list of patterns that require unpacking.
     * 
     * @param patterns the patterns to be added
     */
    void requiresUnpack(String... patterns) {
		this.requiresUnpack.include(patterns);
	}

	/**
     * Adds the specified {@code spec} to the list of requirements that need to be unpacked.
     * 
     * @param spec the specification of the file tree element to be included in the unpacking process
     */
    void requiresUnpack(Spec<FileTreeElement> spec) {
		this.requiresUnpack.include(spec);
	}

	/**
     * Excludes non-zip library files from the given FileCopyDetails.
     * 
     * @param details the FileCopyDetails to exclude non-zip library files from
     */
    void excludeNonZipLibraryFiles(FileCopyDetails details) {
		if (this.librarySpec.isSatisfiedBy(details)) {
			excludeNonZipFiles(details);
		}
	}

	/**
     * Excludes non-zip files from the given FileCopyDetails object.
     * 
     * @param details the FileCopyDetails object containing the file to be checked
     */
    void excludeNonZipFiles(FileCopyDetails details) {
		if (!isZip(details.getFile())) {
			details.exclude();
		}
	}

	/**
     * Checks if the given file is a ZIP archive.
     * 
     * @param file the file to be checked
     * @return true if the file is a ZIP archive, false otherwise
     */
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

	/**
     * Checks if the given input stream represents a ZIP file.
     *
     * @param inputStream the input stream to check
     * @return true if the input stream is a ZIP file, false otherwise
     * @throws IOException if an I/O error occurs while reading the input stream
     */
    private boolean isZip(InputStream inputStream) throws IOException {
		for (byte headerByte : ZIP_FILE_HEADER) {
			if (inputStream.read() != headerByte) {
				return false;
			}
		}
		return true;
	}

	/**
     * Moves all module-info.class files found in the specified CopySpec to the root directory.
     * 
     * @param spec the CopySpec containing the files to be checked and moved
     */
    void moveModuleInfoToRoot(CopySpec spec) {
		spec.filesMatching("module-info.class", this::moveToRoot);
	}

	/**
     * Moves the specified file copy details to the root directory.
     * 
     * @param details the file copy details to be moved
     */
    void moveToRoot(FileCopyDetails details) {
		details.setRelativePath(details.getRelativeSourcePath());
	}

	/**
	 * {@link CopyAction} variant that sorts entries to ensure reproducible ordering.
	 */
	private static final class ReproducibleOrderingCopyAction implements CopyAction {

		private final CopyAction delegate;

		/**
         * Constructs a new ReproducibleOrderingCopyAction with the specified delegate.
         * 
         * @param delegate the CopyAction delegate to be used
         */
        private ReproducibleOrderingCopyAction(CopyAction delegate) {
			this.delegate = delegate;
		}

		/**
         * Executes the copy action processing stream.
         * 
         * @param stream the copy action processing stream
         * @return the work result of the execution
         */
        @Override
		public WorkResult execute(CopyActionProcessingStream stream) {
			return this.delegate.execute((action) -> {
				Map<RelativePath, FileCopyDetailsInternal> detailsByPath = new TreeMap<>();
				stream.process((details) -> detailsByPath.put(details.getRelativePath(), details));
				detailsByPath.values().forEach(action::processFile);
			});
		}

	}

}
