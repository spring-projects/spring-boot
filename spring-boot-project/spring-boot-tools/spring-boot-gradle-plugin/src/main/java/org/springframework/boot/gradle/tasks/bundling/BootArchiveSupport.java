/*
 * Copyright 2012-2019 the original author or authors.
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

import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.file.RelativePath;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.internal.file.copy.CopyActionProcessingStream;
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal;
import org.gradle.api.java.archives.Attributes;
import org.gradle.api.specs.Spec;
import org.gradle.api.specs.Specs;
import org.gradle.api.tasks.WorkResult;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.util.PatternSet;

/**
 * Support class for implementations of {@link BootArchive}.
 *
 * @author Andy Wilkinson
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

	private final Function<FileCopyDetails, ZipCompression> compressionResolver;

	private final PatternSet exclusions = new PatternSet();

	private final String loaderMainClass;

	private LaunchScriptConfiguration launchScript;

	private boolean excludeDevtools = true;

	BootArchiveSupport(String loaderMainClass, Function<FileCopyDetails, ZipCompression> compressionResolver) {
		this.loaderMainClass = loaderMainClass;
		this.compressionResolver = compressionResolver;
		this.requiresUnpack.include(Specs.satisfyNone());
		configureExclusions();
	}

	void configureManifest(Jar jar, String mainClassName, String springBootClasses, String springBootLib) {
		Attributes attributes = jar.getManifest().getAttributes();
		attributes.putIfAbsent("Main-Class", this.loaderMainClass);
		attributes.putIfAbsent("Start-Class", mainClassName);
		attributes.computeIfAbsent("Spring-Boot-Version", (key) -> determineSpringBootVersion());
		attributes.putIfAbsent("Spring-Boot-Classes", springBootClasses);
		attributes.putIfAbsent("Spring-Boot-Lib", springBootLib);
	}

	private String determineSpringBootVersion() {
		String implementationVersion = getClass().getPackage().getImplementationVersion();
		return (implementationVersion != null) ? implementationVersion : "unknown";
	}

	CopyAction createCopyAction(Jar jar) {
		CopyAction copyAction = new BootZipCopyAction(jar.getArchivePath(), jar.isPreserveFileTimestamps(),
				isUsingDefaultLoader(jar), this.requiresUnpack.getAsSpec(), this.exclusions.getAsExcludeSpec(),
				this.launchScript, this.compressionResolver, jar.getMetadataCharset());
		if (!jar.isReproducibleFileOrder()) {
			return copyAction;
		}
		return new ReproducibleOrderingCopyAction(copyAction);
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

	boolean isZip(File file) {
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
		for (int i = 0; i < ZIP_FILE_HEADER.length; i++) {
			if (inputStream.read() != ZIP_FILE_HEADER[i]) {
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
