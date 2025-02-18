/*
 * Copyright 2012-2025 the original author or authors.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import org.springframework.boot.jarmode.tools.JarStructure.Entry.Type;
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
			"Spring-Boot-Lib", "Spring-Boot-Classpath-Index", "Spring-Boot-Layers-Index");

	private static final Set<String> ENTRY_IGNORE_LIST = Set.of("META-INF/", "META-INF/MANIFEST.MF",
			"META-INF/services/java.nio.file.spi.FileSystemProvider");

	private final Manifest originalManifest;

	private final String libLocation;

	private final String classesLocation;

	private final List<String> classpathEntries;

	IndexedJarStructure(Manifest originalManifest, String indexFile) {
		this.originalManifest = originalManifest;
		this.libLocation = getLocation(originalManifest, "Spring-Boot-Lib");
		this.classesLocation = getLocation(originalManifest, "Spring-Boot-Classes");
		this.classpathEntries = readIndexFile(indexFile);
	}

	private static String getLocation(Manifest manifest, String attribute) {
		String location = getMandatoryAttribute(manifest, attribute);
		return (!location.endsWith("/")) ? location + "/" : location;
	}

	private static List<String> readIndexFile(String indexFile) {
		String[] lines = Arrays.stream(indexFile.split("\n"))
			.map((line) -> line.replace("\r", ""))
			.filter(StringUtils::hasText)
			.toArray(String[]::new);
		List<String> classpathEntries = new ArrayList<>();
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
	public Entry resolve(String name) {
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
		return null;
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
		Assert.state(libEntryName.startsWith(this.libLocation), () -> "Invalid library location " + libEntryName);
		return libEntryName.substring(this.libLocation.length());
	}

	private static String getMandatoryAttribute(Manifest manifest, String attribute) {
		String value = manifest.getMainAttributes().getValue(attribute);
		Assert.state(value != null, () -> "Manifest attribute '" + attribute + "' is mandatory");
		return value;
	}

	static IndexedJarStructure get(File file) {
		try {
			try (JarFile jarFile = new JarFile(file)) {
				Manifest manifest = jarFile.getManifest();
				String location = getMandatoryAttribute(manifest, "Spring-Boot-Classpath-Index");
				ZipEntry entry = jarFile.getEntry(location);
				if (entry != null) {
					String indexFile = StreamUtils.copyToString(jarFile.getInputStream(entry), StandardCharsets.UTF_8);
					return new IndexedJarStructure(manifest, indexFile);
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
