/*
 * Copyright 2012-present the original author or authors.
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

	private ClassPathIndexFile(File root, List<String> lines) {
		this.root = root;
		this.lines = lines.stream().map(this::extractName).collect(Collectors.toCollection(LinkedHashSet::new));
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
		return this.lines.stream().map(this::asUrl).toList();
	}

	private URL asUrl(String line) {
		try {
			return new File(this.root, line).toURI().toURL();
		}
		catch (MalformedURLException ex) {
			throw new IllegalStateException(ex);
		}
	}

	static ClassPathIndexFile loadIfPossible(File root, String location) throws IOException {
		return loadIfPossible(root, new File(root, location));
	}

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

	private static boolean lineHasText(String line) {
		return !line.trim().isEmpty();
	}

}
