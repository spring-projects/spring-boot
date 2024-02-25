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

package org.springframework.boot.loader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A class path index file that provides ordering information for JARs.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
final class ClassPathIndexFile {

	private final File root;

	private final List<String> lines;

	/**
	 * Constructs a new ClassPathIndexFile object with the specified root directory and
	 * list of lines.
	 * @param root the root directory of the class path index file
	 * @param lines the list of lines in the class path index file
	 */
	private ClassPathIndexFile(File root, List<String> lines) {
		this.root = root;
		this.lines = lines.stream().map(this::extractName).toList();
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
	 * Loads the ClassPathIndexFile if it is possible.
	 * @param root the root URL of the file
	 * @param location the location of the file
	 * @return the loaded ClassPathIndexFile if it is possible, or null otherwise
	 * @throws IOException if an I/O error occurs while loading the file
	 */
	static ClassPathIndexFile loadIfPossible(URL root, String location) throws IOException {
		return loadIfPossible(asFile(root), location);
	}

	/**
	 * Loads the ClassPathIndexFile if it exists at the specified location.
	 * @param root the root directory where the ClassPathIndexFile is located
	 * @param location the location of the ClassPathIndexFile relative to the root
	 * directory
	 * @return the loaded ClassPathIndexFile if it exists, null otherwise
	 * @throws IOException if an I/O error occurs while loading the ClassPathIndexFile
	 */
	private static ClassPathIndexFile loadIfPossible(File root, String location) throws IOException {
		return loadIfPossible(root, new File(root, location));
	}

	/**
	 * Loads the ClassPathIndexFile if it exists and is a file.
	 * @param root The root directory of the index file.
	 * @param indexFile The index file to be loaded.
	 * @return The loaded ClassPathIndexFile, or null if the file does not exist or is not
	 * a file.
	 * @throws IOException If an I/O error occurs while loading the index file.
	 */
	private static ClassPathIndexFile loadIfPossible(File root, File indexFile) throws IOException {
		if (indexFile.exists() && indexFile.isFile()) {
			try (InputStream inputStream = new FileInputStream(indexFile)) {
				return new ClassPathIndexFile(root, loadLines(inputStream));
			}
		}
		return null;
	}

	/**
	 * Loads the lines from the given input stream.
	 * @param inputStream the input stream to read from
	 * @return a list of strings representing the lines read from the input stream
	 * @throws IOException if an I/O error occurs while reading from the input stream
	 */
	private static List<String> loadLines(InputStream inputStream) throws IOException {
		List<String> lines = new ArrayList<>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
		String line = reader.readLine();
		while (line != null) {
			if (!line.trim().isEmpty()) {
				lines.add(line);
			}
			line = reader.readLine();
		}
		return Collections.unmodifiableList(lines);
	}

	/**
	 * Converts a URL to a File object.
	 * @param url the URL to be converted
	 * @return the File object representing the URL
	 * @throws IllegalArgumentException if the URL does not reference a file
	 */
	private static File asFile(URL url) {
		if (!"file".equals(url.getProtocol())) {
			throw new IllegalArgumentException("URL does not reference a file");
		}
		try {
			return new File(url.toURI());
		}
		catch (URISyntaxException ex) {
			return new File(url.getPath());
		}
	}

}
