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

package org.springframework.boot.loader.launch;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A class path index file that provides an ordered classpath for exploded JARs.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
final class ClassPathIndexFile {

	private final File root;

	private final Set<String> lines;

	/**
	 * Constructs a new ClassPathIndexFile object with the specified root directory and
	 * list of lines.
	 * @param root the root directory of the class path index file
	 * @param lines the list of lines in the class path index file
	 */
	private ClassPathIndexFile(File root, List<String> lines) {
		this.root = root;
		this.lines = lines.stream().map(this::extractName).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Extracts the name from a line in the classpath index file.
	 * @param line the line to extract the name from
	 * @return the extracted name
	 * @throws IllegalStateException if the line is malformed
	 */
	private String extractName(String line) {
		if (line.startsWith("- \"") && line.endsWith("\"")) {
			return line.substring(3, line.length() - 1);
		}
		throw new IllegalStateException("Malformed classpath index line [" + line + "]");
	}

	/**
	 * Returns the size of the lines list.
	 * @return the size of the lines list
	 */
	int size() {
		return this.lines.size();
	}

	/**
	 * Checks if the given name is contained in the lines of the ClassPathIndexFile.
	 * @param name the name to be checked
	 * @return true if the name is contained in the lines, false otherwise
	 */
	boolean containsEntry(String name) {
		if (name == null || name.isEmpty()) {
			return false;
		}
		return this.lines.contains(name);
	}

	/**
	 * Retrieves a list of URLs from the lines of the ClassPathIndexFile.
	 * @return a list of URLs extracted from the lines
	 */
	List<URL> getUrls() {
		return this.lines.stream().map(this::asUrl).toList();
	}

	/**
	 * Converts a given line into a URL object.
	 * @param line the line to be converted into a URL
	 * @return the URL object representing the given line
	 * @throws IllegalStateException if the URL is malformed
	 */
	private URL asUrl(String line) {
		try {
			return new File(this.root, line).toURI().toURL();
		}
		catch (MalformedURLException ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * Loads the ClassPathIndexFile if it exists at the specified location.
	 * @param root the root directory where the ClassPathIndexFile is located
	 * @param location the location of the ClassPathIndexFile relative to the root
	 * directory
	 * @return the loaded ClassPathIndexFile if it exists, null otherwise
	 * @throws IOException if an I/O error occurs while loading the ClassPathIndexFile
	 */
	static ClassPathIndexFile loadIfPossible(File root, String location) throws IOException {
		return loadIfPossible(root, new File(root, location));
	}

	/**
	 * Loads the ClassPathIndexFile if it exists and is a file.
	 * @param root The root directory of the ClassPathIndexFile.
	 * @param indexFile The index file to be loaded.
	 * @return The loaded ClassPathIndexFile if it exists and is a file, otherwise null.
	 * @throws IOException If an I/O error occurs while reading the index file.
	 */
	private static ClassPathIndexFile loadIfPossible(File root, File indexFile) throws IOException {
		if (indexFile.exists() && indexFile.isFile()) {
			List<String> lines = Files.readAllLines(indexFile.toPath())
				.stream()
				.filter(ClassPathIndexFile::lineHasText)
				.toList();
			return new ClassPathIndexFile(root, lines);
		}
		return null;
	}

	/**
	 * Checks if a given line of text has any content.
	 * @param line the line of text to be checked
	 * @return true if the line has content, false otherwise
	 */
	private static boolean lineHasText(String line) {
		return !line.trim().isEmpty();
	}

}
