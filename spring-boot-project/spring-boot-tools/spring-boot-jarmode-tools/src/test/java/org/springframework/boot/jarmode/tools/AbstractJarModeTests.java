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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.junit.jupiter.api.io.TempDir;

import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for jar mode tests.
 *
 * @author Moritz Halbritter
 */
abstract class AbstractJarModeTests {

	@TempDir
	File tempDir;

	Manifest createManifest(String... entries) {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
		for (String entry : entries) {
			int colon = entry.indexOf(':');
			Assert.state(colon > -1, () -> "Colon not found in %s".formatted(entry));
			String key = entry.substring(0, colon).trim();
			String value = entry.substring(colon + 1).trim();
			manifest.getMainAttributes().putValue(key, value);
		}
		return manifest;
	}

	File createArchive(String... entries) throws IOException {
		return createArchive(createManifest(), entries);
	}

	File createArchive(Manifest manifest, String... entries) throws IOException {
		return createArchive(manifest, null, null, null, entries);
	}

	File createArchive(Manifest manifest, Instant creationTime, Instant lastModifiedTime, Instant lastAccessTime,
			String... entries) throws IOException {
		Assert.state(entries.length % 2 == 0, "Entries must be key value pairs");
		File file = new File(this.tempDir, "test.jar");
		try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(file), manifest)) {
			for (int i = 0; i < entries.length; i += 2) {
				ZipEntry entry = new ZipEntry(entries[i]);
				if (creationTime != null) {
					entry.setCreationTime(FileTime.from(creationTime));
				}
				if (lastModifiedTime != null) {
					entry.setLastModifiedTime(FileTime.from(lastModifiedTime));
				}
				if (lastAccessTime != null) {
					entry.setLastAccessTime(FileTime.from(lastAccessTime));
				}
				jar.putNextEntry(entry);
				String resource = entries[i + 1];
				if (resource != null) {
					try (InputStream content = ListLayersCommandTests.class.getResourceAsStream(resource)) {
						assertThat(content).as("Resource " + resource).isNotNull();
						StreamUtils.copy(content, jar);
					}
				}
				jar.closeEntry();
			}
		}
		return file;
	}

	TestPrintStream runCommand(CommandFactory<?> commandFactory, File archive, String... arguments) {
		Context context = new Context(archive, this.tempDir);
		Command command = commandFactory.create(context);
		TestPrintStream out = new TestPrintStream(this);
		command.run(out, new ArrayDeque<>(Arrays.asList(arguments)));
		return out;
	}

	Manifest getJarManifest(File jar) throws IOException {
		try (JarFile jarFile = new JarFile(jar)) {
			return jarFile.getManifest();
		}
	}

	Map<String, String> getJarManifestAttributes(File jar) throws IOException {
		assertThat(jar).exists();
		Manifest manifest = getJarManifest(jar);
		Map<String, String> result = new HashMap<>();
		manifest.getMainAttributes().forEach((key, value) -> result.put(key.toString(), value.toString()));
		return result;
	}

	List<String> getJarEntryNames(File jar) throws IOException {
		assertThat(jar).exists();
		try (JarFile jarFile = new JarFile(jar)) {
			return jarFile.stream().map(ZipEntry::getName).toList();
		}
	}

	List<String> listFilenames() throws IOException {
		return listFilenames(this.tempDir);
	}

	List<String> listFilenames(File directory) throws IOException {
		try (Stream<Path> stream = Files.walk(directory.toPath())) {
			int substring = directory.getAbsolutePath().length() + 1;
			return stream.map((file) -> file.toAbsolutePath().toString())
				.map((file) -> (file.length() >= substring) ? file.substring(substring) : "")
				.filter(StringUtils::hasLength)
				.map((file) -> file.replace(File.separatorChar, '/'))
				.toList();
		}
	}

	interface CommandFactory<T extends Command> {

		T create(Context context);

	}

}
