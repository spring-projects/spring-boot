/*
 * Copyright 2021-2021 the original author or authors.
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

package org.springframework.boot.build.context.properties;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.gradle.api.file.FileCollection;

/**
 * Configuration properties snippets.
 *
 * @author Brian Clozed
 * @author Phillip Webb
 */
class Snippets {

	private final ConfigurationProperties properties;

	private final List<Snippet> snippets = new ArrayList<>();

	Snippets(FileCollection configurationPropertyMetadata) {
		this.properties = ConfigurationProperties.fromFiles(configurationPropertyMetadata);
	}

	void add(String anchor, String title, Consumer<Snippet.Config> config) {
		this.snippets.add(new Snippet(anchor, title, config));
	}

	void writeTo(Path outputDirectory) throws IOException {
		createDirectory(outputDirectory);
		Set<String> remaining = this.properties.stream().filter((property) -> !property.isDeprecated())
				.map(ConfigurationProperty::getName).collect(Collectors.toSet());
		for (Snippet snippet : this.snippets) {
			Set<String> written = writeSnippet(outputDirectory, snippet, remaining);
			remaining.removeAll(written);
		}
		if (!remaining.isEmpty()) {
			throw new IllegalStateException(
					"The following keys were not written to the documentation: " + String.join(", ", remaining));
		}
	}

	private Set<String> writeSnippet(Path outputDirectory, Snippet snippet, Set<String> remaining) throws IOException {
		Table table = new Table();
		Set<String> added = new HashSet<>();
		snippet.forEachOverride((prefix, description) -> {
			CompoundRow row = new CompoundRow(snippet, prefix, description);
			remaining.stream().filter((candidate) -> candidate.startsWith(prefix)).forEach((name) -> {
				if (added.add(name)) {
					row.addProperty(this.properties.get(name));
				}
			});
			table.addRow(row);
		});
		snippet.forEachPrefix((prefix) -> {
			remaining.stream().filter((candidate) -> candidate.startsWith(prefix)).forEach((name) -> {
				if (added.add(name)) {
					table.addRow(new SingleRow(snippet, this.properties.get(name)));
				}
			});
		});
		Asciidoc asciidoc = getAsciidoc(snippet, table);
		writeAsciidoc(outputDirectory, snippet, asciidoc);
		return added;
	}

	private Asciidoc getAsciidoc(Snippet snippet, Table table) {
		Asciidoc asciidoc = new Asciidoc();
		asciidoc.appendln("[[" + snippet.getAnchor() + "]]");
		asciidoc.appendln("== ", snippet.getTitle());
		table.write(asciidoc);
		return asciidoc;
	}

	private void writeAsciidoc(Path outputDirectory, Snippet snippet, Asciidoc asciidoc) throws IOException {
		String[] parts = (snippet.getAnchor()).split("\\.");
		Path path = outputDirectory;
		for (int i = 0; i < parts.length; i++) {
			String name = (i < parts.length - 1) ? parts[i] : parts[i] + ".adoc";
			path = path.resolve(name);
		}
		createDirectory(path.getParent());
		Files.deleteIfExists(path);
		try (OutputStream outputStream = Files.newOutputStream(path)) {
			outputStream.write(asciidoc.toString().getBytes(StandardCharsets.UTF_8));
		}
	}

	private void createDirectory(Path path) throws IOException {
		assertValidOutputDirectory(path);
		if (!Files.exists(path)) {
			Files.createDirectory(path);
		}
	}

	private void assertValidOutputDirectory(Path path) {
		if (path == null) {
			throw new IllegalArgumentException("Directory path should not be null");
		}
		if (Files.exists(path) && !Files.isDirectory(path)) {
			throw new IllegalArgumentException("Path already exists and is not a directory");
		}
	}

}
