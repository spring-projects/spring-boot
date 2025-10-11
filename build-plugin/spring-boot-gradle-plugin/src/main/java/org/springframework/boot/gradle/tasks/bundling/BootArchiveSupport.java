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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import org.gradle.api.file.ConfigurableFilePermissions;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.java.archives.Attributes;
import org.gradle.api.java.archives.Manifest;
import org.gradle.api.provider.Property;
import org.gradle.api.specs.Spec;
import org.gradle.api.specs.Specs;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.util.PatternSet;
import org.gradle.util.GradleVersion;
import org.jspecify.annotations.Nullable;

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

	private @Nullable LaunchScriptConfiguration launchScript;

	BootArchiveSupport(String loaderMainClass, Spec<FileCopyDetails> librarySpec,
			Function<FileCopyDetails, ZipCompression> compressionResolver) {
		this.loaderMainClass = loaderMainClass;
		this.librarySpec = librarySpec;
		this.compressionResolver = compressionResolver;
		this.requiresUnpack.include(Specs.satisfyNone());
	}

	void configureManifest(Manifest manifest, String mainClass, String classes, String lib,
			@Nullable String classPathIndex, @Nullable String layersIndex, String jdkVersion,
			String implementationTitle, @Nullable Object implementationVersion) {
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

	private String determineSpringBootVersion() {
		String version = getClass().getPackage().getImplementationVersion();
		return (version != null) ? version : "unknown";
	}

	CopyAction createCopyAction(Jar jar, ResolvedDependencies resolvedDependencies, boolean supportsSignatureFile) {
		return createCopyAction(jar, resolvedDependencies, supportsSignatureFile, null, null);
	}

	CopyAction createCopyAction(Jar jar, ResolvedDependencies resolvedDependencies, boolean supportsSignatureFile,
			@Nullable LayerResolver layerResolver, @Nullable String jarmodeToolsLocation) {
		File output = jar.getArchiveFile().get().getAsFile();
		Manifest manifest = jar.getManifest();
		boolean preserveFileTimestamps = jar.isPreserveFileTimestamps();
		Integer dirPermissions = getUnixNumericDirPermissions(jar);
		Integer filePermissions = getUnixNumericFilePermissions(jar);
		boolean includeDefaultLoader = isUsingDefaultLoader(jar);
		Spec<FileTreeElement> requiresUnpack = this.requiresUnpack.getAsSpec();
		Spec<FileTreeElement> exclusions = this.exclusions.getAsExcludeSpec();
		LaunchScriptConfiguration launchScript = this.launchScript;
		Spec<FileCopyDetails> librarySpec = this.librarySpec;
		Function<FileCopyDetails, ZipCompression> compressionResolver = this.compressionResolver;
		String encoding = jar.getMetadataCharset();
		CopyAction action = new BootZipCopyAction(output, manifest, preserveFileTimestamps, dirPermissions,
				filePermissions, includeDefaultLoader, jarmodeToolsLocation, requiresUnpack, exclusions, launchScript,
				librarySpec, compressionResolver, encoding, resolvedDependencies, supportsSignatureFile, layerResolver);
		return action;
	}

	private @Nullable Integer getUnixNumericDirPermissions(CopySpec copySpec) {
		return (GradleVersion.current().compareTo(GradleVersion.version("8.3")) >= 0)
				? asUnixNumeric(copySpec.getDirPermissions()) : getDirMode(copySpec);
	}

	private @Nullable Integer getUnixNumericFilePermissions(CopySpec copySpec) {
		return (GradleVersion.current().compareTo(GradleVersion.version("8.3")) >= 0)
				? asUnixNumeric(copySpec.getFilePermissions()) : getFileMode(copySpec);
	}

	private @Nullable Integer asUnixNumeric(Property<ConfigurableFilePermissions> permissions) {
		return permissions.isPresent() ? permissions.get().toUnixNumeric() : null;
	}

	private @Nullable Integer getDirMode(CopySpec copySpec) {
		try {
			return (Integer) copySpec.getClass().getMethod("getDirMode").invoke(copySpec);
		}
		catch (Exception ex) {
			throw new RuntimeException("Failed to get dir mode from CopySpec", ex);
		}
	}

	private @Nullable Integer getFileMode(CopySpec copySpec) {
		try {
			return (Integer) copySpec.getClass().getMethod("getFileMode").invoke(copySpec);
		}
		catch (Exception ex) {
			throw new RuntimeException("Failed to get file mode from CopySpec", ex);
		}
	}

	private boolean isUsingDefaultLoader(Jar jar) {
		return DEFAULT_LAUNCHER_CLASSES.contains(jar.getManifest().getAttributes().get("Main-Class"));
	}

	@Nullable LaunchScriptConfiguration getLaunchScript() {
		return this.launchScript;
	}

	void setLaunchScript(LaunchScriptConfiguration launchScript) {
		this.launchScript = launchScript;
	}

	void requiresUnpack(String... patterns) {
		this.requiresUnpack.include(patterns);
	}

	void requiresUnpack(Spec<FileTreeElement> spec) {
		this.requiresUnpack.include(spec);
	}

	void excludeNonZipLibraryFiles(FileCopyDetails details) {
		if (this.librarySpec.isSatisfiedBy(details)) {
			excludeNonZipFiles(details);
		}
	}

	void excludeNonZipFiles(FileCopyDetails details) {
		if (!isZip(details.getFile())) {
			details.exclude();
		}
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
		for (byte headerByte : ZIP_FILE_HEADER) {
			if (inputStream.read() != headerByte) {
				return false;
			}
		}
		return true;
	}

	void moveModuleInfoToRoot(CopySpec spec) {
		spec.filesMatching("module-info.class", this::moveToRoot);
	}

	void moveToRoot(FileCopyDetails details) {
		details.setRelativePath(details.getRelativeSourcePath());
	}

}
