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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.file.RelativePath;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.internal.file.copy.CopyActionProcessingStream;
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal;
import org.gradle.api.java.archives.Attributes;
import org.gradle.api.java.archives.Manifest;
import org.gradle.api.specs.Spec;
import org.gradle.api.specs.Specs;
import org.gradle.api.tasks.WorkResult;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.util.PatternSet;

/**
 * Support class for implementations of {@link BootArchive}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @see BootJar
 * @see BootWar
 */
class BootArchiveSupport {

	private static final byte[] ZIP_FILE_HEADER = new byte[] { 'P', 'K', 3, 4 };

	private static final Set<String> DEFAULT_LAUNCHER_CLASSES;

	static {
		Set<String> defaultLauncherClasses = new HashSet<>();
		defaultLauncherClasses.add("org.springframework.boot.loader.JarLauncher");
		defaultLauncherClasses.add("org.springframework.boot.loader.PropertiesLauncher");
		defaultLauncherClasses.add("org.springframework.boot.loader.WarLauncher");
		DEFAULT_LAUNCHER_CLASSES = Collections.unmodifiableSet(defaultLauncherClasses);
	}

	private final PatternSet requiresUnpack = new PatternSet();

	private final PatternSet exclusions = new PatternSet();

	private final String loaderMainClass;

	private final Spec<FileCopyDetails> librarySpec;

	private final Function<FileCopyDetails, ZipCompression> compressionResolver;

	private LaunchScriptConfiguration launchScript;

	private boolean excludeDevtools = true;

	BootArchiveSupport(String loaderMainClass, Spec<FileCopyDetails> librarySpec,
			Function<FileCopyDetails, ZipCompression> compressionResolver) {
		this.loaderMainClass = loaderMainClass;
		this.librarySpec = librarySpec;
		this.compressionResolver = compressionResolver;
		this.requiresUnpack.include(Specs.satisfyNone());
		configureExclusions();
	}

	void configureManifest(Manifest manifest, String mainClass, String classes, String lib, String classPathIndex,
			String layersIndex) {
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
	}

	private String determineSpringBootVersion() {
		String version = getClass().getPackage().getImplementationVersion();
		return (version != null) ? version : "unknown";
	}

	CopyAction createCopyAction(Jar jar) {
		return createCopyAction(jar, null, null);
	}

	CopyAction createCopyAction(Jar jar, LayerResolver layerResolver, String layerToolsLocation) {
		File output = jar.getArchiveFile().get().getAsFile();
		Manifest manifest = jar.getManifest();
		boolean preserveFileTimestamps = jar.isPreserveFileTimestamps();
		boolean includeDefaultLoader = isUsingDefaultLoader(jar);
		Spec<FileTreeElement> requiresUnpack = this.requiresUnpack.getAsSpec();
		Spec<FileTreeElement> exclusions = this.exclusions.getAsExcludeSpec();
		LaunchScriptConfiguration launchScript = this.launchScript;
		Spec<FileCopyDetails> librarySpec = this.librarySpec;
		Function<FileCopyDetails, ZipCompression> compressionResolver = this.compressionResolver;
		String encoding = jar.getMetadataCharset();
		CopyAction action = new BootZipCopyAction(output, manifest, preserveFileTimestamps, includeDefaultLoader,
				layerToolsLocation, requiresUnpack, exclusions, launchScript, librarySpec, compressionResolver,
				encoding, layerResolver);
		return jar.isReproducibleFileOrder() ? new ReproducibleOrderingCopyAction(action) : action;
	}

	private boolean isUsingDefaultLoader(Jar jar) {
		return DEFAULT_LAUNCHER_CLASSES.contains(jar.getManifest().getAttributes().get("Main-Class"));
	}

	LaunchScriptConfiguration getLaunchScript() {
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

	boolean isExcludeDevtools() {
		return this.excludeDevtools;
	}

	void setExcludeDevtools(boolean excludeDevtools) {
		this.excludeDevtools = excludeDevtools;
		configureExclusions();
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

	private void configureExclusions() {
		Set<String> excludes = new HashSet<>();
		if (this.excludeDevtools) {
			excludes.add("**/spring-boot-devtools-*.jar");
		}
		this.exclusions.setExcludes(excludes);
	}

	void moveModuleInfoToRoot(CopySpec spec) {
		spec.filesMatching("module-info.class", BootArchiveSupport::moveToRoot);
	}

	private static void moveToRoot(FileCopyDetails details) {
		details.setRelativePath(details.getRelativeSourcePath());
	}

	/**
	 * {@link CopyAction} variant that sorts entries to ensure reproducible ordering.
	 */
	private static final class ReproducibleOrderingCopyAction implements CopyAction {

		private final CopyAction delegate;

		private ReproducibleOrderingCopyAction(CopyAction delegate) {
			this.delegate = delegate;
		}

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
