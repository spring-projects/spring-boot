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

package org.springframework.boot.jarmode.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.jarmode.tools.JarStructure.Entry.Type;
import org.springframework.boot.loader.jarmode.JarModeErrorException;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * {@link JarStructure} implementation backed by a {@code classpath.idx} file.
 *
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 */
class IndexedJarStructure implements JarStructure {

	private static final List<String> MANIFEST_DENY_LIST = List.of("Start-Class", "Spring-Boot-Classes",
			"Spring-Boot-Lib", "Spring-Boot-Lib-Provided", "Spring-Boot-Classpath-Index", "Spring-Boot-Layers-Index");

	private static final Set<String> ENTRY_IGNORE_LIST = Set.of("META-INF/", "META-INF/MANIFEST.MF",
			"META-INF/services/java.nio.file.spi.FileSystemProvider");

	private static final String LOADER_LOCATION = "org/springframework/boot/loader/";

	private static final String WEB_RESOURCES_LOCATION = "META-INF/resources/";

	private final Manifest originalManifest;

	private final String libLocation;

	private final @Nullable String providedLibLocation;

	private final String classesLocation;

	private final Set<String> classpathEntries;

	private final Set<String> indexLocations;

	private final ArchiveType archiveType;

	IndexedJarStructure(Manifest originalManifest, String indexFile, ArchiveType archiveType) {
		this.originalManifest = originalManifest;
		this.libLocation = getLocation(originalManifest, "Spring-Boot-Lib");
		this.providedLibLocation = getOptionalLocation(originalManifest, "Spring-Boot-Lib-Provided");
		this.classesLocation = getLocation(originalManifest, "Spring-Boot-Classes");
		this.classpathEntries = readIndexFile(indexFile);
		this.indexLocations = getIndexLocations(originalManifest);
		this.archiveType = archiveType;
		checkForDuplicateLibraryNames();
	}

	private void checkForDuplicateLibraryNames() {
		Map<String, String> libraries = new HashMap<>();
		for (String classpathEntry : this.classpathEntries) {
			String name = stripLibraryLocation(classpathEntry);
			if (name == null) {
				continue;
			}
			String existing = libraries.putIfAbsent(name, classpathEntry);
			if (existing != null) {
				throw new JarModeErrorException(
						"Library name '%s' is used by both '%s' and '%s'".formatted(name, existing, classpathEntry));
			}
		}
	}

	private static Set<String> getIndexLocations(Manifest manifest) {
		Attributes attributes = manifest.getMainAttributes();
		return Stream
			.of(attributes.getValue("Spring-Boot-Classpath-Index"), attributes.getValue("Spring-Boot-Layers-Index"))
			.filter(StringUtils::hasLength)
			.collect(Collectors.toSet());
	}

	private static String getLocation(Manifest manifest, String attribute) {
		String location = getMandatoryAttribute(manifest, attribute);
		return (!location.endsWith("/")) ? location + "/" : location;
	}

	private static @Nullable String getOptionalLocation(Manifest manifest, String attribute) {
		String location = manifest.getMainAttributes().getValue(attribute);
		if (!StringUtils.hasLength(location)) {
			return null;
		}
		return (!location.endsWith("/")) ? location + "/" : location;
	}

	private static Set<String> readIndexFile(String indexFile) {
		String[] lines = Arrays.stream(indexFile.split("\n"))
			.map((line) -> line.replace("\r", ""))
			.filter(StringUtils::hasText)
			.toArray(String[]::new);
		Set<String> classpathEntries = new LinkedHashSet<>();
		for (String line : lines) {
			Assert.state(line.startsWith("- "), "Classpath index file is malformed");
			classpathEntries.add(line.substring(3, line.length() - 1));
		}
		Assert.state(!classpathEntries.isEmpty(), "Empty classpath index file loaded");
		return classpathEntries;
	}

	@Override
	public String getClassesLocation() {
		return this.classesLocation;
	}

	@Override
	public @Nullable Entry resolve(String name) {
		if (ENTRY_IGNORE_LIST.contains(name)) {
			return null;
		}
		if (this.classpathEntries.contains(name)) {
			return new Entry(name, toStructureDependency(name), Type.LIBRARY);
		}
		if (name.startsWith(this.classesLocation)) {
			return new Entry(name, name.substring(this.classesLocation.length()), Type.APPLICATION_CLASS_OR_RESOURCE);
		}
		if (name.startsWith("org/springframework/boot/loader")) {
			return new Entry(name, name, Type.LOADER);
		}
		if (name.startsWith("META-INF/")) {
			return new Entry(name, name, Type.META_INF);
		}
		return resolveWebResource(name);
	}

	private @Nullable Entry resolveWebResource(String name) {
		if (this.archiveType != ArchiveType.WAR || isWarPackagingEntry(name)) {
			return null;
		}
		return new Entry(name, WEB_RESOURCES_LOCATION + name, Type.WEB_RESOURCE);
	}

	private boolean isWarPackagingEntry(String name) {
		if (this.indexLocations.contains(name) || name.startsWith(this.libLocation)) {
			return true;
		}
		if (this.providedLibLocation != null && name.startsWith(this.providedLibLocation)) {
			return true;
		}
		return name.endsWith("/") && LOADER_LOCATION.startsWith(name);
	}

	@Override
	public Manifest createLauncherManifest(UnaryOperator<String> libraryTransformer) {
		Manifest manifest = new Manifest(this.originalManifest);
		Attributes attributes = manifest.getMainAttributes();
		for (String denied : MANIFEST_DENY_LIST) {
			attributes.remove(new Name(denied));
		}
		attributes.put(Name.MAIN_CLASS, getMandatoryAttribute(this.originalManifest, "Start-Class"));
		attributes.put(Name.CLASS_PATH,
				this.classpathEntries.stream()
					.map(this::toStructureDependency)
					.map(libraryTransformer)
					.collect(Collectors.joining(" ")));
		return manifest;
	}

	private String toStructureDependency(String libEntryName) {
		String name = stripLibraryLocation(libEntryName);
		Assert.state(name != null, () -> "Invalid library location " + libEntryName);
		return name;
	}

	private @Nullable String stripLibraryLocation(String libEntryName) {
		if (libEntryName.startsWith(this.libLocation)) {
			return libEntryName.substring(this.libLocation.length());
		}
		if (this.providedLibLocation != null && libEntryName.startsWith(this.providedLibLocation)) {
			return libEntryName.substring(this.providedLibLocation.length());
		}
		return null;
	}

	private static String getMandatoryAttribute(Manifest manifest, String attribute) {
		String value = manifest.getMainAttributes().getValue(attribute);
		Assert.state(value != null, () -> "Manifest attribute '" + attribute + "' is mandatory");
		return value;
	}

	static @Nullable IndexedJarStructure get(File file) {
		try {
			try (JarFile jarFile = new JarFile(file)) {
				Manifest manifest = jarFile.getManifest();
				String location = getMandatoryAttribute(manifest, "Spring-Boot-Classpath-Index");
				ZipEntry entry = jarFile.getEntry(location);
				if (entry != null) {
					String indexFile = StreamUtils.copyToString(jarFile.getInputStream(entry), StandardCharsets.UTF_8);
					return new IndexedJarStructure(manifest, indexFile, ArchiveType.of(file));
				}
			}
			return null;
		}
		catch (FileNotFoundException | NoSuchFileException ex) {
			return null;
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
