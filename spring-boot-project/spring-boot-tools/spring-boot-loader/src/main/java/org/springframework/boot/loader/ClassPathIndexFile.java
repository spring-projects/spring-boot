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
import java.util.stream.Collectors;

/**
 * A class path index file that provides ordering information for JARs.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
final class ClassPathIndexFile {

	private final File root;

	private final List<String> lines;

	private ClassPathIndexFile(File root, List<String> lines) {
		this.root = root;
		this.lines = lines.stream().map(this::extractName).collect(Collectors.toList());
	}

	private String extractName(String line) {
		if (line.startsWith("- \"") && line.endsWith("\"")) {
			return line.substring(3, line.length() - 1);
		}
		throw new IllegalStateException("Malformed classpath index line [" + line + "]");
	}

	int size() {
		return this.lines.size();
	}

	boolean containsEntry(String name) {
		if (name == null || name.isEmpty()) {
			return false;
		}
		return this.lines.contains(name);
	}

	List<URL> getUrls() {
		return Collections.unmodifiableList(this.lines.stream().map(this::asUrl).collect(Collectors.toList()));
	}

	private URL asUrl(String line) {
		try {
			return new File(this.root, line).toURI().toURL();
		}
		catch (MalformedURLException ex) {
			throw new IllegalStateException(ex);
		}
	}

	static ClassPathIndexFile loadIfPossible(URL root, String location) throws IOException {
		return loadIfPossible(asFile(root), location);
	}

	private static ClassPathIndexFile loadIfPossible(File root, String location) throws IOException {
		return loadIfPossible(root, new File(root, location));
	}

	private static ClassPathIndexFile loadIfPossible(File root, File indexFile) throws IOException {
		if (indexFile.exists() && indexFile.isFile()) {
			try (InputStream inputStream = new FileInputStream(indexFile)) {
				return new ClassPathIndexFile(root, loadLines(inputStream));
			}
		}
		return null;
	}

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
