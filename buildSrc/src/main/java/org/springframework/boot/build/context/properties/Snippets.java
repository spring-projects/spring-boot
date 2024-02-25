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

	/**
     * Initializes the Snippets object with the given configuration property metadata.
     * 
     * @param configurationPropertyMetadata the configuration property metadata used to initialize the Snippets object
     */
    Snippets(FileCollection configurationPropertyMetadata) {
		this.properties = ConfigurationProperties.fromFiles(configurationPropertyMetadata);
	}

	/**
     * Adds a new snippet to the list of snippets.
     * 
     * @param anchor the anchor of the snippet
     * @param title the title of the snippet
     * @param config the configuration for the snippet
     */
    void add(String anchor, String title, Consumer<Snippet.Config> config) {
		this.snippets.add(new Snippet(anchor, title, config));
	}

	/**
     * Writes the snippets to the specified output directory.
     * 
     * @param outputDirectory the path to the output directory
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if any keys were not written to the documentation
     */
    void writeTo(Path outputDirectory) throws IOException {
		createDirectory(outputDirectory);
		Set<String> remaining = this.properties.stream()
			.filter((property) -> !property.isDeprecated())
			.map(ConfigurationProperty::getName)
			.collect(Collectors.toSet());
		for (Snippet snippet : this.snippets) {
			Set<String> written = writeSnippet(outputDirectory, snippet, remaining);
			remaining.removeAll(written);
		}
		if (!remaining.isEmpty()) {
			throw new IllegalStateException(
					"The following keys were not written to the documentation: " + String.join(", ", remaining));
		}
	}

	/**
     * Writes the given snippet to the specified output directory and returns a set of added properties.
     * 
     * @param outputDirectory The path of the output directory.
     * @param snippet The snippet to be written.
     * @param remaining The set of remaining properties.
     * @return The set of added properties.
     * @throws IOException If an I/O error occurs while writing the snippet.
     */
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

	/**
     * Generates an Asciidoc document for the given Snippet and Table.
     * 
     * @param snippet The Snippet object to generate the Asciidoc for.
     * @param table The Table object to include in the Asciidoc.
     * @return The generated Asciidoc document.
     */
    private Asciidoc getAsciidoc(Snippet snippet, Table table) {
		Asciidoc asciidoc = new Asciidoc();
		// We have to prepend 'appendix.' as a section id here, otherwise the
		// spring-asciidoctor-extensions:section-id asciidoctor extension complains
		asciidoc.appendln("[[appendix." + snippet.getAnchor() + "]]");
		asciidoc.appendln("== ", snippet.getTitle());
		table.write(asciidoc);
		return asciidoc;
	}

	/**
     * Writes the given Asciidoc content to the specified output directory.
     * The content is written to a file with the same name as the snippet's anchor,
     * with the extension ".adoc".
     * If the file already exists, it is deleted and replaced with the new content.
     * 
     * @param outputDirectory the directory where the Asciidoc file will be written
     * @param snippet the snippet containing the anchor and content to be written
     * @param asciidoc the Asciidoc content to be written
     * @throws IOException if an I/O error occurs while writing the file
     */
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

	/**
     * Creates a directory at the specified path.
     * 
     * @param path the path where the directory should be created
     * @throws IOException if an I/O error occurs while creating the directory
     * @throws IllegalArgumentException if the specified path is invalid
     */
    private void createDirectory(Path path) throws IOException {
		assertValidOutputDirectory(path);
		if (!Files.exists(path)) {
			Files.createDirectory(path);
		}
	}

	/**
     * Asserts that the given output directory path is valid.
     * 
     * @param path the output directory path to be validated
     * @throws IllegalArgumentException if the directory path is null or if it already exists and is not a directory
     */
    private void assertValidOutputDirectory(Path path) {
		if (path == null) {
			throw new IllegalArgumentException("Directory path should not be null");
		}
		if (Files.exists(path) && !Files.isDirectory(path)) {
			throw new IllegalArgumentException("Path already exists and is not a directory");
		}
	}

}
